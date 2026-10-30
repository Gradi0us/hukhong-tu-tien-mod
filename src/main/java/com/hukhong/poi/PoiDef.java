package com.hukhong.poi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hukhong.story.world.Lore;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Dinh nghia dia diem (data/hukhong/hukhong_poi/*.json). PLAN-v1 F2.
 * encounter: none | approach (quai khi nguoi choi toi gan, don xong -> ruong) | timed (khe nut: song quai, thoi han) | night_squads (dem: doi quan hon).
 */
public record PoiDef(String id, String builder, List<String> biomes, double rarity, int minSpacing, int levelMin, int levelMax,
                     String encounter, List<MobSpawn> mobs, List<Loot> loot, int respawnTicks, String path, boolean travel,
                     JsonObject params, Lore lore) {

    /** at: "room" (mac dinh) hoac "boss". hpMul/dmgMul nhan len chi so cap. */
    public record MobSpawn(String type, int count, int levelOffset, String at, double hpMul, double dmgMul, String name) {}

    /** item: id vat pham, hoac "@tutinh" / "@hontinh" (tien te), "@skillbook:huyen|dia|thien", "@bone". */
    public record Loot(String item, int min, int max, double chance) {}

    public String nameKey() { return "hukhong.poi.n." + id; }
    public String textKey() { return "hukhong.poi.t." + id; }
    public String rumorKey() { return lore.rumor() != null ? lore.rumor() : "hukhong.poi.rumor." + id; }

    public int intParam(String k, int def) { return GsonHelper.getAsInt(params, k, def); }
    public double dblParam(String k, double def) { return GsonHelper.getAsDouble(params, k, def); }
    public String strParam(String k, String def) { return GsonHelper.getAsString(params, k, def); }

    public static PoiDef parse(String id, JsonObject o) {
        List<String> biomes = new ArrayList<>();
        if (o.has("biomes")) for (JsonElement e : o.getAsJsonArray("biomes")) biomes.add(e.getAsString());
        if (biomes.isEmpty()) biomes.add("*");
        int lmin = 1, lmax = 100;
        if (o.has("level") && o.get("level").isJsonArray()) {
            JsonArray a = o.getAsJsonArray("level");
            if (a.size() >= 2) { lmin = a.get(0).getAsInt(); lmax = a.get(1).getAsInt(); }
        }
        List<MobSpawn> mobs = new ArrayList<>();
        if (o.has("mobs")) for (JsonElement e : o.getAsJsonArray("mobs")) {
            JsonObject m = e.getAsJsonObject();
            mobs.add(new MobSpawn(GsonHelper.getAsString(m, "type"), GsonHelper.getAsInt(m, "count", 1), GsonHelper.getAsInt(m, "level_offset", 0),
                    GsonHelper.getAsString(m, "at", "room"), GsonHelper.getAsDouble(m, "hp_mul", 1.0), GsonHelper.getAsDouble(m, "dmg_mul", 1.0),
                    GsonHelper.getAsString(m, "name", "")));
        }
        List<Loot> loot = new ArrayList<>();
        if (o.has("loot")) for (JsonElement e : o.getAsJsonArray("loot")) {
            JsonObject l = e.getAsJsonObject();
            int mn = GsonHelper.getAsInt(l, "min", 1);
            loot.add(new Loot(GsonHelper.getAsString(l, "item"), mn, GsonHelper.getAsInt(l, "max", mn), GsonHelper.getAsDouble(l, "chance", 1.0)));
        }
        Lore lore = o.has("lore") && o.get("lore").isJsonObject() ? Lore.fromJson(o.getAsJsonObject("lore")) : Lore.NONE;
        return new PoiDef(id, GsonHelper.getAsString(o, "builder"), List.copyOf(biomes), GsonHelper.getAsDouble(o, "rarity", 0.3),
                GsonHelper.getAsInt(o, "min_spacing", 400), lmin, lmax, GsonHelper.getAsString(o, "encounter", "none"),
                List.copyOf(mobs), List.copyOf(loot), GsonHelper.getAsInt(o, "respawn_ticks", 24000), GsonHelper.getAsString(o, "path", ""),
                GsonHelper.getAsBoolean(o, "travel", false), o.has("params") ? o.getAsJsonObject("params") : new JsonObject(), lore);
    }
}
