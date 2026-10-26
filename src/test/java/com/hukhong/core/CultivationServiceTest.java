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
}
