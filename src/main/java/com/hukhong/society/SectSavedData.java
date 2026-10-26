package com.hukhong.society;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Sơn môn đã dựng (tâm = mặt nền, Linh Khí Đài ở center.above()). */
public class SectSavedData extends SavedData {
    private static final String NAME = "hukhong_sects";

    private final Map<String, BlockPos> centers = new HashMap<>();

    public static SectSavedData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SectSavedData::new, SectSavedData::load, null), NAME);
    }

    public static SectSavedData load(CompoundTag tag, HolderLookup.Provider reg) {
        SectSavedData d = new SectSavedData();
        CompoundTag c = tag.getCompound("centers");
        for (String k : c.getAllKeys()) {
            int[] a = c.getIntArray(k);
            if (a.length == 3) d.centers.put(k, new BlockPos(a[0], a[1], a[2]));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        CompoundTag c = new CompoundTag();
        centers.forEach((k, p) -> c.putIntArray(k, new int[]{p.getX(), p.getY(), p.getZ()}));
        tag.put("centers", c);
        return tag;
    }

    public BlockPos center(String id) { return centers.get(id); }
    public boolean isBuilt(String id) { return centers.containsKey(id); }
    public void markBuilt(String id, BlockPos c) { centers.put(id, c); setDirty(); }
    public Map<String, BlockPos> all() { return Map.copyOf(centers); }

    /** Tông có sơn môn gần (x,z) nhất trong bán kính r, hoặc null. */
    public String nearest(double x, double z, double r) {
        String best = null;
        double bd = r * r;
        for (var e : centers.entrySet()) {
            double d = (e.getValue().getX() - x) * (e.getValue().getX() - x) + (e.getValue().getZ() - z) * (e.getValue().getZ() - z);
            if (d <= bd) { bd = d; best = e.getKey(); }
        }
        return best;
    }
}
