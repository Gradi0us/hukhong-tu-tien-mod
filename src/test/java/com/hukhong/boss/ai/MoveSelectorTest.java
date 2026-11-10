package com.hukhong.boss.ai;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MoveSelectorTest {
    static final List<MoveSelector.Option> OPTS = List.of(
            new MoveSelector.Option("a", 0, 4, 3, 1),
            new MoveSelector.Option("b", 0, 4, 1, 1),
            new MoveSelector.Option("far", 6, 12, 1, 1),
            new MoveSelector.Option("p2", 0, 4, 5, 2));

    @Test void filters_by_range_and_phase() {
        for (int i = 0; i < 100; i++) {
            MoveSelector.Option o = MoveSelector.pick(OPTS, 2, 1, List.of(), i / 100.0);
            assertTrue(o.id().equals("a") || o.id().equals("b"));
        }
        assertEquals("far", MoveSelector.pick(OPTS, 8, 1, List.of(), 0.5).id());
        assertNull(MoveSelector.pick(OPTS, 20, 3, List.of(), 0.5));
    }

    @Test void weights_respected() {
        Random r = new Random(1);
        int a = 0;
        for (int i = 0; i < 10000; i++) if (MoveSelector.pick(OPTS, 2, 1, List.of(), r.nextDouble()).id().equals("a")) a++;
        assertEquals(0.75, a / 10000.0, 0.03);
    }

    @Test void never_three_in_a_row() {
        Random r = new Random(7);
        List<String> h = new ArrayList<>();
        String p1 = null, p2 = null;
        for (int i = 0; i < 2000; i++) {
            MoveSelector.Option o = MoveSelector.pick(OPTS, 2, 3, h, r.nextDouble());
            assertNotNull(o);
            assertFalse(o.id().equals(p1) && o.id().equals(p2), "lap 3 lan: " + o.id());
            p2 = p1; p1 = o.id();
            MoveSelector.push(h, o.id());
        }
        assertTrue(h.size() <= 4);
    }

    @Test void single_option_banned_after_two() {
        List<MoveSelector.Option> one = List.of(new MoveSelector.Option("x", 0, 5, 1, 1));
        assertNull(MoveSelector.pick(one, 1, 1, List.of("x", "x"), 0.1));
        assertNotNull(MoveSelector.pick(one, 1, 1, List.of("y", "x"), 0.1));
    }

    @Test void evade_rate_matches_chance() {
        Random r = new Random(3);
        int n = 0;
        for (int i = 0; i < 20000; i++) if (MoveSelector.evadeRoll(0.35, 0, r.nextDouble())) n++;
        assertEquals(0.35, n / 20000.0, 0.02);
        assertFalse(MoveSelector.evadeRoll(1.0, 5, 0.0));
    }
}
