package com.hukhong.age;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgeMathTest {
    @Test
    void lifespanTableAndBonusCap() {
        assertEquals(50, AgeMath.lifespan(0, 0));
        assertEquals(80, AgeMath.lifespan(1, 0));
        assertEquals(75, AgeMath.lifespan(0, 25));
        assertEquals(75, AgeMath.lifespan(0, 999));   // cap 50%
        assertEquals(AgeMath.UNLIMITED, AgeMath.lifespan(12, 0));
    }

    @Test
    void stagesStopAtFiveNeverBeyond() {
        assertEquals(0, AgeMath.stage(79, 80));
        assertEquals(1, AgeMath.stage(80, 80));
        assertEquals(1, AgeMath.stage(81, 80));
        assertEquals(2, AgeMath.stage(82, 80));
        assertEquals(5, AgeMath.stage(88, 80));
        assertEquals(5, AgeMath.stage(10_000, 80));
        assertEquals(0, AgeMath.stage(10_000, AgeMath.UNLIMITED));
    }

    @Test
    void warnLevelsAndKeys() {
        assertEquals(0, AgeMath.warnLevel(60, 80));
        assertEquals(1, AgeMath.warnLevel(70, 80));
        assertEquals(2, AgeMath.warnLevel(77, 80));
        assertEquals(3, AgeMath.warnLevel(79, 80));
        assertEquals(4, AgeMath.warnKey(80, 80));
        assertEquals(8, AgeMath.warnKey(95, 80));
    }

    @Test
    void breakthroughClearsSuy() {
        int age = 52;
        assertTrue(AgeMath.stage(age, AgeMath.lifespan(0, 0)) > 0);
        assertEquals(0, AgeMath.stage(age, AgeMath.lifespan(1, 0)));
    }

    @Test
    void nguSuyResourceFlightMeditation() {
        assertEquals(0, AgeMath.maxResourceMult(1));
        assertEquals(-0.10, AgeMath.maxResourceMult(2));
        assertEquals(-0.10, AgeMath.maxResourceMult(5));
        assertFalse(AgeMath.flightBanned(4));
        assertTrue(AgeMath.flightBanned(5));
        assertEquals(1.0, AgeMath.meditationMult(4));
        assertEquals(0.5, AgeMath.meditationMult(5));
    }
}
