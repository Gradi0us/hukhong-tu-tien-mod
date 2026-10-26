package com.hukhong.endgame;

import com.hukhong.chg.ChgKit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndgameKitTest {
    @Test void tier_multipliers_scale_up_and_clamp() {
        assertEquals(1.0, EndgameKit.hpMul(1));
        assertEquals(1.0, EndgameKit.dmgMul(1));
        assertEquals(1.0, EndgameKit.rewardMul(1));
        assertTrue(EndgameKit.hpMul(2) > 1.0 && EndgameKit.dmgMul(2) > 1.0 && EndgameKit.rewardMul(2) > 1.0);
        assertEquals(EndgameKit.hpMul(2), EndgameKit.hpMul(9));
        assertEquals(EndgameKit.hpMul(1), EndgameKit.hpMul(-3));
        assertEquals(2, EndgameKit.clampTier(7));
        assertEquals(1, EndgameKit.clampTier(0));
    }

    @Test void world_tier_default_is_neutral() {
        assertEquals(1, WorldTier.get());
        assertEquals(1.0, WorldTier.hpMul());
        assertEquals(1.0, WorldTier.dmgMul());
        assertEquals(1.0, WorldTier.rewardMul());
    }

    @Test void gateway_needs_all_four_chg_and_level_99() {
        int all = 0;
        for (int i = 1; i <= 4; i++) all |= ChgKit.defeatedBit(i);
        assertTrue(EndgameKit.gatewayEligible(all | 3, 99));
        assertFalse(EndgameKit.gatewayEligible(all, 98));
        assertFalse(EndgameKit.gatewayEligible(all & ~ChgKit.defeatedBit(3), 100));
        assertFalse(EndgameKit.gatewayEligible(3, 100));
    }

    @Test void phases_by_hp_and_skills_unlock_progressively() {
        assertEquals(1, EndgameKit.phase(1f));
        assertEquals(2, EndgameKit.phase(0.7f));
        assertEquals(3, EndgameKit.phase(0.4f));
        assertEquals(4, EndgameKit.phase(0.1f));
        int prev = 0;
        for (int p = 1; p <= 4; p++) {
            int n = EndgameKit.skillsAvailable(p);
            assertTrue(n > prev);
            prev = n;
        }
        assertTrue(EndgameKit.cooldown(4) < EndgameKit.cooldown(1));
        assertEquals(EndgameKit.PHASES, EndgameKit.beamCount(4));
    }

    @Test void islands_are_deterministic_and_clear_main_island() {
        for (int i = 0; i < EndgameKit.ISLAND_COUNT; i++) {
            int[] a = EndgameKit.island(i), b = EndgameKit.island(i);
            assertArrayEquals(a, b);
            assertTrue(Math.hypot(a[0], a[2]) - a[3] > EndgameKit.MAIN_RADIUS);
            assertTrue(a[3] >= 4 && a[3] <= 8);
        }
        assertEquals(0, EndgameKit.depth(9, 8));
        assertTrue(EndgameKit.depth(0, 8) > EndgameKit.depth(7, 8));
    }

    @Test void cutscene_timeline_is_bounded_and_covers_all_lines() {
        assertEquals(-1, EndgameKit.line(0));
        boolean[] seen = new boolean[EndgameKit.LINES];
        for (int t = 0; t < EndgameKit.CUTSCENE_TICKS; t++) {
            int l = EndgameKit.line(t);
            if (l >= 0) seen[l] = true;
            float o = EndgameKit.overlay(t);
            assertTrue(o >= 0f && o <= 1f);
            float a = EndgameKit.lineAlpha(t);
            assertTrue(a >= 0f && a <= 1f);
        }
        for (boolean s : seen) assertTrue(s);
        assertEquals(1f, EndgameKit.overlay(EndgameKit.CUTSCENE_TICKS - 1), 0.02f);
        assertTrue(EndgameKit.validEnding(1) && EndgameKit.validEnding(2) && !EndgameKit.validEnding(0));
    }
}
