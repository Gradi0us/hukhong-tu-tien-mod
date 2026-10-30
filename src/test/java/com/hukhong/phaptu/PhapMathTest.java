package com.hukhong.phaptu;

import com.hukhong.core.Aptitude;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tư chất D..SSS co giãn tuyến tính; giá trị liệt kê trong thiết kế là mức SSS. */
class PhapMathTest {
    private static final double EPS = 1e-9;

    @Test
    void fracIsLinearFromOneSeventhToOne() {
        assertEquals(1.0 / 7, PhapMath.frac(Aptitude.D), EPS);
        assertEquals(1.0, PhapMath.frac(Aptitude.SSS), EPS);
        double step = PhapMath.frac(Aptitude.C) - PhapMath.frac(Aptitude.D);
        for (int i = 1; i < Aptitude.values().length; i++)
            assertEquals(step, PhapMath.frac(Aptitude.values()[i]) - PhapMath.frac(Aptitude.values()[i - 1]), EPS);
        assertEquals(0.0, PhapMath.t(Aptitude.D), EPS);
        assertEquals(1.0, PhapMath.t(Aptitude.SSS), EPS);
    }

    @Test
    void sssHitsListedMaximums() {
        Aptitude a = Aptitude.SSS;
        assertEquals(0.40, PhapMath.quangExplode(a), EPS);       // Quang: nổ tối đa 40%
        assertEquals(0.05, PhapMath.loiHpPct(a), EPS);           // Lôi: 5% máu tối đa
        assertEquals(2.5, PhapMath.loiStunSec(a), EPS);          // Lôi: choáng 1–2,5s
        assertEquals(60.0, PhapMath.thuyBurstDamage(a, 100, 1e9), 1e-3); // Thủy: 60% máu bản thân
        assertEquals(0.40, PhapMath.thoDamagePct(a), EPS);       // Thổ: tháp 40% sát thương
        assertEquals(0.80, PhapMath.thoHpPct(a), EPS);           // Thổ: 80% máu
        assertEquals(4, PhapMath.thoMaxTurrets(a));              // Thổ: 4 tháp ở SSS
        assertEquals(0.35, PhapMath.kimBonus(a), EPS);           // Kim: +35%
        assertEquals(0.30, PhapMath.thuyImmunity(a, 15), EPS);   // Thủy: tầng 3
    }

    @Test
    void dRankIsOneSeventhOfMax() {
        Aptitude a = Aptitude.D;
        assertEquals(0.40 / 7, PhapMath.quangExplode(a), EPS);
        assertEquals(0.35 / 7, PhapMath.kimBonus(a), EPS);
        assertEquals(1.0, PhapMath.loiStunSec(a), EPS);
        assertEquals(1, PhapMath.thoMaxTurrets(a));
    }

    @Test
    void valuesNeverDecreaseWithAptitude() {
        for (int i = 1; i < Aptitude.values().length; i++) {
            Aptitude lo = Aptitude.values()[i - 1], hi = Aptitude.values()[i];
            assertTrue(PhapMath.quangExplode(hi) > PhapMath.quangExplode(lo));
            assertTrue(PhapMath.loiStunSec(hi) > PhapMath.loiStunSec(lo));
            assertTrue(PhapMath.thoShotDamage(hi, 100) > PhapMath.thoShotDamage(lo, 100));
            assertTrue(PhapMath.thoMaxTurrets(hi) >= PhapMath.thoMaxTurrets(lo));
            assertTrue(PhapMath.hoaBurnPerSecond(hi, 5, 100) > PhapMath.hoaBurnPerSecond(lo, 5, 100));
        }
    }

    @Test
    void turretCountIsOneToFour() {
        for (Aptitude a : Aptitude.values()) {
            int n = PhapMath.thoMaxTurrets(a);
            assertTrue(n >= 1 && n <= 4, a + " -> " + n);
        }
    }

