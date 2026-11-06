package com.hukhong.tran;

/** Hằng số Trận Tu (thuần, không phụ thuộc Minecraft; balance_sim và test JVM đọc trực tiếp). Đơn vị: tick trừ khi ghi _SEC. */
public final class TranMath {
    private TranMath() {}

    /** Mặt nạ khoá: cả 4 phím Q/E/R/T cố định cho Trận Tu (sách/cuộn không thay được). */
    public static final int LOCK_MASK = 15;

    // Thế: chọn thế bằng T rồi Q/E/R
    public static final int ARM_TICKS = 60;            // 3 giây chờ chọn thế
    public static final int T_RETURN_CD_TICKS = 4;     // 0,2 giây sau khi về BASE mới bấm T lại được

    // Phù Văn Đạn (đòn thường)
    public static final double BASIC_COEF = 0.6;
    public static final int BASIC_BOLT_DELAY_TICKS = 3;
    public static final int MARK_TICKS = 100;          // 5 giây
    public static final double MARK_BONUS = 0.10;      // +10% sát thương kỹ năng lên mục tiêu đang dấu

    // BASE Q - Khốn Trận
    public static final double Q_COEF = 0.8;
    public static final double Q_RANGE = 14;
    public static final double Q_RADIUS = 4;
    public static final int Q_TICKS = 70;             // 3,5 giây
    public static final double Q_SLOW = 0.60;
    public static final int Q_SLOW_AMP = 3;           // MOVEMENT_SLOWDOWN amp 3 = -60%
    public static final int Q_SLOW_TICKS = 40;
    public static final int Q_ROOT_DELAY_TICKS = 40;  // sau 2 giây thì choáng
    public static final double Q_ROOT_SEC = 1.5;

    // BASE E - Sát Trận (cột sáng)
    public static final double R_RANGE = 16;
    public static final double R_RADIUS = 6;
    public static final double R_TICK_COEF = 0.55;
    public static final int R_HITS = 8;
    public static final int R_INTERVAL_TICKS = 10;
    public static final double R_PILLAR_RADIUS = 1.5;

    // BASE R - Hộ Thân Trận / WARD (dùng chung với PHONG_NGU Q)
    public static final double WARD_RADIUS = 5;
    public static final int WARD_TICKS = 160;         // 8 giây
    /** Hộ Thân Trận (R cơ bản, 2026-11-06): theo chân 20 giây, hồi 30 giây, 0,2 x Phép lực/giây lên địch trong vùng. */
    public static final int HOTHAN_TICKS = 400;
    public static final double HOTHAN_DOT_COEF = 0.20;
    /** Vạn Kiếm Trận: kiếm tự tìm mục tiêu; mục tiêu nhận nhiều kiếm bị giảm tối đa 40% (1 mục tiêu = 0,6). */
    public static final double BLADE_SINGLE_CUT = 0.40, BLADE_SEEK_RADIUS = 8;

    public static double bladeMult(int onTarget, int total) { return 1 - BLADE_SINGLE_CUT * onTarget / (double) Math.max(1, total); }
    public static final double WARD_DR = 0.30;        // người chơi trong trận nhận -30% sát thương

    // Trận Tâm (nội tại): đứng trong trận của mình
    public static final double PASSIVE_DMG = 0.15;
    public static final int REGEN_PER_SEC = 2;

    // ---- KHỐNG CHẾ (T + Q)
    public static final double CAGE_RANGE = 14;
    public static final double CAGE_RADIUS = 4;
    public static final int CAGE_TICKS = 160;         // 8 giây
    public static final int CAGE_HIT_BASE = 6;
    public static final int CAGE_HIT_PER_MOB = 2;
    public static final int CAGE_MOB_CAP = 5;         // số quái tính vào công thức máu lồng (tối đa)
    public static final int CAGE_HIT_INTERVAL_TICKS = 20; // mỗi giây mỗi quái bị nhốt đập lồng 1 lần
    public static final double CAGE_LEASH = 0.6;      // quái bị đẩy về trong bán kính r - 0,6
    public static final double SNARE_RANGE = 14;
    public static final double SNARE_RADIUS = 5;
    public static final int SNARE_TICKS = 120;        // 6 giây
    public static final double SNARE_DOT_COEF = 0.15; // x Phép lực / giây
    public static final int SNARE_TICK_INTERVAL = 20;
    public static final int SNARE_SLOW_AMP = 3;       // -60%
    public static final double SUP_RANGE = 14;
    public static final double SUP_RADIUS = 5;
    public static final int SUP_TICKS = 100;          // 5 giây
    public static final double SUP_DR = 0.30;         // quái trong vùng gây -30% sát thương
    public static final double SUP_STUN_SEC = 1.0;
    public static final int SUP_CHECK_INTERVAL = 5;

