package com.hukhong.passive;

import com.hukhong.core.Aptitude;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PassiveMathTest {
    @Test void aptitudeScalesLinearlyToMax() {
        assertEquals(0.20, PassiveMath.kiemSweepBonus(Aptitude.SSS), 1e-9);
        assertEquals(0.20 / 7, PassiveMath.kiemSweepBonus(Aptitude.D), 1e-9);
        assertEquals(0.30, PassiveMath.daoPct(Aptitude.SSS), 1e-9);
        assertEquals(0.20, PassiveMath.maPct(Aptitude.SSS), 1e-9);
        double step = PassiveMath.daoPct(Aptitude.C) - PassiveMath.daoPct(Aptitude.D);
        for (int i = 1; i < Aptitude.values().length; i++)
            assertEquals(step, PassiveMath.daoPct(Aptitude.values()[i]) - PassiveMath.daoPct(Aptitude.values()[i - 1]), 1e-9);
        assertEquals(PassiveMath.daoPct(Aptitude.D), PassiveMath.daoPct(null), 1e-12);
    }

    @Test void bossCapIsOneThird() {
        assertEquals(0.30, PassiveMath.pctOfMaxHp(0.30, false), 1e-9);
        assertEquals(0.10, PassiveMath.pctOfMaxHp(0.30, true), 1e-9);
    }

    @Test void kienGiapTiersAndCap() {
        assertEquals(0, PassiveMath.theTiers(2));
        assertEquals(1, PassiveMath.theTiers(3));
        assertEquals(5, PassiveMath.theTiers(15));
        assertEquals(5, PassiveMath.theTiers(99));
        assertEquals(3.0 * 5, PassiveMath.theArmor(Aptitude.SSS, 15), 1e-9);
        assertEquals(6.0 * 5, PassiveMath.theMaxHp(Aptitude.SSS, 15), 1e-9);
        assertEquals(0, PassiveMath.theArmor(Aptitude.SSS, 2), 1e-9);
        assertTrue(PassiveMath.theArmor(Aptitude.D, 9) < PassiveMath.theArmor(Aptitude.SSS, 9));
    }

    @Test void maSlowScalesToEightyPercent() {
        assertEquals(0, PassiveMath.maSlow(0), 1e-9);
        assertEquals(0.16, PassiveMath.maSlow(1), 1e-9);
        assertEquals(0.80, PassiveMath.maSlow(5), 1e-9);
        assertEquals(0.80, PassiveMath.maSlow(9), 1e-9);
    }

    @Test void trongThuongScalesAndCaps() {
        assertEquals(0.15, PassiveMath.ttAmpPerStack(Aptitude.SSS), 1e-9);
        assertEquals(0.20, PassiveMath.ttExecPct(Aptitude.SSS), 1e-9);
        assertEquals(0.15 / 7, PassiveMath.ttAmpPerStack(Aptitude.D), 1e-9);
        assertEquals(3, PassiveMath.ttClamp(99));
        assertEquals(0, PassiveMath.ttClamp(-2));
        assertEquals(1.45, PassiveMath.ttDamageMult(0.15, 3, false), 1e-9);
        assertEquals(1.15, PassiveMath.ttDamageMult(0.15, 9, true), 1e-9); // boss: 0.05 x 3
        assertEquals(1.0, PassiveMath.ttDamageMult(0.15, 0, false), 1e-9);
    }

    @Test void trongThuongExecuteRules() {
        assertTrue(PassiveMath.ttExecutes(1, 19, 100, 0.20, false, false));
        assertFalse(PassiveMath.ttExecutes(1, 20, 100, 0.20, false, false));
        assertFalse(PassiveMath.ttExecutes(0, 5, 100, 0.20, false, false));
        assertFalse(PassiveMath.ttExecutes(2, 5, 100, 0.20, true, false));
        assertFalse(PassiveMath.ttExecutes(2, 5, 100, 0.20, false, true));
    }
}
