package com.hukhong.events;

/** Toán thuần: co giãn boss/tập kích theo số người chơi gần + pha boss. */
public final class BossScaling {
    private BossScaling() {}

    public static final int MAX_PLAYERS = 8;

    public static int clamp(int n) { return Math.max(1, Math.min(MAX_PLAYERS, n)); }
    /** HP boss: +75% mỗi người thêm. */
    public static double hpMul(int n) { return 1 + 0.75 * (clamp(n) - 1); }
    /** Sát thương boss: +15% mỗi người thêm. */
    public static double dmgMul(int n) { return 1 + 0.15 * (clamp(n) - 1); }
    /** Số quái mỗi đợt tập kích: +40% mỗi người thêm. */
    public static int waveSize(int base, int n) { return (int) Math.ceil(base * (1 + 0.4 * (clamp(n) - 1))); }
    /** Quái tập kích: +25% HP mỗi người thêm. */
    public static double mobHpMul(int n) { return 1 + 0.25 * (clamp(n) - 1); }

    /** Pha boss theo tỉ lệ máu: 1 (&gt;66%), 2 (&gt;33%), 3 (còn lại). */
    public static int phase(float hpFrac) { return hpFrac > 0.66f ? 1 : hpFrac > 0.33f ? 2 : 3; }

    /** Số chunk bị tẩy khi thắng tập kích (tăng nhẹ theo số lần thắng, trần 60). */
    public static int cleanseCount(int invasionsWon) { return Math.min(60, 20 + 5 * Math.max(0, invasionsWon)); }
}
