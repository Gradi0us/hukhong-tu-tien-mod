package com.hukhong.story.world;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Trường "lore" chung cho JSON (sự kiện / địa điểm / mẫu nhiệm vụ / lô đấu giá). PLAN-v2 §2.2.
 * "chapter": [min,max] (hoặc một số); clue = id Văn Kiện (a1.., b1.., c1..) hoặc null.
 */
public record Lore(Faction faction, int chapterMin, int chapterMax, List<String> requiresFlags, List<String> setsFlags,
                   String clue, String rumor, String modaoLine) {
    public static final Lore NONE = new Lore(Faction.THANH, 0, 99, List.of(), List.of(), null, null, null);

    public static Lore fromJson(JsonObject o) {
        if (o == null) return NONE;
        int min = 0, max = 99;
        if (o.has("chapter")) {
            JsonElement c = o.get("chapter");
            if (c.isJsonArray() && c.getAsJsonArray().size() >= 2) {
                min = c.getAsJsonArray().get(0).getAsInt();
                max = c.getAsJsonArray().get(1).getAsInt();
            } else if (c.isJsonPrimitive()) { min = c.getAsInt(); max = min; }
        }
        if (o.has("chapter_min")) min = o.get("chapter_min").getAsInt();
        if (o.has("chapter_max")) max = o.get("chapter_max").getAsInt();
        return new Lore(o.has("faction") ? Faction.parse(o.get("faction").getAsString()) : Faction.THANH, min, max,
                strs(o, "requires_flags"), strs(o, "sets_flags"), str(o, "clue"), str(o, "rumor"), str(o, "modao_line"));
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() && !o.get(k).getAsString().isEmpty() ? o.get(k).getAsString() : null;
    }

    private static List<String> strs(JsonObject o, String k) {
        List<String> l = new ArrayList<>();
        if (o.has(k)) for (JsonElement e : o.getAsJsonArray(k)) l.add(e.getAsString());
        return List.copyOf(l);
    }
}
