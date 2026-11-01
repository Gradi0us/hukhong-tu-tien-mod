package com.hukhong.tran;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranMathTest {
    @Test
    void rTongBangTamNhipCongHeSo() {
        assertEquals(8, TranMath.R_HITS);
        assertEquals(0.55 * 8, TranMath.rTotalCoef(), 1e-9);
    }

    @Test
    void daiTranGiamHoiChieuMotNua() {
        assertEquals(0.5, TranMath.cooldownMult(true), 1e-9);
        assertEquals(1.0, TranMath.cooldownMult(false), 1e-9);
    }
}
