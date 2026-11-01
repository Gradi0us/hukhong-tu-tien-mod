package com.hukhong.tran;

/** Hằng số Trận Tu (thuần, không phụ thuộc Minecraft; balance_sim và test JVM đọc trực tiếp). Đơn vị: tick trừ khi ghi _SEC. */
public final class TranMath {
    private TranMath() {}

    // Phù Văn Đạn (đòn thường)
    public static final double BASIC_COEF = 0.6;
    public static final int BASIC_BOLT_DELAY_TICKS = 3;
    public static final int MARK_TICKS = 100;          // 5 giây
    public static final double MARK_BONUS = 0.10;      // +10% sát thương kỹ năng lên mục tiêu đang dấu

    // Q - Khốn Trận
    public static final double Q_COEF = 0.8;
    public static final double Q_RANGE = 14;
    public static final double Q_RADIUS = 4;
    public static final int Q_TICKS = 70;             // 3,5 giây
    public static final double Q_SLOW = 0.60;         // chậm 60% (MOVEMENT_SLOWDOWN amp 3)
    public static final int Q_SLOW_AMP = 3;
    public static final int Q_SLOW_TICKS = 40;        // 2 giây
    public static final int Q_ROOT_DELAY_TICKS = 40;  // sau 2 giây thì choáng
    public static final double Q_ROOT_SEC = 1.5;

    // E - Hộ Thân Trận
    public static final double WARD_RADIUS = 5;
    public static final int WARD_TICKS = 160;         // 8 giây
    public static final double WARD_DR = 0.30;        // người chơi trong trận nhận -30% sát thương

    // R - Sát Trận
    public static final double R_RANGE = 16;
    public static final double R_RADIUS = 6;
    public static final double R_TICK_COEF = 0.55;
    public static final int R_HITS = 8;
    public static final int R_INTERVAL_TICKS = 10;
    public static final double R_PILLAR_RADIUS = 1.5;

    // T - Đại Trận
    public static final double ULT_RADIUS = 12;
    public static final int ULT_TICKS = 120;          // 6 giây
    public static final int ULT_PULL_INTERVAL_TICKS = 5;
    public static final double ULT_PULL = 0.25;
    public static final double ULT_COEF = 3.0;        // nổ cuối trận
    public static final double ULT_CD_MULT = 0.5;     // hồi chiêu x0,5 cho mọi kỹ năng Trận Tu khi đứng trong Đại Trận

    // Trận Tâm (nội tại)
    public static final double PASSIVE_DMG = 0.15;    // +15% sát thương gây ra khi đứng trong trận của mình
    public static final int REGEN_PER_SEC = 2;        // +2 Trận Lực/giây trong trận của mình

    /** Tổng hệ số R lên một mục tiêu nếu trúng đủ 8 nhịp (chỉ để hiển thị/kiểm thử). */
    public static double rTotalCoef() { return R_TICK_COEF * R_HITS; }

    /** Hệ số hồi chiêu: x0,5 khi đứng trong Đại Trận của mình. */
    public static double cooldownMult(boolean insideOwnUlt) { return insideOwnUlt ? ULT_CD_MULT : 1.0; }
}
