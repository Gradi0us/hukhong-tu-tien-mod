package com.hukhong.dodge;

import com.hukhong.core.Realm;

/** Toán Thể Lực + Lách Né (thuần, unit test được). Đơn vị thời gian = tick. */
public final class StaminaMath {
    private StaminaMath() {}

    /** Dừng hồi sau khi vừa tiêu (~1 giây). */
    public static final int REGEN_DELAY = 20;
    /** Số tick bất khả xâm phạm sau khi lách. */
    public static final int IFRAMES = 8;
    /** Cửa sổ né hoàn hảo: đòn trúng trong N tick đầu. */
    public static final int PERFECT_WINDOW = 4;
    /** Hồi chiêu giữa hai lần lách. */
    public static final int COOLDOWN = 12;
    /** Chạy nhanh tiêu mỗi tick (2,4/giây). */
    public static final float SPRINT_DRAIN = 0.12f;
    /** Thời gian buff Thân Pháp (tick). */
    public static final int THAN_PHAP_TICKS = 80;

    public static int realm(int level) { return Realm.indexOf(Math.max(0, level)); }

    /** Thể Lực tối đa: 100 (Phàm) .. 196 (Thần). */
    public static float max(int level) { return 100f + 8f * realm(level); }

    /** Hồi mỗi tick: 1,5% tối đa (đầy từ 0 trong ~3,3 giây). */
    public static float regenPerTick(float max) { return max * 0.015f; }

    /** Chi phí lách: 30 -> 18 theo cảnh giới. */
    public static float dodgeCost(int level) { return Math.max(18f, 30f - realm(level)); }

    /** Tốc độ lao (block/tick ban đầu): 0,9 -> 1,6 theo cảnh giới. */
    public static double dodgeSpeed(int level) { return Math.min(1.6, 0.9 + 0.06 * realm(level)); }

    /** Một tick thể lực: đang chạy nhanh thì tiêu, nếu đã đủ trễ thì hồi; kẹp [0,max]. */
    public static float tick(float cur, float max, long sinceSpend, boolean sprinting) {
        float v;
        if (sprinting) v = cur - SPRINT_DRAIN;
        else if (sinceSpend >= REGEN_DELAY) v = cur + regenPerTick(max);
        else v = cur;
        return Math.max(0f, Math.min(max, v));
    }

    public static boolean inIFrames(long now, long dodgeStart) {
        return dodgeStart >= 0 && now >= dodgeStart && now - dodgeStart < IFRAMES;
    }

    public static boolean inPerfect(long now, long dodgeStart) {
        return dodgeStart >= 0 && now >= dodgeStart && now - dodgeStart < PERFECT_WINDOW;
    }

    /**
     * Hướng lao (x,z đã chuẩn hóa) theo input và yaw (độ) — cùng công thức Entity.moveRelative.
     * Không có input -> lùi lại (backstep).
     */
    public static double[] direction(float forward, float strafe, float yawDeg) {
        if (Math.abs(forward) + Math.abs(strafe) < 0.01f) { forward = -1f; strafe = 0f; }
        double len = Math.sqrt(forward * forward + strafe * strafe);
        double f = forward / len, s = strafe / len;
        double r = Math.toRadians(yawDeg), sin = Math.sin(r), cos = Math.cos(r);
        return new double[]{s * cos - f * sin, f * cos + s * sin};
    }

    /** 0 = lăn tới, 1 = lướt ngang, 2 = lùi lại. */
    public static int style(float forward, float strafe) {
        if (forward > 0.1f) return 0;
        if (forward < -0.1f || Math.abs(strafe) < 0.1f) return 2;
        return 1;
    }
}
