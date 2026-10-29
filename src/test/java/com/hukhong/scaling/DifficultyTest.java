package com.hukhong.scaling;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DifficultyTest {
    @Test void multGrowsAndCaps() {
        assertEquals(1.0, Difficulty.multOf(0), 1e-9);
        assertEquals(1.0 + 50 * 0.015, Difficulty.multOf(50), 1e-9);
        assertEquals(3.0, Difficulty.multOf(10000), 1e-9);
    }

    @Test void damageGrowsSlowerThanHp() {
        double k = Difficulty.multOf(60);
        assertTrue(Difficulty.dmgPart(k) > 1.0 && Difficulty.dmgPart(k) < k);
    }

    @Test void perSecondScalesWithLevel() {
        assertTrue(Difficulty.perSecond(80) > Difficulty.perSecond(1));
    }
}
