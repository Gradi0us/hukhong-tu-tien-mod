package com.hukhong.profession.khi;

/**
 * Cong thuc thoi gian va diem cua Doan Tao Dai (PLAN-v1 §3). Dung chung client va server: server chay lai
 * cung ham tu seed de cham diem tick client gui len (chong gian). Thoi gian = tick ke tu luc bam Nung.
 */
public final class ForgeMath {
    private ForgeMath() {}

    /** Nung: so tick de nhiet tu 0 len 100. */
    public static final int HEAT_TICKS = 200;
    /** Rèn: chu ky day kim (tick) di het mot luot len xuong. */
    public static final int NEEDLE_PERIOD = 60;
    /** Khoang tick toi thieu giua 2 lan bam. */
    public static final int MIN_GAP = 6;
    /** Tick bam duoc lech toi da so voi thoi diem server nhan (chong bam hang loat). */
    public static final int TIME_TOL = 10;

    public static int clampTier(int tier) { return Math.max(1, Math.min(5, tier)); }

    /** Bac 1..5 -> so nhat rèn 5..9. */
    public static int strikes(int tier) { return 4 + clampTier(tier); }

    private static long mix(long x) {
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    /** So ngau nhien deterministic trong [0,1) tu seed va salt. */
    private static double unit(long seed, int salt) {
        return (mix(seed + salt * 0x9E3779B97F4A7C15L) >>> 11) * 0x1.0p-53;
    }

    // ---- 3.1 Nung ----

    /** Nua be rong vung vang (theo nhiet 0..100): bac 1 = 16, moi bac hep them 2, bac 5 = 8. */
    public static int heatHalfWidth(int tier) { return 16 - 2 * (clampTier(tier) - 1); }

    /** Tam vung vang, trong [40, 80]. */
    public static double heatCenter(long seed) { return 40 + unit(seed, 1) * 40; }

    /** Nhiet tai tick {@code e}: tang deu, tran 100. */
    public static double heat(int e) { return Math.max(0, Math.min(100, e * 100.0 / HEAT_TICKS)); }

    /** Diem khi rut phoi: 3 trong vung vang, 2 cach <=4, 1 cach <=10, 0 con lai. */
    public static int heatScore(long seed, int tier, int e) {
        double d = Math.max(0, Math.abs(heat(e) - heatCenter(seed)) - heatHalfWidth(tier));
        if (d <= 0) return 3;
        if (d <= 4) return 2;
        if (d <= 10) return 1;
        return 0;
    }

    // ---- 3.2 Rèn ----

    /** Vi tri kim 0..100 tai tick {@code e} (song tam giac, lech pha theo seed). */
    public static double needle(long seed, int e) {
        int half = NEEDLE_PERIOD / 2;
        int ph = Math.floorMod(e + (int) (unit(seed, 2) * NEEDLE_PERIOD), NEEDLE_PERIOD);
        return ph < half ? ph * 100.0 / half : (NEEDLE_PERIOD - ph) * 100.0 / half;
    }

    /** Tam vung sang cua nhat thu {@code k} (0-based), trong [15, 85]. */
    public static double strikeCenter(long seed, int k) { return 15 + unit(seed, 16 + k) * 70; }

    /** Nua be rong vung sang: bac hep dan (18 -> 10), cap nghe mo rong (+1 moi 4 cap). */
    public static int strikeHalfWidth(int tier, int level) {
        return Math.max(3, 18 - 2 * (clampTier(tier) - 1) + Math.max(0, level) / 4);
    }

    /** Diem 1 nhat: 2 = Hoan hao (trong nua vung sang), 1 = Tot (trong vung sang), 0 = Truot. */
    public static int strikeScore(long seed, int tier, int level, int k, int e) {
        double d = Math.abs(needle(seed, e) - strikeCenter(seed, k));
        int hw = strikeHalfWidth(tier, level);
        if (d <= hw / 2.0) return 2;
        if (d <= hw) return 1;
        return 0;
    }

    /** Diem rèn quy ve thang 0..6 (tong 2 diem moi nhat, chia theo so nhat). */
    public static int hammerPts(int sum, int n) { return (int) Math.round(sum * 3.0 / Math.max(1, n)); }

    // ---- 3.4 Phẩm ----

    /** Tong diem -> phẩm 0..3 (Hạ, Trung, Thượng, Cực). Tong toi da 14. */
    public static int quality(int total) {
        if (total <= 4) return 0;
        if (total <= 7) return 1;
        if (total <= 10) return 2;
        return 3;
    }
}
