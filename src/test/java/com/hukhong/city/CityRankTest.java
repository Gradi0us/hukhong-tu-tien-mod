package com.hukhong.city;

import com.hukhong.city.CityRank.Rank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CityRankTest {
    @Test
    void thresholdsScaleWithTier() {
        assertEquals(2000, CityRank.pointsFor(Rank.THANH_CHU, Tier.NHAN));
        assertEquals(3000, CityRank.pointsFor(Rank.THANH_CHU, Tier.DIA));
        assertEquals(5000, CityRank.pointsFor(Rank.THANH_CHU, Tier.THIEN));
        assertEquals(50, CityRank.levelFor(Rank.THANH_CHU, Tier.NHAN));
        assertEquals(90, CityRank.levelFor(Rank.THANH_CHU, Tier.THIEN));
    }

    @Test
    void rankNeedsBothPointsAndLevel() {
        assertEquals(Rank.CU_DAN, CityRank.rankFor(0, 1, Tier.NHAN, true));
        assertEquals(Rank.CU_DAN, CityRank.rankFor(150, 9, Tier.NHAN, true)); // đủ điểm, thiếu cảnh giới
        assertEquals(Rank.HO_VE, CityRank.rankFor(150, 10, Tier.NHAN, true));
        assertEquals(Rank.THONG_LINH, CityRank.rankFor(800, 30, Tier.NHAN, true));
        assertEquals(Rank.DOI_TRUONG, CityRank.rankFor(5000, 20, Tier.NHAN, true)); // cấp 20 chỉ đủ Đội Trưởng
    }

    @Test
    void lordSeatTakenCapsAtDeputy() {
        assertEquals(Rank.THANH_CHU, CityRank.rankFor(2000, 50, Tier.NHAN, true));
        assertEquals(Rank.PHO_THANH_CHU, CityRank.rankFor(2000, 50, Tier.NHAN, false));
    }

    @Test
    void cycleCapAndKillPoints() {
        assertEquals(200, CityRank.cycleCap(Tier.NHAN));
        assertEquals(500, CityRank.cycleCap(Tier.THIEN));
        assertEquals(20, CityRank.allowed(180, 50, Tier.NHAN));
        assertEquals(0, CityRank.allowed(200, 5, Tier.NHAN));
        assertEquals(50, CityRank.allowed(0, 50, Tier.NHAN));
        assertEquals(1, CityRank.killPoints(1));
        assertEquals(7, CityRank.killPoints(30));
    }

    @Test
    void nextRankChain() {
        assertEquals(Rank.HO_VE, CityRank.next(Rank.CU_DAN));
        assertNull(CityRank.next(Rank.THANH_CHU));
    }

    @Test
    void standingTracksResidencyAndCycle() {
        CityStanding s = CityStanding.DEFAULT.withResident("a");
        assertTrue(s.resident("a"));
        assertEquals(0, s.of("a"));
        s = s.withAdded("a", 30, 5).withAdded("a", 20, 5);
        assertEquals(50, s.of("a"));
        assertEquals(50, s.earnedIn(5));
        assertEquals(0, s.earnedIn(6));
        assertEquals(10, s.withAdded("a", 10, 6).earnedIn(6));
    }
}
