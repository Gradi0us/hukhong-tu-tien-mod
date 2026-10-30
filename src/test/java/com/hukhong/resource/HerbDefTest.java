package com.hukhong.resource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HerbDefTest {
    private static JsonObject load(String path) throws Exception {
        try (var in = HerbDefTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void twelveHerbsParseAndEverySeasonHasABloomer() throws Exception {
        assertEquals(12, HerbDef.IDS.size());
        int all = 0;
        for (String id : HerbDef.IDS) {
            HerbDef d = HerbDef.parse(id, load("/data/hukhong/hukhong_herbs/" + id + ".json"));
            assertTrue(d.seasonMask() != 0 && !d.biomes().isEmpty(), id);
            all |= d.seasonMask();
        }
        assertEquals(0b1111, all, "every season must have at least one blooming herb");
    }

    @Test
    void seasonGatingAndTrieuOverride() {
        int ginseng = 0b1100; // thu + dong
        assertTrue(HerbMath.blooms(ginseng, 2, false));
        assertFalse(HerbMath.blooms(ginseng, 0, false));
        assertTrue(HerbMath.blooms(ginseng, 0, true), "Linh Khi Trieu overrides the season");
    }

    @Test
    void growthWiltAndWither() {
        assertEquals(1, HerbMath.nextAge(0, true, true, false));
        assertEquals(2, HerbMath.nextAge(2, true, true, false));
        assertEquals(0, HerbMath.nextAge(0, false, true, true));
        assertEquals(1, HerbMath.nextAge(2, false, false, true));
        assertTrue(HerbMath.witherAway(0, false, false, true));
        assertFalse(HerbMath.witherAway(0, false, true, true), "tended herbs never wither away");
        assertFalse(HerbMath.witherAway(0, true, false, true));
    }

    @Test
    void qualityByZoneAndTrieu() {
        assertEquals(0, HerbMath.quality(5, false));
        assertEquals(1, HerbMath.quality(20, false));
        assertEquals(3, HerbMath.quality(80, false));
        assertEquals(1, HerbMath.quality(5, true));
        assertEquals(3, HerbMath.quality(80, true), "quality is capped");
        assertEquals(1, HerbMath.dropCount(0, true));
        assertEquals(2, HerbMath.dropCount(2, false));
        assertEquals(3, HerbMath.dropCount(3, true));
    }

    @Test
    void badHerbJsonRejected() {
        assertThrows(IllegalArgumentException.class, () -> HerbDef.parse("x", JsonParser.parseString("{\"seasons\":[\"mua\"],\"biomes\":[\"a:b\"]}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> HerbDef.parse("x", JsonParser.parseString("{\"seasons\":[\"xuan\"],\"biomes\":[]}").getAsJsonObject()));
    }
}
