package com.hukhong.client.ambient;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Trạng thái khí quyển phía client (dữ liệu thô từ server). Thuần Java.
 * Hook API cho hệ thống khác: {@link #forceInvasion}, {@link #forceRaid}, {@link #forceBiCanhTier}, {@link #pulseFlash}.
 */
public final class AmbientState {
    private AmbientState() {}

    private static volatile Set<Long> chunks = Set.of();
    private static volatile List<BlockPos> hearts = List.of();
    private static volatile boolean invasion, raid;
    private static volatile int evX, evZ;
    private static volatile int bicanhTier = -1;
    // hook override (null = dùng dữ liệu server)
    private static volatile Boolean forceInv, forceRaid;
    private static volatile int forceTier = Integer.MIN_VALUE;
    private static volatile int pendingFlash;

    public static void apply(AmbientSyncPayload p) {
        chunks = new HashSet<>(p.chunks());
        hearts = List.copyOf(p.hearts());
        invasion = (p.flags() & AmbientSyncPayload.F_INVASION) != 0;
        raid = (p.flags() & AmbientSyncPayload.F_RAID) != 0;
        evX = p.cx();
        evZ = p.cz();
        bicanhTier = p.bicanhTier();
    }

    public static void reset() {
        chunks = Set.of(); hearts = List.of(); invasion = false; raid = false; bicanhTier = -1;
        forceInv = null; forceRaid = null; forceTier = Integer.MIN_VALUE;
    }

    // ---------------------------------------------------------------- hook API
    /** Ép trạng thái xâm lăng quanh (x,z); null bỏ ép. */
    public static void forceInvasion(Boolean on, int x, int z) { forceInv = on; if (on != null) { evX = x; evZ = z; } }
    public static void forceRaid(Boolean on) { forceRaid = on; }
    public static void forceBiCanhTier(int tier) { forceTier = tier; }
    /** Yêu cầu một tia chớp tím xa (không sát thương). */
    public static void pulseFlash() { pendingFlash++; }
    static boolean takeFlash() { if (pendingFlash > 0) { pendingFlash--; return true; } return false; }

    public static boolean invasion() { return forceInv != null ? forceInv : invasion; }
    public static boolean raid() { return forceRaid != null ? forceRaid : raid; }
    public static int eventX() { return evX; }
    public static int eventZ() { return evZ; }
    public static int bicanhTier() { return forceTier != Integer.MIN_VALUE ? forceTier : bicanhTier; }
    public static List<BlockPos> hearts() { return hearts; }
    public static boolean anyCorruption() { return !chunks.isEmpty() || !hearts.isEmpty(); }

    /** Mật độ nhiễm 0..1 tại (x,z): trung bình có trọng số khoảng cách các chunk quanh đó; mượt khi đi qua ranh giới. */
    public static float corruptionAt(double x, double z) {
        Set<Long> c = chunks;
        float dens = 0f;
        if (!c.isEmpty()) {
            int cx = Mth.floor(x) >> 4, cz = Mth.floor(z) >> 4;
            double sum = 0, all = 0;
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    double mx = ((cx + dx) << 4) + 8 - x, mz = ((cz + dz) << 4) + 8 - z;
                    double w = Math.max(0, 1 - Math.sqrt(mx * mx + mz * mz) / 64.0);
                    if (w <= 0) continue;
                    all += w;
                    if (c.contains(net.minecraft.world.level.ChunkPos.asLong(cx + dx, cz + dz))) sum += w;
                }
            }
            dens = all > 0 ? (float) (sum / all) : 0f;
            dens = Mth.clamp((dens - 0.04f) / 0.7f, 0f, 1f);
            dens = dens * dens * (3 - 2 * dens);
        }
        for (BlockPos h : hearts) {
            double d = Math.sqrt((h.getX() + .5 - x) * (h.getX() + .5 - x) + (h.getZ() + .5 - z) * (h.getZ() + .5 - z));
            if (d < 40) dens = Math.max(dens, (float) Mth.clamp(1.0 - (d - 8) / 32.0, 0, 1));
        }
        return dens;
    }

    /** 0..1: mức xâm lăng/đột kích ở vị trí này (xâm lăng giảm dần trong 120..260 block quanh tâm). */
    public static float warAt(double x, double z) {
        float w = 0f;
        if (raid()) w = 1f;
        if (invasion()) {
            double d = Math.sqrt((x - evX) * (x - evX) + (z - evZ) * (z - evZ));
            w = Math.max(w, 1f - (float) Mth.smoothstep((d - 120) / 140.0));
        }
        return w;
    }
}
