package com.hukhong.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Dinh nghia linh thao (data/hukhong/hukhong_herbs/*.json). Thuan Java: test duoc bang JUnit. */
public record HerbDef(String id, String item, int seasonMask, List<String> biomes, int rarity) {
    public static final List<String> SEASONS = List.of("xuan", "ha", "thu", "dong");
    /** 12 linh thao (thu tu dang ky). 4 loai dau co vat pham san trong AlchemyRegistry. */
    public static final List<String> IDS = List.of("linh_thao", "thanh_diep", "hoa_lien", "huyet_tham",
            "bang_lien", "hoa_linh_chi", "tu_linh_thao", "ngung_hon_hoa", "kim_tinh_thao", "thuy_tam_thao",
            "phong_linh_diep", "dia_long_can");

    public static HerbDef parse(String id, JsonObject o) {
        int mask = 0;
        JsonArray ss = o.getAsJsonArray("seasons");
        if (ss == null || ss.isEmpty()) throw new IllegalArgumentException(id + ": seasons required");
        for (var e : ss) {
            int i = SEASONS.indexOf(e.getAsString().toLowerCase(Locale.ROOT));
            if (i < 0) throw new IllegalArgumentException(id + ": bad season " + e.getAsString());
            mask |= 1 << i;
        }
        List<String> biomes = new ArrayList<>();
        JsonArray bs = o.getAsJsonArray("biomes");
        if (bs == null || bs.isEmpty()) throw new IllegalArgumentException(id + ": biomes required");
        for (var e : bs) biomes.add(e.getAsString());
        int rarity = o.has("rarity") ? Math.max(1, o.get("rarity").getAsInt()) : 8;
        String item = o.has("item") ? o.get("item").getAsString() : "hukhong:" + id;
        return new HerbDef(id, item, mask, List.copyOf(biomes), rarity);
    }
}
