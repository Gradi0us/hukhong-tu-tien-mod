package com.hukhong.dongphu;

import com.hukhong.auction.Tx;
import com.hukhong.core.CultivationService;
import com.hukhong.currency.SpiritStoneToll;
import com.hukhong.dongphu.DongPhuData.Claim;
import com.hukhong.entity.ModEntities;
import com.hukhong.entity.V3Entities;
import com.hukhong.events.BossScaling;
import com.hukhong.events.RaidLevel;
import com.hukhong.events.VoidRaid;
import com.hukhong.treasure.TreasureFx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Công phá Động Phủ (Hư Không): 3 đợt quái mọc ở mép vùng bảo hộ, đợt cuối có mini-boss. Sống sót = thưởng
 * (EXP, Linh Thạch, rương báu theo tầng). Chủ nhân chết / thoát / sang chiều khác = thất bại, tiến độ tầng giữ nguyên.
 * Dùng lại: VoidRaid.makeMiniBoss, RaidLevel.apply/raidLevel, BossScaling.waveSize, TreasureFx.place.
 */
public final class DongPhuRaid {
    private DongPhuRaid() {}

    static final int WAVES = 3, WARN_TICKS = 200, WAVE_TIMEOUT = 2400, TOTAL_TIMEOUT = 3 * WAVE_TIMEOUT + 1200, MAX_ALIVE = 24;
    static final int[] BASE = {5, 7, 9};

    static final class Raid {
        final UUID owner;
        final ServerLevel lv;
        final BlockPos core;
        final int tier;
        final float radius;
        final ServerBossEvent bar = new ServerBossEvent(Tx.t("hukhong.dongphu.raid.bar0", "Công phá Động Phủ", "Dong Phu Assault"),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6);
        final List<Mob> mobs = new ArrayList<>();
        int stage, timer = WARN_TICKS, age, toSpawn, waveTotal, tries, players = 1;

        Raid(UUID owner, ServerLevel lv, BlockPos core, int tier, float radius) {
            this.owner = owner; this.lv = lv; this.core = core; this.tier = tier; this.radius = radius;
        }
    }

    private static final Map<UUID, Raid> RAIDS = new LinkedHashMap<>();

    public static boolean active(UUID owner) { return RAIDS.containsKey(owner); }

    public static void clear() {
        for (Raid r : RAIDS.values()) { r.bar.removeAllPlayers(); for (Mob m : r.mobs) m.discard(); }
        RAIDS.clear();
    }