    @Test
    void thuyImmunityHasThreeTiers() {
        assertEquals(0, PhapMath.thuyTier(4));
        assertEquals(1, PhapMath.thuyTier(5));
        assertEquals(2, PhapMath.thuyTier(10));
        assertEquals(3, PhapMath.thuyTier(15));
        assertEquals(0.0, PhapMath.thuyImmunity(Aptitude.SSS, 4), EPS);
        assertEquals(0.10, PhapMath.thuyImmunity(Aptitude.SSS, 5), EPS);
        assertEquals(0.20, PhapMath.thuyImmunity(Aptitude.SSS, 12), EPS);
    }

    @Test
    void stunHalvedOnWorldBossesAndExplosionsAreCapped() {
        assertEquals(2.0, PhapMath.stunFor(2.0, false), EPS);
        assertEquals(1.0, PhapMath.stunFor(2.0, true), EPS);
        // 5% của boss 1.000.000 máu bị chặn bởi trần theo Phép lực
        assertEquals(PhapMath.LOI_DMG_CAP_MULT * 100, PhapMath.loiExplodeDamage(Aptitude.SSS, 1_000_000, 100), 1e-3);
        assertEquals(0.05 * 200, PhapMath.loiExplodeDamage(Aptitude.SSS, 200, 100), 1e-3);
    }

    @Test
    void kimTradesSpeedAndCooldown() {
        assertEquals(0.25, PhapMath.kimSpeed(1.0), EPS); // buff tốc độ kém hiệu quả 75%
        assertTrue(PhapMath.cooldownMult(PhapElement.KIM) > 1.0);
        for (PhapElement e : PhapElement.values()) if (e != PhapElement.KIM) assertEquals(1.0, PhapMath.cooldownMult(e), EPS);
    }

    @Test
    void elementRollIsDeterministicAndRerollDiffers() {
        Set<PhapElement> seen = EnumSet.noneOf(PhapElement.class);
        for (long seed = 0; seed < 400; seed++) {
            PhapElement a = PhapElement.roll(seed, false);
            assertEquals(a, PhapElement.roll(seed, false));
            PhapElement b = PhapElement.roll(seed, true);
            assertEquals(b, PhapElement.roll(seed, true));
            assertNotEquals(a, b, "reroll phải ra nguyên tố khác, seed=" + seed);
            seen.add(a);
        }
        assertEquals(PhapElement.values().length, seen.size(), "mọi nguyên tố đều có thể roll ra");
        for (PhapElement e : PhapElement.values()) assertEquals(e, PhapElement.byId(e.id()));
    }

    // ---------------------------------------------------------------- Thổ rework

    @Test
    void thoSustainedDpsIsWithinBandOfOtherElements() {
        for (Aptitude a : Aptitude.values()) {
            double mean = 0;
            int n = 0;
            for (PhapElement e : PhapElement.values()) {
                if (e == PhapElement.THO) continue;
                mean += PhapMath.sustainedSp(e, a, 150);
                n++;
            }
            mean /= n;
            double r = PhapMath.sustainedSp(PhapElement.THO, a, 150) / mean;
            assertTrue(r >= 0.88 && r <= 1.08, "Tho ratio " + a + " = " + r);
        }
    }

    @Test
    void thoLowAptitudeNoLongerCollapses() {
        // trước rework: tháp D chỉ 1/7 sát thương SSS; giờ sàn 40%
        assertEquals(0.40 * (0.40 + 0.60 / 7), PhapMath.thoDamagePct(Aptitude.D), EPS);
        assertTrue(PhapMath.thoDamagePct(Aptitude.D) > 0.15);
        assertEquals(0.40, PhapMath.thoDamagePct(Aptitude.SSS), EPS);
        assertEquals(0.80, PhapMath.thoHpPct(Aptitude.SSS), EPS);
        assertTrue(PhapMath.thoHpPct(Aptitude.D) >= 0.40);
    }

    @Test
    void thoTurretsFireSlowerWhenCrowded() {
        assertEquals(PhapMath.THO_SHOT_TICKS, PhapMath.thoShotTicks(1));
        for (int n = 2; n <= 4; n++) assertTrue(PhapMath.thoShotTicks(n) > PhapMath.thoShotTicks(n - 1));
    }

