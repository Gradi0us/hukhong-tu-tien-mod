package com.hukhong.structures;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Tim file cau truc theo the loai: (a) thu muc nguoi dung config/hukhong/structures/&lt;cat&gt;/ (uu tien),
 * (b) data/hukhong/structure/&lt;cat&gt;/ trong mod. Chon tat dinh theo seed.
 */
public final class StructureLibrary {
    private StructureLibrary() {}

    private static final Logger LOG = LogUtils.getLogger();

    public static final List<String> CATEGORIES = List.of("city_large", "city_mountain", "city_ruined", "port", "sect", "trade", "village", "ruin", "tower");

    public static Path userRoot() { return FMLPaths.GAMEDIR.get().resolve("config").resolve("hukhong").resolve("structures"); }

    /** Tao san thu muc nguoi dung (de nguoi dung biet bo file vao dau). */
    public static void ensureUserDirs() {
        try {
            for (String c : CATEGORIES) Files.createDirectories(userRoot().resolve(c));
        } catch (IOException | RuntimeException e) {
            LOG.warn("Cannot create user structure folders: {}", e.toString());
        }
    }

    private static boolean ok(String n) {
        String s = n.toLowerCase(java.util.Locale.ROOT);
        return s.endsWith(".nbt") || s.endsWith(".schem");
    }

    private interface Opener { InputStream open() throws IOException; }

    private record Entry(String name, Opener opener, Path sidecar) {
        Entry(String name, Opener opener) { this(name, opener, null); }
    }

    private static List<Entry> userFiles(String cat) {
        List<Entry> out = new ArrayList<>();
        Path dir = userRoot().resolve(cat);
        if (!Files.isDirectory(dir)) return out;
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> Files.isRegularFile(p) && ok(p.getFileName().toString()))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(p -> out.add(new Entry("user:" + p.getFileName(), () -> Files.newInputStream(p),
                            p.resolveSibling(p.getFileName() + ".markers.json"))));
        } catch (IOException | RuntimeException e) {
            LOG.warn("Cannot list {}: {}", dir, e.toString());
        }
        return out;
    }

    private static List<Entry> modFiles(MinecraftServer server, String cat) {
        List<Entry> out = new ArrayList<>();
        try {
            Map<ResourceLocation, Resource> m = server.getResourceManager().listResources("structure/" + cat,
                    rl -> rl.getNamespace().equals("hukhong") && ok(rl.getPath()));
            m.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(e -> out.add(new Entry("mod:" + e.getKey().getPath(), () -> e.getValue().open())));
        } catch (RuntimeException e) {
            LOG.warn("Cannot list mod structures {}: {}", cat, e.toString());
        }
        return out;
    }

    /**
     * Chon + doc 1 cau truc cho (category, key). User file thang mod file. Tra null neu khong co file hop le
     * (caller roi ve builder thu tuc). Moi lan loi doc file -&gt; thu file ke tiep.
     */
    public static LoadedStructure pick(MinecraftServer server, String category, String key, long seed) {
        for (List<Entry> list : List.of(userFiles(category), modFiles(server, category))) {
            if (list.isEmpty()) continue;
            int start = Math.floorMod((int) ((seed * 0x9E3779B97F4A7C15L + key.hashCode() * 31L) >>> 20), list.size());
            for (int i = 0; i < list.size(); i++) {
                Entry e = list.get((start + i) % list.size());
                try (InputStream in = e.opener().open()) {
                    LoadedStructure st = StructureParser.parse(e.name(), in);
                    readSidecar(st, e.sidecar());
                    LOG.info("Structure '{}' ({}x{}x{}, {} BE, {} markers) chosen for {}/{}", e.name(), st.sx, st.sy, st.sz,
                            st.blockEntities.size(), st.markers.size(), category, key);
                    return st;
                } catch (IOException | RuntimeException ex) {
                    LOG.warn("Skipping structure {}: {}", e.name(), ex.toString());
                }
            }
        }
        return null;
    }


    /** Doc dung file theo ten (vd "lobby_market" hoac "lobby_market.schem") trong thu muc nguoi dung; null neu khong co/loi. Dung cho lenh thu. */
    public static LoadedStructure pickNamed(String category, String name) {
        String n = name.toLowerCase(java.util.Locale.ROOT);
        for (Entry e : userFiles(category)) {
            String f = e.name().substring(5).toLowerCase(java.util.Locale.ROOT);
            if (!f.equals(n) && !f.startsWith(n + ".")) continue;
            try (InputStream in = e.opener().open()) {
                LoadedStructure st = StructureParser.parse(e.name(), in);
                readSidecar(st, e.sidecar());
                return st;
            } catch (IOException | RuntimeException ex) {
                LOG.warn("Cannot load structure {}: {}", e.name(), ex.toString());
            }
        }
        return null;
    }

    /** Sidecar &lt;file&gt;.markers.json: {"markers":[{"id":"altar","x":0,"y":0,"z":0}]} (toa do cuc bo cua file). Chi them marker neu file chua co marker cung id. */
    private static void readSidecar(LoadedStructure st, Path sc) {
        if (sc == null || !Files.isRegularFile(sc)) return;
        try (java.io.Reader r = Files.newBufferedReader(sc, java.nio.charset.StandardCharsets.UTF_8)) {
            com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("markers");
            for (com.google.gson.JsonElement el : arr) {
                com.google.gson.JsonObject o = el.getAsJsonObject();
                int x = o.get("x").getAsInt(), y = o.get("y").getAsInt(), z = o.get("z").getAsInt();
                if (x < 0 || y < 0 || z < 0 || x >= st.sx || y >= st.sy || z >= st.sz) continue;
                LoadedStructure.Marker m = new LoadedStructure.Marker(x, y, z, o.get("id").getAsString());
                if (st.markers.stream().noneMatch(k -> k.id().equals(m.id()) && k.x() == m.x() && k.y() == m.y() && k.z() == m.z()))
                    st.markers.add(m);
            }
        } catch (IOException | RuntimeException ex) {
            LOG.warn("Bad markers sidecar {}: {}", sc, ex.toString());
        }
    }
}
