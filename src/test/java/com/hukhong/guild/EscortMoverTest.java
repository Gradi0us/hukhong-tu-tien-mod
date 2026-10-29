package com.hukhong.guild;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EscortMoverTest {
    @Test
    void routeEndsAtDestinationWithBoundedSpacing() {
        List<int[]> r = EscortMover.plan(0, 0, 120, -50);
        int[] end = r.get(r.size() - 1);
        assertEquals(120, end[0]);
        assertEquals(-50, end[1]);
        double px = 0, pz = 0;
        for (int[] p : r) {
            assertTrue(Math.hypot(p[0] - px, p[1] - pz) <= EscortMover.STEP + 1.5, "buoc qua dai");
            px = p[0]; pz = p[1];
        }
    }

    @Test
    void waitHysteresis() {
        assertFalse(EscortMover.shouldWait(false, 11.9));
        assertTrue(EscortMover.shouldWait(false, 12.1));
        assertTrue(EscortMover.shouldWait(true, 10.0));
        assertFalse(EscortMover.shouldWait(true, 7.9));
    }

    @Test
    void speedMatchesPlayerWalk() {
        assertEquals(4.3, EscortMover.SPEED * 20, 0.1);
    }
}
