package com.hukhong.story.freequest;

/** Thuần số học yêu cầu quest Đột Phá (không đụng registry, test JUnit được). */
public final class FreeQuestMath {
    private FreeQuestMath() {}

    /** Phẩm đan cần cho Bình Cảnh ở cấp `level` (9->1 ... 89->9; 94->5, 98->9 là Tiểu Phá Đan). */
    public static int pillTier(int level) {
        if (level == 94) return 5;
        if (level == 98) return 9;
        return (level + 1) / 10;
    }

    /** Cấp có Bình Cảnh (chính hoặc nhẹ) mà quest áp dụng. */
    public static boolean isQuestLevel(int level) {
        return (level < 90 && level % 10 == 9) || level == 94 || level == 98;
    }

    /** Thảo dược dùng loại nào: false = Linh Thảo (phẩm 1-3), true = Huyết Sâm (phẩm cao). */
    public static boolean herbIsHuyetTham(int tier) { return tier >= 4; }

    /** Số thảo dược cần nộp: 4 ở phẩm 1, +2 mỗi phẩm. */
    public static int herbCount(int tier) { return 4 + 2 * (Math.max(1, tier) - 1); }

    /** Số Hư Hóa cần hạ (lựa chọn thay thế). */
    public static int killCount(int tier) { return 3 + 2 * Math.max(1, tier); }

    /** Mức tối thiểu của quái tính vào quest: không thấp hơn cấp người chơi - 5. */
    public static int minMobLevel(int playerLevel) { return Math.max(1, playerLevel - 5); }
}
