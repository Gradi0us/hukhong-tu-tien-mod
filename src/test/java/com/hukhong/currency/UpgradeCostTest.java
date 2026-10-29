package com.hukhong.currency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpgradeCostTest {
    @Test void base5MatchesPlanTable() {
        assertEquals(5, UpgradeCost.honTinh(1, 5));
        assertEquals(66, UpgradeCost.honTinh(5, 5));
        assertEquals(Math.round(Math.round(5 * Math.pow(10, 1.6)) * 1.5), UpgradeCost.honTinh(10, 5));
    }

    @Test void milestoneStepsEveryTen() {
        assertTrue(UpgradeCost.honTinh(10, 5) > UpgradeCost.honTinh(9, 5) * 1.4);
        assertTrue(UpgradeCost.honTinh(20, 5) > UpgradeCost.honTinh(19, 5) * 1.4);
    }

    @Test void stageBaseKeepsLevelOneCost() {
        assertEquals(2, UpgradeCost.honTinh(1, UpgradeCost.STAGE_BASE));
    }
}
