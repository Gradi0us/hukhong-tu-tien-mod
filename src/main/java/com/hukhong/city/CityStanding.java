package com.hukhong.city;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/** Data attachment: điểm cống hiến thành theo từng thành (key = CityDef.id). Có key = đã là Cư Dân thành đó. */
public record CityStanding(Map<String, Integer> points, long cycle, int earned) {
    public static final CityStanding DEFAULT = new CityStanding(Map.of(), 0, 0);

    public static final Codec<CityStanding> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("points", Map.of()).forGetter(CityStanding::points),
            Codec.LONG.optionalFieldOf("cycle", 0L).forGetter(CityStanding::cycle),
            Codec.INT.optionalFieldOf("earned", 0).forGetter(CityStanding::earned)
    ).apply(i, CityStanding::new));

    public boolean resident(String city) { return points.containsKey(city); }

    public int of(String city) { return points.getOrDefault(city, 0); }

    public int earnedIn(long curCycle) { return curCycle == cycle ? earned : 0; }

    public CityStanding withResident(String city) {
        if (points.containsKey(city)) return this;
        Map<String, Integer> m = new HashMap<>(points);
        m.put(city, 0);
        return new CityStanding(m, cycle, earned);
    }

    public CityStanding withAdded(String city, int add, long curCycle) {
        Map<String, Integer> m = new HashMap<>(points);
        m.merge(city, add, Integer::sum);
        return new CityStanding(m, curCycle, earnedIn(curCycle) + add);
    }
}