    @Test
    void thoShieldScalesWithSpellPowerAndIsCappedByHp() {
        assertEquals(PhapMath.THO_SHIELD_COEF * 50, PhapMath.thoShield(50, 1000), 1e-3);
        assertEquals(PhapMath.THO_SHIELD_HP_CAP * 100, PhapMath.thoShield(1e6, 100), 1e-3);
        assertTrue(PhapMath.thoShield(100, 1000) > PhapMath.thoShield(50, 1000));
    }

    @Test
    void thoFortitudeReductionRisesWithTurretsAndStillness() {
        Aptitude a = Aptitude.SSS;
        assertEquals(0.0, PhapMath.thoReduction(a, false, 0), EPS);
        assertEquals(PhapMath.THO_DR_STILL, PhapMath.thoReduction(a, true, 0), EPS);
        assertTrue(PhapMath.thoReduction(a, false, 3) > PhapMath.thoReduction(a, false, 1));
        assertTrue(PhapMath.thoReduction(a, true, 4) > PhapMath.thoReduction(a, false, 4));
        assertEquals(PhapMath.thoReduction(a, true, 4), PhapMath.thoReduction(a, true, 99), EPS); // tối đa 4 tháp
        assertTrue(PhapMath.thoReduction(a, true, 4) < 0.35, "giảm sát thương không quá 35%");
        assertTrue(PhapMath.thoReduction(Aptitude.D, true, 1) < PhapMath.thoReduction(a, true, 1));
    }

    @Test
    void thoUltimateAndEHaveRealDamage() {
        // T Thổ: 3 đợt x 60% + nổ 175% của "sát thương T hiện tại" => tổng lên một mục tiêu >= 6 Phép lực
        assertEquals(3 * 0.6 + 1.75, PhapMath.THO_T_TOTAL, EPS);
        assertTrue(PhapMath.coef(PhapElement.THO, 3) * PhapMath.THO_T_TOTAL >= 6.0);
        assertTrue(PhapMath.coef(PhapElement.THO, 1) > 0, "E không còn 0 sát thương");
        assertEquals(900, PhapMath.THO_T_COOLDOWN_TICKS, "T Thổ hồi 45s");
    }

    @Test
    void thoLaserRampsFrom30To300PercentOverSixSecondsAndClamps() {
        assertEquals(0.30, PhapMath.thoLaserMult(0), EPS);
        assertEquals(3.00, PhapMath.thoLaserMult(PhapMath.THO_LASER_RAMP_TICKS), EPS);
        assertEquals(3.00, PhapMath.thoLaserMult(10_000), EPS);
        assertEquals(0.30, PhapMath.thoLaserMult(-5), EPS);
        assertEquals(1.65, PhapMath.thoLaserMult(PhapMath.THO_LASER_RAMP_TICKS / 2), EPS);
        assertEquals(120, PhapMath.THO_LASER_RAMP_TICKS, "6s");
        // DPS = nhịp x (20 / pulseTicks) = sát thương trụ thường/s x hệ số
        float shot = 10f;
        int shotTicks = 16;
        double dps0 = PhapMath.thoLaserPulse(shot, shotTicks, 0) * 20.0 / PhapMath.THO_LASER_PULSE_TICKS;
        double dpsMax = PhapMath.thoLaserPulse(shot, shotTicks, 999) * 20.0 / PhapMath.THO_LASER_PULSE_TICKS;
        double normal = shot * 20.0 / shotTicks;
        assertEquals(0.30 * normal, dps0, 1e-4);
        assertEquals(3.00 * normal, dpsMax, 1e-4);
    }

    @Test
    void thoLocksEAndTOnly() {
        assertEquals(2 | 8, PhapMath.lockMask(PhapElement.THO));
        assertEquals(8, PhapMath.lockMask(PhapElement.QUANG));
        assertEquals(0, PhapMath.lockMask(PhapElement.KIM));
        assertEquals(0, PhapMath.lockMask(null));
    }
}
