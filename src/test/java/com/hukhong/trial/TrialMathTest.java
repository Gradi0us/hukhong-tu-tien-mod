package com.hukhong.trial;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TrialMathTest {
    @Test void gate() { assertFalse(TrialMath.requiresTrial(80)); assertTrue(TrialMath.requiresTrial(90)); assertTrue(TrialMath.requiresTrial(100)); }
    @Test void wavesMonotonic() {
        assertEquals(3, TrialMath.waves(90));
        assertEquals(7, TrialMath.waves(100));
        int prev = 0;
        for (int l = 90; l <= 100; l++) { assertTrue(TrialMath.waves(l) >= prev); prev = TrialMath.waves(l); }
    }
    @Test void damageScales() {
        assertEquals(6f, TrialMath.damage(90, 0), 1e-4);
        assertTrue(TrialMath.damage(99, 0) > TrialMath.damage(90, 0));
        assertTrue(TrialMath.damage(90, 2) > TrialMath.damage(90, 0));
    }
    @Test void strikesAndTelegraph() {
        assertEquals(1, TrialMath.strikesInWave(90, 0));
        assertEquals(3, TrialMath.strikesInWave(99, 2));
        assertEquals(20, TrialMath.telegraphTicks(9));
    }
    @Test void tauHoa() {
        assertEquals(0, TrialMath.tauHoaChance(false, 99));
        assertEquals(0, TrialMath.tauHoaChance(true, 50));
        assertEquals(0.3, TrialMath.tauHoaChance(true, 100), 1e-9);
    }
    @Test void cloneAndPenalty() { assertEquals(1.0, TrialMath.cloneScale(100)); assertTrue(TrialMath.cloneScale(90) < 1.0); assertEquals(2400, TrialMath.penaltyTicks(2)); }
}
