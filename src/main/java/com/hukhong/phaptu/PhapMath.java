package com.hukhong.phaptu;

import com.hukhong.core.Aptitude;

/**
 * Toán thuần của Pháp Tu (test được, dùng chung server + BalanceSimTest).
 * Mọi giá trị bị động liệt kê trong yêu cầu là mức SSS (tối đa); tư chất D..SSS co giãn TUYẾN TÍNH:
 * frac(D) = 1/7 ... frac(SSS) = 1 (giá trị = max x frac), còn khoảng [min, max] dùng t(D) = 0 ... t(SSS) = 1.
 */
public final class PhapMath {
    private PhapMath() {}

    // ---- tư chất
    public static double frac(Aptitude a) { return (a.ordinal() + 1) / 7.0; }

    public static double t(Aptitude a) { return a.ordinal() / 6.0; }

    public static double lerp(double min, double max, Aptitude a) { return min + (max - min) * t(a); }

    // ---- Quang: Quang Mang
    public static final int QUANG_STACKS = 15;
    public static final double QUANG_EXPLODE_MAX = 0.40;      // % sát thương đã gây trong 15 đòn (SSS)
    public static final int QUANG_FREE_WINDOW_TICKS = 400;    // 20s để dùng hết vòng kỹ năng miễn phí
    public static double quangExplode(Aptitude a) { return QUANG_EXPLODE_MAX * frac(a); }

    // ---- Lôi: Lôi Ấn
    public static final int LOI_STACKS = 5;
    public static final double LOI_HP_PCT_MAX = 0.05;         // % máu tối đa mục tiêu (SSS)
    public static final double LOI_STUN_MIN = 1.0, LOI_STUN_MAX = 2.5;
    /** Trần sát thương nổ tính theo Phép lực của người dùng (để % máu không giết boss trong vài nhịp). */
    public static final double LOI_DMG_CAP_MULT = 25;
    public static double loiHpPct(Aptitude a) { return LOI_HP_PCT_MAX * frac(a); }
    public static double loiStunSec(Aptitude a) { return lerp(LOI_STUN_MIN, LOI_STUN_MAX, a); }
    /** Choáng toàn phần với quái nhỏ / boss thường; boss thế giới (hukhong_world_boss) chỉ nửa thời gian. */
    public static double stunFor(double sec, boolean worldBoss) { return worldBoss ? sec * 0.5 : sec; }
    public static float loiExplodeDamage(Aptitude a, double targetMaxHp, double spellPower) {
        return (float) Math.min(loiHpPct(a) * targetMaxHp, LOI_DMG_CAP_MULT * spellPower);
    }

    // ---- Hỏa: Thiêu Nhiên (thiết kế: không có trong yêu cầu gốc)
    public static final int HOA_STACKS = 5;
    public static final int HOA_DURATION_TICKS = 80;
    public static final int HOA_TICK = 10;                     // nhịp cháy
    public static final double HOA_BURN_COEF = 0.16;           // x Phép lực / giây / tầng (SSS)
    public static final int HOA_SPREAD_TICKS = 40;             // đủ 5 tầng: lây 2 tầng cho tối đa 3 quái trong 4 block mỗi 2s
    public static double hoaBurnPerSecond(Aptitude a, int stacks, double spellPower) {
        return HOA_BURN_COEF * (0.35 + 0.65 * frac(a)) * stacks * spellPower;
    }

    // ---- Thủy: Thủy Ấn
    public static final int THUY_MAX_STACKS = 15;
    public static final int THUY_PULSE_TICKS = 15;             // 0,75s
    public static final double THUY_IMMUNE_MAX = 0.30;         // tầng 3 (SSS); mỗi tầng 1/3
    public static final double THUY_HP_PCT_MAX = 0.60;         // % máu tối đa của chính mình (SSS)
    public static final double THUY_DMG_CAP_MULT = 35;
    public static final double THUY_RADIUS = 6.0;
    public static int thuyTier(int stacks) { return Math.max(0, Math.min(3, stacks / 5)); }
    public static double thuyImmunity(Aptitude a, int stacks) { return THUY_IMMUNE_MAX * frac(a) * thuyTier(stacks) / 3.0; }
    public static float thuyBurstDamage(Aptitude a, double ownMaxHp, double spellPower) {
        return (float) Math.min(THUY_HP_PCT_MAX * frac(a) * ownMaxHp, THUY_DMG_CAP_MULT * spellPower);
    }

