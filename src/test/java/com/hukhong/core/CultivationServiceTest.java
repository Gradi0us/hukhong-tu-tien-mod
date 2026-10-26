package com.hukhong.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CultivationServiceTest {
    @Test
    void expCurve() {
        assertEquals(40, CultivationService.expToNext(1));
        assertEquals(Math.round(40.0 * Math.pow(10, 2.15)), CultivationService.expToNext(10));
        assertTrue(CultivationService.expToNext(50) > CultivationService.expToNext(49));
    }

    @Test
    void bottleneckQuartersExp() {
        CultivationData d = CultivationData.DEFAULT.withLevel(9);
        assertTrue(d.inBottleneck());
        assertEquals(10, CultivationService.applyExp(d, 40).exp());
        assertEquals(40, CultivationService.applyExp(CultivationData.DEFAULT.withLevel(8), 40).exp());
    }

    @Test
    void milestoneLockAndBreakthrough() {
        CultivationData d = CultivationService.applyBreakthrough(CultivationData.DEFAULT.withLevel(9));
        assertEquals(10, d.level());
        assertTrue(d.milestoneLocked());
        assertEquals(d, CultivationService.applyExp(d, 1000));
        CultivationData u = d.withMilestoneUnlocked(true);
        assertFalse(u.milestoneLocked());
        assertTrue(CultivationService.applyExp(u, 10).exp() > 0);
    }

    @Test
    void codecRoundTripDefault() {
        var json = CultivationData.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, CultivationData.DEFAULT)
                .getOrThrow();
        var back = CultivationData.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(CultivationData.DEFAULT, back);
        assertNull(back.roll());
    }

    @Test
    void lightBottleneckBar3xAndPill() {
        assertEquals(3 * CultivationService.expToNext(94), CultivationService.barSize(94));
        assertEquals(CultivationService.expToNext(93), CultivationService.barSize(93));
        var d = new CultivationData(94, 0, 100, 100, java.util.Set.of(), java.util.Map.of(), false);
        assertFalse(CultivationService.inLightBottleneck(d));
        d = CultivationService.applyExp(d, CultivationService.expToNext(94));
        assertEquals(94, d.level());
        assertTrue(CultivationService.inLightBottleneck(d));
        assertEquals(94, CultivationService.applyLightBreakthrough(d, 98).level());
        var up = CultivationService.applyLightBreakthrough(d, 94);
        assertEquals(95, up.level());
        assertTrue(up.milestoneLocked());
        d = CultivationService.applyExp(d, 2 * CultivationService.expToNext(94));
        assertEquals(95, d.level()); // không có đan: tích đủ x3 vẫn lên
        assertTrue(CultivationData.isMilestoneLevel(100));
    }
}
