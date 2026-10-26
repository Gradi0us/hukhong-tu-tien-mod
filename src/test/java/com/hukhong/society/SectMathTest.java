package com.hukhong.society;

import com.hukhong.city.Tier;
import com.hukhong.core.Aptitude;
import com.hukhong.core.Path;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SectMathTest {
    private static SectDef def(String id) { return SectDef.byId(id); }

    @Test
    void rankNeedsLevelAndCh() {
        assertEquals(SectMath.Rank.NGOAI, SectMath.rankFor(Tier.NHAN, 20, 499));
        assertEquals(SectMath.Rank.NOI, SectMath.rankFor(Tier.NHAN, 20, 500));
        assertEquals(SectMath.Rank.NGOAI, SectMath.rankFor(Tier.NHAN, 19, 99999));
        assertEquals(SectMath.Rank.TRUONG_LAO, SectMath.rankFor(Tier.NHAN, 60, 12000));
        assertEquals(SectMath.Rank.NOI, SectMath.rankFor(Tier.DIA, 49, 99999));
    }

    @Test
    void afkExpIsVerySlow() {
        long e = 1000;
        assertEquals(100.0, SectMath.afkExpPerSecond(e, 1) * 3600, 1e-9);   // 0,1 E mỗi giờ
        assertEquals(200.0, SectMath.afkExpPerSecond(e, 2) * 3600, 1e-9);
        assertEquals(10.0, e / (SectMath.afkExpPerSecond(e, 1) * 3600), 1e-9); // ~10 giờ/cấp
    }

    @Test
    void rankMultiplierMatchesR22() {
        assertEquals(1.0, SectMath.rankMult(SectMath.Rank.NGOAI));
        assertEquals(1.5, SectMath.rankMult(SectMath.Rank.NOI));
        assertEquals(2.0, SectMath.rankMult(SectMath.Rank.CHAN_TRUYEN));
        assertEquals(2.0, SectMath.rankMult(SectMath.Rank.TRUONG_LAO));
        assertEquals(0.25, SectMath.buffFactor(SectMath.Rank.NGOAI));
    }

    @Test
    void eligibilityGates() {
        SectDef kiem = def("thien_kiem_tong");
        assertEquals(SectMath.OK, SectMath.check(kiem, 20, Aptitude.B, 30, Set.of(Path.KIEM), false));
        assertEquals(SectMath.LOW_LEVEL, SectMath.check(kiem, 19, Aptitude.B, 30, Set.of(Path.KIEM), false));
        assertEquals(SectMath.LOW_APT, SectMath.check(kiem, 20, Aptitude.C, 30, Set.of(Path.KIEM), false));
        assertEquals(SectMath.TOO_OLD, SectMath.check(kiem, 20, Aptitude.B, 46, Set.of(Path.KIEM), false));
        assertEquals(SectMath.WRONG_PATH, SectMath.check(kiem, 20, Aptitude.B, 30, Set.of(Path.THE), false));
    }

    @Test
    void invitationRelaxesLevelAndAge() {
        SectDef kiem = def("thien_kiem_tong");
        assertEquals(SectMath.OK, SectMath.check(kiem, 10, Aptitude.B, 50, Set.of(Path.KIEM), true));
        assertEquals(SectMath.TOO_OLD, SectMath.check(kiem, 10, Aptitude.B, 51, Set.of(Path.KIEM), true));
        assertEquals(SectMath.LOW_LEVEL, SectMath.check(kiem, 9, Aptitude.B, 30, Set.of(Path.KIEM), true));
    }

    @Test
    void tradeSectsAcceptAnyPath() {
        assertEquals(SectMath.OK, SectMath.check(def("dan_dinh_tong"), 10, Aptitude.D, 55, Set.of(Path.MA), false));
        assertEquals(SectMath.WRONG_PATH, SectMath.check(def("quang_minh_dao_cung"), 20, Aptitude.A, 30, Set.of(Path.DAO, Path.MA), false));
        assertEquals(SectMath.WRONG_PATH, SectMath.check(def("van_tuong_thanh_tong"), 30, Aptitude.S, 30, Set.of(Path.KIEM), false));
    }

    @Test
    void inviteChanceScalesWithAptitudeAndTier() {
        assertTrue(SectMath.inviteChance(Tier.NHAN, Aptitude.S) > SectMath.inviteChance(Tier.NHAN, Aptitude.D));
        assertTrue(SectMath.inviteChance(Tier.NHAN, Aptitude.B) > SectMath.inviteChance(Tier.THIEN, Aptitude.B));
        assertEquals(0.12 / 30, SectMath.inviteChance(Tier.NHAN, Aptitude.B), 1e-12);
    }

    @Test
    void cycleCapAndQuests() {
        assertEquals(400, SectMath.cycleCap(Tier.NHAN));
        assertEquals(30, SectMath.allowedCh(370, 60, Tier.NHAN));
        assertEquals(0, SectMath.allowedCh(400, 60, Tier.NHAN));
        assertEquals(8, SectMath.questNeed(SectMath.Q_KILL, Tier.NHAN));
        assertEquals(12, SectMath.questNeed(SectMath.Q_KILL, Tier.DIA));
        assertEquals(60, SectMath.questCh(SectMath.Q_KILL, Tier.NHAN));
        assertEquals(90, SectMath.questCh(SectMath.Q_KILL, Tier.DIA));
        assertEquals(7, SectDef.ALL.stream().filter(SectDef::buildable).count());
        assertEquals(7, SectDef.ALL.size());
    }

    @Test
    void sectDataCycleAccounting() {
        SectData d = SectData.DEFAULT.withSect("ba_the_mon").withEarned(3, 100).withEarned(3, 50);
        assertEquals(150, d.totalCh());
        assertEquals(150, d.earnedIn(3));
        assertEquals(0, d.earnedIn(4));
        SectData n = d.withEarned(4, 10);
        assertEquals(160, n.totalCh());
        assertEquals(10, n.earnedIn(4));
        assertEquals(160, n.wallet());
    }

    @Test
    void daiBiGatesHigherRanks() {
        assertEquals(SectMath.Rank.NOI, SectMath.rankFor(Tier.NHAN, 70, 99999, 0));
        assertEquals(SectMath.Rank.CHAN_TRUYEN, SectMath.rankFor(Tier.NHAN, 70, 99999, 1));
        assertEquals(SectMath.Rank.TRUONG_LAO, SectMath.rankFor(Tier.NHAN, 70, 99999, 2));
        assertEquals(SectMath.Rank.CHAN_TRUYEN, SectMath.rankFor(Tier.NHAN, 40, 3000, 2));
    }

    @Test
    void trialTargetNeedsLevelAndCh() {
        assertNull(SectMath.trialTarget(Tier.NHAN, 20, 400, 0));
        assertEquals(SectMath.Rank.CHAN_TRUYEN, SectMath.trialTarget(Tier.NHAN, 40, 3000, 0));
        assertEquals(SectMath.Rank.TRUONG_LAO, SectMath.trialTarget(Tier.NHAN, 60, 12000, 1));
        assertNull(SectMath.trialTarget(Tier.NHAN, 99, 99999, 2));
        assertTrue(SectMath.trialKills(Tier.THIEN, 1) > SectMath.trialKills(Tier.NHAN, 0));
    }

    @Test
    void hiddenSectNeedsInviteAndBadName() {
        SectDef h = def("huyet_anh_ma_mon");
        assertTrue(h.hidden());
        assertEquals(SectMath.NEED_INVITE, SectMath.checkHidden(h, false, 5));
        assertEquals(SectMath.NO_BAD_NAME, SectMath.checkHidden(h, true, 0));
        assertEquals(SectMath.OK, SectMath.checkHidden(h, true, 1));
        assertEquals(SectMath.OK, SectMath.checkHidden(def("ba_the_mon"), false, 0));
        assertEquals(SectMath.OK, SectMath.check(h, 10, Aptitude.C, 30, Set.of(Path.MA), true, 2));
        assertEquals(SectMath.NEED_INVITE, SectMath.check(h, 10, Aptitude.C, 30, Set.of(Path.MA), false, 2));
    }

    @Test
    void altarSeatsAndFee() {
        AltarSeats s = new AltarSeats();
        java.util.UUID a = java.util.UUID.randomUUID(), b = java.util.UUID.randomUUID();
        assertTrue(s.tryTake("x", a, 1));
        assertTrue(s.tryTake("x", a, 1));
        assertFalse(s.tryTake("x", b, 1));
        s.release(a);
        assertTrue(s.tryTake("x", b, 1));
        assertEquals(70, SectMath.chargeFee(100, 30));
        assertEquals(-1, SectMath.chargeFee(10, 30));
    }

    @Test
    void chainQuestEscalates() {
        assertEquals(SectMath.Q_KILL, SectMath.questKind(SectMath.Q_CHAIN, 0));
        assertEquals(SectMath.Q_HERB, SectMath.questKind(SectMath.Q_CHAIN, 1));
        assertTrue(SectMath.questNeed(SectMath.Q_CHAIN, Tier.NHAN, 2) > SectMath.questNeed(SectMath.Q_CHAIN, Tier.NHAN, 0));
        assertTrue(SectMath.questCh(SectMath.Q_CHAIN, Tier.NHAN, 0) > SectMath.questCh(SectMath.Q_KILL, Tier.NHAN));
    }

    @Test
    void altarFeeOncePerSittingHour() {
        int charges = 0;
        for (int secs = 0; secs < 7200; secs++) if (SectMath.feeDue(secs)) charges++;
        assertEquals(2, charges); // 2 giờ ngồi cộng dồn = 2 lần thu, bất kể đứng/ngồi lại
        assertTrue(SectMath.feeDue(0));
        assertFalse(SectMath.feeDue(1));
        assertFalse(SectMath.feeDue(3599));
        assertTrue(SectMath.feeDue(3600));
    }
}
