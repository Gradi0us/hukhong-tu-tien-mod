package com.hukhong.events;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.entity.BossLevel;
import com.hukhong.entity.HuAnh;
import com.hukhong.entity.HuGiap;
import com.hukhong.entity.HuHoa;
import com.hukhong.entity.HuMob;
import com.hukhong.entity.ModEntities;
import com.hukhong.entity.V3Entities;
import com.hukhong.item.ModItems;
import com.hukhong.world.SafeZones;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Hu Khong Dot Kich": ban dem, moi 5 phut ngoai khu an toan, moi nguoi choi tung xuc xac suat (30% co ban,
 * 10% rung/rung ram, 50% trong chunk nhiem Hu Hoa). Co canh bao 10s, 3 dot quai quanh nguoi choi, dot cuoi co
 * mini-boss, bossbar tien do, thuong EXP + Hon Tinh (+ tay chunk nhiem neu xay ra trong vung nhiem).
 * Quai trong vung nhiem tu duoc +30% cap/suc manh qua {@link CorruptBuff} khi finalizeSpawn.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class VoidRaid {
    private VoidRaid() {}

    public static final int ROLL_EVERY = 6000;            // 5 phut
    public static final int WARN_TICKS = 200;
    public static final int WAVES = 3;
    public static final int WAVE_TIMEOUT = 3600;
    public static final int TOTAL_TIMEOUT = 14000;
    public static final int MAX_ALIVE = 28;
    private static final int[] BASE_SIZE = {6, 8, 10};
    public static final double P_BASE = 0.30, P_FOREST = 0.10, P_CORRUPT = 0.50;

    /** Thuan: xac suat theo vung. Vung nhiem uu tien cao nhat. */
    public static double chance(boolean corrupted, boolean jungleOrForest) {
        return corrupted ? P_CORRUPT : jungleOrForest ? P_FOREST : P_BASE;
    }

    /** Thuan: dayTime trong khoang dem (13000..23000). */
    public static boolean isNight(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= 13000 && t <= 23000;
    }

    private static final class Raid {
        final UUID player;
        final ServerLevel level;
        final boolean corrupted;
        final ServerBossEvent bar = new ServerBossEvent(Component.translatable("hukhong.raid.warn_bar"),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6);
        final List<Mob> mobs = new ArrayList<>();
        int stage;                    // 0 = canh bao, 1..WAVES = dot
        int timer = WARN_TICKS, age, toSpawn, waveTotal, tries;
        int players = 1;
        Mob boss;
        final List<com.hukhong.siege.Rift> rifts = new ArrayList<>();
        int riftDelay;                // tick con lai cho den khi khe nut san sang cho quai tro ra

        Raid(ServerPlayer p, boolean corrupted) {
            this.player = p.getUUID();
            this.level = (ServerLevel) p.level();
            this.corrupted = corrupted;
        }
    }

    private static final Map<UUID, Raid> RAIDS = new LinkedHashMap<>();

    public static boolean active(UUID player) { return RAIDS.containsKey(player); }
    public static int activeCount() { return RAIDS.size(); }
    public static void clear() {
        for (Raid r : RAIDS.values()) { r.bar.removeAllPlayers(); closeRifts(r); com.hukhong.siege.SiegeDirector.sectDefenseEnd(r.player); }
        RAIDS.clear();
    }

    private static void closeRifts(Raid r) {
        for (com.hukhong.siege.Rift f : r.rifts) f.close();
        r.rifts.clear();
    }

    // ------------------------------------------------------------------ bat dau
    /** Bat dau dot kich cho {@code p} (bo qua xac suat / dem). @return false neu da co dot kich hoac khong o Overworld. */
    public static boolean start(ServerPlayer p) {
        if (RAIDS.containsKey(p.getUUID()) || p.level().dimension() != Level.OVERWORLD) return false;
        ServerLevel ow = (ServerLevel) p.level();
        Raid r = new Raid(p, CorruptionData.get(ow).isCorrupted(p.blockPosition()));
        r.players = playersNear(ow, p);
        r.bar.addPlayer(p);
        com.hukhong.siege.SiegeDirector.raidSting(p);
        var sect = com.hukhong.siege.SiegeDirector.sectNear(ow, p.position(), 48);
        if (sect != null) com.hukhong.siege.SiegeDirector.sectDefense(ow, sect, p.getUUID());
        RAIDS.put(p.getUUID(), r);
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("hukhong.raid.title")));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("hukhong.raid.subtitle")));
        p.sendSystemMessage(Component.translatable("hukhong.raid.warn"));
        return true;
    }

    private static int playersNear(ServerLevel ow, ServerPlayer p) {
        int n = 0;
        for (ServerPlayer q : ow.players()) if (!q.isSpectator() && q.distanceToSqr(p) < 48 * 48) n++;
        return BossScaling.clamp(n);
    }

    // ------------------------------------------------------------------ vong doi
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        net.minecraft.server.level.ServerLevel ow = e.getServer().overworld();
        if (ow == null) return;
        long t = ow.getGameTime();
        if (t % ROLL_EVERY == 0 && isNight(ow.getDayTime()) && !ow.players().isEmpty()) roll(ow);
        if (RAIDS.isEmpty() || t % 5 != 0) return;
        for (Iterator<Raid> it = RAIDS.values().iterator(); it.hasNext(); ) {
            Raid r = it.next();
            if (step(r)) {
                r.bar.removeAllPlayers();
                closeRifts(r);
                com.hukhong.siege.SiegeDirector.sectDefenseEnd(r.player);
                it.remove();
            }
        }
    }

    private static void roll(ServerLevel ow) {
        CorruptionData cd = CorruptionData.get(ow);
        for (ServerPlayer p : new ArrayList<>(ow.players())) {
            if (p.isSpectator() || !p.isAlive() || RAIDS.containsKey(p.getUUID())) continue;
            boolean cor = cd.isCorrupted(p.blockPosition());
            boolean jf = ow.getBiome(p.blockPosition()).is(BiomeTags.IS_JUNGLE) || ow.getBiome(p.blockPosition()).is(BiomeTags.IS_FOREST);
            if (ow.random.nextDouble() >= chance(cor, jf)) continue;
            if (SafeZones.isSafe(ow, p.blockPosition())) continue;
            start(p);
        }
    }

    /** @return true neu dot kich ket thuc (thang / thua). */
    private static boolean step(Raid r) {
        ServerPlayer p = r.level.getServer().getPlayerList().getPlayer(r.player);
        if (p == null || !p.isAlive() || p.level() != r.level) return fail(r, p, "hukhong.raid.fail_left");
        r.age += 5;
        if (r.age > TOTAL_TIMEOUT) return fail(r, p, "hukhong.raid.fail_time");
        if (r.stage == 0) {
            r.timer -= 5;
            r.bar.setProgress(Math.max(0f, r.timer / (float) WARN_TICKS));
            if (r.timer <= 0) nextWave(r, p);
            return false;
        }
        r.mobs.removeIf(m -> m.isRemoved() || !m.isAlive());
        if (r.riftDelay > 0) r.riftDelay -= 5;
        if (r.toSpawn > 0 && r.riftDelay <= 0) spawnSome(r, p);
        if (r.toSpawn <= 0 && !r.rifts.isEmpty()) closeRifts(r);   // da ra het quai: khe nut khep lai
        int alive = r.mobs.size();
        int dead = r.waveTotal - r.toSpawn - alive;
        r.bar.setProgress(r.waveTotal <= 0 ? 0f : Math.max(0f, Math.min(1f, (dead + 0f) / r.waveTotal)));
        r.timer -= 5;
        if (r.toSpawn <= 0 && alive == 0) {
            if (r.stage >= WAVES) return win(r, p);
            nextWave(r, p);
        } else if (r.timer <= 0) {
            return fail(r, p, "hukhong.raid.fail_time");
        }
        return false;
    }

    private static void nextWave(Raid r, ServerPlayer p) {
        r.stage++;
        closeRifts(r);
        openRaidRifts(r, p);
        r.timer = WAVE_TIMEOUT;
        r.tries = 0;
        r.waveTotal = r.toSpawn = BossScaling.waveSize(BASE_SIZE[r.stage - 1], r.players) + (r.stage >= WAVES ? 1 : 0);
        r.bar.setName(Component.translatable("hukhong.raid.bar", r.stage, WAVES));
        r.bar.setColor(r.stage >= WAVES ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED);
        r.bar.setProgress(0f);
        p.sendSystemMessage(Component.translatable(r.stage >= WAVES ? "hukhong.raid.final" : "hukhong.raid.wave", r.stage, WAVES));
    }

    /** Mo 2 (dot cuoi: 3) khe nut Hu Khong quanh nguoi choi; quai chi tro ra sau khi khe san sang. */
    private static void openRaidRifts(Raid r, ServerPlayer p) {
        ServerLevel sl = r.level;
        int n = r.stage >= WAVES ? 3 : 2;
        double base = sl.random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < n; i++) {
            double a = base + i * Math.PI * 2 / n + (sl.random.nextDouble() - 0.5) * 0.5, d = 15 + sl.random.nextDouble() * 6;
            int x = (int) (p.getX() + Math.cos(a) * d), z = (int) (p.getZ() + Math.sin(a) * d);
            if (!sl.hasChunk(x >> 4, z >> 4)) continue;
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos bp = new BlockPos(x, y, z);
            if (!sl.getFluidState(bp.below()).isEmpty() || Math.abs(y - p.getY()) > 12) continue;
            r.rifts.add(com.hukhong.siege.SiegeDirector.openRift(sl, bp, p.position().subtract(x, 0, z), 1.5 + 0.15 * r.stage));
        }
        r.riftDelay = r.rifts.isEmpty() ? 0 : com.hukhong.siege.Rift.READY_TICKS + 20;
    }

    private static EntityType<? extends Mob> pick(int stage, net.minecraft.util.RandomSource rnd) {
        int w = rnd.nextInt(100);
        if (stage == 1) return w < 40 ? ModEntities.HU_THI.get() : w < 70 ? V3Entities.HU_TRUNG.get() : w < 90 ? ModEntities.HU_COT.get() : ModEntities.HU_CHU.get();
        if (stage == 2) return w < 25 ? ModEntities.HU_THI.get() : w < 40 ? ModEntities.HU_COT.get() : w < 55 ? ModEntities.HU_CHU.get()
                : w < 75 ? V3Entities.HU_GIAP.get() : w < 90 ? V3Entities.HU_ANH.get() : V3Entities.HU_TRUNG.get();
        return w < 20 ? V3Entities.HU_GIAP.get() : w < 40 ? V3Entities.HU_ANH.get() : w < 60 ? V3Entities.HU_TRIEU_SU.get()
                : w < 75 ? ModEntities.HU_THI.get() : w < 90 ? ModEntities.HU_COT.get() : ModEntities.HU_CHU.get();
    }

    private static void spawnSome(Raid r, ServerPlayer p) {
        ServerLevel sl = r.level;
        int spawned = 0;
        while (r.toSpawn > 0 && spawned < 2 && r.mobs.size() < MAX_ALIVE && r.tries < 80) {
            r.tries++;
            double a = sl.random.nextDouble() * Math.PI * 2, d = 14 + sl.random.nextDouble() * 8;
            int x = (int) (p.getX() + Math.cos(a) * d), z = (int) (p.getZ() + Math.sin(a) * d);
            if (!r.rifts.isEmpty()) { // quai tro ra tu chan khe nut
                net.minecraft.world.phys.Vec3 sp = r.rifts.get(sl.random.nextInt(r.rifts.size())).spawnPoint();
                x = (int) Math.floor(sp.x);
                z = (int) Math.floor(sp.z);
            }
            if (!sl.hasChunk(x >> 4, z >> 4)) continue;
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos bp = new BlockPos(x, y, z);
            if (!sl.getFluidState(bp.below()).isEmpty() || Math.abs(y - p.getY()) > 24) continue;
            boolean boss = r.stage >= WAVES && r.toSpawn == 1 && r.boss == null;
            EntityType<? extends Mob> type = boss ? V3Entities.HU_GIAP.get() : pick(r.stage, sl.random);
            Mob m = type.create(sl);
            if (m == null) continue;
            m.moveTo(x + 0.5, y, z + 0.5, sl.random.nextFloat() * 360f, 0f);
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(bp), MobSpawnType.EVENT, null);
            if (boss) makeMiniBoss(m);
            m.setTarget(p);
            sl.addFreshEntity(m);
            if (boss) r.boss = m;
            r.mobs.add(m);
            if (!r.rifts.isEmpty()) {
                if (boss) com.hukhong.siege.SiegeDirector.bossEntrance(sl, m.position());
                else com.hukhong.siege.SiegeDirector.emerge(sl, m.position());
            }
            r.toSpawn--;
            spawned++;
        }
        if (r.tries >= 80 && r.toSpawn > 0) r.toSpawn = 0;  // khong tim duoc cho dat: bo phan con lai, khong ket vong
    }

    /** Mini-boss: Hu Giap cuong hoa (HP x6, sat thuong x1.8), ten + phat sang + nhac boss. */
    public static void makeMiniBoss(Mob m) {
        int lv = m instanceof HuMob h ? h.huLevel() + 4 : 10;
        HuHoa.applyStats(m, lv, 6.0, 1.8);
        m.setCustomName(BossLevel.tag(Component.translatable("hukhong.raid.boss"), lv));
        m.setCustomNameVisible(true);
        m.setGlowingTag(true);
        m.setPersistenceRequired();
        com.hukhong.audio.BossMusic.watch(m, "tam_ma");
    }

    /** Chi dung cho GameTest: sinh truc tiep mot dot (khong vong doi) quanh {@code p}, tra danh sach quai (dot cuoi co mini-boss). */
    public static List<Mob> debugWave(ServerPlayer p, int stage) {
        Raid r = new Raid(p, false);
        r.stage = stage;
        r.players = 1;
        r.waveTotal = r.toSpawn = BASE_SIZE[stage - 1] + (stage >= WAVES ? 1 : 0);
        for (int i = 0; i < 40 && r.toSpawn > 0; i++) spawnSome(r, p);
        return r.mobs;
    }

    private static boolean win(Raid r, ServerPlayer p) {
        ServerLevel sl = r.level;
        int lvl = HuHoa.levelFor(sl, p.blockPosition(), sl.random);
        for (ServerPlayer q : sl.players()) {
            if (q.isSpectator() || q.distanceToSqr(p) > 64 * 64) continue;
            long exp = HuHoa.exp(12.0, lvl, CultivationService.get(q).level());
            if (exp > 0) CultivationService.addExp(q, exp);
            sl.addFreshEntity(new ItemEntity(sl, q.getX(), q.getY() + 0.5, q.getZ(), new ItemStack(ModItems.HON_TINH.get(), 5 + sl.random.nextInt(6))));
            q.sendSystemMessage(Component.translatable("hukhong.raid.win", exp));
        }
        if (r.corrupted) CorruptionData.get(sl).cleanseNear(p.blockPosition().getX(), p.blockPosition().getZ(), 2);
        return true;
    }

    private static boolean fail(Raid r, ServerPlayer p, String key) {
        for (Mob m : r.mobs) if (m.isAlive()) { m.discard(); }
        if (p != null) p.sendSystemMessage(Component.translatable(key));
        return true;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        Raid r = RAIDS.get(e.getEntity().getUUID());
        if (r != null) r.bar.removePlayer((ServerPlayer) e.getEntity());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) { clear(); }
}
