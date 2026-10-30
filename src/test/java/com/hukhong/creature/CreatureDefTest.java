package com.hukhong.creature;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hukhong.creature.pet.PetMath;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CreatureDefTest {
    private static JsonObject load(String path) throws Exception {
        try (var in = CreatureDefTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void everyKindHasAParsableDefinitionWithModItemDrops() throws Exception {
        Set<String> mats = Set.copyOf(CreatureKind.MATERIALS);
        Set<String> extra = Set.of("huyen_thiet", "huyet_tham", "tinh_than_sa", "thanh_diep", "linh_thao", "bang_lien", "hon_tinh");
        for (CreatureKind k : CreatureKind.values()) {
            CreatureDef d = CreatureDef.parse(k.id, load("/data/hukhong/hukhong_creatures/" + k.id + ".json"));
            assertEquals(k.id, d.id());
            assertTrue(d.hpMul() > 0 && d.dmgMul() > 0, k.id);
            for (CreatureDef.Drop dr : d.drops()) {
                assertTrue(dr.item().startsWith("hukhong:"), k.id + " must drop mod items only: " + dr.item());
                String p = dr.item().substring("hukhong:".length());
                assertTrue(mats.contains(p) || extra.contains(p), dr.item());
            }
            assertEquals(k.hostile(), d.role().equals("hostile"), k.id + " role/class mismatch");
            if (k.tameSlot != null) {
                assertNotNull(d.tame(), k.id + " tameable kind needs tame block");
                assertEquals(k.tameSlot, d.tame().slot());
                assertEquals(k.tameItem, d.tame().item());
                assertEquals(0.3, d.tame().hpFrac(), 1e-9, "taming threshold is 30% HP");
            } else assertNull(d.tame(), k.id);
            if (d.spawn() != null) assertFalse(d.spawn().biomes().isEmpty());
        }
    }

    @Test
    void spawnGroupsMatchDesign() throws Exception {
        CreatureDef q = CreatureDef.parse("quan_hon", load("/data/hukhong/hukhong_creatures/quan_hon.json"));
        assertEquals(3, q.spawn().min());
        assertEquals(5, q.spawn().max());
        assertEquals("night", q.spawn().time());
    }

    @Test
    void badJsonIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> CreatureDef.parse("x", JsonParser.parseString("{\"role\":\"god\"}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> CreatureDef.parse("x",
                JsonParser.parseString("{\"tame\":{\"item\":\"a:b\",\"hp_frac\":1.5}}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> CreatureDef.parse("x",
                JsonParser.parseString("{\"drops\":[{\"item\":\"a:b\",\"chance\":2}]}").getAsJsonObject()));
    }

    @Test
    void petMathRules() {
        assertTrue(PetMath.canTame(3, 10, 0.3));
        assertFalse(PetMath.canTame(3.5, 10, 0.3));
        assertFalse(PetMath.canTame(0, 10, 0.3));
        assertEquals(1, PetMath.petLevel(0));
        assertEquals(57, PetMath.petLevel(57));
        assertFalse(PetMath.pickable(false, 1, 5));
        assertTrue(PetMath.pickable(false, 0, 40));
        assertFalse(PetMath.charmable(5000));
    }
}
