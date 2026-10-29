package com.hukhong.world.calendar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalendarE5Test {
    @Test void linhKhiBoostIsOnePointFive() {
        assertEquals(150, LinhKhiTrieu.boosted(100));
        assertEquals(1, LinhKhiTrieu.boosted(1));
        assertEquals(0, LinhKhiTrieu.boosted(0));
        assertTrue(LinhKhiTrieu.inZone(60, 0, 0, 0));
        assertFalse(LinhKhiTrieu.inZone(65, 0, 0, 0));
    }

    @Test void loiVuPicksStableLairAndNonLethalScale() {
        for (long s = 0; s < 50; s++) {
            int l = LoiVuHandler.lairFor(s * 7919);
            assertTrue(l >= 1 && l <= 4);
            assertEquals(l, LoiVuHandler.lairFor(s * 7919));
        }
        assertEquals(3f, LoiVuHandler.strikeDamage(10f));
        assertEquals(12f, LoiVuHandler.strikeDamage(100f));
        assertEquals(20, LoiVuHandler.rewardExp(100));
    }

    @Test void commanderLevelAndDocChain() {
        assertEquals(30, NguyetThucHandler.commanderLevel(5));
        assertEquals(65, NguyetThucHandler.commanderLevel(99));
        for (int lv : new int[]{30, 45, 55, 65}) {
            var d = NguyetThucHandler.docFor(lv, 0.5);
            assertNotNull(d);
            assertEquals('a', d.chain());
        }
    }

    @Test void newEventsParseWithLore() throws Exception {
        for (String id : new String[]{"loi_vu", "giai_sinh_tu", "thu_trieu", "linh_khi_trieu", "nguyet_thuc"}) {
            try (var in = CalendarE5Test.class.getResourceAsStream("/data/hukhong/hukhong_events/" + id + ".json")) {
                assertNotNull(in, id);
                var o = com.google.gson.JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                assertTrue(o.has("lore") && o.get("lore").isJsonObject(), id + " needs lore");
                EventDef d = EventDef.parse(id, o);
                assertTrue(d.duration() > 0);
                assertNotNull(d.lore().faction());
            }
        }
    }
}
