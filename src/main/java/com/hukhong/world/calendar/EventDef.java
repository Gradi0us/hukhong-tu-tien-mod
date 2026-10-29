package com.hukhong.world.calendar;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hukhong.story.world.Lore;
import net.minecraft.util.GsonHelper;

/** Dinh nghia su kien lich (data/hukhong/hukhong_events/*.json). */
public record EventDef(String id, String type, boolean story, LichMath.Recurrence rec, long duration, Loc loc,
                       int levelMin, int levelMax, String rewardKey, Lore lore) {

    public enum LocKind { CITY, NEAREST_CITY, RANDOM_CITY, FIXED }

    /** CITY: city = chi so thanh; FIXED: x,z. NEAREST_CITY duoc xu ly nhu RANDOM_CITY (tat dinh theo lan xuat hien). */
    public record Loc(LocKind kind, int city, int x, int z) {}

    public String nameKey() { return "hukhong.cal.ev." + id; }

    public static EventDef parse(String id, JsonObject o) {
        String type = GsonHelper.getAsString(o, "type", id);
        boolean story = "story".equals(GsonHelper.getAsString(o, "lane", "world"));
        JsonObject r = GsonHelper.getAsJsonObject(o, "recurrence");
        long offset = GsonHelper.getAsLong(o, "start_offset", 0L);
        String rt = GsonHelper.getAsString(r, "type", "every");
        LichMath.Recurrence rec = switch (rt) {
            case "weekly" -> new LichMath.Recurrence(LichMath.Kind.WEEKLY, 1, GsonHelper.getAsInt(r, "day", 0), 0, offset);
            case "random" -> new LichMath.Recurrence(LichMath.Kind.RANDOM, 1, 0, GsonHelper.getAsFloat(r, "weight", 0.3f), offset);
            default -> new LichMath.Recurrence(LichMath.Kind.EVERY, GsonHelper.getAsInt(r, "days", 1), 0, 0, offset);
        };
        Loc loc = parseLoc(o.get("location"));
        int[] lv = {1, 100};
        if (o.has("level") && o.get("level").isJsonArray() && o.getAsJsonArray("level").size() == 2) {
            lv[0] = o.getAsJsonArray("level").get(0).getAsInt();
            lv[1] = o.getAsJsonArray("level").get(1).getAsInt();
        }
        Lore lore = o.has("lore") && o.get("lore").isJsonObject() ? Lore.fromJson(o.getAsJsonObject("lore")) : Lore.NONE;
        return new EventDef(id, type, story, rec, GsonHelper.getAsLong(o, "duration", 6000L), loc, lv[0], lv[1],
                GsonHelper.getAsString(o, "rewards", "hukhong.cal.reward." + id), lore);
    }

    private static Loc parseLoc(JsonElement e) {
        if (e == null) return new Loc(LocKind.RANDOM_CITY, 0, 0, 0);
        if (e.isJsonPrimitive()) {
            if (e.getAsJsonPrimitive().isNumber()) return new Loc(LocKind.CITY, e.getAsInt(), 0, 0);
            return "nearest_city".equals(e.getAsString()) ? new Loc(LocKind.NEAREST_CITY, 0, 0, 0) : new Loc(LocKind.RANDOM_CITY, 0, 0, 0);
        }
        JsonObject o = e.getAsJsonObject();
        if (o.has("city")) return new Loc(LocKind.CITY, o.get("city").getAsInt(), 0, 0);
        if (o.has("xz")) return new Loc(LocKind.FIXED, -1, o.getAsJsonArray("xz").get(0).getAsInt(), o.getAsJsonArray("xz").get(1).getAsInt());
        return new Loc(LocKind.RANDOM_CITY, 0, 0, 0);
    }

    /** Chi so thanh cua mot lan xuat hien (-1 neu toa do co dinh). */
    public int cityOf(long start, int cities) {
        return switch (loc.kind()) {
            case CITY -> Math.max(0, Math.min(cities - 1, loc.city()));
            case FIXED -> -1;
            default -> LichMath.pickIndex(id, start, cities);
        };
    }
}
