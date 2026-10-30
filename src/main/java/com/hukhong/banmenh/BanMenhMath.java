package com.hukhong.banmenh;

/** Toán thuần Bản Mệnh Kiếm: tiến hóa bằng Hồn Tinh (trần = cảnh giới) + hiệu ứng loại Ngọc Tâm / Trọng Nhạc (S2 §2.1). */
public final class BanMenhMath {
    private BanMenhMath() {}

    public static final int MAX_STAGE = 6;
    /** Cấp tối thiểu cho từng giai đoạn: Phàm, Linh, Huyền, Địa, Thiên, Tiên, Thần. */
    public static final int[] STAGE_LEVELS = {1, 20, 40, 60, 80, 90, 100};
    /** Hồn Tinh cần để tiến lên giai đoạn s (Hồn Tinh trong mod chưa chia phẩm -> tính theo số lượng). */

    public static final int OK = 0, MAXED = 1, REALM_CAP = 2, NEED_TINH = 3;

    /** Dang Thu Ho (ket thuc Sieu Do, co banmenh.thu_ho): +5% hoi tai nguyen, +5% tai nguyen toi da. Ngu bay duong dai: noi dung sau. */
    public static final double THU_HO_REGEN = 0.05, THU_HO_MAX_RESOURCE = 0.05;
    public static final double NGOC_TAM_REGEN = 0.25, NGOC_TAM_MAX_RESOURCE = 0.15, NGOC_TAM_SKILL_COST = 0.90;
    public static final double TRONG_NHAC_SPEED = -0.06;

    /** Số kiếm bay quanh người theo giai đoạn (0..6): Phàm/Linh 1, Huyền 1, Địa 2, Thiên 3, Tiên 5, Thần 9. */
    private static final int[] ORBIT_COUNT = {1, 1, 1, 2, 3, 5, 9};
    /** Phi kiếm: tick bay tới mục tiêu trước khi server áp sát thương, và tick bay về. */
    public static final int STRIKE_DELAY_TICKS = 6, STRIKE_RETURN_TICKS = 8;
    /** Tốc độ phi kiếm (block/tick) dùng để tính thời gian bay = thời điểm server áp sát thương. */
    public static final double FLY_SPEED = 1.6;
    public static int flightTicks(double dist) { return Math.max(3, Math.min(14, (int) Math.ceil(dist / FLY_SPEED))); }
    /** Số kiếm phóng cho đòn đánh thường: 1, từ giai đoạn 5 là 2. */
    public static int strikeSwords(int stage) { return stage >= 5 ? 2 : 1; }

    public static int orbitCount(int stage) { return ORBIT_COUNT[Math.max(0, Math.min(MAX_STAGE, stage))]; }

    /** Tầm phi kiếm (block): 8 ở Phàm -> 12 ở Thần. */
    public static double strikeRange(int stage) { return 8.0 + Math.max(0, Math.min(MAX_STAGE, stage)) * (4.0 / MAX_STAGE); }

    public static int stageForLevel(int level) {
        int s = 0;
        for (int i = 0; i < STAGE_LEVELS.length; i++) if (level >= STAGE_LEVELS[i]) s = i;
        return s;
    }

    public static int tinhCost(int targetStage) {
        int s = Math.max(0, Math.min(MAX_STAGE, targetStage));
        return s == 0 ? 0 : (int) com.hukhong.currency.UpgradeCost.honTinh(s, com.hukhong.currency.UpgradeCost.STAGE_BASE);
    }

    /** Kiểm tra tiến hóa từ giai đoạn cur: trần theo cảnh giới (level), cần đủ Hồn Tinh. */
    public static int check(int cur, int level, int tinh) {
        if (cur >= MAX_STAGE) return MAXED;
        if (cur + 1 > stageForLevel(level)) return REALM_CAP;
        return tinh >= tinhCost(cur + 1) ? OK : NEED_TINH;
    }

    /** Giai đoạn hiệu lực: không vượt trần cảnh giới. */
    public static int effectiveStage(int stored, int level) {
        return Math.max(0, Math.min(Math.min(MAX_STAGE, stored), stageForLevel(level)));
    }

    public static double regenBonus(BanMenhType t) { return t == BanMenhType.NGOC_TAM ? NGOC_TAM_REGEN : 0; }

    public static double maxResourceBonus(BanMenhType t) { return t == BanMenhType.NGOC_TAM ? NGOC_TAM_MAX_RESOURCE : 0; }

    public static double speedBonus(BanMenhType t) { return t == BanMenhType.TRONG_NHAC ? TRONG_NHAC_SPEED : 0; }

    /** Lv40 (giai đoạn Huyền, >= 2): kỹ năng -10% tiêu hao. */
    public static double skillCostFactor(BanMenhType t, int stage) {
        return t == BanMenhType.NGOC_TAM && stage >= 2 ? NGOC_TAM_SKILL_COST : 1.0;
    }

    /** Lv80 (giai đoạn Thiên, >= 4): ngự kiếm miễn phí trong combat. */
    public static boolean freeFlight(BanMenhType t, int stage, boolean inCombat) {
        return t == BanMenhType.NGOC_TAM && stage >= 4 && inCombat;
    }
}
