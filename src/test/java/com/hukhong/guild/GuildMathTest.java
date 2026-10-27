package com.hukhong.guild;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GuildMathTest {
    @Test
    void promotionNeedsPointsAndLevel() {
        assertFalse(GuildMath.canTrial(0, 99, 50));
        assertFalse(GuildMath.canTrial(0, 100, 9));
        assertTrue(GuildMath.canTrial(0, 100, 10));
        assertTrue(GuildMath.canTrial(4, 5000, 85));
        assertFalse(GuildMath.canTrial(5, 99999, 100), "max rank has no further trial");
        for (int r = 1; r < GuildMath.RANKS; r++) {
            assertTrue(GuildMath.pointsFor(r) > GuildMath.pointsFor(r - 1));
            assertTrue(GuildMath.levelFor(r) > GuildMath.levelFor(r - 1));
        }
    }

    @Test
    void discountIsFivePercentPerRank() {
        assertEquals(0, GuildMath.discountPct(0));
        assertEquals(25, GuildMath.discountPct(5));
        assertEquals(100, GuildMath.discounted(100, 0));
        assertEquals(75, GuildMath.discounted(100, 5));
        assertEquals(1, GuildMath.discounted(1, 5), "never free");
        assertEquals(75, GuildMath.discounted(100, 99), "rank is clamped");
    }

    @Test
    void sectMembersGetSeventyPercent() {
        assertEquals(70, GuildMath.scale(100, true));
        assertEquals(100, GuildMath.scale(100, false));
        assertEquals(1, GuildMath.scale(1, true));
        assertEquals(0, GuildMath.scale(0, true));
    }

    @Test
    void dailyQuestsAreDeterministicAndRankGated() {
        List<GuildMath.Quest> a = GuildMath.daily(0, 5, 20, 77L), b = GuildMath.daily(0, 5, 20, 77L);
        assertEquals(a, b);
        assertEquals(GuildMath.SLOTS, a.size());
        assertEquals(GuildMath.K_KILL, a.get(0).kind());
        assertEquals(GuildMath.K_HERB, a.get(1).kind());
        assertNotEquals(a, GuildMath.daily(0, 6, 20, 77L), "new day, new list");
        for (long day = 0; day < 40; day++) {
            for (GuildMath.Quest q : GuildMath.daily(0, day, 20, day)) {
                assertTrue(q.kind() <= GuildMath.K_COURIER, "rank Dong only gets kill/herb/courier");
                assertTrue(q.ha() > 0 && q.points() > 0 && q.amount() > 0);
            }
        }
    }

    @Test
    void higherRankPaysMore() {
        long low = GuildMath.daily(0, 1, 30, 1L).get(0).ha(), high = GuildMath.daily(5, 1, 30, 1L).get(0).ha();
        assertTrue(high > low);
        GuildMath.Quest k = GuildMath.daily(2, 3, 40, 9L).get(0);
        assertTrue(k.lvLo() >= 1 && k.lvHi() >= k.lvLo());
    }

    @Test
    void trialsExistForEveryPromotion() {
        for (int r = 1; r < GuildMath.RANKS; r++) {
            GuildMath.Trial t = GuildMath.trial(r);
            assertTrue(t.kind() == GuildMath.K_HUNT || t.kind() == GuildMath.K_BICANH);
            assertTrue(t.amount() >= 1);
        }
    }

    @Test
    void guildDataProgressAndRefresh() {
        GuildData d = GuildData.EMPTY.withProg(2, 3).withPoints(40).withTrial(1);
        assertEquals(3, d.progOf(2));
        assertEquals(1, d.activeCount());
        GuildData n = d.refreshed(7, 33);
        assertEquals(GuildData.NONE, n.progOf(2));
        assertEquals(40, n.points());
        assertEquals(1, n.trial(), "trial survives the daily reset");
        assertEquals(33, n.genLevel());
        assertEquals(0, n.withRank(1).rank() - 1);
        assertEquals(GuildData.NONE, d.withRank(1).trial(), "promotion clears the trial");
    }
}
