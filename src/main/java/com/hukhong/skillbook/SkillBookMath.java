package com.hukhong.skillbook;

/**
 * Toán Cường Hóa kỹ năng (cấp 1..10): +8% sát thương/cấp, -3% hồi chiêu/cấp (trần -30%), chi phí, giới hạn theo cấp tu.
 * Thuần Java - kiểm thử bằng JUnit; cũng được BalanceSimTest dùng để mô phỏng "cấp kỹ năng điển hình".
 */
public final class SkillBookMath {
    private SkillBookMath() {}

    public static final int MAX_LEVEL = 10;
    public static final double DMG_PER_LEVEL = 0.08;
    public static final double CD_PER_LEVEL = 0.03, CD_CAP = 0.30;

    /** Cách trả phí Cường Hóa. */
    public enum Mode {
        /** Hồn Tinh + Cống Hiến tông môn (CH ví). */
        CONTRIBUTION,
        /** Nửa Hồn Tinh + bản sao bí tịch cùng kỹ năng (chỉ kỹ năng học từ bí tịch). */
        SCROLL,
        /** Chỉ Hồn Tinh (x3) - dành cho người ngoài tông môn. */
        HON_TINH;

        public static Mode of(int o) { return values()[Math.max(0, Math.min(values().length - 1, o))]; }
    }

    public record Cost(int honTinh, int contribution, int scrolls) {
        public static final Cost NONE = new Cost(0, 0, 0);
    }

    public static int clampLevel(int lv) { return Math.max(1, Math.min(MAX_LEVEL, lv)); }

    /** Hệ số sát thương/hiệu lực: 1 + 0,08 (lv-1). Cấp 10 = x1,72. */
    public static double dmgMult(int lv) { return 1.0 + DMG_PER_LEVEL * (clampLevel(lv) - 1); }

    /** Hệ số hồi chiêu: 1 - min(0,30; 0,03 (lv-1)). Cấp 10 = x0,73. */
    public static double cdMult(int lv) { return 1.0 - Math.min(CD_CAP, CD_PER_LEVEL * (clampLevel(lv) - 1)); }

    public static int cooldown(int baseTicks, int lv) { return Math.max(1, (int) Math.round(baseTicks * cdMult(lv))); }

    /** Cấp kỹ năng tối đa theo cấp tu luyện: 1 + level/10 (cấp tu 90+ mở cấp 10). */
    public static int levelCap(int playerLevel) { return clampLevel(1 + Math.max(0, playerLevel) / 10); }

    /**
     * Cấp kỹ năng "điển hình" của người chơi cấp tu `playerLevel` - giả định cho BalanceSimTest:
     * người chơi điển hình chỉ đầu tư khoảng 1/6 khoảng cấp cho phép (Hồn Tinh/Cống Hiến cạnh tranh với Lõi, Phá Cảnh); cấp 10 là đầu tư tối đa, không phải điển hình.
     */
    public static int typicalLevel(int playerLevel) { return clampLevel(1 + (levelCap(playerLevel) - 1) / 6); }

    /** Chi phí nâng từ cấp `from` lên from+1 (from 1..9). */
    public static Cost cost(int from, SkillGrade grade, Mode mode) {
        int f = clampLevel(from);
        if (f >= MAX_LEVEL) return Cost.NONE;
        double g = grade.costMult;
        int hon = (int) Math.round((2 + 2 * f) * g);
        return switch (mode) {
            case CONTRIBUTION -> new Cost(hon, (int) Math.round(30 * f * g), 0);
            case SCROLL -> new Cost(Math.max(1, hon / 2), 0, 1 + f / 3);
            case HON_TINH -> new Cost(hon * 3, 0, 0);
        };
    }

    /** Tổng Hồn Tinh để đi từ cấp 1 đến cấp 10 bằng CONTRIBUTION (để cân đối nguồn rơi Hồn Tinh). */
    public static int totalHonTinh(SkillGrade grade) {
        int t = 0;
        for (int l = 1; l < MAX_LEVEL; l++) t += cost(l, grade, Mode.CONTRIBUTION).honTinh();
        return t;
    }
}
