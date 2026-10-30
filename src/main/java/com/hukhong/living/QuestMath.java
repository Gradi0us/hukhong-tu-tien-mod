package com.hukhong.living;

import java.util.Random;

/** Toan nhiem vu ngau nhien: thuong, so luong, vi tri muc tieu (thuan, kiem thu duoc). */
public final class QuestMath {
    private QuestMath() {}

    /** Thuong Ha: co so theo cap, nhan he so mau (1.0..2.5), nhiem vu dac biet x2. */
    public static long reward(int level, double mult, boolean special) {
        long base = 120L + 25L * Math.max(1, Math.min(100, level));
        return Math.max(1, Math.round(base * mult * (special ? 2.0 : 1.0)));
    }

    /** Do lech (dx, dz) tai khoang cach dist..dist2 theo goc ngau nhien. */
    public static int[] offset(Random r, int min, int max) {
        double d = min + r.nextDouble() * Math.max(0, max - min);
        double a = r.nextDouble() * Math.PI * 2;
        return new int[]{(int) Math.round(Math.cos(a) * d), (int) Math.round(Math.sin(a) * d)};
    }

    public static double distXZ(int ax, int az, int bx, int bz) {
        double dx = ax - bx, dz = az - bz;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** So luong can thiet: [lo, hi] tang nhe theo cap. */
    public static int count(Random r, int lo, int hi, int level) {
        int extra = level >= 60 ? 2 : level >= 30 ? 1 : 0;
        return lo + r.nextInt(Math.max(1, hi - lo + 1)) + extra;
    }
}
