package com.hukhong.honcot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HonCotMathTest {
    @Test
    void tierMultAndRequiredLevel() {
        assertEquals(0.70, HonCotMath.tierMult(0), 1e-9);
        assertEquals(1.40, HonCotMath.tierMult(2), 1e-9);
        assertEquals(1.40, HonCotMath.tierMult(99), 1e-9); // kẹp
        assertEquals(40, HonCotMath.requiredLevel(0));
        assertTrue(HonCotMath.canEquip(60, 1));
        assertFalse(HonCotMath.canEquip(79, 2));
    }

    @Test
    void statValueScalesByTierAndRoll() {
        assertEquals(0.15, HonCotMath.statValue(0.15, 1, 50), 1e-9);
        assertEquals(0.15 * 1.4 * 1.1, HonCotMath.statValue(0.15, 2, 100), 1e-9);
        assertEquals(0.15 * 0.7 * 0.9, HonCotMath.statValue(0.15, 0, 0), 1e-9);
        assertEquals(1.0, HonCotMath.rollMult(50), 1e-9);
        assertEquals(1.1, HonCotMath.rollMult(500), 1e-9);
    }

    @Test
    void rollQualityBounded() {
        assertEquals(0, HonCotMath.rollQuality(0, 0));
        assertEquals(100, HonCotMath.rollQuality(0.999, 0.9999));
        assertEquals(50, HonCotMath.rollQuality(0.25, 0.75));
    }

    @Test
    void dropChanceGates() {
        assertEquals(0, HonCotMath.dropChance(60, false));
        assertEquals(0.01, HonCotMath.dropChance(61, false), 1e-9);
        assertEquals(0.05, HonCotMath.dropChance(500, false), 1e-9);
        assertEquals(0.25, HonCotMath.dropChance(1, true), 1e-9);
        assertFalse(HonCotMath.bossDrops(0, 0.9));
        assertTrue(HonCotMath.bossDrops(0, 0.1));
        assertTrue(HonCotMath.bossDrops(HonCotMath.BOSS_PITY, 0.99));
    }

    @Test
    void tierForLevelAndBoss() {
        assertEquals(0, HonCotMath.tierFor(70, false, 0.9));
        assertEquals(1, HonCotMath.tierFor(85, false, 0.9));
        assertEquals(2, HonCotMath.tierFor(99, false, 0.0));
        assertEquals(1, HonCotMath.tierFor(30, true, 0.1));
        assertEquals(2, HonCotMath.tierFor(30, true, 0.9));
    }

    @Test
    void pickBoneCoversAllAndNeverOverflows() {
        assertEquals(Bone.THIEN_MUC, HonCotMath.pickBone(0));
        assertEquals(Bone.HOI_SINH, HonCotMath.pickBone(0.9999999));
        assertEquals(Bone.HOI_SINH, HonCotMath.pickBone(1.0));
    }

    @Test
    void everySlotHasTwoBonesWithStats() {
        int[] per = new int[BoneSlot.COUNT];
        for (Bone b : Bone.values()) { per[b.slot.ordinal()]++; assertFalse(b.stats.isEmpty()); }
        for (int c : per) assertEquals(2, c);
    }

    @Test
    void resonanceBonusAtFullSet() {
        assertEquals(1.0, HonCotMath.resonanceCoef(0), 1e-9);
        assertEquals(2.0, HonCotMath.resonanceCoef(2), 1e-9);
        assertEquals(5.0, HonCotMath.resonanceCoef(6), 1e-9);
        assertEquals(5.0, HonCotMath.resonanceCoef(9), 1e-9);
    }
}
