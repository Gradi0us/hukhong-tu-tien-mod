package com.hukhong.phaptu;

/** Toán thuần của Quang E "Thiểm Bộ": giữ E = chạy tia sáng, bấm nhanh E = lùi + Bá Thể. */
public final class ThiemBoMath {
    private ThiemBoMath() {}

    /** Giữ E: +120% tốc độ di chuyển (ADD_MULTIPLIED_TOTAL). */
    public static final double SPEED_BONUS = 1.2;
    /** Giữ E: tăng chiều cao bước chân (block). */
    public static final double STEP_BONUS = 1.0;
    /** Giữ E: hao 8% Pháp Lực tối đa mỗi giây. */
    public static final double DRAIN_PCT = 0.08;
    /** Pháp Lực tối thiểu (tỉ lệ tối đa) để bắt đầu chạy. */
    public static final double MIN_START_PCT = 0.10;
    /** Giữ từ 5 tick (0,25s) trở lên = giữ; ngắn hơn = bấm nhanh. */
    public static final int HOLD_TICKS = 5;
    /** Bấm nhanh: lùi tối đa (block). */
    public static final double DASH_DIST = 6.5;
    /** Bấm nhanh: Bá Thể miễn nhiễm mọi sát thương 3s. */
    public static final int IMMUNE_TICKS = 60;
    /** Hồi chiêu bấm nhanh: 12s (các nguyên tố khác vẫn 8s) vì miễn thương 3s. */
    public static final int COOLDOWN = 240;
    public static final int COST = 25;

    public static double drainPerTick(int maxMana, double costFactor) {
        return maxMana * DRAIN_PCT / 20.0 * costFactor;
    }

    public static int minStartMana(int maxMana) { return (int) Math.ceil(maxMana * MIN_START_PCT); }

    public static boolean isHold(int heldTicks) { return heldTicks >= HOLD_TICKS; }

    /** Xung vận tốc ngang để đi khoảng dist block (ma sát mặt đất ~0,546/tick -> tổng ~2,2 x v, cộng đà không khí). */
    public static double impulse(double dist) { return Math.max(0, dist) * 0.3; }

    /** Khoảng lùi an toàn: trừ biên 0,5 block sát tường. */
    public static double safeDist(double clearDist) { return Math.max(0, Math.min(DASH_DIST, clearDist - 0.5)); }

    // ---- Lôi Trận (Pháp Lôi, giữ E): cùng tốc độ/tiêu hao như Thiểm Bộ + cường hoá thân thể
    /** Giảm sát thương nhận khi đang Lôi Trận. */
    public static final double LOI_DR = 0.20;
    /** Mỗi LOI_ZAP_TICKS: sét nhỏ đánh kẻ địch gần nhất trong LOI_ZAP_RANGE ô, LOI_ZAP_COEF x Phép lực. */
    public static final int LOI_ZAP_TICKS = 10;
    public static final double LOI_ZAP_RANGE = 4.0, LOI_ZAP_COEF = 0.30;
    public static double loiZapDamage(double spellPower) { return LOI_ZAP_COEF * spellPower; }
}
