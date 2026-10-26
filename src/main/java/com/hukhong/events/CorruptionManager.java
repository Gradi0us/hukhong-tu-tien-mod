package com.hukhong.events;

import com.hukhong.HuKhong;
import com.hukhong.entity.HuMob;
import com.hukhong.entity.HuVuong;
import com.hukhong.entity.ModEntities;
import com.hukhong.vfx.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Hu Hoa lan theo chunk, Tam Hu Hoa sinh boss the gioi, lich tap kich thanh, world border 15000 (R24). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CorruptionManager {
    private CorruptionManager() {}

    public static final int BORDER_RADIUS = 15000;
    private static final int HEARTS = 3;
    private static final int SPREAD_EVERY = 600;       // 30s / lan 1 chunk
    private static final int INVASION_MIN = 72000;     // 3 ngay game
    private static final int INVASION_VAR = 48000;
    private static final int BOSS_COOLDOWN = 24000;
    private static final double BOSS_CHANCE = 0.04;    // moi lan kiem tra (5s) khi nguoi choi toi gan Tam
    private static final Map<BlockPos, Long> bossCooldown = new HashMap<>();

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) {
        ServerLevel ow = e.getServer().getLevel(Level.OVERWORLD);
        if (ow == null) return;
        WorldBorder wb = ow.getWorldBorder();
        if (wb.getSize() > 2.0 * BORDER_RADIUS + 1) {   // chua dat (mac dinh ~6e7)
            BlockPos sp = ow.getSharedSpawnPos();
            wb.setCenter(sp.getX() + 0.5, sp.getZ() + 0.5);
            wb.setSize(2.0 * BORDER_RADIUS);
        }
        CorruptionData d = CorruptionData.get(ow);
        if (!d.seeded()) {
            RandomSource r = RandomSource.create(ow.getSeed() ^ 0x48554B48L);
            BlockPos sp = ow.getSharedSpawnPos();
            for (int i = 0; i < HEARTS; i++) {
                double ang = (i + r.nextDouble() * 0.6) * Math.PI * 2 / HEARTS;
                double dist = 4000 + r.nextDouble() * 7000;
                d.addHeart(new BlockPos(sp.getX() + (int) (Math.cos(ang) * dist), 0, sp.getZ() + (int) (Math.sin(ang) * dist)));
            }
            d.markSeeded();
        }
    }

    @SubscribeEvent
    public static void onStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e) { bossCooldown.clear(); }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        ServerLevel ow = e.getServer().getLevel(Level.OVERWORLD);
        if (ow == null || ow.players().isEmpty()) return;
        long t = ow.getGameTime();
        CorruptionData d = CorruptionData.get(ow);

        if (t % SPREAD_EVERY == 0) spread(ow, d);
        if (t % 40 == 0) effects(ow, d, t);
        if (t % 100 == 0) checkHearts(ow, d, t);
        if (t % 20 == 0) schedule(ow, d, t);
    }

    // ---------------------------------------------------------------- lan
    private static void spread(ServerLevel ow, CorruptionData d) {
        if (d.size() == 0 || d.size() >= CorruptionData.MAX_CHUNKS) return;
        List<Long> all = new ArrayList<>(d.chunks());
        long p = all.get(ow.random.nextInt(all.size()));
        int cx = (int) p, cz = (int) (p >> 32);
        int[][] dir = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int[] dd = dir[ow.random.nextInt(4)];
        int nx = cx + dd[0], nz = cz + dd[1];
        BlockPos c = ow.getSharedSpawnPos();
        double lim = BORDER_RADIUS / 16.0 - 2, dx = nx - (c.getX() >> 4), dz = nz - (c.getZ() >> 4);
        if (Math.abs(dx) > lim || Math.abs(dz) > lim) return;
        d.add(nx, nz);
    }

    /** Hat + quai bonus quanh nguoi choi trong vung nhiem. */
    private static void effects(ServerLevel ow, CorruptionData d, long t) {
        for (ServerPlayer p : ow.players()) {
            if (!d.isCorrupted(p.blockPosition())) continue;
            ow.sendParticles(ModParticles.HU_KHONG.get(), p.getX(), p.getY() + 1, p.getZ(), 12, 6, 2, 6, 0.01);
            if (t % 200 != 0) continue;
            int near = ow.getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(40), m -> m instanceof HuMob).size();
            if (near >= 6) continue;
            double a = ow.random.nextDouble() * Math.PI * 2, r = 16 + ow.random.nextDouble() * 10;
            int x = (int) (p.getX() + Math.cos(a) * r), z = (int) (p.getZ() + Math.sin(a) * r);
            if (!ow.hasChunk(x >> 4, z >> 4) || !d.isCorrupted(x >> 4, z >> 4)) continue;
            int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos bp = new BlockPos(x, y, z);
            if (!ow.getFluidState(bp.below()).isEmpty()) continue;
            Mob m = switch (ow.random.nextInt(3)) {
                case 0 -> ModEntities.HU_THI.get().create(ow);
                case 1 -> ModEntities.HU_COT.get().create(ow);
                default -> ModEntities.HU_CHU.get().create(ow);
            };
            if (m == null) continue;
            m.moveTo(x + 0.5, y, z + 0.5, ow.random.nextFloat() * 360f, 0f);
            m.finalizeSpawn(ow, ow.getCurrentDifficultyAt(bp), MobSpawnType.EVENT, null);
            ow.addFreshEntity(m);
        }
    }

    // ---------------------------------------------------------------- boss
    private static void checkHearts(ServerLevel ow, CorruptionData d, long t) {
        for (BlockPos h : new ArrayList<>(d.hearts())) {
            ServerPlayer near = null;
            for (ServerPlayer p : ow.players()) {
                double dx = p.getX() - h.getX(), dz = p.getZ() - h.getZ();
                if (dx * dx + dz * dz < 48 * 48) { near = p; break; }
            }
            if (near == null) continue;
            if (ow.random.nextInt(8) == 0)
                ow.sendParticles(ModParticles.HU_KHONG.get(), h.getX() + 0.5, near.getY() + 1, h.getZ() + 0.5, 30, 1.5, 3, 1.5, 0.05);
            if (bossCooldown.getOrDefault(h, -BOSS_COOLDOWN * 2L) + BOSS_COOLDOWN > t) continue;
            if (ow.random.nextDouble() > BOSS_CHANCE) continue;
            AABB box = new AABB(h).inflate(96, 256, 96);
            if (!ow.getEntitiesOfClass(HuVuong.class, box).isEmpty()) continue;
            bossCooldown.put(h, t);
            int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, h.getX(), h.getZ());
            spawnBoss(ow, new BlockPos(h.getX(), y, h.getZ()));
        }
    }

    public static HuVuong spawnBoss(ServerLevel sl, BlockPos pos) {
        HuVuong b = ModEntities.HU_VUONG.get().create(sl);
        if (b == null) return null;
        b.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
        b.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        sl.addFreshEntity(b);
        b.initScaling(sl);
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(b) < 128 * 128) p.displayClientMessage(Component.translatable("hukhong.boss.appear"), false);
        return b;
    }

    /** Boss chet: tay vung quanh, doi Tam sang noi khac. */
    public static void onBossKilled(ServerLevel sl, BlockPos at) {
        ServerLevel ow = sl.getServer().getLevel(Level.OVERWORLD);
        if (ow == null) return;
        CorruptionData d = CorruptionData.get(ow);
        BlockPos heart = null;
        double best = 96 * 96;
        for (BlockPos h : d.hearts()) {
            double dx = h.getX() - at.getX(), dz = h.getZ() - at.getZ();
            if (dx * dx + dz * dz < best) { best = dx * dx + dz * dz; heart = h; }
        }
        if (heart == null) return;
        d.removeHeart(heart);
        d.cleanseRadius(heart, 8);
        BlockPos sp = ow.getSharedSpawnPos();
        double ang = ow.random.nextDouble() * Math.PI * 2, dist = 5000 + ow.random.nextDouble() * 8000;
        d.addHeart(new BlockPos(sp.getX() + (int) (Math.cos(ang) * dist), 0, sp.getZ() + (int) (Math.sin(ang) * dist)));
    }

    /** Lenh test: nhiem n chunk quanh vi tri. */
    public static int seedAround(ServerLevel ow, BlockPos p, int n) {
        CorruptionData d = CorruptionData.get(ow);
        int c = 0, r = (int) Math.ceil(Math.sqrt(n)) + 1;
        for (int dx = -r; dx <= r && c < n; dx++)
            for (int dz = -r; dz <= r && c < n; dz++)
                if (d.add((p.getX() >> 4) + dx, (p.getZ() >> 4) + dz)) c++;
        return c;
    }

    // ---------------------------------------------------------------- lich tap kich
    private static void schedule(ServerLevel ow, CorruptionData d, long t) {
        if (InvasionManager.active() || InvasionManager.warning()) return;
        var city = com.hukhong.city.CitySavedData.get(ow);
        if (!city.isBuilt() || city.state() == com.hukhong.city.CitySavedData.State.RUIN) return;
        if (d.nextInvasionAt() < 0) { d.setNextInvasionAt(t + INVASION_MIN + ow.random.nextInt(INVASION_VAR)); return; }
        if (t >= d.nextInvasionAt() - InvasionManager.WARN_TICKS) {
            if (InvasionManager.warn(ow, InvasionManager.WARN_TICKS) == null)
                d.setNextInvasionAt(t + InvasionManager.WARN_TICKS + INVASION_MIN + ow.random.nextInt(INVASION_VAR));
        }
    }
}
