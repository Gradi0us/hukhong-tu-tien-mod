package com.hukhong.pvp;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PvpMathTest {
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2), C = new UUID(0, 3), D = new UUID(0, 4);

    @Test void burnIsFivePercentFloor() {
        assertEquals(0, PvpMath.burn(19));
        assertEquals(1, PvpMath.burn(20));
        assertEquals(30, PvpMath.burn(600));
        assertEquals(0, PvpMath.burn(-5));
    }

    @Test void duelPrizeConservesMoney() {
        for (long stake : new long[]{100, 333, 10_000, 1})
            assertEquals(PvpMath.duelTotal(stake), PvpMath.duelPrize(stake) + PvpMath.burn(PvpMath.duelTotal(stake)));
        assertEquals(570, PvpMath.duelPrize(300));
    }

    @Test void minStakeByRealm() {
        assertEquals(100, PvpMath.minStake(1));
        assertEquals(100, PvpMath.minStake(9));
        assertEquals(200, PvpMath.minStake(10));
        assertEquals(1100, PvpMath.minStake(100));
    }

    @Test void balanceOnlyAboveGapAndCapped() {
        assertEquals(1.0, PvpMath.balanceMul(10, true));
        assertEquals(1.0, PvpMath.balanceMul(40, false));
        assertEquals(1.04, PvpMath.balanceMul(11, true), 1e-9);
        assertEquals(2.0, PvpMath.balanceMul(90, true));
    }

    @Test void pairKeyIsSymmetricAndDaily() {
        assertEquals(PvpMath.pairKey(A, B, 5), PvpMath.pairKey(B, A, 5));
        assertNotEquals(PvpMath.pairKey(A, B, 5), PvpMath.pairKey(A, B, 5 + PvpMath.DAY_TICKS));
        assertTrue(PvpMath.pairAllowed(2));
        assertFalse(PvpMath.pairAllowed(3));
    }

    @Test void timeoutAndMatchWinner() {
        assertEquals(1, PvpMath.timeoutWinner(0.6, 0.4));
        assertEquals(-1, PvpMath.timeoutWinner(0.1, 0.9));
        assertEquals(0, PvpMath.timeoutWinner(0.5, 0.502));
        assertEquals(1, PvpMath.matchWinner(2, 0, 0, 0));
        assertEquals(-1, PvpMath.matchWinner(1, 2, 9, 0));
        assertEquals(-1, PvpMath.matchWinner(1, 1, 0.4, 0.9)); // hoa hiep -> tong % mau
        assertEquals(0, PvpMath.matchWinner(0, 0, 1, 1));
    }

    @Test void pariMutuelSharesNetPotProportionally() {
        List<PvpMath.Bet> bets = List.of(new PvpMath.Bet(A, 1, 100), new PvpMath.Bet(B, 1, 300), new PvpMath.Bet(C, -1, 600));
        PvpMath.Settlement s = PvpMath.settle(bets, 1);
        // tong 1000, dot 50, chia 950 cho 400 theo 1:3
        assertEquals(237, s.pay().get(A));
        assertEquals(712, s.pay().get(B));
        assertNull(s.pay().get(C));
        assertEquals(1000, s.paid() + s.burned());
        assertTrue(s.burned() >= PvpMath.burn(1000)); // dot >= 5% (phan du lam tron cung bi dot)
        assertEquals(1000 - 237 - 712, s.burned());
    }

    @Test void pariMutuelRefundsWhenOneSideEmptyOrDraw() {
        List<PvpMath.Bet> one = List.of(new PvpMath.Bet(A, 1, 100), new PvpMath.Bet(B, 1, 50));
        PvpMath.Settlement s = PvpMath.settle(one, 1); // khong ai cuoc ben thua
        assertEquals(100, s.pay().get(A));
        assertEquals(50, s.pay().get(B));
        assertEquals(0, s.burned());
        List<PvpMath.Bet> two = List.of(new PvpMath.Bet(A, 1, 100), new PvpMath.Bet(B, -1, 50));
        PvpMath.Settlement d = PvpMath.settle(two, 0);
        assertEquals(100, d.pay().get(A));
        assertEquals(50, d.pay().get(B));
        assertEquals(0, d.burned());
        assertTrue(PvpMath.settle(List.of(), 1).pay().isEmpty());
    }

    @Test void pariMutuelConservationRandomized() {
        java.util.Random r = new java.util.Random(7);
        for (int n = 0; n < 200; n++) {
            List<PvpMath.Bet> bets = new ArrayList<>();
            long total = 0;
            int count = 1 + r.nextInt(8);
            for (int i = 0; i < count; i++) {
                long amt = 10 + r.nextInt(5000);
                total += amt;
                bets.add(new PvpMath.Bet(new UUID(1, r.nextInt(5)), r.nextBoolean() ? 1 : -1, amt));
            }
            for (int w : new int[]{1, -1, 0}) {
                PvpMath.Settlement s = PvpMath.settle(bets, w);
                assertEquals(total, s.paid() + s.burned(), "conservation");
                assertTrue(s.burned() >= 0);
            }
        }
    }

    @Test void bracketSeedingAndPrize() {
        List<UUID> seeds = PvpMath.seedBracket(List.of(A, B, C));
        assertEquals(8, seeds.size());
        assertEquals(A, seeds.get(0));
        assertNull(seeds.get(3));
        assertEquals(4, PvpMath.matchesInRound(0));
        assertEquals(2, PvpMath.matchesInRound(1));
        assertEquals(1, PvpMath.matchesInRound(2));
        assertEquals(950, PvpMath.bracketPrize(1000));
        long[] t = PvpMath.sideTotals(List.of(new PvpMath.Bet(A, 1, 5), new PvpMath.Bet(D, -1, 7)));
        assertEquals(5, t[0]);
        assertEquals(7, t[1]);
    }
}
