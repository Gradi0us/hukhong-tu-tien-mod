package com.hukhong.client.render.orbit;

/** Toán thuần cho phi kiếm (không phụ thuộc Minecraft để test được). */
public final class OrbitMath {
    private OrbitMath() {}

    /** Ease-in-out (smootherstep): u trong [0,1] -> [0,1], vận tốc 0 ở hai đầu. */
    public static double easeInOut(double u) {
        u = Math.max(0, Math.min(1, u));
        return u * u * u * (u * (u * 6 - 15) + 10);
    }

    /** Dao động nhẹ của kiếm treo sau vai (block). */
    public static double bob(double time, int salt) { return 0.05 * Math.sin(time * 0.08 + salt * 1.3); }
}
