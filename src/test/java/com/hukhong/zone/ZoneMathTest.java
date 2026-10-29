package com.hukhong.zone;

import com.hukhong.city.CityDef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZoneMathTest {
    private static int[][] cities(int sx, int sz) {
        int[][] c = new int[CityDef.ALL.size()][];
        for (CityDef d : CityDef.ALL) c[d.index()] = new int[]{sx + d.ax(), sz + d.az()};
        return c;
    }

    @Test
    void levelByRadiusBands() {
        assertEquals(20, ZoneMath.levelForRadius(120));
        assertEquals(40, ZoneMath.levelForRadius(250));
        assertEquals(65, ZoneMath.levelForRadius(450));
        assertEquals(85, ZoneMath.levelForRadius(700));
        assertEquals(85, ZoneMath.levelForRadius(9999));
        for (int r = 120; r <= 700; r++) {
            assertTrue(ZoneMath.levelForRadius(r) >= ZoneMath.levelForRadius(r - 1 < 120 ? 120 : r - 1), "don dieu " + r);
            assertTrue(ZoneMath.levelForRadius(r) <= 85);
        }
    }

    @Test
    void tiersAndClue() {
        assertEquals(0, ZoneMath.tier(200));
        assertEquals(1, ZoneMath.tier(300));
        assertEquals(2, ZoneMath.tier(600));
        assertEquals("", ZoneMath.tierLabel(0));
        assertEquals("Tinh Anh", ZoneMath.tierLabel(1));
        assertEquals("Cổ", ZoneMath.tierLabel(2));
        assertNull(ZoneMath.clueFor(3, 449));
        assertEquals(ZoneMath.clueFor(3, 500), ZoneMath.clueFor(3, 700));
        for (int id = 1; id < 30; id++) assertTrue(ZoneMath.clueFor(id, 500).matches("c([1-9]|10)"));
    }

    @Test
    void placementAvoidsCitiesSpawnAndEachOther() {
        for (long seed = 1; seed <= 40; seed++) {
            int sx = (int) (seed * 37 % 200) - 100, sz = (int) (seed * 91 % 200) - 100;
            int[][] cities = cities(sx, sz);
            List<ZoneMath.Spec> z = ZoneMath.place(seed * 7919, sx, sz, cities);
            assertEquals(z, ZoneMath.place(seed * 7919, sx, sz, cities), "tat dinh");
            assertTrue(z.size() >= 8 && z.size() <= 14, "so vung " + z.size());
            for (ZoneMath.Spec a : z) {
                assertTrue(a.radius() >= 120 && a.radius() <= 700);
                assertEquals(ZoneMath.levelForRadius(a.radius()), a.level());
                assertTrue(Math.hypot(a.cx() - sx, a.cz() - sz) >= a.radius() + ZoneMath.SPAWN_CLEAR, "spawn");
                for (int[] c : cities) assertTrue(Math.hypot(a.cx() - c[0], a.cz() - c[1]) >= a.radius() + ZoneMath.CITY_CLEAR, "thanh");
                for (ZoneMath.Spec b : z) if (a != b) assertTrue(Math.hypot(a.cx() - b.cx(), a.cz() - b.cz()) >= a.radius() + b.radius(), "de nhau");
            }
        }
    }

    @Test
    void desiredRadiusGrowsWithDistance() {
        assertTrue(ZoneMath.desiredRadius(12000, 0.5) > ZoneMath.desiredRadius(1000, 0.5));
        assertEquals(120, ZoneMath.desiredRadius(0, 0));
        assertTrue(ZoneMath.desiredRadius(99999, 1) <= 700);
    }

    @Test
    void coversRespectsCleansedRing() {
        assertTrue(ZoneMath.covers(100, 0, 0, 0, 200, 0));
        assertFalse(ZoneMath.covers(250, 0, 0, 0, 200, 0));
        assertFalse(ZoneMath.covers(50, 0, 0, 0, 200, 100), "tam da sach");
        assertTrue(ZoneMath.covers(150, 0, 0, 0, 200, 100));
    }
}
