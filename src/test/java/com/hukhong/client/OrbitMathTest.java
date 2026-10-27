package com.hukhong.client;

import com.hukhong.banmenh.BanMenhMath;
import com.hukhong.client.render.orbit.OrbitMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrbitMathTest {
    @Test
    void swordCountPerStage() {
        int[] want = {1, 1, 1, 2, 3, 5, 9};
        for (int s = 0; s <= 6; s++) assertEquals(want[s], BanMenhMath.orbitCount(s));
        assertEquals(1, BanMenhMath.orbitCount(-4));
        assertEquals(9, BanMenhMath.orbitCount(40));
    }

    @Test
    void strikeRangeGrowsFrom8To12() {
        assertEquals(8.0, BanMenhMath.strikeRange(0), 1e-9);
        assertEquals(12.0, BanMenhMath.strikeRange(6), 1e-9);
        assertEquals(12.0, BanMenhMath.strikeRange(99), 1e-9);
    }

    @Test
    void orbitStaysAroundPlayerAndSwordsAreSpread() {
        double[] a = new double[3], b = new double[3];
        for (int count : new int[]{1, 2, 3, 5, 9}) {
            for (int i = 0; i < count; i++) {
                OrbitMath.orbit(i, count, 12.5, a);
                double r = Math.hypot(a[0], a[2]);
                assertTrue(r > 0.8 && r < 1.2, "radius " + r);
                assertTrue(a[1] > 0.5 && a[1] < 1.8, "height " + a[1]);
                for (int j = i + 1; j < count; j++) {
                    OrbitMath.orbit(j, count, 12.5, b);
                    double d = Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
                    assertTrue(d > 0.3, "swords overlap " + i + "/" + j + " of " + count);
                }
            }
        }
    }

    @Test
    void strikeBlendOutAndBack() {
        int d = BanMenhMath.STRIKE_DELAY_TICKS, r = BanMenhMath.STRIKE_RETURN_TICKS;
        assertEquals(-1, OrbitMath.strikeBlend(-1, d, r));
        assertEquals(0, OrbitMath.strikeBlend(0, d, r), 1e-9);
        assertEquals(1, OrbitMath.strikeBlend(d, d, r), 1e-9);   // chạm mục tiêu đúng lúc server áp sát thương
        assertTrue(OrbitMath.strikeBlend(d + r - 0.01, d, r) < 0.01);
        assertEquals(-1, OrbitMath.strikeBlend(d + r, d, r));
    }
}
