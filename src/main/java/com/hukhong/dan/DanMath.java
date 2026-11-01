package com.hukhong.dan;

/** Hằng số Đan Tu (thuần, không phụ thuộc Minecraft; balance_sim và test JVM đọc trực tiếp). Đơn vị: tick trừ khi ghi _SEC. */
public final class DanMath {
    private DanMath() {}

    // Độc (tầng) trên mục tiêu
    public static final int MAX_STACKS = 10;
    public static final double STACK_DPS_COEF = 0.05;   // mỗi tầng: 0,05 x Phép lực mỗi giây
    public static final int POISON_TICKS = 120;         // 6 giây, cộng thêm sẽ làm mới
    public static final int POISON_INTERVAL_TICKS = 20; // sát thương độc mỗi giây

    // Đòn thường: Bạo Đan (viên đan nổ ném ra)
    public static final double BASIC_COEF = 0.5;
    public static final int BASIC_STACKS = 1;
    public static final int BASIC_DELAY_TICKS = 6;      // viên đan bay 0,3 giây
    public static final double BASIC_RADIUS = 1.5;

    // Q - Độc Đan Vũ: 5 viên rơi quanh điểm ngắm, rồi để lại hồ độc
    public static final double Q_RANGE = 14;
    public static final double Q_SCATTER = 3;
    public static final int Q_PILLS = 5;
    public static final int Q_PILL_GAP_TICKS = 4;
    public static final double Q_COEF = 0.4;
    public static final int Q_STACKS = 2;
    public static final double Q_PILL_RADIUS = 1.5;
    public static final double POOL_RADIUS = 2;
    public static final int POOL_TICKS = 80;            // 4 giây, mỗi giây +1 tầng cho quái trong hồ
    public static final int POOL_INTERVAL_TICKS = 20;
    public static final int POOL_STACKS = 1;

    // E - Hồi Xuân Đan
    public static final double HEAL_COEF = 0.8;
    public static final double HEAL_MAX_PCT = 0.08;     // + 8% Sinh lực tối đa
    public static final double HEAL_RADIUS = 6;

    // R - Đan Hỏa Phần Thiên: nón lửa, tiêu hao toàn bộ độc trên mục tiêu
    public static final double R_RANGE = 8;
    public static final double R_COS = 0.6;             // nửa góc nón ~53 độ
    public static final double R_COEF = 1.2;
    public static final double R_PER_STACK = 0.15;      // mỗi tầng độc tiêu hao +15%

    // T - Cửu Chuyển Kim Đan: 10 giây miễn phí tài nguyên, đòn thường thành bom
    public static final int KIM_TICKS = 200;
    public static final double BOMB_RADIUS = 3;
    public static final double ULT_BOMB_COEF = 1.0;
    public static final int BOMB_STACKS = 2;

    // Nội tại Đan Tâm
    public static final int DAN_LO_QUALITY_BONUS = 1;   // +1 bậc phẩm chất khi Đan Tu đứng trước Đan Lô
    public static final double DOC_GAIN_MULT = 0.5;     // Đan Độc tích -50% với Đan Tu

    /** Cộng tầng độc, kẹp tối đa {@link #MAX_STACKS}. */
    public static int addStacks(int cur, int n) { return Math.min(MAX_STACKS, cur + Math.max(0, n)); }

    /** Sát thương độc mỗi nhịp (1 giây) với {@code stacks} tầng. */
    public static double poisonDamage(double sp, int stacks) { return STACK_DPS_COEF * sp * Math.max(0, Math.min(MAX_STACKS, stacks)); }

    /** Sát thương R lên một mục tiêu đang có {@code stacks} tầng độc (bị tiêu hao). */
    public static double rDamage(double sp, int stacks) { return R_COEF * sp * (1 + R_PER_STACK * Math.max(0, Math.min(MAX_STACKS, stacks))); }

    /** Tổng sát thương độc một mục tiêu nhận nếu đủ {@link #MAX_STACKS} tầng suốt {@link #POISON_TICKS} (chỉ để hiển thị/kiểm thử). */
    public static double poisonFullDuration(double sp) { return poisonDamage(sp, MAX_STACKS) * POISON_TICKS / 20.0; }
}
