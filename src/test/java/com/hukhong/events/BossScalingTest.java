package com.hukhong.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossScalingTest {
    @Test void solo_is_baseline() {
        assertEquals(1.0, BossScaling.hpMul(1), 1e-9);
        assertEquals(1.0, BossScaling.dmgMul(1), 1e-9);
        assertEquals(8, BossScaling.waveSize(8, 1));
        assertEquals(1.0, BossScaling.mobHpMul(0), 1e-9);
    }

    @Test void scales_monotonic_and_capped() {
        for (int n = 1; n < BossScaling.MAX_PLAYERS; n++) {
            assertTrue(BossScaling.hpMul(n + 1) > BossScaling.hpMul(n));
            assertTrue(BossScaling.dmgMul(n + 1) > BossScaling.dmgMul(n));
            assertTrue(BossScaling.waveSize(12, n + 1) > BossScaling.waveSize(12, n));
        }
        assertEquals(BossScaling.hpMul(8), BossScaling.hpMul(50), 1e-9);
        assertEquals(6.25, BossScaling.hpMul(8), 1e-9);
        // HP tăng nhanh hơn sát thương: boss không one-shot nhóm đông
        assertTrue(BossScaling.hpMul(4) > BossScaling.dmgMul(4));
    }

    @Test void phases() {
        assertEquals(1, BossScaling.phase(1f));
        assertEquals(1, BossScaling.phase(0.67f));
        assertEquals(2, BossScaling.phase(0.66f));
        assertEquals(2, BossScaling.phase(0.34f));
        assertEquals(3, BossScaling.phase(0.33f));
        assertEquals(3, BossScaling.phase(0f));
    }

    @Test void cleanse_count_grows_with_cap() {
        assertEquals(20, BossScaling.cleanseCount(0));
        assertEquals(25, BossScaling.cleanseCount(1));
        assertEquals(60, BossScaling.cleanseCount(100));
    }
}
