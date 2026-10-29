package com.hukhong.story;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hukhong.story.world.Lore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Moi Van Kien co lang key (vi + en) va model item; bo A/B/C du 10; Lore.fromJson doc dung. */
class StoryDocsLangTest {
    private static String res(String path) throws IOException {
        try (java.io.InputStream in = StoryDocsLangTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "thieu resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Set<String> keys(String file) throws IOException {
        JsonObject o = JsonParser.parseString(res("/assets/hukhong/lang/" + file)).getAsJsonObject();
        Set<String> k = new HashSet<>(o.keySet());
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve("docs/research"))) root = root.getParent();
        Path dir = root == null ? null : root.resolve("docs/research");
        if (dir != null) { // khoa cho cho controller merge (tools/merge_lang.py)
            List<Path> txts;
            try (Stream<Path> s = Files.list(dir)) {
                txts = s.filter(x -> x.getFileName().toString().startsWith("lang-") && x.toString().endsWith(".txt")).toList();
            }
            for (Path p : txts)
                for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 3 && parts[1].trim().startsWith("vi:")) k.add(parts[0].trim());
                }
        }
        return k;
    }

    @Test
    void everyDocHasLangKeysAndModel() throws IOException {
        Set<String> vi = keys("vi_vn.json"), en = keys("en_us.json");
        for (StoryDocs.Doc d : StoryDocs.ALL) {
            for (String k : new String[]{d.itemKey(), d.textKey()}) {
                assertTrue(vi.contains(k), "thieu vi " + k);
                assertTrue(en.contains(k), "thieu en " + k);
            }
            assertNotNull(StoryDocsLangTest.class.getResource("/assets/hukhong/models/item/van_kien_" + d.id() + ".json"), "thieu model " + d.id());
        }
    }

    @Test
    void setsHaveTenEachAndIdsUnique() {
        assertEquals(10, StoryDocs.sizeOf('a'));
        assertEquals(10, StoryDocs.sizeOf('b'));
        assertEquals(10, StoryDocs.sizeOf('c'));
        assertEquals(30, StoryDocs.ALL.size());
        assertEquals(30, StoryDocs.ALL.stream().map(StoryDocs.Doc::id).distinct().count());
    }

    @Test
    void loreParses() {
        Lore l = Lore.fromJson(JsonParser.parseString("{\"faction\":\"tien_nhan\",\"chapter\":[3,10],\"requires_flags\":[\"x\"],"
                + "\"sets_flags\":[\"wl.a\"],\"clue\":\"c3\",\"rumor\":\"r\",\"modao_line\":\"m\"}").getAsJsonObject());
        assertEquals(com.hukhong.story.world.Faction.TIEN_NHAN, l.faction());
        assertEquals(3, l.chapterMin());
        assertEquals(10, l.chapterMax());
        assertEquals("c3", l.clue());
        assertNotNull(StoryDocs.get(l.clue()));
        assertEquals(Lore.NONE, Lore.fromJson(null));
    }
}
