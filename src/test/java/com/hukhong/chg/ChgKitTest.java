package com.hukhong.chg;

import com.hukhong.events.BossScaling;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChgKitTest {
    @Test void levels_and_stats_follow_canon_and_increase() {
        int[] lv = {67, 78, 86, 98};
        for (int i = 1; i <= 4; i++) {
            assertEquals(lv[i - 1], ChgKit.def(i).level());
            assertEquals("chg_" + i, ChgKit.def(i).entityId());
            if (i > 1) {
                assertTrue(ChgKit.def(i).hp() > ChgKit.def(i - 1).hp());
                assertTrue(ChgKit.def(i).dmg() > ChgKit.def(i - 1).dmg());
            }
        }
    }

    @Test void phases_unlock_one_skill_each_and_cooldown_shrinks() {
        for (int i = 1; i <= 4; i++) {
            var d = ChgKit.def(i);
            assertEquals(3, d.skills().length);
            for (int ph = 1; ph <= 3; ph++) assertEquals(ph, ChgKit.skillsAvailable(d, ph));
            assertTrue(ChgKit.cooldown(d, 2) < ChgKit.cooldown(d, 1));
            assertTrue(ChgKit.cooldown(d, 3) < ChgKit.cooldown(d, 2));
        }
        assertEquals(60, ChgKit.cooldown(ChgKit.def(1), 3));            // 100 * 0.6
        assertEquals(36, ChgKit.cooldown(ChgKit.def(3), 3));            // 80 * 0.6 * 0.75 (SWIFT)
        assertEquals(6, ChgKit.rainStrikes(1));
        assertEquals(10, ChgKit.rainStrikes(3));
        assertEquals(4, ChgKit.summonCount(3));
    }

    @Test void kits_are_distinct() {
        var passives = new java.util.HashSet<ChgKit.Passive>();
        var shapes = new java.util.HashSet<String>();
        for (int i = 1; i <= 4; i++) {
            passives.add(ChgKit.def(i).passive());
            StringBuilder sb = new StringBuilder();
            for (var s : ChgKit.def(i).skills()) sb.append(s.shape()).append(s.fx());
            shapes.add(sb.toString());
        }
        assertEquals(4, passives.size());
        assertEquals(4, shapes.size());
    }

    @Test void scaling_matches_boss_scaling_and_phase_multiplier() {
        assertEquals(0.0, ChgKit.damageModifier(1, 1), 1e-9);
        assertEquals(0.35, ChgKit.damageModifier(1, 3), 1e-9);
        assertEquals(BossScaling.hpMul(4) - 1, ChgKit.hpModifier(4), 1e-9);
        assertEquals(1.45 * 1.15 - 1, ChgKit.damageModifier(4, 2), 1e-9);
        assertEquals(ChgKit.hpModifier(8), ChgKit.hpModifier(99), 1e-9);  // trần 8 người
        assertEquals(0.2, ChgKit.speedModifier(3), 1e-9);
    }

    @Test void story_flags_do_not_collide_with_lair_hints() {
        int lair = 0b11;  // gợi ý chuỗi A+B
        assertTrue(ChgKit.revealed(lair, 1));
        assertTrue(ChgKit.revealed(lair, 2));
        assertFalse(ChgKit.revealed(lair, 3));
        assertFalse(ChgKit.revealed(0b01, 2));
        for (int i = 1; i <= 4; i++) {
            assertFalse(ChgKit.defeated(lair, i));
            assertTrue(ChgKit.defeated(lair | ChgKit.defeatedBit(i), i));
            assertTrue((ChgKit.defeatedBit(i) & 0b11) == 0);
            for (int j = 1; j < i; j++) assertNotEquals(ChgKit.defeatedBit(i), ChgKit.defeatedBit(j));
        }
    }

    @Test void event_window_near_threshold() {
        assertFalse(ChgKit.eventEligible(63, 1));
        assertTrue(ChgKit.eventEligible(64, 1));
        assertTrue(ChgKit.eventEligible(82, 1));
        assertFalse(ChgKit.eventEligible(83, 1));
        assertTrue(ChgKit.lairEligible(83, 4));
        assertFalse(ChgKit.lairEligible(82, 4));
    }

    @Test void lair_positions_are_deterministic_distinct_and_inside_border() {
        long seed = 123456789L;
        for (int i = 1; i <= 4; i++) {
            int[] a = ChgKit.lairXZ(seed, i), b = ChgKit.lairXZ(seed, i);
            assertArrayEquals(a, b);
            assertTrue(Math.abs(a[0]) < 14500 && Math.abs(a[1]) < 14500);
            for (int j = i + 1; j <= 4; j++) {
                int[] c = ChgKit.lairXZ(seed, j);
                assertTrue(Math.hypot(a[0] - c[0], a[1] - c[1]) > 2000);
            }
        }
        assertFalse(java.util.Arrays.equals(ChgKit.lairXZ(1L, 1), ChgKit.lairXZ(2L, 1)));
    }
}
