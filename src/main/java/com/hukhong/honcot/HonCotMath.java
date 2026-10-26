package com.hukhong.honcot;

/** Toán thuần của Hồn Cốt: phẩm, roll, giá trị chỉ số, yêu cầu cấp, tỉ lệ rơi. */
public final class HonCotMath {
    private HonCotMath() {}

    public static final int TIER_TU = 0, TIER_HAC = 1, TIER_HONG = 2;
    public static final int MIN_DROP_LEVEL = 61;
    public static final double BOSS_DROP_CHANCE = 0.25;
    public static final int BOSS_PITY = 4; // sau 4 lần boss không rơi -> lần 5 chắc chắn rơi

    private static final double[] TIER_MULT = {0.70, 1.00, 1.40};
    private static final int[] TIER_REQ_LEVEL = {40, 60, 80};

    public static int clampTier(int t) { return Math.max(TIER_TU, Math.min(TIER_HONG, t)); }

    public static double tierMult(int tier) { return TIER_MULT[clampTier(tier)]; }

    /** Cấp tu luyện tối thiểu để mang phẩm này (canon: trần mở ở 40 / 60 / 80). */
    public static int requiredLevel(int tier) { return TIER_REQ_LEVEL[clampTier(tier)]; }

    public static boolean canEquip(int playerLevel, int tier) { return playerLevel >= requiredLevel(tier); }

    /** roll 0..100 -> hệ số 0.90..1.10. */
    public static double rollMult(int roll) { return 0.90 + 0.20 * Math.max(0, Math.min(100, roll)) / 100.0; }

    /** Giá trị cuối của một chỉ số Hồn Cốt (knockback_resistance tự bị kẹp ở 1 bởi attribute). */
    public static double statValue(double base, int tier, int roll) { return base * tierMult(tier) * rollMult(roll); }

    /** Roll chất lượng 0..100 từ hai số ngẫu nhiên [0,1) (phân phối tam giác: hiếm ở hai đầu). */
    public static int rollQuality(double r1, double r2) {
        return (int) Math.round(Math.max(0, Math.min(1, (r1 + r2) / 2.0)) * 100);
    }

    /** Tỉ lệ rơi từ quái Hư Hóa thường: 0 dưới cấp 61, 1% tại 61 tăng 0.05%/cấp, trần 5%. */
    public static double dropChance(int huLevel, boolean boss) {
        if (boss) return BOSS_DROP_CHANCE;
        if (huLevel < MIN_DROP_LEVEL) return 0;
        return Math.min(0.05, 0.01 + 0.0005 * (huLevel - MIN_DROP_LEVEL));
    }

    /** Boss thế giới: rơi nếu r < 25% hoặc đã chạm bảo hiểm (pity). */
    public static boolean bossDrops(int pity, double r) { return pity >= BOSS_PITY || r < BOSS_DROP_CHANCE; }

    /** Phẩm theo cấp quái (61-80 Tử, 81-95 Hắc, 96+ Hồng); boss: Hắc/Hồng 50:50 theo r. */
    public static int tierFor(int huLevel, boolean boss, double r) {
        if (boss) return r < 0.5 ? TIER_HAC : TIER_HONG;
        if (huLevel >= 96) return TIER_HONG;
        if (huLevel >= 81) return TIER_HAC;
        return TIER_TU;
    }

    /** Hồn Cốt ngẫu nhiên theo r [0,1). */
    public static Bone pickBone(double r) {
        Bone[] all = Bone.values();
        return all[Math.max(0, Math.min(all.length - 1, (int) (r * all.length)))];
    }

    /** Vạn Cốt Cộng Minh: hệ số sát thương theo số Hồn Cốt đang mang (mạnh hơn nếu đủ 6). */
    public static double resonanceCoef(int equipped) {
        int n = Math.max(0, Math.min(BoneSlot.COUNT, equipped));
        return 1.0 + 0.5 * n + (n == BoneSlot.COUNT ? 1.0 : 0.0);
    }
}
