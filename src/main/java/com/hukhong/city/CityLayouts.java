package com.hukhong.city;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/** Bo cuc da quyet dinh MOT LAN cho moi thanh: o Khu Cho + o Hiep Hoi (xem {@link CityLayout}). Overworld. Co muc nghia la thanh da di cu xong. */
public class CityLayouts extends SavedData {
    private static final String NAME = "hukhong_city_layouts";
    private final Map<String, CityLayout.Layout> map = new LinkedHashMap<>();

    public static CityLayouts get(ServerLevel ow) {
        return ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(CityLayouts::new, CityLayouts::load, null), NAME);
    }

    private static CityLayout.Plot read(CompoundTag t, String k) {
        int[] a = t.getIntArray(k);
        return a.length == 4 ? new CityLayout.Plot(a[0], a[1], a[2], a[3]) : null;
    }

    public static CityLayouts load(CompoundTag tag, HolderLookup.Provider reg) {
        CityLayouts d = new CityLayouts();
        CompoundTag all = tag.getCompound("layouts");
        for (String k : all.getAllKeys()) {
            CompoundTag c = all.getCompound(k);
            d.map.put(k, new CityLayout.Layout(read(c, "m"), read(c, "g")));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        CompoundTag all = new CompoundTag();
        map.forEach((k, v) -> {
            CompoundTag c = new CompoundTag();
            if (v.market() != null) c.putIntArray("m", new int[]{v.market().cx(), v.market().cz(), v.market().y(), v.market().face()});
            if (v.guild() != null) c.putIntArray("g", new int[]{v.guild().cx(), v.guild().cz(), v.guild().y(), v.guild().face()});
            all.put(k, c);
        });
        tag.put("layouts", all);
        return tag;
    }

    public boolean has(String id) { return map.containsKey(id); }
    public CityLayout.Layout get(String id) { return map.get(id); }
    public CityLayout.Plot market(String id) { var l = map.get(id); return l == null ? null : l.market(); }
    public CityLayout.Plot guild(String id) { var l = map.get(id); return l == null ? null : l.guild(); }
    public Map<String, CityLayout.Layout> all() { return map; }
    public void put(String id, CityLayout.Layout l) { map.put(id, l); setDirty(); }
    public void remove(String id) { if (map.remove(id) != null) setDirty(); }
}
