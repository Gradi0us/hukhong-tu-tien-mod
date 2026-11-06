package com.hukhong.world.yeulam;

import com.hukhong.HuKhong;
import com.hukhong.creature.CreatureKind;
import com.hukhong.creature.CreatureRegistry;
import com.hukhong.creature.LinhQuai;
import com.hukhong.creature.YeuThuAge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Đại Sâm Lâm: các vùng rừng cổ thụ cực lớn (bán kính 170-280 ô) đặt tất định theo seed trên lưới 1400 ô.
 * Cây cổ thụ do CoThuFeature mọc khi sinh địa hình; Yêu Thú sinh thêm quanh người chơi ở trong rừng, càng vào sâu càng nhiều năm.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class DaiSamLam {
    private DaiSamLam() {}

    public static final int CELL = 1400, MIN_R = 170, MAX_R = 280, SAFE_SPAWN = 500;
    public static final int SPAWN_PERIOD = 100, NEAR_CAP = 6;

    public record Region(int cx, int cz, int r) {
        public double depth(double x, double z) { return Math.max(0, 1 - Math.hypot(x - cx, z - cz) / r); }
    }

    private static long mix(long seed, long a, long b) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 33; h *= 0xFF51AFD7ED558CCDL; h ^= h >>> 33; h *= 0xC4CEB9FE1A85EC53L; h ^= h >>> 33;
        return h;
    }

    /** Vùng của ô lưới (gx, gz); null nếu ô này không có rừng (1/3 ô trống) hoặc quá gần điểm hồi sinh. */
    public static Region cell(long seed, int gx, int gz) {
        long h = mix(seed, gx, gz);
        if (Math.floorMod(h, 3) == 0) return null;
        int span = CELL / 2 - MAX_R - 20;
        int cx = gx * CELL + CELL / 2 + (int) Math.floorMod(h >>> 8, 2L * span + 1) - span;
        int cz = gz * CELL + CELL / 2 + (int) Math.floorMod(h >>> 24, 2L * span + 1) - span;
        int r = MIN_R + (int) Math.floorMod(h >>> 40, (long) (MAX_R - MIN_R + 1));
        if (Math.hypot(cx, cz) < SAFE_SPAWN + r) return null;
        return new Region(cx, cz, r);
    }

    /** 2026-11-06: 1/3 số Đại Sâm Lâm bị Hư Không xâm thực - quái cây ở đó có tỉ lệ biến dị (mạnh hơn). */
    public static final double CORRUPT_CHANCE = 0.45, CORRUPT_PLANT_BIAS = 0.6;

    public static boolean corrupted(long seed, Region r) { return r != null && Math.floorMod(mix(seed ^ 0x48555L, r.cx(), r.cz()), 3) == 0; }

    public static boolean corruptedAt(long seed, double x, double z) { return corrupted(seed, at(seed, x, z)); }

    /** Còn bị xâm thực (tĩnh hoặc lan tới, chưa được giải phóng qua Lịch Luyện). */
    public static boolean corruptedLive(ServerLevel sl, Region r) {
        if (r == null) return false;
        ServerLevel ow = sl.getServer().overworld();
        if (CleansedForests.get(ow).has(r)) return false;
        return corrupted(sl.getSeed(), r) || VoidSpread.get(ow).has(r); // tĩnh hoặc đã bị Hư Không lan tới
    }

    public static boolean corruptedLiveAt(ServerLevel sl, double x, double z) { return corruptedLive(sl, at(sl.getSeed(), x, z)); }

    /** Vùng chứa (x, z), null nếu ngoài mọi Đại Sâm Lâm. */
    public static Region at(long seed, double x, double z) {
        int gx = Math.floorDiv((int) Math.floor(x), CELL), gz = Math.floorDiv((int) Math.floor(z), CELL);
        Region c = cell(seed, gx, gz);
        return c != null && c.depth(x, z) > 0 ? c : null;
    }

    public static double depth(long seed, double x, double z) {
        Region c = at(seed, x, z);
        return c == null ? 0 : c.depth(x, z);
    }

    /** Vùng gần (x, z) nhất trong 3x3 ô quanh đó (lệnh / chỉ đường). */
    public static Region nearest(long seed, double x, double z) {
        int gx = Math.floorDiv((int) Math.floor(x), CELL), gz = Math.floorDiv((int) Math.floor(z), CELL);
        Region best = null;
        double bd = Double.MAX_VALUE;
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
            Region c = cell(seed, gx + i, gz + j);
            if (c == null) continue;
            double d = Math.hypot(c.cx() - x, c.cz() - z);
            if (d < bd) { bd = d; best = c; }
        }
        return best;
    }

    public static final List<CreatureKind> BEASTS = CreatureKind.COMMON_BEASTS;

    private static final Map<UUID, Boolean> INSIDE = new HashMap<>();

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0 || !(p.level() instanceof ServerLevel sl)
                || sl.dimension() != Level.OVERWORLD || p.isSpectator()) return;
        long seed = sl.getSeed();
        Region r = at(seed, p.getX(), p.getZ());
        boolean in = r != null;
        Boolean was = INSIDE.put(p.getUUID(), in);
        boolean bad = corruptedLive(sl, r);
        if (was != null && was != in)
            p.displayClientMessage(Component.translatable(in ? (bad ? "hukhong.dai_sam_lam.enter_hu" : "hukhong.dai_sam_lam.enter") : "hukhong.dai_sam_lam.leave")
                    .withColor(in ? (bad ? 0xB060FF : 0x7FD27F) : 0xB0B0B0), true);
        if (in && bad) // không khí Hư Không: bụi tím đen trôi quanh người chơi
            sl.sendParticles(p, net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, false, p.getX(), p.getY() + 2, p.getZ(), 30, 8, 3, 8, 0.02);
        if (in && p.tickCount % SPAWN_PERIOD == 0) spawnNear(sl, p, r);
    }

    /** Sinh 1 Yêu Thú quanh người chơi (24-44 ô) nếu gần đó chưa đủ. Trả con vừa sinh (test gọi tay). */
    public static LinhQuai spawnNear(ServerLevel sl, ServerPlayer p, Region r) {
        if (sl.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return null;
        int near = sl.getEntitiesOfClass(LinhQuai.class, p.getBoundingBox().inflate(48), q -> q.kind().beast()).size();
        if (near >= NEAR_CAP) return null;
        var rnd = sl.random;
        double a = rnd.nextDouble() * Math.PI * 2, d = 24 + rnd.nextDouble() * 20;
        int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
        if (!sl.hasChunk(x >> 4, z >> 4)) return null;
        int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos bp = new BlockPos(x, y, z);
        if (!sl.getFluidState(bp.below()).isEmpty() || !sl.getBlockState(bp).isAir()) return null;
        boolean bad = corruptedLive(sl, r);
        CreatureKind k = bad && rnd.nextDouble() < CORRUPT_PLANT_BIAS ? CreatureKind.PLANTS.get(rnd.nextInt(CreatureKind.PLANTS.size()))
                : BEASTS.get(rnd.nextInt(BEASTS.size()));
        LinhQuai m = spawnBeast(sl, k, bp, YeuThuAge.roll(r.depth(x, z), rnd), MobSpawnType.EVENT);
        if (m != null && bad && k.plant() && !m.corrupted() && rnd.nextDouble() < CORRUPT_CHANCE) m.setCorrupted(true);
        return m;
    }

    public static LinhQuai spawnBeast(ServerLevel sl, CreatureKind k, BlockPos bp, YeuThuAge age, MobSpawnType type) {
        if (!(CreatureRegistry.type(k).create(sl) instanceof LinhQuai m)) return null;
        m.moveTo(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
        m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(bp), type, null);
        m.setAge(age);
        sl.addFreshEntity(m);
        return m;
    }
}
