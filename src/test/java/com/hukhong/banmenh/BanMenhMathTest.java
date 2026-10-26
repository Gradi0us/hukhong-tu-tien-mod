package com.hukhong.banmenh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BanMenhMathTest {
    @Test
    void realmIsCapAndTinhIsRequired() {
        assertEquals(BanMenhMath.REALM_CAP, BanMenhMath.check(0, 19, 99));
        assertEquals(BanMenhMath.NEED_TINH, BanMenhMath.check(0, 20, 1));
        assertEquals(BanMenhMath.OK, BanMenhMath.check(0, 20, BanMenhMath.tinhCost(1)));
        assertEquals(BanMenhMath.REALM_CAP, BanMenhMath.check(1, 39, 99));
        assertEquals(BanMenhMath.OK, BanMenhMath.check(5, 100, BanMenhMath.tinhCost(6)));
        assertEquals(BanMenhMath.MAXED, BanMenhMath.check(6, 100, 999));
    }

    @Test
    void costsIncreaseAndStageNeverExceedsRealm() {
        for (int s = 2; s <= 6; s++) assertTrue(BanMenhMath.tinhCost(s) > BanMenhMath.tinhCost(s - 1));
        assertEquals(0, BanMenhMath.effectiveStage(5, 1));
        assertEquals(2, BanMenhMath.effectiveStage(5, 40));
        assertEquals(6, BanMenhMath.effectiveStage(9, 100));
        assertEquals(0, BanMenhMath.effectiveStage(-3, 100));
    }

    @Test
    void typeEffects() {
        assertEquals(0.25, BanMenhMath.regenBonus(BanMenhType.NGOC_TAM));
        assertEquals(0.15, BanMenhMath.maxResourceBonus(BanMenhType.NGOC_TAM));
        assertEquals(0, BanMenhMath.regenBonus(BanMenhType.LIET_DIEM));
        assertEquals(-0.06, BanMenhMath.speedBonus(BanMenhType.TRONG_NHAC));
        assertEquals(0, BanMenhMath.speedBonus(null));
        assertEquals(1.0, BanMenhMath.skillCostFactor(BanMenhType.NGOC_TAM, 1));
        assertEquals(0.9, BanMenhMath.skillCostFactor(BanMenhType.NGOC_TAM, 2));
        assertEquals(1.0, BanMenhMath.skillCostFactor(BanMenhType.THANH_PHONG, 6));
        assertTrue(BanMenhMath.freeFlight(BanMenhType.NGOC_TAM, 4, true));
        assertFalse(BanMenhMath.freeFlight(BanMenhType.NGOC_TAM, 3, true));
        assertFalse(BanMenhMath.freeFlight(BanMenhType.NGOC_TAM, 6, false));
        assertFalse(BanMenhMath.freeFlight(BanMenhType.HAN_BANG, 6, true));
    }
}
