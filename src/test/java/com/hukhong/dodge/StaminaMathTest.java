package com.hukhong.dodge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StaminaMathTest {
    @Test
    void maxAndCostScaleWithRealm() {
        assertEquals(100f, StaminaMath.max(1));
        assertTrue(StaminaMath.max(50) > StaminaMath.max(10));
        assertEquals(196f, StaminaMath.max(100));
        assertEquals(30f, StaminaMath.dodgeCost(1));
        assertTrue(StaminaMath.dodgeCost(80) < StaminaMath.dodgeCost(10));
        assertEquals(18f, StaminaMath.dodgeCost(100));
        assertTrue(StaminaMath.dodgeSpeed(80) > StaminaMath.dodgeSpeed(1));
        assertTrue(StaminaMath.dodgeSpeed(100) <= 1.6);
    }

    @Test
    void regenPausesThenRefills() {
        float max = 100f;
        assertEquals(50f, StaminaMath.tick(50f, max, StaminaMath.REGEN_DELAY - 1, false));
        assertEquals(51.5f, StaminaMath.tick(50f, max, StaminaMath.REGEN_DELAY, false), 1e-4);
        assertEquals(max, StaminaMath.tick(99.9f, max, 100, false));
        float v = 0f;
        int t = 0;
        while (v < max && t < 1000) { v = StaminaMath.tick(v, max, StaminaMath.REGEN_DELAY + t, false); t++; }
        assertTrue(t <= 80, "đầy trong <= 4 giây, thực tế " + t + " tick");
    }

    @Test
    void sprintDrainsAndClampsAtZero() {
        assertEquals(10f - StaminaMath.SPRINT_DRAIN, StaminaMath.tick(10f, 100f, 999, true), 1e-5);
        assertEquals(0f, StaminaMath.tick(0.05f, 100f, 999, true));
    }

    @Test
    void iframeAndPerfectWindows() {
        long s = 100;
        assertFalse(StaminaMath.inIFrames(99, s));
        assertTrue(StaminaMath.inIFrames(100, s));
        assertTrue(StaminaMath.inIFrames(100 + StaminaMath.IFRAMES - 1, s));
        assertFalse(StaminaMath.inIFrames(100 + StaminaMath.IFRAMES, s));
        assertTrue(StaminaMath.inPerfect(100 + StaminaMath.PERFECT_WINDOW - 1, s));
        assertFalse(StaminaMath.inPerfect(100 + StaminaMath.PERFECT_WINDOW, s));
        assertTrue(StaminaMath.PERFECT_WINDOW < StaminaMath.IFRAMES);
    }

    @Test
    void directionMatchesMoveRelative() {
        // yaw 0 = nhìn +Z: tới = +Z, lùi (không input) = -Z, trái (strafe +) = +X
        double[] f = StaminaMath.direction(1f, 0f, 0f);
        assertEquals(0, f[0], 1e-6); assertEquals(1, f[1], 1e-6);
        double[] b = StaminaMath.direction(0f, 0f, 0f);
        assertEquals(-1, b[1], 1e-6);
        double[] l = StaminaMath.direction(0f, 1f, 0f);
        assertEquals(1, l[0], 1e-6);
        double[] d = StaminaMath.direction(1f, 1f, 90f);
        assertEquals(1, Math.hypot(d[0], d[1]), 1e-6);
        assertEquals(0, StaminaMath.style(1f, 0f));
        assertEquals(1, StaminaMath.style(0f, 1f));
        assertEquals(2, StaminaMath.style(0f, 0f));
    }
}
