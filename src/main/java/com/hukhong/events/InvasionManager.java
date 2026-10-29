package com.hukhong.events;

import com.hukhong.HuKhong;
import com.hukhong.city.CityBuilder;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import com.hukhong.city.CitySavedData;
import com.hukhong.entity.ModEntities;
import com.hukhong.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Tập kích thử: 3 đợt quái Hư Hóa quanh tường, bảo vệ Trận Nhãn (Ấn Hồn Đài).
 * Ngân sách: tổng 36 quái (cap sống ≤ 40), tối đa 2 spawn / 5 tick, tối đa 60 lần thử vị trí, timeout 10 phút.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class InvasionManager {
    private InvasionManager() {}

    private static final int[] WAVE_SIZE = {8, 12, 16};
    private static final int MAX_ALIVE = 40;
    private static final int TIMEOUT_TICKS = 12000;
    private static final int LOSE_TICKS = 400;   // quái ≥2 con gần đài liên tục ~20s
    private static final int NEAR_ALTAR_SQ = 7 * 7;
    private static final int MAX_SPAWN_TRIES = 60;

    private static final class Invasion {
        final ServerLevel level;
        final BlockPos center;
        final BlockPos altar;
        final CityDef city;
        final ServerBossEvent bar = new ServerBossEvent(Component.translatable("hukhong.invade.bossbar", 1, WAVE_SIZE.length),
                BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        final List<Mob> mobs = new ArrayList<>();
        final int players;
        final int[] sizes = new int[WAVE_SIZE.length];
        int wave = 1, toSpawn, killedTotal, plannedTotal;
        int age, lossTicks, waveDelay, tries;

        Invasion(ServerLevel l, CityDef cd, BlockPos c) {
            level = l; city = cd; center = c; altar = c.above();
            int n = 0;
            for (ServerPlayer p : l.players())
                if (p.distanceToSqr(altar.getX(), altar.getY(), altar.getZ()) < 160 * 160) n++;
            players = BossScaling.clamp(n);
            for (int i = 0; i < sizes.length; i++) { sizes[i] = BossScaling.waveSize(WAVE_SIZE[i], players); plannedTotal += sizes[i]; }
            toSpawn = sizes[0];
        }
    }

    private static Invasion cur;

    // ---- cảnh báo trước khi quân Hư Hóa tới ----
    public static final int WARN_TICKS = 1200;
    private static int warnLeft;
    private static ServerLevel warnLevel;
    private static final ServerBossEvent WARN_BAR = new ServerBossEvent(Component.translatable("hukhong.invade.warn_bar", 60),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    public static boolean warning() { return warnLeft > 0; }

    private static int warnCity;

    /** Chỉ số thành đang bị tập kích (hoặc đang báo trước), -1 nếu không có. */
    public static int targetCity() { return cur != null ? cur.city.index() : warnLeft > 0 ? warnCity : -1; }

    /** Thành hợp lệ: chỉ số đã cho, hoặc (idx &lt; 0) chọn theo {@link InvasionTargets}. @return null nếu OK (và đặt ra), hoặc key lỗi. */
    private static String resolve(ServerLevel ow, int idx, int[] out) {
        if (idx < 0) idx = InvasionTargets.choose(ow);
        if (idx < 0) return CitySavedData.get(ow).isBuilt() ? "hukhong.invade.err_ruin" : "hukhong.city.not_built";
        CityDef cd = CityDef.ALL.get(idx);
        if (CityService.builtCenter(ow, cd) == null) return "hukhong.city.not_built";
        if (CityStates.isRuin(ow, cd)) return "hukhong.invade.err_ruin";
        out[0] = idx;
        return null;
    }

    /** Báo trước {@code ticks} tick rồi tự khởi động tập kích (thành chọn theo chương / ngẫu nhiên). @return null nếu OK, hoặc key lỗi. */
    public static String warn(ServerLevel ow, int ticks) { return warn(ow, ticks, -1); }

    public static String warn(ServerLevel ow, int ticks, int cityIdx) {
        if (cur != null || warnLeft > 0) return "hukhong.invade.err_active";
        int[] t = new int[1];
        String err = resolve(ow, cityIdx, t);
        if (err != null) return err;
        warnCity = t[0];
        warnLeft = Math.max(1, ticks);
        warnLevel = ow;
        title(ow, Component.translatable("hukhong.invade.warn_title"), Component.translatable("hukhong.invade.warn_sub", Math.max(1, ticks / 20)));
        return null;
    }

    private static void tickWarn() {
        if (warnLeft <= 0) return;
        ServerLevel ow = warnLevel;
        warnLeft--;
        if (warnLeft % 20 == 0 || warnLeft == 0) {
            WARN_BAR.setName(Component.translatable("hukhong.invade.warn_bar", warnLeft / 20));
            WARN_BAR.setProgress(Math.max(0f, Math.min(1f, warnLeft / (float) WARN_TICKS)));
            for (ServerPlayer p : ow.players()) WARN_BAR.addPlayer(p);
        }
        if (warnLeft == 0) {
            WARN_BAR.removeAllPlayers();
            warnLevel = null;
            start(ow, warnCity);
        }
    }

    public static boolean active() { return cur != null; }

    /** @return null nếu OK, hoặc translation key lỗi. Thành chọn theo chương / ngẫu nhiên. */
    public static String start(ServerLevel ow) { return start(ow, -1); }

    public static String start(ServerLevel ow, int cityIdx) {
        if (cur != null) return "hukhong.invade.err_active";
        int[] t = new int[1];
        String err = resolve(ow, cityIdx, t);
        if (err != null) return err;
        CityDef cd = CityDef.ALL.get(t[0]);
        BlockPos c = CityService.builtCenter(ow, cd);
        cur = new Invasion(ow, cd, c);
        com.hukhong.siege.SiegeDirector.begin(ow, c);
        cur.waveDelay = com.hukhong.siege.SiegeDirector.ALARM_TICKS + com.hukhong.siege.SiegeDirector.RIFT_OPEN_TICKS;
        title(ow, Component.translatable("hukhong.invade.title"), Component.translatable("hukhong.invade.subtitle"));
        if (cd.index() != 0) for (ServerPlayer p : ow.players())
            p.sendSystemMessage(Component.translatable("hukhong.invade.target", Component.translatable(cd.nameKey())));
        return null;
    }

    public static void cancel() { if (cur != null) end(null); }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        if (cur != null) { cur.bar.removeAllPlayers(); cur = null; }
        com.hukhong.siege.SiegeDirector.shutdown();
        warnLeft = 0; warnLevel = null; WARN_BAR.removeAllPlayers();
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        tickWarn();
        Invasion inv = cur;
        if (inv == null) return;
        inv.age++;
        if (inv.level.players().isEmpty() || inv.age > TIMEOUT_TICKS) { end(Component.translatable("hukhong.invade.timeout")); return; }

        if (inv.toSpawn > 0 && inv.waveDelay <= 0 && inv.age % 5 == 0) spawnBatch(inv);
        if (inv.waveDelay > 0) inv.waveDelay--;
        if (inv.age % 20 != 0) return;

        // dọn xác + cập nhật thanh
        int before = inv.mobs.size();
        inv.mobs.removeIf(m -> m.isRemoved() || !m.isAlive());
        inv.killedTotal += before - inv.mobs.size();
        inv.bar.setName(Component.translatable("hukhong.invade.bossbar", inv.wave, WAVE_SIZE.length));
        inv.bar.setProgress(Math.min(1f, (float) inv.killedTotal / inv.plannedTotal));
        for (ServerPlayer p : inv.level.players()) {
            if (p.distanceToSqr(inv.altar.getX(), inv.altar.getY(), inv.altar.getZ()) < 160 * 160) inv.bar.addPlayer(p);
            else inv.bar.removePlayer(p);
        }

        // quái chưa có mục tiêu → tiến về Trận Nhãn; đếm quái sát đài
        int near = 0;
        for (Mob m : inv.mobs) {
            if (m.getTarget() == null) m.getNavigation().moveTo(inv.altar.getX() + 0.5, inv.altar.getY(), inv.altar.getZ() + 0.5, 1.0);
            if (m.distanceToSqr(inv.altar.getX() + 0.5, inv.altar.getY(), inv.altar.getZ() + 0.5) < NEAR_ALTAR_SQ) near++;
        }
        inv.lossTicks = near >= 2 ? inv.lossTicks + 20 : Math.max(0, inv.lossTicks - 20);
        if (inv.lossTicks >= LOSE_TICKS) { lose(inv); return; }

        // hết đợt?
        if (inv.toSpawn == 0 && inv.mobs.isEmpty() && inv.waveDelay <= 0) {
            if (inv.wave >= WAVE_SIZE.length) { win(inv); return; }
            inv.wave++;
            inv.toSpawn = inv.sizes[inv.wave - 1];
            inv.waveDelay = 100;
            com.hukhong.siege.SiegeDirector.waveStart(inv.wave);
            broadcast(inv, Component.translatable("hukhong.invade.wave", inv.wave, WAVE_SIZE.length));
        }
    }

    private static void spawnBatch(Invasion inv) {
        int spawned = 0;
        for (int attempt = 0; attempt < 4 && spawned < 2 && inv.toSpawn > 0; attempt++) {
            if (inv.mobs.size() >= MAX_ALIVE) return;
            if (++inv.tries > MAX_SPAWN_TRIES + inv.killedTotal + inv.plannedTotal) { // hết ngân sách thử: bỏ phần còn lại
                inv.plannedTotal -= inv.toSpawn;
                inv.toSpawn = 0;
                return;
            }
            int x, z;
            boolean viaRift = com.hukhong.siege.SiegeDirector.active();
            if (viaRift) { // quai tro ra tu khe nut Hu Khong ngoai tuong; chua co khe san sang thi cho
                net.minecraft.world.phys.Vec3 rp = com.hukhong.siege.SiegeDirector.riftSpawnPoint();
                if (rp == null) return;
                x = (int) Math.floor(rp.x);
                z = (int) Math.floor(rp.z);
            } else {
                double ang = inv.level.random.nextDouble() * Math.PI * 2;
                double r = 26 + inv.level.random.nextDouble() * 8;
                x = inv.center.getX() + (int) (Math.cos(ang) * r);
                z = inv.center.getZ() + (int) (Math.sin(ang) * r);
            }
            if (!inv.level.hasChunk(x >> 4, z >> 4)) continue;
            int y = inv.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos bp = new BlockPos(x, y, z);
            if (!inv.level.getFluidState(bp.below()).isEmpty()) continue;
            Mob m = switch (inv.level.random.nextInt(3)) {
                case 0 -> ModEntities.HU_THI.get().create(inv.level);
                case 1 -> ModEntities.HU_COT.get().create(inv.level);
                default -> ModEntities.HU_CHU.get().create(inv.level);
            };
            if (m == null) continue;
            m.moveTo(x + 0.5, y, z + 0.5, inv.level.random.nextFloat() * 360f, 0f);
            m.finalizeSpawn(inv.level, inv.level.getCurrentDifficultyAt(bp), MobSpawnType.EVENT, null);
            m.setPersistenceRequired();
            var hpAttr = m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (hpAttr != null && inv.players > 1) {
                hpAttr.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "invasion_hp"),
                        BossScaling.mobHpMul(inv.players) - 1, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                m.setHealth(m.getMaxHealth());
            }
            inv.level.addFreshEntityWithPassengers(m);
            inv.mobs.add(m);
            if (viaRift) com.hukhong.siege.SiegeDirector.emerge(inv.level, new net.minecraft.world.phys.Vec3(x + 0.5, y, z + 0.5));
            inv.toSpawn--;
            spawned++;
        }
    }

    private static void win(Invasion inv) {
        CitySavedData cd = CitySavedData.get(inv.level);
        if (inv.city.index() == 0) cd.addWin(); // bo dem cua buoc truyen c5_invasion chi tinh thanh goc
        com.hukhong.story.world.HuAnh.addNear(inv.level, inv.center, -10);
        CorruptionData.get(inv.level).cleanseNear(inv.center.getX(), inv.center.getZ(), BossScaling.cleanseCount(cd.invasionsWon()));
        for (ServerPlayer p : inv.level.players()) {
            if (p.distanceToSqr(inv.altar.getX(), inv.altar.getY(), inv.altar.getZ()) > 160 * 160) continue;
            com.hukhong.currency.Wallet.grantHonTinh(p, 5);
        }
        com.hukhong.siege.SiegeDirector.victory();
        title(inv.level, Component.translatable("hukhong.invade.win"), Component.translatable("hukhong.invade.win_sub"));
        end(null);
    }

    private static void lose(Invasion inv) {
        CityStates.setRuin(inv.level, inv.city, true);
        com.hukhong.story.world.HuAnh.addNear(inv.level, inv.center, 20);
        CityBuilder.ruin(inv.level, inv.center);
        title(inv.level, Component.translatable("hukhong.invade.lose"), Component.translatable("hukhong.invade.lose_sub"));
        end(null);
    }

    private static void end(Component msg) {
        Invasion inv = cur;
        cur = null;
        if (inv == null) return;
        inv.bar.removeAllPlayers();
        inv.bar.setVisible(false);
        if (!com.hukhong.siege.SiegeDirector.inVictory()) com.hukhong.siege.SiegeDirector.abort();
        for (Mob m : inv.mobs) if (!m.isRemoved()) m.discard();
        if (msg != null) inv.level.getServer().getPlayerList().broadcastSystemMessage(msg, false);
    }

    private static void broadcast(Invasion inv, Component c) {
        for (ServerPlayer p : inv.level.players()) p.displayClientMessage(c, true);
    }

    private static void title(ServerLevel l, Component t, Component sub) {
        for (ServerPlayer p : l.players()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
            p.connection.send(new ClientboundSetTitleTextPacket(t));
        }
    }
}
