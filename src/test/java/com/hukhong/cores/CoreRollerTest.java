package com.hukhong.cores;

import com.google.gson.JsonParser;
import com.hukhong.core.Aptitude;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CoreRollerTest {
    private static Map<ResourceLocation, CoreDef> load() throws Exception {
        Map<ResourceLocation, CoreDef> m = new LinkedHashMap<>();
        String[] names = {"hoa_chung", "huyet_dang", "thiet_bi", "thanh_tam_chu", "kim_cuong_bat_hoai", "dung_nham_quyen",
                "diem_kiem_khi", "ngu_phong_bo", "ho_thanh_quang_vuc", "tinh_ha_quan_dinh"};
        for (String n : names) {
            try (var in = CoreRollerTest.class.getResourceAsStream("/data/hukhong/hukhong/core/" + n + ".json")) {
                var def = CoreDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(new java.io.InputStreamReader(in))).getOrThrow();
                m.put(ResourceLocation.fromNamespaceAndPath("hukhong", n), def);
            }
        }
        return m;
    }

    @Test
    void jsonParsesAndRollIsDeterministic() throws Exception {
        var all = load();
        assertEquals(10, all.size());
        for (String path : new String[]{"kiem", "dao", "the", "ma"}) {
            for (int m = 1; m <= 9; m++) {
                var a = CoreRoller.roll(m, 42L + m, 5, Aptitude.A, Set.of(path), Set.of(), all);
                var b = CoreRoller.roll(m, 42L + m, 5, Aptitude.A, Set.of(path), Set.of(), all);
                assertEquals(a, b);
                assertEquals(3, a.ids().size());
                assertTrue(a.tier() <= CoreRoller.ceilTier(m));
                assertTrue(a.fourthTier() <= Math.min(4, CoreRoller.ceilTier(m) + 1));
            }
        }
        for (int m = 1; m <= 9; m++) {
            int s = 0;
            for (int w : CoreRoller.tierWeights(m, 15)) s += w;
            assertEquals(100, s);
            s = 0;
            for (int w : CoreRoller.tierWeights(m, -6)) s += w;
            assertEquals(100, s);
        }
    }
}
