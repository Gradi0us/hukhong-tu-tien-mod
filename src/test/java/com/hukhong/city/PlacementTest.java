package com.hukhong.city;

import com.hukhong.society.SectDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlacementTest {
    @Test
    void deterministicPerSeedAndId() {
        assertArrayEquals(Placement.site(0, 0, 600, 3600, 42L, "a"), Placement.site(0, 0, 600, 3600, 42L, "a"));
        assertFalse(java.util.Arrays.equals(Placement.site(0, 0, 600, 3600, 42L, "a"), Placement.site(0, 0, 600, 3600, 43L, "a")));
    }

    @Test
    void jitterWithinRange() {
        for (long seed = 0; seed < 200; seed++) {
            int[] j = Placement.jitter(seed, "x", Placement.JITTER);
            assertTrue(Math.abs(j[0]) <= Placement.JITTER && Math.abs(j[1]) <= Placement.JITTER);
        }
    }

    @Test
    void allCitiesAndSectsInsideWorldRadius() {
        double max = Placement.WORLD_RADIUS - Placement.BORDER_MARGIN + 1;
        for (long seed : new long[]{1L, 99L, -7L, 123456789L}) {
            for (CityDef d : CityDef.ALL) {
                int[] p = Placement.site(100, -50, d.ax(), d.az(), seed, d.id());
                assertTrue(Math.hypot(p[0] - 100, p[1] + 50) <= max, d.id());
            }
            for (SectDef d : SectDef.ALL) {
                int[] p = Placement.site(100, -50, d.ax(), d.az(), seed, "sect:" + d.id());
                assertTrue(Math.hypot(p[0] - 100, p[1] + 50) <= max, d.id());
            }
        }
    }

    @Test
    void farAnchorIsClampedIntoRadius() {
        int[] p = Placement.site(0, 0, 14000, 14000, 5L, "far");
        assertTrue(Math.hypot(p[0], p[1]) <= Placement.WORLD_RADIUS - Placement.BORDER_MARGIN + 1);
    }

    @Test
    void cityListMatchesCanon() {
        assertEquals(10, CityDef.ALL.stream().map(CityDef::id).distinct().count());
        assertEquals(6, CityDef.ALL.stream().filter(d -> d.tier() == Tier.NHAN).count());
        assertEquals(3, CityDef.ALL.stream().filter(d -> d.tier() == Tier.DIA).count());
        assertEquals(1, CityDef.ALL.stream().filter(d -> d.tier() == Tier.THIEN).count());
        assertEquals("luu_ly_cang", CityDef.ALL.get(0).id());
    }
}
