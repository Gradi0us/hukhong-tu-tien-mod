package com.hukhong.city;

import java.util.Random;

/** Đặt vị trí tất định (thuần, test được): neo cố định tương đối spawn + lệch theo seed, kẹp trong bán kính thế giới (R24). */
public final class Placement {
    private Placement() {}

    public static final int WORLD_RADIUS = 15000;
    public static final int BORDER_MARGIN = 1400;
    public static final int JITTER = 400;

    /** Lệch tất định ±range theo (seed, id). */
    public static int[] jitter(long seed, String id, int range) {
        Random r = new Random(seed * 0x9E3779B97F4A7C15L ^ id.hashCode() * 0xC2B2AE3D27D4EB4FL);
        return new int[]{r.nextInt(2 * range + 1) - range, r.nextInt(2 * range + 1) - range};
    }

    /** Vị trí tuyệt đối (x,z) = spawn + neo + lệch, kéo về trong bán kính (WORLD_RADIUS - BORDER_MARGIN) quanh spawn. */
    public static int[] site(int spawnX, int spawnZ, int ax, int az, long seed, String id) {
        int[] j = jitter(seed, id, JITTER);
        double dx = ax + j[0], dz = az + j[1];
        double max = WORLD_RADIUS - BORDER_MARGIN, d = Math.hypot(dx, dz);
        if (d > max) { dx *= max / d; dz *= max / d; }
        return new int[]{spawnX + (int) Math.round(dx), spawnZ + (int) Math.round(dz)};
    }
}
