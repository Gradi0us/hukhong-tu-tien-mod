package com.hukhong.world.calendar;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LichMathTest {
    private static final long D = LichMath.DAY;

    @Test void day_is_four_minecraft_days() {
        assertEquals(4 * 24000L, D);
        assertEquals(0, LichMath.day(0));
        assertEquals(0, LichMath.day(D - 1));
        assertEquals(1, LichMath.day(D));
        assertEquals(-1, LichMath.day(-1));
    }

    @Test void week_and_season_cycle() {
        assertEquals(0, LichMath.dayOfWeek(0));
        assertEquals(6, LichMath.dayOfWeek(6 * D));
        assertEquals(0, LichMath.dayOfWeek(7 * D));
        assertEquals(0, LichMath.season(0));
        assertEquals(0, LichMath.season(7 * D - 1));
        assertEquals(1, LichMath.season(7 * D));
        assertEquals(3, LichMath.season(21 * D));
        assertEquals(0, LichMath.season(28 * D));
        assertEquals(3, LichMath.dayOfSeason(3 * D + 5));
    }

    @Test void clock() {
        assertEquals(0, LichMath.hour(0));
        assertEquals(12, LichMath.hour(D / 2));
        assertEquals(30, LichMath.minute(D / 48));
    }

    @Test void every_n_days() {
        var r = new LichMath.Recurrence(LichMath.Kind.EVERY, 2, 0, 0, 1000);
        assertEquals(List.of(1000L, 2 * D + 1000, 4 * D + 1000), r.starts("x", 0, 5 * D));
        assertEquals(List.of(2 * D + 1000), r.starts("x", 1001, 4 * D));
        assertTrue(r.starts("x", 1001, 2 * D + 1000).isEmpty());
    }

    @Test void weekly_hits_one_day_per_week() {
        var r = new LichMath.Recurrence(LichMath.Kind.WEEKLY, 1, 3, 0, 500);
        assertEquals(List.of(3 * D + 500, 10 * D + 500), r.starts("w", 0, 14 * D));
    }

    @Test void random_is_deterministic_and_matches_weight() {
        var r = new LichMath.Recurrence(LichMath.Kind.RANDOM, 1, 0, 0.35, 0);
        assertEquals(r.starts("yv", 0, 1000 * D), r.starts("yv", 0, 1000 * D));
        int n = r.starts("yv", 0, 1000 * D).size();
        assertTrue(n > 280 && n < 420, "got " + n);
        var never = new LichMath.Recurrence(LichMath.Kind.RANDOM, 1, 0, 0.0, 0);
        assertTrue(never.starts("yv", 0, 100 * D).isEmpty());
        var always = new LichMath.Recurrence(LichMath.Kind.RANDOM, 1, 0, 1.0, 0);
        assertEquals(10, always.starts("yv", 0, 10 * D).size());
    }

    @Test void window_is_split_consistent() {
        var r = new LichMath.Recurrence(LichMath.Kind.RANDOM, 1, 0, 0.5, 777);
        var all = r.starts("a", 0, 50 * D);
        var split = new java.util.ArrayList<>(r.starts("a", 0, 20 * D + 5));
        split.addAll(r.starts("a", 20 * D + 5, 50 * D));
        assertEquals(all, split);
    }

    @Test void join_timing_window() {
        assertEquals("hukhong.cal.err_early", CalendarService.timing(0, 10000, 16000));
        assertNull(CalendarService.timing(10000 - 2400, 10000, 16000));
        assertNull(CalendarService.timing(12000, 10000, 16000));
        assertEquals("hukhong.cal.err_over", CalendarService.timing(16000, 10000, 16000));
    }

    @Test void key_start_parse() {
        assertEquals(123456L, CalendarData.startOf("thu_trieu@123456"));
        assertEquals(123456L, CalendarData.startOf("thu_trieu@123456|uuid"));
    }

    @Test void sample_events_parse() throws Exception {
        String[] ids = {"nguyet_thuc", "yeu_vuong", "linh_khi_trieu", "thu_trieu", "prophecy_bi_canh_ch3", "prophecy_chg_hang_ch7"};
        for (String id : ids) {
            try (var in = LichMathTest.class.getResourceAsStream("/data/hukhong/hukhong_events/" + id + ".json")) {
                assertTrue(in != null, id);
                var o = com.google.gson.JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                EventDef d = EventDef.parse(id, o);
                assertTrue(d.duration() > 0);
                assertTrue(!d.rec().starts(id, 0, 28 * D).isEmpty() || d.rec().kind() == LichMath.Kind.RANDOM, id);
            }
        }
    }
}
