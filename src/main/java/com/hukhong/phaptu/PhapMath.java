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
    /** Sàn tư chất: tháp D vẫn có 40% / 50% giá trị SSS (trước đây D chỉ 1/7 -> Thổ tư chất thấp quá yếu). */
    public static final double THO_DMG_FLOOR = 0.40, THO_HP_FLOOR = 0.50;
    /** Tỉ lệ thời gian tháp thật sự bắn trúng (mô hình DPS) và giãn nhịp bắn khi nhiều tháp (tránh SSS x4 vượt trội). */
    public static final double THO_UPTIME = 0.5;
    public static final int THO_CROWD_TICKS = 8;
    public static double thoDamagePct(Aptitude a) { return THO_DMG_MAX * (THO_DMG_FLOOR + (1 - THO_DMG_FLOOR) * frac(a)); }
    public static double thoHpPct(Aptitude a) { return THO_HP_MAX * (THO_HP_FLOOR + (1 - THO_HP_FLOOR) * frac(a)); }
    /** Nhịp bắn (tick) của mỗi tháp khi chủ có n tháp tối đa: càng nhiều tháp càng chậm. */
    public static int thoShotTicks(int n) { return THO_SHOT_TICKS + THO_CROWD_TICKS * Math.max(0, n - 1); }

    // ---- Thổ: Thạch Giáp (E) - khiên hấp thụ + miễn đẩy lùi; nội tại Kiên Cố (giảm sát thương khi đứng yên / theo số tháp)
    public static final double THO_SHIELD_COEF = 2.4, THO_SHIELD_HP_CAP = 0.6;
    public static final int THO_SHIELD_TICKS = 140, THO_STILL_TICKS = 40;
    public static final double THO_DR_STILL = 0.10, THO_DR_PER_TURRET = 0.04;
    public static float thoShield(double spellPower, double maxHp) { return (float) Math.min(THO_SHIELD_COEF * spellPower, THO_SHIELD_HP_CAP * maxHp); }
    /** Kiên Cố: giảm sát thương = (đứng yên >=2s ? 10% : 0) + 4%/tháp (tối đa 4), co giãn (0,5 + 0,5 x tư chất). */
    public static double thoReduction(Aptitude a, boolean still, int turrets) {
        double raw = (still ? THO_DR_STILL : 0) + THO_DR_PER_TURRET * Math.max(0, Math.min(THO_MAX_TURRETS, turrets));
        return raw * (0.5 + 0.5 * frac(a));
    }
    /** Thiên Sơn Áp Đỉnh (T): choáng (giây) và bán kính. */
    public static final double THO_MOUNTAIN_STUN = 1.0, THO_MOUNTAIN_RADIUS = 7.5;

    // ---- Thổ: Hợp Thể (hai trụ cùng chủ cách <= 5 block gộp thành một trụ laser đơn mục tiêu kiểu Inferno Tower)
    public static final double THO_MERGE_RADIUS = 5.0;
    public static final double THO_MERGED_SCALE = 1.6;
    /** Laser: bắt đầu 30% sát thương một trụ thường, tăng tuyến tính tới 300% sau 6s khoá cùng mục tiêu; đổi mục tiêu thì về 30%. */
    public static final double THO_LASER_MIN = 0.30, THO_LASER_MAX = 3.00;
    public static final int THO_LASER_RAMP_TICKS = 120, THO_LASER_PULSE_TICKS = 4;
    public static double thoLaserMult(int lockedTicks) {
        double k = Math.max(0, Math.min(1.0, lockedTicks / (double) THO_LASER_RAMP_TICKS));
        return THO_LASER_MIN + (THO_LASER_MAX - THO_LASER_MIN) * k;
    }
    /** Sát thương một nhịp laser (mỗi THO_LASER_PULSE_TICKS tick) từ sát thương một phát của trụ thường và nhịp bắn thường. */
    public static float thoLaserPulse(float shotDamage, int shotTicks, int lockedTicks) {
        return (float) (shotDamage * (20.0 / Math.max(1, shotTicks)) * thoLaserMult(lockedTicks) * THO_LASER_PULSE_TICKS / 20.0);
    }

    // ---- Thổ E giữ: xuyên tường 5s
    public static final int THO_PHASE_TICKS = 100, THO_PHASE_COOLDOWN_TICKS = 300, THO_PHASE_COST = 25;
    /** Xuyên tường: tốc độ hạ nhẹ khi không bấm Space/Shift (block/tick, > 0,03125 để máy chủ không coi là bay). */
    public static final double THO_PHASE_SINK = 0.04;

    // ---- Thổ T "Thiên Sơn Áp Đỉnh": hồi chiêu 45s, 3 đợt núi rơi x 60%, giữ T dẫn nổ 175% trong ~6s, nổ rút 99% Pháp Lực hiện có
    public static final int THO_T_COOLDOWN_TICKS = 900, THO_T_WAVES = 3, THO_T_WAVE_GAP = 30, THO_T_FALL_DELAY = 18, THO_T_WINDOW_TICKS = 120, THO_T_HOLD_TICKS = 8;
    public static final double THO_T_WAVE_FRAC = 0.60, THO_T_BLAST_MULT = 1.75, THO_T_MANA_DRAIN = 0.99;
    public static final double THO_T_AREA = 9.0, THO_T_IMPACT_RADIUS = 3.8, THO_T_BLAST_RADIUS = 13.0;
    public static final int THO_T_IMPACTS = 5;
    /** Tổng hệ số T Thổ so với "sát thương T hiện tại" nếu mọi đợt + nổ đều trúng cùng mục tiêu. */
    public static final double THO_T_TOTAL = THO_T_WAVES * THO_T_WAVE_FRAC + THO_T_BLAST_MULT;
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
            /* THO   */ {0.50, 0.50, 2.10, 2.00}, // T Thổ: 3 đợt x 60% + nổ 175% = THO_T_TOTAL x hệ số
            /* KIM   */ {0.63, 1.44, 3.32, 9.8},
    };

    /** Mặt nạ phím cố định theo nguyên tố (bit i = phím i): Thổ E+T (2|8), Quang/Lôi/Hỏa/Thủy T (8), Kim không. Dùng cho UI client. */
    public static int lockMask(PhapElement e) {
        if (e == null) return 0;
        if (e == PhapElement.THO) return 2 | 8;
        return com.hukhong.phaptu.beam.PhapBeamMath.locksT(e) ? 8 : 0;
    }

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
            case THO -> thoMaxTurrets(a) * thoShotDamage(a, spellPower) * (20.0 / thoShotTicks(thoMaxTurrets(a))) * THO_UPTIME;
            case LOI -> Math.min(loiHpPct(a) * 400, LOI_DMG_CAP_MULT * spellPower) / 3.0;
            case THUY -> 0.30 * spellPower * frac(a);
            default -> 0;
        };
    }

    /**
     * DPS bền vững lên MỘT mục tiêu tính theo Phép lực (spellPower = 1): tổng hệ số 4 phím / hồi chiêu gốc (Q liên tục, E/R/T theo chu kỳ)
     * x hệ số bị động sát thương, chia hồi chiêu Kim, + bị động. Dùng để so các nguyên tố với nhau (không gồm đòn đánh thường).
     */
    public static double sustainedSp(PhapElement e, Aptitude a, double spellPower) {
        com.hukhong.skill.Skill[] ks = {com.hukhong.skill.Skill.PHAP_1, com.hukhong.skill.Skill.PHAP_2, com.hukhong.skill.Skill.PHAP_3, com.hukhong.skill.Skill.PHAP_ULT};
        double sum = 0;
        for (int i = 0; i < 4; i++) {
            double c = coef(e, i) * (e == PhapElement.THO && i == 3 ? THO_T_TOTAL : 1.0);
            int cdt = e == PhapElement.THO && i == 3 ? THO_T_COOLDOWN_TICKS : ks[i].cooldownTicks;
            sum += c / (cdt / 20.0 * cooldownMult(e));
        }
        return sum * damageMult(e, a) + passiveDps(e, a, spellPower) / spellPower;
    }
}
