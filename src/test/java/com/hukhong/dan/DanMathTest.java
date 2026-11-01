package com.hukhong.dan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DanMathTest {
    @Test
    void rBanMuoiTangBangMotHaiNhanPhepLuc() {
        // 10 tầng: hệ số 1 + 0,15 x 10 = 2,5 lần; tổng 1,2 x 2,5 x sp
        assertEquals(1.2 * 2.5 * 2.5, DanMath.rDamage(2.5, 10), 1e-9);
        assertEquals(1.2 * 2.5, DanMath.rDamage(2.5, 0), 1e-9);
    }

    @Test
    void tangDocKepTaiMuoi() {
        assertEquals(10, DanMath.addStacks(8, 5));
        assertEquals(10, DanMath.addStacks(10, 1));
        assertEquals(3, DanMath.addStacks(0, 3));
    }

    @Test
    void docMoiTangMotNhanNamPhanTramSp() {
        assertEquals(0.05 * 2.0, DanMath.poisonDamage(2.0, 1), 1e-9);
        assertEquals(0.05 * 2.0 * 10, DanMath.poisonDamage(2.0, 99), 1e-9); // kẹp ở 10
    }
}
