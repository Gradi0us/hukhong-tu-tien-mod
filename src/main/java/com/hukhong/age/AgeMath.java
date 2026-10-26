package com.hukhong.age;

/** Toán thuần Thọ Nguyên / Thiên Nhân Ngũ Suy (S6 §1.4-1.5). R1: không bao giờ chết vì tuổi; dừng ở GĐ V. */
public final class AgeMath {
    private AgeMath() {}

    public static final int UNLIMITED = Integer.MAX_VALUE;
    private static final int[] BASE = {50, 80, 100, 120, 140, 170, 200, 240, 300, 400, 600, 1000};
    public static final int YEARS_PER_STAGE = 2;
    public static final int MAX_STAGE = 5;

    public static int baseLifespan(int realmIndex) {
        return realmIndex < 0 ? BASE[0] : realmIndex >= BASE.length ? UNLIMITED : BASE[realmIndex];
    }

    /** Thọ Nguyên hiệu lực = cơ bản + Thọ bổ sung (≤ 50% cơ bản). */
    public static int lifespan(int realmIndex, int bonusYears) {
        int b = baseLifespan(realmIndex);
        if (b == UNLIMITED) return UNLIMITED;
        return b + Math.max(0, Math.min(bonusYears, b / 2));
    }

    /** Ngũ Suy 0..5: vượt Thọ mỗi 2 năm lên 1 GĐ, kẹt ở V. */
    public static int stage(int ageYears, int lifespan) {
        if (lifespan == UNLIMITED || ageYears < lifespan) return 0;
        return Math.min(MAX_STAGE, (ageYears - lifespan) / YEARS_PER_STAGE + 1);
    }

    /** Mức cảnh báo trước hạn: 0 không, 1 ≤10 năm, 2 ≤3 năm, 3 ≤1 năm. */
    public static int warnLevel(int ageYears, int lifespan) {
        if (lifespan == UNLIMITED) return 0;
        int left = lifespan - ageYears;
        if (left <= 0) return 0;
        return left <= 1 ? 3 : left <= 3 ? 2 : left <= 10 ? 1 : 0;
    }

    /** Khóa cảnh báo tăng dần: 0 | 1..3 (trước hạn) | 4..8 (Ngũ Suy I..V). */
    public static int warnKey(int ageYears, int lifespan) {
        int s = stage(ageYears, lifespan);
        return s > 0 ? 3 + s : warnLevel(ageYears, lifespan);
    }

    public static final double[] HP_MULT = {0, 0, 0, -0.10, -0.20, -0.40};
    public static final double[] DMG_MULT = {0, 0, 0, -0.15, -0.25, -0.35};
    public static double regenMult(int stage) { return stage <= 0 ? 1.0 : 0.9; }
}
