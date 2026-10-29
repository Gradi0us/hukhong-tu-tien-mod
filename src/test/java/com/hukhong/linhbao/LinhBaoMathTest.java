package com.hukhong.linhbao;

import com.hukhong.banmenh.BanMenhService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LinhBaoMathTest {
    @Test
    void coefTablesCoverAllStagesAndNeverDrop() {
        for (double[] t : new double[][]{LinhBaoMath.DAO_BOLT_COEF, LinhBaoMath.THE_HIT_COEF, LinhBaoMath.MA_HIT_COEF}) {
            assertEquals(7, t.length);
            for (int s = 1; s < 7; s++) assertTrue(t[s] >= t[s - 1], "he so khong duoc giam theo giai doan");
        }
    }

    @Test
    void bonusScalesWithQualityAndStage() {
        assertEquals(100 * LinhBaoMath.THE_HIT_COEF[0] * BanMenhService.QUALITY[0], LinhBaoMath.theHit(100, 0), 1e-9);
        assertEquals(100 * LinhBaoMath.MA_HIT_COEF[6] * BanMenhService.QUALITY[6], LinhBaoMath.maHit(100, 6), 1e-9);
        assertEquals(50 * LinhBaoMath.DAO_BOLT_COEF[4] * BanMenhService.QUALITY[4], LinhBaoMath.daoBolt(50, 4), 1e-9);
        for (int s = 1; s <= 6; s++) {
            assertTrue(LinhBaoMath.theHit(100, s) > LinhBaoMath.theHit(100, s - 1));
            assertTrue(LinhBaoMath.maHit(100, s) > LinhBaoMath.maHit(100, s - 1));
            assertTrue(LinhBaoMath.daoBolt(100, s) > LinhBaoMath.daoBolt(100, s - 1));
        }
        // ngoai bien duoc kep
        assertEquals(LinhBaoMath.theHit(10, 6), LinhBaoMath.theHit(10, 99), 1e-9);
        assertEquals(LinhBaoMath.theHit(10, 0), LinhBaoMath.theHit(10, -3), 1e-9);
    }

    @Test
    void daoHealOnlyFromDiaStageAndLifestealGrows() {
        assertEquals(0, LinhBaoMath.daoHeal(100, 2));
        assertTrue(LinhBaoMath.daoHeal(100, 3) > 0);
        assertTrue(LinhBaoMath.daoHeal(100, 6) > LinhBaoMath.daoHeal(100, 3));
        assertTrue(LinhBaoMath.maLifesteal(6) > LinhBaoMath.maLifesteal(0));
        assertTrue(LinhBaoMath.maLifesteal(6) < 0.2, "hut mau Ma khong duoc vuot tay");
    }

    @Test
    void orbSitsOnRightShoulderAboveHead() {
        // yaw 0 = quay mat ve +z, ben phai = -x
        double[] o = LinhBaoMath.orbOffset(0, 0);
        assertTrue(o[0] < -0.4 && o[1] > 1.8);
        double[] w = LinhBaoMath.orbOffset(90, 0);   // quay ve -x: ben phai = -z
        assertTrue(w[2] < -0.4);
    }

    @Test
    void colorsStayInRgbRange() {
        for (int s = 0; s <= 6; s++) {
            for (int c : new int[]{LinhBaoMath.daoColor(s), LinhBaoMath.theColor(s), LinhBaoMath.maColor(s)}) assertEquals(0, c >>> 24);
        }
        assertEquals(0xB86A28, LinhBaoMath.theColor(0));
        assertEquals(0xFFC060, LinhBaoMath.theColor(6));
    }
}
