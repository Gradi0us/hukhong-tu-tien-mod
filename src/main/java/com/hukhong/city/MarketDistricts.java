package com.hukhong.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Khu Cho (Thuong Hoi) moi thanh + ban kinh Quang Truong An Hon (T) moi thanh. Overworld.
 * sites: vi tri da chon (chua chac da dung); built: tam khu cho da dung xong (y = mat nen). plazaT: vet T cua dien dan (0/thieu = dien dan cu).
 */
public class MarketDistricts extends SavedData {
    private static final String NAME = "hukhong_market_districts";
    private final Map<String, BlockPos> sites = new LinkedHashMap<>();
    private final Map<String, BlockPos> built = new LinkedHashMap<>();
    private final Map<String, Integer> plazaT = new LinkedHashMap<>();

    public static MarketDistricts get(ServerLevel ow) {
        return ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(MarketDistricts::new, MarketDistricts::load, null), NAME);
    }

    private static void readMap(CompoundTag tag, String key, Map<String, BlockPos> out) {
        CompoundTag c = tag.getCompound(key);
        for (String k : c.getAllKeys()) {
            int[] a = c.getIntArray(k);
            if (a.length == 3) out.put(k, new BlockPos(a[0], a[1], a[2]));
        }
    }

    private static CompoundTag writeMap(Map<String, BlockPos> m) {
        CompoundTag c = new CompoundTag();
        m.forEach((k, v) -> c.putIntArray(k, new int[]{v.getX(), v.getY(), v.getZ()}));
        return c;
    }

    public static MarketDistricts load(CompoundTag tag, HolderLookup.Provider reg) {
        MarketDistricts d = new MarketDistricts();
        readMap(tag, "sites", d.sites);
        readMap(tag, "built", d.built);
        CompoundTag p = tag.getCompound("plaza");
        for (String k : p.getAllKeys()) d.plazaT.put(k, p.getInt(k));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        tag.put("sites", writeMap(sites));
        tag.put("built", writeMap(built));
        CompoundTag p = new CompoundTag();
        plazaT.forEach(p::putInt);
        tag.put("plaza", p);
        return tag;
    }

    public BlockPos site(String id) { return sites.get(id); }
    public void setSite(String id, BlockPos p) { sites.put(id, p); setDirty(); }
    /** Tam khu cho da dung xong, hoac null. */
    public BlockPos built(String id) { return built.get(id); }
    public Map<String, BlockPos> allBuilt() { return built; }
    public void setBuilt(String id, BlockPos p) { built.put(id, p); setDirty(); }
    /** Di cu: quen vi tri khu cho cu (da go) de dung lai o o moi. */
    public void forget(String id) { boolean a = built.remove(id) != null, b = sites.remove(id) != null; if (a || b) setDirty(); }
    public int plazaT(String id) { return plazaT.getOrDefault(id, 0); }
    public Map<String, Integer> allPlaza() { return plazaT; }
    public void setPlazaT(String id, int t) { if (plazaT.getOrDefault(id, -1) != t) { plazaT.put(id, t); setDirty(); } }
}
