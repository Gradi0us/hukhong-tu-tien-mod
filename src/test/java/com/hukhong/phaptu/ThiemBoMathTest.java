package com.hukhong.phaptu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThiemBoMathTest {
    @Test void drainIs8PercentPerSecond() {
        assertEquals(80.0, ThiemBoMath.drainPerTick(1000, 1.0) * 20, 1e-9);
        assertEquals(40.0, ThiemBoMath.drainPerTick(1000, 0.5) * 20, 1e-9);
    }

    @Test void minStartMana() {
        assertEquals(100, ThiemBoMath.minStartMana(1000));
        assertEquals(1, ThiemBoMath.minStartMana(3));
    }

    @Test void holdVsTap() {
        assertFalse(ThiemBoMath.isHold(ThiemBoMath.HOLD_TICKS - 1));
        assertTrue(ThiemBoMath.isHold(ThiemBoMath.HOLD_TICKS));
    }

    @Test void safeDistClampsToWallAndMax() {
        assertEquals(ThiemBoMath.DASH_DIST, ThiemBoMath.safeDist(50), 1e-9);
        assertEquals(2.5, ThiemBoMath.safeDist(3.0), 1e-9);
        assertEquals(0, ThiemBoMath.safeDist(0.2), 1e-9);
        assertTrue(ThiemBoMath.impulse(ThiemBoMath.DASH_DIST) > 1.5);
        assertEquals(0, ThiemBoMath.impulse(-1), 1e-9);
    }

    @Test void constants() {
        assertEquals(60, ThiemBoMath.IMMUNE_TICKS);
        assertEquals(1.2, ThiemBoMath.SPEED_BONUS, 1e-9);
        assertTrue(ThiemBoMath.COOLDOWN > ThiemBoMath.IMMUNE_TICKS);
    }
}
