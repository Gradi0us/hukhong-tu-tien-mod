package com.hukhong.alchemy;

/** Toán thuần (test được) cho Đan Lô: phẩm chất theo thời gian trong dải lửa, Đan Độc (S7 §1.2, §1.3). */
public final class AlchemyMath {
    private AlchemyMath() {}

    public static final int BREW_TICKS = 400, BAND_CENTER = 55, HEAT_MAX = 100, AMBIENT = 25, PRESS = 6;
    public static final double TICKS_PER_DAY = 24000.0;

    public enum Quality {
        HA(0.8, 1.2), TRUNG(1.0, 1.0), THUONG(1.2, 0.8), CUC(1.5, 0.5);
        public final double effect, toxin;
        Quality(double e, double t) { effect = e; toxin = t; }
        public static Quality of(int ord) { return values()[Math.max(0, Math.min(3, ord))]; }
    }

    /** null = hỏng (phế liệu). */
    public static Quality qualityFor(double ratio) {
        if (ratio >= 0.90) return Quality.CUC;
        if (ratio >= 0.70) return Quality.THUONG;
        if (ratio >= 0.45) return Quality.TRUNG;
        if (ratio >= 0.25) return Quality.HA;
        return null;
    }

    public static int bandHalf(int tier, boolean diHoa) { return Math.max(5, 16 - 2 * tier) + (diHoa ? 4 : 0); }

    public static boolean inBand(int heat, int half) { return Math.abs(heat - BAND_CENTER) <= half; }

    public static int clampHeat(int h) { return Math.max(0, Math.min(HEAT_MAX, h)); }

    /** Lửa nguội dần: -1 mỗi 8 tick (Dị Hỏa: 12 tick). */
    public static int passive(int heat, int tick, boolean diHoa) {
        return (tick % (diHoa ? 12 : 8) == 0) ? clampHeat(heat - 1) : heat;
    }

    public static double toxinAdd(double groupCoef, int tier, Quality q) { return groupCoef * tier * q.toxin; }

    /** Hệ số hiệu quả đan theo Đan Độc (§1.3). */
    public static double effectMult(double tox) {
        if (tox >= 100) return 0.0;
        if (tox > 85) return 0.25;
        if (tox > 60) return 0.5;
        if (tox > 30) return 0.75;
        return 1.0;
    }

    public static double addToxin(double tox, double add) { return Math.min(100.0, tox + add); }

    public static double decay(double tox, int ticks) { return Math.max(0.0, tox - ticks / TICKS_PER_DAY); }
}
