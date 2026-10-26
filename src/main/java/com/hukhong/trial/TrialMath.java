package com.hukhong.trial;

/** Toán thuần của Khảo Hạch (S3): số đợt, số sét, sát thương, chỉ số bóng Tâm Ma. */
public final class TrialMath {
    private TrialMath() {}

    public static final int MIN_LEVEL = 90;

    /** Khảo Hạch áp dụng cho mốc 90+ (90, 95, 99, 100). */
    public static boolean requiresTrial(int level) { return level >= MIN_LEVEL; }

    /** Số đợt Lôi Kiếp: 3 ở cấp 90, +1 mỗi mốc kế, tối đa 7 (cấp 100 = 7). */
    public static int waves(int level) {
        if (level >= 100) return 7;
        return Math.min(6, 3 + Math.max(0, (level - 90) / 4));
    }

    /** Số tia sét trong đợt w (0-based), tăng dần. */
    public static int strikesInWave(int level, int wave) { return 1 + wave / 2 + (level >= 99 ? 1 : 0); }

    /** Sát thương một tia (HP): tăng theo cấp và theo đợt. Cấp 90 đợt 0 = 6. */
    public static float damage(int level, int wave) {
        return 6f + (level - 90) * 0.8f + wave * 1.5f;
    }

    /** Tick telegraph trước khi sét giáng (ngắn dần theo đợt, tối thiểu 20). */
    public static int telegraphTicks(int wave) { return Math.max(20, 40 - wave * 4); }

    /** Hệ số chỉ số bóng Tâm Ma so với người chơi. */
    public static double cloneScale(int level) { return level >= 100 ? 1.0 : 0.7 + (level - 90) * 0.02; }

    /** Xác suất Tẩu Hỏa khi thua Tâm Ma, chỉ Ma tu Ác Tính >= 80. */
    public static double tauHoaChance(boolean ma, double acTinh) {
        if (!ma || acTinh < 80) return 0;
        return Math.min(0.5, (acTinh - 70) / 100.0);
    }

    /** Hồi chiêu Bình Cảnh kéo dài (tick) sau lần thất bại thứ n (n>=1). */
    public static int penaltyTicks(int fails) { return 1200 * Math.min(fails, 5); }
}