    /** Bắt đầu công phá. Gọi từ DongPhuService.tick (chủ nhân đang trong vùng). */
    public static void start(ServerPlayer p, ServerLevel lv, Claim c) {
        Vec3 core = Vec3.atBottomCenterOf(c.pos());
        float r = DongPhuService.radius(c.tier());
        Raid raid = new Raid(p.getUUID(), lv, c.pos(), c.tier(), r);
        raid.bar.addPlayer(p);
        RAIDS.put(p.getUUID(), raid);
        DongPhuFx.redSky(p, lv, core, r);
        if (p.connection != null) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Tx.t("hukhong.dongphu.raid.sub", "Giữ vững Động Phủ trong ba đợt!", "Hold the Dong Phu through three waves!")));
            p.connection.send(new ClientboundSetTitleTextPacket(Tx.t("hukhong.dongphu.raid.title", "HƯ KHÔNG CÔNG PHÁ", "THE VOID ASSAULTS")));
        }
        p.sendSystemMessage(Tx.t("hukhong.dongphu.raid.warn", "Hư Không công phá Động Phủ! Còn 10 giây để chuẩn bị.",
                "The Void assaults your Dong Phu! 10 seconds to prepare."));
    }

    /** Mỗi 5 tick. */
    public static void tick(MinecraftServer s) {
        if (RAIDS.isEmpty()) return;
        for (Iterator<Raid> it = RAIDS.values().iterator(); it.hasNext(); ) {
            Raid r = it.next();
            if (step(r, s)) {
                r.bar.removeAllPlayers();
                it.remove();
            }
        }
    }

    /** @return true khi công phá kết thúc (thắng/thua). */
    private static boolean step(Raid r, MinecraftServer s) {
        ServerPlayer p = s.getPlayerList().getPlayer(r.owner);
        if (p == null || !p.isAlive() || p.level() != r.lv) return fail(r, p, "hukhong.dongphu.raid.fail_left",
                "Chủ Động Phủ đã rời đi: công phá thất bại. Tiến độ tầng vẫn được giữ.", "The owner left: assault failed. Tier progress is kept.");
        r.age += 5;
        if (r.age > TOTAL_TIMEOUT) return fail(r, p, "hukhong.dongphu.raid.fail_time",
                "Hết thời gian: công phá thất bại.", "Out of time: assault failed.");
        if (r.stage == 0) {
            r.timer -= 5;
            r.bar.setProgress(Math.max(0f, r.timer / (float) WARN_TICKS));
            if (r.timer <= 0) nextWave(r, p);
            return false;
        }
        r.mobs.removeIf(m -> m.isRemoved() || !m.isAlive());
        if (r.toSpawn > 0) spawnSome(r, p);
        int alive = r.mobs.size();
        int dead = r.waveTotal - r.toSpawn - alive;
        r.bar.setProgress(r.waveTotal <= 0 ? 0f : Math.max(0f, Math.min(1f, dead / (float) r.waveTotal)));
        r.timer -= 5;
        if (r.toSpawn <= 0 && alive == 0) {
            if (r.stage >= WAVES) return win(r, p);
            nextWave(r, p);
            return false;
        }
        if (r.timer <= 0) return fail(r, p, "hukhong.dongphu.raid.fail_time", "Hết thời gian: công phá thất bại.", "Out of time: assault failed.");
        return false;
    }

    private static void nextWave(Raid r, ServerPlayer p) {
        r.stage++;
        r.timer = WAVE_TIMEOUT;
        r.tries = 0;
        r.players = Math.max(1, (int) r.lv.players().stream().filter(q -> !q.isSpectator() && q.distanceToSqr(Vec3.atBottomCenterOf(r.core)) < (double) r.radius * r.radius).count());
        r.waveTotal = r.toSpawn = BossScaling.waveSize(BASE[r.stage - 1], r.players) + (r.stage >= WAVES ? 1 : 0);
        r.bar.setName(Tx.t("hukhong.dongphu.raid.bar", "Công phá Động Phủ · Đợt %s/%s", "Dong Phu Assault · Wave %s/%s", r.stage, WAVES));
        r.bar.setColor(r.stage >= WAVES ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED);
        r.bar.setProgress(0f);
        p.sendSystemMessage(r.stage >= WAVES
                ? Tx.t("hukhong.dongphu.raid.final", "Đợt cuối: Hư Không Thống Lĩnh xuất hiện!", "Final wave: the Void Warlord emerges!")
                : Tx.t("hukhong.dongphu.raid.wave", "Đợt %s/%s ập đến từ mép vùng bảo hộ.", "Wave %s/%s breaks in from the edge of the ward.", r.stage, WAVES));
    }

    static EntityType<? extends Mob> pick(int stage, net.minecraft.util.RandomSource rnd) {
        int w = rnd.nextInt(100);
        if (stage == 1) return w < 40 ? ModEntities.HU_THI.get() : w < 70 ? V3Entities.HU_TRUNG.get() : w < 90 ? ModEntities.HU_COT.get() : ModEntities.HU_CHU.get();
        if (stage == 2) return w < 25 ? ModEntities.HU_THI.get() : w < 40 ? ModEntities.HU_COT.get() : w < 55 ? ModEntities.HU_CHU.get()
                : w < 75 ? V3Entities.HU_GIAP.get() : w < 90 ? V3Entities.HU_ANH.get() : V3Entities.HU_TRUNG.get();
        return w < 20 ? V3Entities.HU_GIAP.get() : w < 40 ? V3Entities.HU_ANH.get() : w < 60 ? V3Entities.HU_TRIEU_SU.get()
                : w < 75 ? ModEntities.HU_THI.get() : w < 90 ? ModEntities.HU_COT.get() : ModEntities.HU_CHU.get();
    }

    /** Sinh quái ở mép vùng (bán kính - 2 ± 1.5), tối đa 2 con mỗi nhịp. MobSpawnType.EVENT nên không bị Hộ Trận chặn. */
    private static void spawnSome(Raid r, ServerPlayer p) {
        ServerLevel sl = r.lv;
        Vec3 c = Vec3.atBottomCenterOf(r.core);
        int spawned = 0;
        while (r.toSpawn > 0 && spawned < 2 && r.mobs.size() < MAX_ALIVE && r.tries < 80) {
            r.tries++;
            double a = sl.random.nextDouble() * Math.PI * 2, d = r.radius - 2 + (sl.random.nextDouble() - 0.5) * 3;
            int x = (int) Math.floor(c.x + Math.cos(a) * d), z = (int) Math.floor(c.z + Math.sin(a) * d);
            if (!sl.hasChunk(x >> 4, z >> 4)) continue;
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos bp = new BlockPos(x, y, z);
            if (!sl.getFluidState(bp.below()).isEmpty() || Math.abs(y - c.y) > 16) continue;
            boolean boss = r.stage >= WAVES && r.toSpawn == 1;
            EntityType<? extends Mob> type = boss ? V3Entities.HU_GIAP.get() : pick(r.stage, sl.random);
            Mob m = type.create(sl);
            if (m == null) continue;
            m.moveTo(x + 0.5, y, z + 0.5, sl.random.nextFloat() * 360f, 0f);
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(bp), MobSpawnType.EVENT, null);
            RaidLevel.apply(m, RaidLevel.raidLevel(sl, c, p, false), false);
            if (boss) VoidRaid.makeMiniBoss(m);
            m.setTarget(p);
            sl.addFreshEntity(m);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, x + 0.5, y + 0.5, z + 0.5, 25, 0.6, 0.8, 0.6, 0.3);
            r.mobs.add(m);
            r.toSpawn--;
            spawned++;
        }
        if (r.tries >= 80 && r.toSpawn > 0) r.toSpawn = 0; // không tìm được chỗ đặt: bỏ phần còn lại, không kẹt vòng
    }

    private static boolean win(Raid r, ServerPlayer p) {
        int tier = r.tier;
        ServerLevel lv = r.lv;
        if (p != null) {
            CultivationService.addExp(p, 200L * tier);
            ItemStack stones = new ItemStack(SpiritStoneToll.stone(), 8 + 6 * tier);
            if (!p.getInventory().add(stones)) p.drop(stones, false);
            p.sendSystemMessage(Tx.t("hukhong.dongphu.raid.win", "Động Phủ đứng vững! Thưởng: %s EXP và Linh Thạch.", "The Dong Phu stands! Reward: %s EXP and spirit stones.", 200 * tier));
        }
        placeReward(lv, r.core, tier);
        lv.playSound(null, r.core, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.2f, 1f);
        lv.sendParticles(DongPhuFx.JADE_DUST, r.core.getX() + 0.5, r.core.getY() + 1.5, r.core.getZ() + 0.5, 80, 1.5, 1.0, 1.5, 0.1);
        return true;
    }

    /** Rương báu cấp (tầng - 1, tối đa 3) cạnh lõi, nạp Linh Thạch. */
    private static void placeReward(ServerLevel lv, BlockPos core, int tier) {
        BlockPos[] cand = {core.offset(2, 0, 0), core.offset(-2, 0, 0), core.offset(0, 0, 2), core.offset(0, 0, -2)};
        for (BlockPos bp : cand) {
            if (!lv.getBlockState(bp).isAir()) continue;
            Container c = TreasureFx.place(lv, bp, Math.min(3, tier - 1), Direction.NORTH);
            if (c != null) { c.setItem(0, new ItemStack(SpiritStoneToll.stone(), 16 + 8 * tier)); c.setItem(1, new ItemStack(Items.AMETHYST_SHARD, 4 * tier)); }
            return;
        }
    }

    private static boolean fail(Raid r, ServerPlayer p, String key, String vi, String en) {
        for (Mob m : r.mobs) if (m.isAlive()) m.discard();
        r.mobs.clear();
        if (p != null) p.sendSystemMessage(Tx.t(key, vi, en));
        return true;
    }
}
