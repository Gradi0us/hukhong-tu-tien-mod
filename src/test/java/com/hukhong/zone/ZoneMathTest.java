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

    @Test
    void nearRingsCloseAndClear() {
        int[][] avoid = {{440, 4080}, {1500, -300}};
        var old = ZoneMath.place(42L, 0, 0, new int[][]{{1500, -300}});
        var near = ZoneMath.nearRings(42L, 0, 0, avoid, old, 100);
        org.junit.jupiter.api.Assertions.assertTrue(near.size() >= 8, "vong gan qua it: " + near.size());
        long close = near.stream().filter(s -> Math.hypot(s.cx(), s.cz()) <= 2200).count();
        org.junit.jupiter.api.Assertions.assertTrue(close >= 3, "it vung trong 2200: " + close);
        for (var s : near) {
            org.junit.jupiter.api.Assertions.assertTrue(Math.hypot(s.cx(), s.cz()) - s.radius() >= ZoneMath.SPAWN_CLEAR - 1, "de len spawn");
            for (int[] a : avoid)
                org.junit.jupiter.api.Assertions.assertTrue(Math.hypot(s.cx() - a[0], s.cz() - a[1]) - s.radius() >= ZoneMath.CITY_CLEAR - 1, "de len vi tri tranh");
            for (var o : old)
                org.junit.jupiter.api.Assertions.assertTrue(Math.hypot(s.cx() - o.cx(), s.cz() - o.cz()) >= s.radius() + o.radius(), "chong vung cu");
            org.junit.jupiter.api.Assertions.assertTrue(s.id() >= 100);
        }
    }
}
