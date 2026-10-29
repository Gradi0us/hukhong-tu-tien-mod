package com.hukhong.currency;

/** Gia nang cap Hon Tinh dung chung (ban menh, Linh Bao, Linh Truong, ...): round(base * n^1.6) * 1.5^(so boi cua 10 <= n). */
public final class UpgradeCost {
    private UpgradeCost() {}

    public static final double EXPONENT = 1.6;
    public static final double MILESTONE_MULT = 1.5;
    /** Co so cho giai doan phap khi / ban menh: giai doan 1 = 2 Hon Tinh (bang cu: 2/4/8/12/20/32). */
    public static final long STAGE_BASE = 2;

    public static long honTinh(int targetLevel, long base) {
        int n = Math.max(1, targetLevel);
        double v = Math.round(base * Math.pow(n, EXPONENT)) * Math.pow(MILESTONE_MULT, n / 10);
        return Math.round(v);
    }
}
