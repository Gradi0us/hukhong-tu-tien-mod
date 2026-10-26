package com.hukhong.honcot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HonCotBonusTest {
    @Test
    void scaleCostKeepsMinimumOne() {
        assertEquals(0, HonCotMath.scaleCost(0, 0.85));
        assertEquals(1, HonCotMath.scaleCost(1, 0.85));
        assertEquals(3, HonCotMath.scaleCost(4, HonCotMath.PHONG_HANH_COST_FACTOR));
        assertEquals(9, HonCotMath.scaleCost(10, 0.9));
    }

    @Test
    void tinhTamOnlyReducesGain() {
        assertEquals(4.0, HonCotMath.acTinhDelta(5, true), 1e-9);
        assertEquals(5.0, HonCotMath.acTinhDelta(5, false), 1e-9);
        assertEquals(-10.0, HonCotMath.acTinhDelta(-10, true), 1e-9);
    }
}
