package com.hukhong.story.freequest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FreeQuestMathTest {
    @Test
    void khaiMachNeedsFourHerbs() {
        assertEquals(1, FreeQuestMath.pillTier(9));
        assertEquals(4, FreeQuestMath.herbCount(1));
        assertFalse(FreeQuestMath.herbIsHuyetTham(1));
    }

    @Test
    void scalesWithGrade() {
        assertTrue(FreeQuestMath.herbCount(4) > FreeQuestMath.herbCount(3));
        assertTrue(FreeQuestMath.killCount(6) > FreeQuestMath.killCount(1));
        assertTrue(FreeQuestMath.herbIsHuyetTham(4));
        assertEquals(2, FreeQuestMath.pillTier(19));
        assertEquals(5, FreeQuestMath.pillTier(94));
        assertEquals(9, FreeQuestMath.pillTier(98));
    }

    @Test
    void questLevelsAndMobFloor() {
        assertTrue(FreeQuestMath.isQuestLevel(9));
        assertTrue(FreeQuestMath.isQuestLevel(98));
        assertFalse(FreeQuestMath.isQuestLevel(10));
        assertFalse(FreeQuestMath.isQuestLevel(99));
        assertEquals(1, FreeQuestMath.minMobLevel(3));
        assertEquals(14, FreeQuestMath.minMobLevel(19));
    }
}
