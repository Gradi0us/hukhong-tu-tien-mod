package com.hukhong.story.chain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hukhong.story.chain.CityArcKit.Arc;
import com.hukhong.story.chain.CityArcKit.Ctx;
import com.hukhong.world.calendar.EventDef;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 7 cung truyen thanh: chuyen buoc, nguon Di thu C, moi hanh dong / co / khoa lang trong thoai + su kien Lich deu ton tai. */
class CityArcKitTest {
    private static String res(String p) throws IOException {
        try (InputStream in = CityArcKitTest.class.getResourceAsStream(p)) {
            assertNotNull(in, p);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Khoa lang vi (file lang + docs/research/lang-*.txt chua merge). */
    static Set<String> langKeys() throws IOException {
        Set<String> k = new HashSet<>(JsonParser.parseString(res("/assets/hukhong/lang/vi_vn.json")).getAsJsonObject().keySet());
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve("docs/research"))) root = root.getParent();
        if (root != null) {
            try (var s = Files.list(root.resolve("docs/research"))) {
                for (Path p : s.filter(x -> x.getFileName().toString().startsWith("lang-") && x.toString().endsWith(".txt")).toList())
                    for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                        String[] parts = line.split("\\|");
                        if (parts.length >= 3 && parts[1].trim().startsWith("vi:")) k.add(parts[0].trim());
                    }
            }
        }
        return k;
    }

    @Test
    void everyArcWalksThreeStepsToDone() {
        Map<String, Ctx[]> walk = Map.of(
                CityArcKit.CHO_MAU, new Ctx[]{Ctx.NONE.withNight(true).withNearCity(true), Ctx.NONE.withReached(true), Ctx.NONE.withCleared(true)},
                CityArcKit.HAC_TRIEU, new Ctx[]{Ctx.NONE.withCleared(true), Ctx.NONE.withPearls(5), null},
                CityArcKit.DAU_GIA, new Ctx[]{Ctx.NONE.withScene(true), Ctx.NONE.withReached(true), Ctx.NONE.withCleared(true)},
                CityArcKit.LOI_VU, new Ctx[]{Ctx.NONE.withReached(true), Ctx.NONE.withStorm(true), null},
                CityArcKit.DAI_HOI, new Ctx[]{Ctx.NONE.withCleared(true), Ctx.NONE.withCleared(true), Ctx.NONE.withRival(true)},
                CityArcKit.KIEM_TRUNG, new Ctx[]{Ctx.NONE.withReached(true), Ctx.NONE.withCleared(true), Ctx.NONE.withModao(true)},
                CityArcKit.HOANG_DO, new Ctx[]{Ctx.NONE.withNearCity(true), Ctx.NONE.withRetaken(true), null});
        assertEquals(CityArcKit.ARCS.size(), walk.size());
        for (Arc a : CityArcKit.ARCS) {
            Ctx[] w = walk.get(a.id());
            int st = 1;
            for (int i = 0; i < 3; i++) {
                assertEquals(st, CityArcKit.advance(a.id(), st, Ctx.NONE), a.id() + " khong duoc tu chuyen buoc " + st);
                if (w[i] == null) { assertEquals(3, st, a.id() + " buoc doi thoai phai la 3"); break; }
                int nx = CityArcKit.advance(a.id(), st, w[i]);
                assertEquals(st == 3 ? 9 : st + 1, nx, a.id() + " buoc " + st);
                st = nx;
            }
        }
        assertEquals(3, CityArcKit.advance(CityArcKit.LOI_VU, 1, Ctx.NONE.withStorm(true)));
        assertEquals(3, CityArcKit.advance(CityArcKit.LOI_VU, 2, Ctx.NONE.withStorm(true)));
        assertEquals(1, CityArcKit.advance(CityArcKit.CHO_MAU, 1, Ctx.NONE.withNearCity(true)), "ban ngay khong mo Cho Mau");
        assertEquals(2, CityArcKit.advance(CityArcKit.HAC_TRIEU, 2, Ctx.NONE.withPearls(4)));
    }

    @Test
    void offerRules() {
        Arc a = CityArcKit.byId(CityArcKit.CHO_MAU);
        assertFalse(CityArcKit.offerApplies(a, List.of(), 3, 50));
        assertTrue(CityArcKit.offerApplies(a, List.of(), 4, 30));
        assertTrue(CityArcKit.offerApplies(a, List.of(), 9, 80));
        assertFalse(CityArcKit.offerApplies(a, List.of("qc.cho_mau.2"), 5, 50));
        assertFalse(CityArcKit.offerApplies(a, List.of("qc.cho_mau.done"), 5, 50));
        assertTrue(CityArcKit.isNight(14000) && !CityArcKit.isNight(6000) && CityArcKit.isNight(24000L * 3 + 15000));
    }

    @Test
    void allTenCDocsObtainable() {
        Set<String> c = CityArcKit.guaranteedC();
        for (int i = 1; i <= 10; i++) assertTrue(c.contains("c" + i), "c" + i + " khong co nguon co dinh");
        assertEquals("c10", CityArcKit.chgClue(3));
        assertNull(CityArcKit.chgClue(1));
        for (Arc a : CityArcKit.ARCS) for (String d : a.docs()) assertNotNull(com.hukhong.story.StoryDocs.get(d), d);
        assertTrue(CityArcKit.byId(CityArcKit.CHO_MAU).docs().contains("a7"));
        assertTrue(CityArcKit.byId(CityArcKit.DAU_GIA).docs().contains("b7"));
    }

    private static final Set<String> ACTIONS = Set.of("set_flag", "give_item", "give_skill", "exp", "chapter", "step_done", "duel", "qc", "doc", "message");

    /** Duyet JSON thoai: kiem hanh dong hop le, gom co dat / co dung / khoa text / chuoi qc. */
    private static void scan(JsonObject o, Set<String> setFlags, Set<String> usedFlags, Set<String> textKeys, Set<String> chains) {
        for (var en : o.entrySet()) {
            JsonElement v = en.getValue();
            String k = en.getKey();
            if (k.equals("actions")) {
                for (JsonElement ae : v.getAsJsonArray()) {
                    JsonObject a = ae.getAsJsonObject();
                    String t = a.get("type").getAsString();
                    assertTrue(ACTIONS.contains(t), "hanh dong la: " + t);
                    if (t.equals("set_flag")) setFlags.add(a.get("flag").getAsString());
                    if (t.equals("qc")) { chains.add(a.get("chain").getAsString()); assertTrue(a.get("stage").getAsInt() >= 0); }
                    if (t.equals("doc")) assertNotNull(com.hukhong.story.StoryDocs.get(a.get("id").getAsString()));
                }
            } else if (k.equals("flags") || k.equals("not_flags")) {
                for (JsonElement f : v.getAsJsonArray()) usedFlags.add(f.getAsString());
            } else if (k.equals("text") && v.isJsonArray()) {
                for (JsonElement x : v.getAsJsonArray()) textKeys.add(x.getAsString());
            } else if (k.equals("text") && v.isJsonPrimitive()) {
                textKeys.add(v.getAsString());
            } else if (v.isJsonObject()) scan(v.getAsJsonObject(), setFlags, usedFlags, textKeys, chains);
            else if (v.isJsonArray()) for (JsonElement x : (JsonArray) v) if (x.isJsonObject()) scan(x.getAsJsonObject(), setFlags, usedFlags, textKeys, chains);
        }
    }

    @Test
    void arcDialoguesReferenceExistingActionsFlagsAndLang() throws IOException {
        Set<String> lang = langKeys();
        Set<String> known = Set.of("ch2_done", "ch4_done", "ch1_done", "ch5_done", "ch6_done", "doubt_mo_dao", "trust_mo_dao",
                "mo_dao_left", "mo_dao_died", "ending_seal", "ending_absorb", "ending_truth", "rival_saved", "rival_lost");
        for (Arc a : CityArcKit.ARCS) {
            JsonObject d = JsonParser.parseString(res("/data/hukhong/hukhong/dialogue/" + a.npc() + ".json")).getAsJsonObject();
            assertEquals(a.npc(), d.get("npc").getAsString());
            assertEquals(a.cityId(), d.get("city").getAsString());
            assertNotNull(com.hukhong.city.CityDef.byId(a.cityId()));
            assertEquals(a.city(), com.hukhong.city.CityDef.byId(a.cityId()).index());
            Set<String> set = new HashSet<>(), used = new HashSet<>(), text = new HashSet<>(), chains = new HashSet<>();
            scan(d, set, used, text, chains);
            assertEquals(Set.of(a.id()), chains, a.npc());
            for (String f : used) assertTrue(f.startsWith("qc." + a.id() + ".") || known.contains(f), a.npc() + " co la: " + f);
            for (String k : text) assertTrue(lang.contains(k), "thieu lang " + k);
            assertTrue(lang.contains(d.get("name_key").getAsString()));
            for (int s = 1; s <= 3; s++) {
                assertTrue(lang.contains("hukhong.qc." + a.id() + ".s" + s), a.id() + ".s" + s);
                assertTrue(lang.contains("hukhong.qc." + a.id() + ".hint." + s), a.id() + ".hint." + s);
            }
            for (String k : List.of("done", "reset")) assertTrue(lang.contains("hukhong.qc." + a.id() + "." + k));
            assertTrue(lang.contains(a.modaoKey()), a.modaoKey());
            EventDef ev = EventDef.parse(a.calEvent(), JsonParser.parseString(res("/data/hukhong/hukhong_events/" + a.calEvent() + ".json")).getAsJsonObject());
            assertTrue(ev.story());
            assertEquals(a.chMin(), ev.lore().chapterMin());
            assertEquals(a.chMax(), ev.lore().chapterMax());
            assertFalse(ev.rec().starts(ev.id(), 0, 28L * 96000).isEmpty(), a.calEvent());
            assertTrue(lang.contains(ev.nameKey()), ev.nameKey());
            assertTrue(lang.contains(ev.rewardKey()), ev.rewardKey());
        }
        Set<String> set = new HashSet<>(), used = new HashSet<>(), text = new HashSet<>(), chains = new HashSet<>();
        scan(JsonParser.parseString(res("/data/hukhong/hukhong/dialogue/hang_chieu.json")).getAsJsonObject(), set, used, text, chains);
        assertTrue(used.contains("qc.dau_gia.c.seen") && set.contains("qc.told.hc_auction"));
        for (String k : text) assertTrue(lang.contains(k), "thieu lang " + k);
        set = new HashSet<>(); used = new HashSet<>(); text = new HashSet<>(); chains = new HashSet<>();
        scan(JsonParser.parseString(res("/data/hukhong/hukhong/dialogue/mo_dao.json")).getAsJsonObject(), set, used, text, chains);
        assertTrue(used.contains("ending_truth") && text.contains("hukhong.dlg.mo_dao.ch6_truth.p1"));
        for (String k : text) assertTrue(lang.contains(k), "thieu lang " + k);
    }
}
