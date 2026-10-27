package com.hukhong.client;

import com.hukhong.banmenh.BanMenhMath;
import com.hukhong.client.render.orbit.OrbitMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrbitMathTest {
    @Test
    void easeInOutEndpointsAndMonotonic() {
        assertEquals(0, OrbitMath.easeInOut(0), 1e-9);
        assertEquals(1, OrbitMath.easeInOut(1), 1e-9);
        assertEquals(0.5, OrbitMath.easeInOut(0.5), 1e-9);
        double prev = 0;
        for (int i = 1; i <= 20; i++) { double v = OrbitMath.easeInOut(i / 20.0); assertTrue(v >= prev); prev = v; }
    }

    @Test
    void flightTicksFollowDistance() {
        assertEquals(3, BanMenhMath.flightTicks(1));
        assertTrue(BanMenhMath.flightTicks(12) > BanMenhMath.flightTicks(4));
        assertEquals(14, BanMenhMath.flightTicks(500));
        assertEquals(1, BanMenhMath.strikeSwords(4));
        assertEquals(2, BanMenhMath.strikeSwords(5));
    }
}
