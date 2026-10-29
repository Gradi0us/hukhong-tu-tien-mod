package com.hukhong.passive;

import com.hukhong.core.Aptitude;

/** Toán thuần của nội tại 4 đạo (test được). Giá trị scale tuyến tính theo tư chất: D thấp nhất ... SSS = tối đa. */
public final class PassiveMath {
    private PassiveMath() {}

    // Kiếm
    public static final double KIEM_SWEEP_MAX = 0.20, KIEM_HEAL_MAX = 0.20;
    public static final int KIEM_EVERY = 4;
    // Đạo
    public static final double DAO_MAX = 0.30, BOSS_CAP = 1.0 / 3.0;
    public static final int DAO_EVERY = 5;
    /** Đạo: kỹ năng gây sát thương x1.12 (giảm từ 1.25 theo phản hồi). */
    public static final double DAO_SKILL_MULT = 1.12;
    // Thể
    public static final double THE_ARMOR_MAX = 3.0, THE_HP_MAX = 6.0; // x2 (29/10)
    public static final int THE_PER_TIER = 3, THE_STACK_CAP = 15, THE_HOLD_TICKS = 300;
    // Ma
    public static final double MA_PCT_MAX = 0.20, MA_SLOW_MAX = 0.80, MA_RANGE = 16;
    public static final int MA_EVERY = 5, MA_COOLDOWN_TICKS = 10, MA_MARK_TICKS = 160;

    /** max x (ordinal+1)/số bậc: D = max/7, SSS = max. */
    public static double scale(Aptitude a, double max) {
        Aptitude ap = a == null ? Aptitude.D : a;
        return max * (ap.ordinal() + 1) / (double) Aptitude.values().length;
    }

    public static double kiemSweepBonus(Aptitude a) { return scale(a, KIEM_SWEEP_MAX); }
    public static double kiemHealPct(Aptitude a) { return scale(a, KIEM_HEAL_MAX); }
    public static double daoPct(Aptitude a) { return scale(a, DAO_MAX); }
    public static double maPct(Aptitude a) { return scale(a, MA_PCT_MAX); }

    /** Phần trăm máu tối đa thực gây ra: boss bị chặn ở 1/3. */
    public static double pctOfMaxHp(double pct, boolean boss) { return boss ? pct * BOSS_CAP : pct; }

    public static int theTiers(int stacks) { return Math.min(Math.max(0, stacks), THE_STACK_CAP) / THE_PER_TIER; }
    public static double theArmor(Aptitude a, int stacks) { return theTiers(stacks) * scale(a, THE_ARMOR_MAX); }
    public static double theMaxHp(Aptitude a, int stacks) { return theTiers(stacks) * scale(a, THE_HP_MAX); }

    /** Làm chậm theo tầng nổ: tối đa 80% ở 5 tầng. */
    public static double maSlow(int stacks) { return MA_SLOW_MAX * Math.min(Math.max(0, stacks), MA_EVERY) / MA_EVERY; }
}
