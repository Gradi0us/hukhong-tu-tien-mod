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

    @Test
    void thanCoreOnlyAtMilestone12AndOnlyOwnPath() throws Exception {
        var all = load();
        for (String n : new String[]{"kiem", "dao", "the", "ma"}) {
            try (var in = CoreRollerTest.class.getResourceAsStream("/data/hukhong/hukhong/core/than_" + n + ".json")) {
                var def = CoreDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(new java.io.InputStreamReader(in))).getOrThrow();
                assertEquals(CoreTier.KIM, def.minTier());
                all.put(ResourceLocation.fromNamespaceAndPath("hukhong", "than_" + n), def);
            }
        }
        var p = CoreRoller.roll(12, 7L, 0, Aptitude.B, Set.of("dao"), Set.of(), all);
        assertEquals(java.util.List.of(ResourceLocation.fromNamespaceAndPath("hukhong", "than_dao")), p.ids());
        assertEquals(CoreTier.KIM.ordinal(), p.tier());
        assertFalse(p.stuckRoll());
        for (int m = 1; m <= 11; m++) {
            var r = CoreRoller.roll(m, 9L + m, 5, Aptitude.A, Set.of("dao"), Set.of(), all);
            assertTrue(r.ids().stream().noneMatch(x -> x.getPath().startsWith("than_")));
            assertFalse(r.fourth().getPath().startsWith("than_"));
        }
    }

    @Test
    void tierCapRisesOneStepEvery30Levels() {
        assertEquals(1, CoreRoller.tierCapFor(10));
        assertEquals(1, CoreRoller.tierCapFor(29));
        assertEquals(2, CoreRoller.tierCapFor(30));
        assertEquals(2, CoreRoller.tierCapFor(59));
        assertEquals(3, CoreRoller.tierCapFor(60));
        assertEquals(4, CoreRoller.tierCapFor(90));
        assertEquals(4, CoreRoller.tierCapFor(99));
    }

    @Test
    void floorFollowsBestRolledCappedByLevel() {
        int tu = CoreTier.TU.ordinal();
        // ví dụ của người chơi: ra Tử ở cấp 40 -> cấp 50 và 60 tối thiểu Tử
        assertTrue(CoreRoller.floorTier(tu, 5) >= tu);
        assertTrue(CoreRoller.floorTier(tu, 6) >= tu);
        assertEquals(tu, CoreRoller.floorTier(tu, 5));
        assertEquals(tu, CoreRoller.floorTier(tu, 6));
        // cấp 20: trần sàn = Hoàng nên chưa ép lên Tử
        assertEquals(CoreTier.HOANG.ordinal(), CoreRoller.floorTier(tu, 2));
        // chưa từng roll gì -> sàn 0; sàn không bao giờ vượt trần phẩm của mốc
        assertEquals(0, CoreRoller.floorTier(0, 9));
        for (int m = 1; m <= 11; m++)
            for (int best = 0; best <= 7; best++) {
                int f = CoreRoller.floorTier(best, m);
                assertTrue(f >= 0 && f <= CoreRoller.ceilTier(m), "m=" + m + " best=" + best);
                assertTrue(f <= Math.max(0, best));
            }
        // trần phẩm không giảm theo cấp
        for (int m = 2; m <= 11; m++) assertTrue(CoreRoller.ceilTier(m) >= CoreRoller.ceilTier(m - 1));
    }

    @Test
    void rolledTierNeverBelowFloorNorAboveCeil() {
        for (int m = 1; m <= 11; m++)
            for (int best = 0; best <= 6; best++)
                for (long seed = 0; seed < 300; seed++) {
                    int t = CoreRoller.rollTier(m, 0, best, new java.util.Random(seed)).tier();
                    assertTrue(t >= CoreRoller.floorTier(best, m), "m=" + m + " best=" + best + " t=" + t);
                    assertTrue(t <= CoreRoller.ceilTier(m));
                }
    }

    @Test
    void level99GuaranteedThatThaiWith80_20Split() {
        assertEquals(11, CoreRoller.MILESTONE_99);
        int[] w = CoreRoller.tierWeights(11, 15); // Vận không đổi phân bố mốc 99
        assertEquals(80, w[CoreTier.THAT_THAI.ordinal()]);
        assertEquals(20, w[CoreTier.CUU_THAI.ordinal()]);
        int that = 0, cuu = 0, n = 20000;
        for (long seed = 0; seed < n; seed++) {
            int t = CoreRoller.rollTier(11, 0, 0, new java.util.Random(seed * 7919L + 1)).tier();
            assertTrue(t >= CoreTier.THAT_THAI.ordinal(), "t=" + t);
            if (t == CoreTier.THAT_THAI.ordinal()) that++; else cuu++;
        }
        assertEquals(0.80, that / (double) n, 0.02);
        assertEquals(0.20, cuu / (double) n, 0.02);
    }

    @Test
    void rainbowTiersScaleStatsOverHong() throws Exception {
        var all = new LinkedHashMap<ResourceLocation, CoreDef>();
        try (var st = java.nio.file.Files.list(java.nio.file.Path.of(CoreRollerTest.class.getResource("/data/hukhong/hukhong/core/hoa_chung.json").toURI()).getParent())) {
            for (var f : (Iterable<java.nio.file.Path>) st::iterator) {
                if (f.getFileName().toString().startsWith("than_")) continue; // Thần Lõi: 1 giá trị cố định
                var def = CoreDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(java.nio.file.Files.readString(f))).getOrThrow();
                all.put(ResourceLocation.fromNamespaceAndPath("hukhong", f.getFileName().toString()), def);
            }
        }
        assertEquals(32, all.size());
        for (var def : all.values())
            for (var ps : def.passives()) {
                assertEquals(7, ps.values().size(), def.nameKey());
                double hong = ps.valueAt(CoreTier.HONG);
                assertEquals(hong * CoreTier.THAT_THAI.statMul, ps.valueAt(CoreTier.THAT_THAI), Math.abs(hong) * 0.01 + 1e-4);
                assertEquals(hong * CoreTier.CUU_THAI.statMul, ps.valueAt(CoreTier.CUU_THAI), Math.abs(hong) * 0.01 + 1e-4);
                assertTrue(Math.abs(ps.valueAt(CoreTier.CUU_THAI)) > Math.abs(ps.valueAt(CoreTier.THAT_THAI)));
            }
        // JSON cũ 5 ô vẫn có giá trị cầu vồng suy ra từ Hồng
        var legacy = new CoreDef.Passive(ResourceLocation.fromNamespaceAndPath("hukhong", "attack_power"),
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE, java.util.List.of(1.0, 2.0, 3.0, 4.0, 5.0));
        assertEquals(7.5, legacy.valueAt(CoreTier.THAT_THAI), 1e-9);
        assertEquals(5.0, legacy.valueAt(CoreTier.KIM), 1e-9);
    }

    @Test
    void rollRespectsBestTierFloorAtMilestone() throws Exception {
        var all = load();
        for (long seed = 0; seed < 200; seed++) {
            var p = CoreRoller.roll(6, seed, 0, Aptitude.D, Set.of("kiem"), Set.of(), all, CoreTier.TU.ordinal());
            assertTrue(p.tier() >= CoreTier.TU.ordinal());
        }
        var p99 = CoreRoller.roll(11, 5L, 0, Aptitude.A, Set.of("kiem"), Set.of(), all, 0);
        assertTrue(p99.tier() >= CoreTier.THAT_THAI.ordinal());
    }
}
