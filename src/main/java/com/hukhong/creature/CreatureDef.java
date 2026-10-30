package com.hukhong.creature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Dinh nghia sinh vat (data/hukhong/hukhong_creatures/*.json). Thuan Java -> test JSON bang JUnit. */
public record CreatureDef(String id, String role, double hpMul, double dmgMul, double expWeight, float honTinh,
                          List<Drop> drops, Spawn spawn, Tame tame) {
    public static final List<String> ROLES = List.of("passive", "neutral", "elite", "hostile", "ambient", "aquatic", "pet");

    public record Drop(String item, double chance, int min, int max) {}

    /** time: any | night | day. */
    public record Spawn(List<String> biomes, int weight, int min, int max, String time) {}

    public record Tame(String item, double hpFrac, String slot) {}

    public static CreatureDef parse(String id, JsonObject o) {
        String role = str(o, "role", "passive");
        if (!ROLES.contains(role)) throw new IllegalArgumentException(id + ": bad role " + role);
        List<Drop> drops = new ArrayList<>();
        if (o.has("drops")) for (JsonElement e : o.getAsJsonArray("drops")) {
            JsonObject d = e.getAsJsonObject();
            int min = d.has("min") ? d.get("min").getAsInt() : 1;
            int max = d.has("max") ? d.get("max").getAsInt() : min;
            double ch = d.has("chance") ? d.get("chance").getAsDouble() : 1.0;
            if (min < 1 || max < min || ch < 0 || ch > 1) throw new IllegalArgumentException(id + ": bad drop " + d);
            drops.add(new Drop(d.get("item").getAsString(), ch, min, max));
        }
        Spawn sp = null;
        if (o.has("spawn")) {
            JsonObject s = o.getAsJsonObject("spawn");
            List<String> bs = new ArrayList<>();
            JsonArray ba = s.getAsJsonArray("biomes");
            if (ba == null || ba.isEmpty()) throw new IllegalArgumentException(id + ": spawn.biomes required");
            for (JsonElement e : ba) bs.add(e.getAsString());
            int min = s.has("min") ? s.get("min").getAsInt() : 1, max = s.has("max") ? s.get("max").getAsInt() : min;
            if (min < 1 || max < min) throw new IllegalArgumentException(id + ": bad group size");
            sp = new Spawn(List.copyOf(bs), s.has("weight") ? s.get("weight").getAsInt() : 10, min, max, str(s, "time", "any"));
        }
        Tame t = null;
        if (o.has("tame")) {
            JsonObject s = o.getAsJsonObject("tame");
            double f = s.has("hp_frac") ? s.get("hp_frac").getAsDouble() : 0.3;
            if (f <= 0 || f > 1) throw new IllegalArgumentException(id + ": bad hp_frac");
            t = new Tame(s.get("item").getAsString(), f, str(s, "slot", "spirit"));
        }
        return new CreatureDef(id, role, num(o, "hp_mul", 1.0), num(o, "dmg_mul", 1.0), num(o, "exp_weight", 0.5),
                (float) num(o, "hon_tinh", 0.0), List.copyOf(drops), sp, t);
    }

    private static String str(JsonObject o, String k, String d) { return o.has(k) ? o.get(k).getAsString() : d; }

    private static double num(JsonObject o, String k, double d) { return o.has(k) ? o.get(k).getAsDouble() : d; }
}