    // ---- Thổ: Thổ Tháp
    public static final double THO_DMG_MAX = 0.40, THO_HP_MAX = 0.80;
    public static final int THO_MAX_TURRETS = 4, THO_SHOT_TICKS = 16, THO_LIFE_TICKS = 1200, THO_TAUNT_RANGE = 12;
    /** Một phát bắn của tháp = THO_SHOT_COEF x Phép lực x % (tháp gây % sát thương của chủ). */
    public static final double THO_SHOT_COEF = 1.25;
    public static double thoDamagePct(Aptitude a) { return THO_DMG_MAX * frac(a); }
    public static double thoHpPct(Aptitude a) { return THO_HP_MAX * frac(a); }
    public static int thoMaxTurrets(Aptitude a) { return 1 + (int) Math.round((THO_MAX_TURRETS - 1) * t(a)); }
    public static float thoShotDamage(Aptitude a, double spellPower) { return (float) (thoDamagePct(a) * THO_SHOT_COEF * spellPower); }
    public static float thoTurretHp(Aptitude a, double ownMaxHp) { return (float) Math.max(4, thoHpPct(a) * ownMaxHp); }

    // ---- Kim: Kim Cương
    public static final double KIM_BONUS_MAX = 0.35, KIM_COOLDOWN_MULT = 1.30, KIM_SPEED_EFFECT = 0.25;
    public static double kimBonus(Aptitude a) { return KIM_BONUS_MAX * frac(a); }
    /** Phần buff tốc độ còn lại cho Kim (giảm 75%). */
    public static double kimSpeed(double buff) { return buff * KIM_SPEED_EFFECT; }
    public static double cooldownMult(PhapElement e) { return e == PhapElement.KIM ? KIM_COOLDOWN_MULT : 1.0; }

    // ---- kỹ năng: tổng hệ số x Phép lực lên MỘT mục tiêu cho mỗi phím Q/E/R/T (cân bằng bởi BalanceSimTest)
    private static final double[][] COEF = {
            /* QUANG */ {0.52, 1.19, 2.51, 7.3},
            /* LOI   */ {0.57, 1.26, 2.84, 8.1},
            /* HOA   */ {0.43, 0.92, 2.05, 6.2},
            /* THUY  */ {0.59, 0.86, 2.28, 7.6},
            /* THO   */ {0.48, 0.00, 1.95, 4.6},
            /* KIM   */ {0.63, 1.44, 3.32, 9.8},
    };

    public static double coef(PhapElement e, int slot) { return COEF[e.ordinal()][Math.max(0, Math.min(3, slot))]; }

    /** Hệ số nhân sát thương gây ra từ bị động (Quang Mang nổ ~ +40% x frac x phần đòn Quang; Kim +35% x frac). */
    public static double damageMult(PhapElement e, Aptitude a) {
        return switch (e) {
            case QUANG -> 1 + quangExplode(a) * 0.9;
            case KIM -> 1 + kimBonus(a);
            default -> 1.0;
        };
    }

    /** Sát thương thêm mỗi giây từ bị động trong giao chiến đơn mục tiêu: Hỏa cháy 5 tầng, Thổ tháp, Lôi nổ, Thủy xung. */
    public static double passiveDps(PhapElement e, Aptitude a, double spellPower) {
        return switch (e) {
            case HOA -> hoaBurnPerSecond(a, HOA_STACKS, spellPower) * 0.7;
            case THO -> thoMaxTurrets(a) * thoShotDamage(a, spellPower) * (20.0 / THO_SHOT_TICKS) * 0.75;
            case LOI -> Math.min(loiHpPct(a) * 400, LOI_DMG_CAP_MULT * spellPower) / 3.0;
            case THUY -> 0.30 * spellPower * frac(a);
            default -> 0;
        };
    }
}
