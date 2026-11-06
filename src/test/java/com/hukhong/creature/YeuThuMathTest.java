package com.hukhong.creature;

import com.hukhong.world.yeulam.DaiSamLam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YeuThuMathTest {
    @Test
    void outsideForestNoHundredThousand() {
        double[] w = YeuThuAge.weights(0);
        assertEquals(0, w[4]);
        assertTrue(w[0] > w[1] && w[1] > w[2]);
    }

    @Test
    void coreHasAllAgesAndOlderThanEdge() {
        double[] edge = YeuThuAge.weights(0.1), core = YeuThuAge.weights(1);
        assertTrue(core[4] > 0);
        double sumE = 0, sumC = 0;
        for (double v : edge) sumE += v;
        for (double v : core) sumC += v;
        assertTrue(core[3] / sumC > edge[3] / sumE);
        assertTrue(core[0] / sumC < edge[0] / sumE);
    }

    @Test
    void pickCoversRange() {
        double[] w = {1, 1, 1, 1, 1};
        assertEquals(YeuThuAge.Y10, YeuThuAge.pick(w, 0));
        assertEquals(YeuThuAge.Y100000, YeuThuAge.pick(w, 0.999));
    }

    @Test
    void boneOnlyFromThousandYears() {
        assertEquals(0, YeuThuAge.Y100.boneChance);
        assertTrue(YeuThuAge.Y1000.boneChance > 0);
        assertTrue(YeuThuAge.Y100000.boneChance > YeuThuAge.Y10000.boneChance);
    }

    @Test
    void forestRegionsDeterministicAndAwayFromSpawn() {
        int found = 0;
        for (int gx = -4; gx <= 4; gx++) for (int gz = -4; gz <= 4; gz++) {
            var r = DaiSamLam.cell(12345L, gx, gz);
            assertEquals(r, DaiSamLam.cell(12345L, gx, gz));
            if (r == null) continue;
            found++;
            assertTrue(Math.hypot(r.cx(), r.cz()) >= DaiSamLam.SAFE_SPAWN + r.r());
            assertSame(r.equals(DaiSamLam.at(12345L, r.cx(), r.cz())), true);
            assertEquals(1.0, r.depth(r.cx(), r.cz()), 1e-9);
            assertNull(DaiSamLam.at(12345L, r.cx() + r.r() + 5, r.cz()) == r ? r : null);
        }
        assertTrue(found >= 20, "qua it rung: " + found);
        assertNull(DaiSamLam.at(12345L, 0, 0));
    }
}