    // ---- SÁT THƯƠNG (T + E)
    public static final int KL_MAX = 5;               // 5 Kiệt Lực -> Kiệt Sức
    public static final int KL_DECAY_TICKS = 80;      // mỗi 4 giây giảm 1
    public static final int KL_EXHAUST_TICKS = 100;   // Kiệt Sức 5 giây
    public static final int KL_SLOW_AMP = 1;          // Slowness II
    public static final int KL_WEAK_AMP = 0;          // Weakness I
    /** Sát Trận (Q, giữ để nạp): vòng tròn tụ lực tại điểm ngắm; thả Q (hoặc đủ tối đa + 1 giây) thì cột sáng khổng lồ. */
    public static final double CHARGE_RANGE = 16;
    public static final double CHARGE_MIN_R = 2, CHARGE_MAX_R = 7;
    public static final int CHARGE_TICKS = 60;          // 3 giây giữ để đạt bán kính tối đa
    public static final int CHARGE_EXTRA_TICKS = 20;    // giữ thêm 1 giây ở tối đa thì tự thả
    public static final double CHARGE_MIN_COEF = 6.0, CHARGE_MAX_COEF = 24.0; // 2026-11-05: x3
    public static final int CHARGE_SLOW_AMP = 4;        // MOVEMENT_SLOWDOWN amp 4 (~-75%, gần -80% nhất) cho quái trong vòng tụ lực
    public static final int CHARGE_SLOW_TICKS = 10;     // mỗi lần làm mới kéo dài 0,5 giây
    public static final int CHARGE_REFRESH_TICKS = 4;   // vẽ lại vòng và làm chậm mỗi 4 tick
    public static final int GIANT_EXHAUST_TICKS = 60;   // Kiệt Sức 3 giây sau cột sáng
    public static final double BLADE_RANGE = 14;
    public static final double BLADE_RADIUS = 4;
    public static final int BLADE_COUNT = 12;
    public static final int BLADE_INTERVAL_TICKS = 3;  // 12 lưỡi trong ~2 giây
    public static final double BLADE_COEF = 0.45;
    public static final double BLADE_HIT_R = 1.2;
    public static final double DOOM_RANGE = 14;
    public static final double DOOM_RADIUS = 6;
    public static final int DOOM_CHARGE_TICKS = 30;   // 1,5 giây tụ lực
    public static final double DOOM_COEF = 4.0;

    // ---- PHÒNG NGỰ (T + R)
    public static final double PN_WARD_RADIUS = 6;    // Hộ Thân: r6, 8 giây, -30% sát thương
    public static final double HEAL_RADIUS = 6;
    public static final int HEAL_TICKS = 120;         // 6 giây
    public static final int HEAL_TICK_INTERVAL = 20;
    public static final double HEAL_PCT = 0.03;       // 3% máu tối đa / giây
    public static final int HEAL_LUC_PER_SEC = 2;     // +2 Trận Lực / giây
    public static final double SHIELD_RADIUS = 6;
    public static final int SHIELD_TICKS = 160;       // 8 giây
    public static final double SHIELD_PCT = 0.20;     // hấp thụ 20% máu tối đa

    /** Số lồng: 6 + 2 x min(số quái lúc đặt, cap). */
    public static int cageHitLimit(int mobsAtPlace) {
        return CAGE_HIT_BASE + CAGE_HIT_PER_MOB * Math.min(Math.max(0, mobsAtPlace), CAGE_MOB_CAP);
    }

    /** Tỉ lệ tụ lực (0..1) sau {@code held} tick giữ Q. */
    public static double chargeFraction(int held) {
        return Math.min(1.0, Math.max(0, held) / (double) CHARGE_TICKS);
    }

    /** Bán kính vòng tụ lực / cột sáng theo tỉ lệ tụ lực. */
    public static double chargeRadius(double f) { return CHARGE_MIN_R + (CHARGE_MAX_R - CHARGE_MIN_R) * f; }

    /** Hệ số sát thương (nhân Phép lực) theo tỉ lệ tụ lực. */
    public static double chargeCoef(double f) { return CHARGE_MIN_COEF + (CHARGE_MAX_COEF - CHARGE_MIN_COEF) * f; }

    /** Tổng hệ số R lên một mục tiêu nếu trúng đủ 8 nhịp (chỉ để hiển thị/kiểm thử). */
    public static double rTotalCoef() { return R_TICK_COEF * R_HITS; }
}
