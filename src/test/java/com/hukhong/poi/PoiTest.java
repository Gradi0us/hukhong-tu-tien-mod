package com.hukhong.poi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoiTest {
    private static Path dir() {
        try {
            return Path.of(PoiTest.class.getResource("/data/hukhong/hukhong_poi/bia_da.json").toURI()).getParent();
        } catch (java.net.URISyntaxException ex) { throw new IllegalStateException(ex); }
    }
    private static final Set<String> ENC = Set.of("none", "approach", "timed", "night_squads");
    private static final Set<String> PATHS = Set.of("", "kiem", "dao", "the", "ma", "phap");
    private static final Set<String> MOBS = Set.of("hu_thi", "hu_cot", "hu_chu");

    private static List<PoiDef> defs() throws IOException {
        List<PoiDef> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir())) {
            for (Path p : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".json"))::iterator) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                String id = p.getFileName().toString().replace(".json", "");
                out.add(PoiDef.parse(id, o));
            }
        }
        return out;
    }

    @Test void every_location_json_parses_and_is_consistent() throws IOException {
        List<PoiDef> all = defs();
        Set<String> ids = new HashSet<>();
        for (PoiDef d : all) {
            ids.add(d.id());
            assertTrue(PoiBuilders.known(d.builder()), d.id() + ": unknown builder " + d.builder());
            assertTrue(ENC.contains(d.encounter()), d.id() + ": encounter " + d.encounter());
            assertTrue(PATHS.contains(d.path()), d.id() + ": path");
            assertTrue(d.rarity() > 0 && d.rarity() <= 1, d.id() + ": rarity");
            assertTrue(d.minSpacing() >= 100, d.id() + ": spacing");
            assertTrue(d.levelMin() >= 1 && d.levelMax() >= d.levelMin() && d.levelMax() <= 100, d.id() + ": level");
            assertFalse(d.biomes().isEmpty());
            assertNotNull(d.lore());
            assertTrue(d.lore().chapterMin() <= d.lore().chapterMax(), d.id() + ": chapter range");
            if (d.lore().clue() != null) assertTrue(d.lore().clue().matches("c([1-9]|10)"), d.id() + ": clue must be a C document");
            if (d.encounter().equals("approach") || d.encounter().equals("timed") || d.encounter().equals("night_squads"))
                assertFalse(d.mobs().isEmpty(), d.id() + ": encounter needs mobs");
            for (PoiDef.MobSpawn m : d.mobs()) {
                String t = m.type().replace("hukhong:", "");
                assertTrue(MOBS.contains(t) || m.type().startsWith("minecraft:"), d.id() + ": mob " + m.type());
                assertTrue(m.count() >= 1);
                assertTrue(m.at().equals("room") || m.at().equals("boss"));
            }
            for (PoiDef.Loot l : d.loot()) {
                assertTrue(l.item().startsWith("@") ? l.item().equals("@tutinh") || l.item().equals("@hontinh") || l.item().equals("@bone") || l.item().matches("@skillbook:(pham|huyen|dia|thien)")
                        : l.item().matches("[a-z0-9_]+:[a-z0-9_/]+"), d.id() + ": loot " + l.item());
                assertTrue(l.min() >= 0 && l.max() >= l.min() && l.chance() > 0 && l.chance() <= 1);
            }
        }
        for (String need : List.of("bia_da", "tho_dia_mieu", "linh_tuyen", "khach_dien", "thac_linh_khi", "khe_nut_nho", "dong_phu", "co_chien_truong",
                "mo_vo_danh", "tan_tich_tong_mon", "kiem_trung", "ma_uyen", "dao_quan", "vo_dai", "ngu_hanh_tran_dai"))
            assertTrue(ids.contains(need), "missing location " + need);
    }

    @Test void path_locations_cover_every_path_once() throws IOException {
        Set<String> seen = new HashSet<>();
        for (PoiDef d : defs()) if (!d.path().isEmpty()) assertTrue(seen.add(d.path()), "duplicate path " + d.path());
        assertEquals(Set.of("kiem", "dao", "the", "ma", "phap"), seen);
    }

    @Test void story_locations_with_c_clue_and_inn_has_travel() throws IOException {
        PoiDef inn = defs().stream().filter(d -> d.id().equals("khach_dien")).findFirst().orElseThrow();
        assertTrue(inn.travel());
        assertEquals("hukhong.poi.rumor.khach_dien", inn.rumorKey());
        PoiDef bia = defs().stream().filter(d -> d.id().equals("bia_da")).findFirst().orElseThrow();
        assertEquals("hukhong.poi.t.bia_da", bia.textKey());
        assertEquals("hukhong.poi.n.bia_da", bia.nameKey());
    }

    @Test void candidates_are_deterministic_inside_cell_and_respect_rarity() {
        long seed = 123456789L;
        int hits = 0, cells = 0;
        for (int cx = -40; cx < 40; cx++) for (int cz = -40; cz < 40; cz++) {
            cells++;
            int[] a = PoiMath.candidate(seed, "bia_da", 0.5, cx, cz), b = PoiMath.candidate(seed, "bia_da", 0.5, cx, cz);
            assertEquals(a == null, b == null);
            if (a == null) continue;
            hits++;
            assertEquals(a[0], b[0]);
            assertEquals(a[1], b[1]);
            assertTrue(a[0] >= cx * PoiMath.CELL + PoiMath.MARGIN && a[0] < (cx + 1) * PoiMath.CELL - PoiMath.MARGIN + 1);
            assertTrue(a[1] >= cz * PoiMath.CELL + PoiMath.MARGIN && a[1] < (cz + 1) * PoiMath.CELL - PoiMath.MARGIN + 1);
            assertEquals(cx, PoiMath.cellOf(a[0]));
        }
        double f = hits / (double) cells;
        assertTrue(f > 0.44 && f < 0.56, "rarity 0.5 gave " + f);
        assertNull(PoiMath.candidate(seed, "x", 0.0, 3, 3));
        // khac id / khac seed -> khac vi tri
        int[] p = PoiMath.candidate(seed, "bia_da", 1.0, 2, 2), q = PoiMath.candidate(seed, "linh_tuyen", 1.0, 2, 2), r = PoiMath.candidate(seed + 1, "bia_da", 1.0, 2, 2);
        assertTrue(p[0] != q[0] || p[1] != q[1]);
        assertTrue(p[0] != r[0] || p[1] != r[1]);
    }

    @Test void candidate_in_chunk_is_found_in_exactly_one_chunk() {
        long seed = 99L;
        int[] c = PoiMath.candidate(seed, "dong_phu", 1.0, 1, -2);
        int found = 0;
        for (int chx = PoiMath.CELL / 16; chx < 2 * PoiMath.CELL / 16; chx++) for (int chz = -2 * PoiMath.CELL / 16; chz < -PoiMath.CELL / 16; chz++) {
            int[] r = PoiMath.candidateInChunk(seed, "dong_phu", 1.0, chx, chz);
            if (r != null) { found++; assertEquals(c[0], r[0]); assertEquals(c[1], r[1]); assertEquals(c[0] >> 4, chx); }
        }
        assertEquals(1, found);
    }

    @Test void spacing_and_compass() {
        assertTrue(PoiMath.farEnough(0, 0, 400, 0, 400));
        assertFalse(PoiMath.farEnough(0, 0, 399, 0, 400));
        assertEquals(0, PoiMath.compass8(0, -10));
        assertEquals(2, PoiMath.compass8(10, 0));
        assertEquals(4, PoiMath.compass8(0, 10));
        assertEquals(6, PoiMath.compass8(-10, 0));
        assertEquals(1, PoiMath.compass8(10, -10));
    }

    @Test void puzzle_order_is_a_permutation_and_solves() {
        for (int seed = -5; seed < 60; seed++) for (int n : new int[]{4, 5}) {
            int[] lay = PoiPuzzle.layout(seed, n), ord = PoiPuzzle.slotOrder(seed, n);
            boolean[] a = new boolean[n], b = new boolean[n];
            for (int v : lay) a[v] = true;
            for (int v : ord) b[v] = true;
            for (int k = 0; k < n; k++) assertTrue(a[k] && b[k]);
            int p = 0;
            PoiPuzzle.Step st = null;
            for (int slot : ord) { st = PoiPuzzle.press(ord, p, slot); assertFalse(st.reset()); p = st.progress(); }
            assertTrue(st.solved());
            // tuong sinh: phan tu tai buoc k = (start + k) trong elements(n)
            int[] el = PoiPuzzle.elements(n);
            for (int k = 0; k < n; k++) assertEquals(el[(PoiPuzzle.start(seed, n) + k) % n], PoiPuzzle.elementAt(seed, n, ord[k]));
        }
    }

    @Test void wrong_press_resets_progress() {
        int[] ord = PoiPuzzle.slotOrder(7, 4);
        PoiPuzzle.Step s1 = PoiPuzzle.press(ord, 0, ord[0]);
        assertEquals(1, s1.progress());
        PoiPuzzle.Step bad = PoiPuzzle.press(ord, 1, ord[0]);
        assertTrue(bad.reset());
        assertEquals(0, bad.progress());
        assertTrue(PoiPuzzle.press(ord, 4, 0).solved());
    }

    @Test void rumor_pointer_picks_nearest_undiscovered() {
        PoiInstance inn = new PoiInstance("khach_dien", 0, 64, 0, 1);
        PoiInstance far = new PoiInstance("bia_da", 900, 64, 0, 1), near = new PoiInstance("tho_dia_mieu", -120, 64, 30, 2);
        List<PoiInstance> all = List.of(inn, far, near);
        PoiInstance t = PoiRumor.nearestUndiscovered(all, Set.of(), 0, 0, i -> i != inn);
        assertEquals(near, t);
        assertEquals(far, PoiRumor.nearestUndiscovered(all, Set.of(near.key), 0, 0, i -> i != inn));
        assertNull(PoiRumor.nearestUndiscovered(all, Set.of(near.key, far.key), 0, 0, i -> i != inn));
        assertEquals("hukhong.poi.rumor.chapter.1", PoiRumor.storyKey(0));
        assertEquals("hukhong.poi.rumor.chapter.10", PoiRumor.storyKey(99));
    }

    @Test void doc_source_pointer_skips_owned_documents() {
        PoiInstance a = new PoiInstance("bia_da", 100, 64, 0, 1), b = new PoiInstance("dong_phu", 500, 64, 0, 1), other = new PoiInstance("linh_tuyen", 10, 64, 0, 1);
        PoiDef da = defOf("c2"), db = defOf("c5"), dn = defOf(null);
        java.util.function.Function<String, PoiDef> f = id -> id.equals("bia_da") ? da : id.equals("dong_phu") ? db : dn;
        List<PoiInstance> all = List.of(a, b, other);
        assertEquals(a, PoiRumor.nearestDocSource(all, f, c -> false, 0, 0, null));
        assertEquals(b, PoiRumor.nearestDocSource(all, f, c -> c.equals("c2"), 0, 0, null));
        assertNull(PoiRumor.nearestDocSource(all, f, c -> true, 0, 0, null));
        assertEquals(b, PoiRumor.nearestDocSource(all, f, c -> false, 0, 0, a));
    }

    private static PoiDef defOf(String clue) {
        JsonObject o = new JsonObject();
        o.addProperty("builder", "stele");
        if (clue != null) { JsonObject l = new JsonObject(); l.addProperty("clue", clue); o.add("lore", l); }
        return PoiDef.parse("x", o);
    }

    @Test void exp_boost_never_lowers_and_scales() {
        assertEquals(130, PoiBoost.apply(100, PoiBoost.SOAK));
        assertEquals(150, PoiBoost.apply(100, PoiBoost.MEDITATE));
        assertEquals(1, PoiBoost.apply(1, 1.3));
        assertEquals(0, PoiBoost.apply(0, 1.5));
    }

    @Test void mob_level_is_clamped_into_location_range() {
        assertEquals(35, PoiMobs.clampLevel(1, 35, 70, 0));
        assertEquals(70, PoiMobs.clampLevel(100, 35, 70, 0));
        assertEquals(45, PoiMobs.clampLevel(45, 35, 70, 0));
        assertEquals(73, PoiMobs.clampLevel(100, 35, 70, 3));
    }
}
