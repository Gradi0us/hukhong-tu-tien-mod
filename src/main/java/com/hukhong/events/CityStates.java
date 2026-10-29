package com.hukhong.events;

import com.hukhong.city.CityDef;
import com.hukhong.city.CitySavedData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Trang thai RUIN cua 10 thanh (Dot 5). Thanh 0 dung CitySavedData (giu nguyen); thanh 1..9 luu o day.
 * Mac dinh: thanh "city_ruined" (3, 9) la RUIN tu dau; cac thanh khac NORMAL. Muc tuong minh {@code true} = bi that thu (co the tai chiem).
 */
public class CityStates extends SavedData {
    private static final String NAME = "hukhong_city_states";
    private final Map<String, Boolean> ruin = new HashMap<>();

    public static CityStates get(ServerLevel ow) {
        return ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(CityStates::new, CityStates::load, null), NAME);
    }

    public static CityStates load(CompoundTag tag, HolderLookup.Provider reg) {
        CityStates d = new CityStates();
        CompoundTag c = tag.getCompound("ruin");
        for (String k : c.getAllKeys()) d.ruin.put(k, c.getBoolean(k));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        CompoundTag c = new CompoundTag();
        ruin.forEach(c::putBoolean);
        tag.put("ruin", c);
        return tag;
    }

    /** Thuan: trang thai mac dinh cua thanh khi chua co muc tuong minh. */
    public static boolean defaultRuin(CityDef d) { return d.index() != 0 && "city_ruined".equals(d.category()); }

    public boolean isRuinRaw(CityDef d) { return ruin.getOrDefault(d.id(), defaultRuin(d)); }
    /** Bi that thu do tap kich (khac voi do nat tu dau). */
    public boolean fell(CityDef d) { return Boolean.TRUE.equals(ruin.get(d.id())); }
    public void setRaw(CityDef d, boolean r) { ruin.put(d.id(), r); setDirty(); }

    public static boolean isRuin(ServerLevel ow, CityDef d) {
        if (d.index() == 0) return CitySavedData.get(ow).state() == CitySavedData.State.RUIN;
        return get(ow).isRuinRaw(d);
    }

    public static void setRuin(ServerLevel ow, CityDef d, boolean r) {
        if (d.index() == 0) CitySavedData.get(ow).setState(r ? CitySavedData.State.RUIN : CitySavedData.State.NORMAL);
        else get(ow).setRaw(d, r);
    }

    /** Thanh dang RUIN co the tai chiem: thanh 0, thanh bi that thu, hoac Hoang Do (9) khi nguoi choi o chuong >= 10. */
    public static boolean retakable(ServerLevel ow, CityDef d, int chapter) {
        if (!isRuin(ow, d)) return false;
        return d.index() == 0 || get(ow).fell(d) || (d.index() == 9 && chapter >= 10);
    }
}
