package com.hukhong.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Thành 1..9 đã dựng (tâm) + Thành Chủ mỗi thành. Thành 0 (Lưu Ly Cảng) nằm ở CitySavedData. */
public class CityRegistryData extends SavedData {
    private static final String NAME = "hukhong_cities";

    private final Map<String, BlockPos> centers = new HashMap<>();
    private final Map<String, UUID> lords = new HashMap<>();
    private final Map<String, String> lordNames = new HashMap<>();

    public static CityRegistryData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CityRegistryData::new, CityRegistryData::load, null), NAME);
    }

    public static CityRegistryData load(CompoundTag tag, HolderLookup.Provider reg) {
        CityRegistryData d = new CityRegistryData();
        CompoundTag c = tag.getCompound("centers");
        for (String k : c.getAllKeys()) {
            int[] a = c.getIntArray(k);
            if (a.length == 3) d.centers.put(k, new BlockPos(a[0], a[1], a[2]));
        }
        CompoundTag l = tag.getCompound("lords");
        for (String k : l.getAllKeys()) d.lords.put(k, NbtUtils.loadUUID(l.get(k)));
        CompoundTag n = tag.getCompound("lord_names");
        for (String k : n.getAllKeys()) d.lordNames.put(k, n.getString(k));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        CompoundTag c = new CompoundTag();
        centers.forEach((k, p) -> c.putIntArray(k, new int[]{p.getX(), p.getY(), p.getZ()}));
        tag.put("centers", c);
        CompoundTag l = new CompoundTag();
        lords.forEach((k, u) -> l.put(k, NbtUtils.createUUID(u)));
        tag.put("lords", l);
        CompoundTag n = new CompoundTag();
        lordNames.forEach(n::putString);
        tag.put("lord_names", n);
        return tag;
    }

    public BlockPos center(String id) { return centers.get(id); }
    public boolean isBuilt(String id) { return centers.containsKey(id); }
    public void markBuilt(String id, BlockPos c) { centers.put(id, c); setDirty(); }

    public UUID lord(String id) { return lords.get(id); }
    public String lordName(String id) { return lordNames.getOrDefault(id, ""); }

    /** Chiếm ghế Thành Chủ nếu trống hoặc đã là mình. */
    public boolean claimLord(String id, UUID who, String name) {
        UUID cur = lords.get(id);
        if (cur != null && !cur.equals(who)) return false;
        if (cur == null) { lords.put(id, who); lordNames.put(id, name); setDirty(); }
        return true;
    }

    public boolean lordFreeFor(String id, UUID who) {
        UUID cur = lords.get(id);
        return cur == null || cur.equals(who);
    }
}
