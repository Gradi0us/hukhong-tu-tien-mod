package com.hukhong.phaptu.beam;

import com.hukhong.core.Aptitude;
import com.hukhong.phaptu.PhapElement;
import com.hukhong.phaptu.PhapMath;

/**
 * Toán thuần của Tuyệt Kỹ "Tinh Hà Quang Trụ" (phím T của Pháp Tu nguyên tố Quang/Lôi/Hỏa/Thủy): nạp lực -> sẵn sàng -> kênh tia.
 * Thổ và Kim giữ phím T cũ. Mọi hằng số ở đây là nguồn sự thật duy nhất cho server, HUD và test.
 */
public final class PhapBeamMath {
    private PhapBeamMath() {}

    // ---- thời gian (tick)
    public static final int CHARGE_TICKS = 30;          // giữ T 1,5s
    public static final int READY_TICKS = 100;          // sẵn sàng tối đa 5s
    public static final int FIRE_MAX_TICKS = 80;        // kênh tối đa 4s
    public static final int PULSE_TICKS = 4;            // mỗi 4 tick một nhịp sát thương
    public static final int CANCEL_LOCK_TICKS = 30;     // nhả T sớm: khoá nhẹ 1,5s chống bấm liên tục
    public static final int EXPIRE_LOCK_TICKS = 100;    // để hết 5s sẵn sàng mà không bắn: khoá 5s
    public static final int STACK_ICD_TICKS = 10;       // bị động nguyên tố: tối đa 1 tầng / 0,5s / mục tiêu

    // ---- hình học
    public static final double RANGE = 40.0, RADIUS = 1.6;

    // ---- chi phí / hồi chiêu
    public static final double UPFRONT_PCT = 0.40;      // % Pháp Lực tối đa trả khi bắt đầu bắn
    public static final double DRAIN_PCT_PER_SEC = 0.14; // % Pháp Lực tối đa mỗi giây khi kênh
    public static final int COOLDOWN_TICKS = 900;       // 45s (29/10: giảm từ 75s theo phản hồi)

    // ---- làm chậm di chuyển (ADD_MULTIPLIED_TOTAL)
    public static final double CHARGE_SLOW = -0.60, FIRE_SLOW = -0.85;

    // ---- sát thương
    public static final double APT_MIN = 0.70, APT_MAX = 1.30;
    /** Hệ số nhịp = coef T cũ x PULSE_RATIO: 20 nhịp x 1,30 (SSS) x 0,105 ~ 2,73 lần T cũ. */
    public static final double PULSE_RATIO = 0.17;   // 29/10: x1.62 (0.105 -> 0.17) theo phản hồi "sát thương hơi thấp"

    public static boolean locksT(PhapElement e) {
        return e == PhapElement.QUANG || e == PhapElement.LOI || e == PhapElement.HOA || e == PhapElement.THUY;
    }

    /** Tư chất Pháp Tu D 0,70 ... SSS 1,30 (tuyến tính, PhapMath.lerp). */
    public static double aptMult(Aptitude a) { return PhapMath.lerp(APT_MIN, APT_MAX, a); }

    public static double pulseCoef(PhapElement e) { return PhapMath.coef(e, 3) * PULSE_RATIO; }

    /** Sát thương một nhịp lên MỘT mục tiêu (chưa gồm hệ số Cường Hóa). */
    public static double pulseDamage(PhapElement e, Aptitude a, double spellPower) {
        return spellPower * pulseCoef(e) * aptMult(a);
    }

    public static int pulses(int ticks) { return Math.max(0, ticks / PULSE_TICKS); }

    /** Cả đợt kênh 4s (20 nhịp) lên một mục tiêu. */
    public static double fullChannelDamage(PhapElement e, Aptitude a, double spellPower) {
        return pulses(FIRE_MAX_TICKS) * pulseDamage(e, a, spellPower);
    }

    /** Sát thương T cũ của nguyên tố đó (để so sánh). */
    public static double oldUltDamage(PhapElement e, double spellPower) { return PhapMath.coef(e, 3) * spellPower; }

    public static int upfrontCost(int resourceMax, double costFactor) {
        return (int) Math.max(1, Math.round(resourceMax * UPFRONT_PCT * costFactor));
    }

    public static double drainPerTick(int resourceMax, double costFactor) {
        return resourceMax * DRAIN_PCT_PER_SEC * costFactor / 20.0;
    }

    /** Tổng Pháp Lực cho đợt kênh đủ `ticks` tick. */
    public static double totalCost(int resourceMax, double costFactor, int ticks) {
        return upfrontCost(resourceMax, costFactor) + drainPerTick(resourceMax, costFactor) * ticks;
    }

    /** Tiến độ nạp 0..1. */
    public static float chargeProgress(int age) { return Math.max(0f, Math.min(1f, age / (float) CHARGE_TICKS)); }
}
