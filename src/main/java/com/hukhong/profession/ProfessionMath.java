package com.hukhong.profession;

/** Hằng số và công thức nghề phụ (một chỗ chỉnh cân bằng). */
public final class ProfessionMath {
    private ProfessionMath() {}

    public static final int MAX_LEVEL = 20;
    /** Kim Pháp Tu + Đoán Tạo Sư: +50% kinh nghiệm chế tạo. */
    public static final double KIM_XP_MULT = 1.5;
    /** Kim Pháp Tu + Đoán Tạo Sư: xác suất +1 bậc phẩm khi rèn (mỗi lần chế tạo). */
    public static final float KIM_TIER_CHANCE = 0.3f;
    /** Đan Lô: +3% cơ hội thêm 1 viên mỗi cấp Luyện Đan Sư. */
    public static final double PILL_BONUS_PER_LEVEL = 0.03;

    /** Trần cấp nghề theo chỉ số cảnh giới (Realm.indexOf): 1 + realmIndex*2, tối đa 20. */
    public static int cap(int realmIndex) { return Math.min(MAX_LEVEL, 1 + Math.max(0, realmIndex) * 2); }

    /** Kinh nghiệm cần để lên từ {@code level}: round(20 * level^1.5). */
    public static long need(int level) { return Math.round(20.0 * Math.pow(Math.max(1, level), 1.5)); }
}
