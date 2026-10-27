package com.hukhong.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/** Du lieu dung the gioi: co "da dung xong het", toa do cang da tim (khong tim lai), ranh gioi tung khu dan cu (thanh/cang/tong/tan tich). */
public class WorldBuildData extends SavedData {
    private static final String NAME = "hukhong_worldbuild";

    /** kind: city | port | ruin | sect. Hop (minX..maxX, minZ..maxZ); (ax,y,az) = diem tham chieu (ban tho/tam). */
    public record Settlement(String id, String kind, int minX, int minZ, int maxX, int maxZ, int y, int ax, int az) {
        public BlockPos altar() { return new BlockPos(ax, y, az); }
        public boolean contains(double x, double z) { return x >= minX && x <= maxX && z >= minZ && z <= maxZ; }
        public int cx() { return (minX + maxX) / 2; }
        public int cz() { return (minZ + maxZ) / 2; }
    }

    private boolean done;
    private final Map<String, int[]> portSites = new LinkedHashMap<>();
    private final Map<String, Settlement> settlements = new LinkedHashMap<>();

    public static WorldBuildData get(ServerLevel ow) {
        return ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(WorldBuildData::new, WorldBuildData::load, null), NAME);
    }

    public static WorldBuildData load(CompoundTag tag, HolderLookup.Provider reg) {
        WorldBuildData d = new WorldBuildData();
        d.done = tag.getBoolean("done");
        CompoundTag p = tag.getCompound("ports");
        for (String k : p.getAllKeys()) { int[] a = p.getIntArray(k); if (a.length == 2) d.portSites.put(k, a); }
        CompoundTag s = tag.getCompound("settlements");
        for (String k : s.getAllKeys()) {
            CompoundTag c = s.getCompound(k);
            d.settlements.put(k, new Settlement(k, c.getString("kind"), c.getInt("x0"), c.getInt("z0"), c.getInt("x1"), c.getInt("z1"),
                    c.getInt("y"), c.getInt("ax"), c.getInt("az")));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        tag.putBoolean("done", done);
        CompoundTag p = new CompoundTag();
        portSites.forEach(p::putIntArray);
        tag.put("ports", p);
        CompoundTag s = new CompoundTag();
        settlements.forEach((k, v) -> {
            CompoundTag c = new CompoundTag();
            c.putString("kind", v.kind());
            c.putInt("x0", v.minX()); c.putInt("z0", v.minZ()); c.putInt("x1", v.maxX()); c.putInt("z1", v.maxZ());
            c.putInt("y", v.y()); c.putInt("ax", v.ax()); c.putInt("az", v.az());
            s.put(k, c);
        });
        tag.put("settlements", s);
        return tag;
    }

    public boolean done() { return done; }
    public void setDone() { done = true; setDirty(); }
    public int[] portSite(String id) { return portSites.get(id); }
    public void setPortSite(String id, int x, int z) { portSites.put(id, new int[]{x, z}); setDirty(); }
    public Map<String, Settlement> settlements() { return settlements; }
    public void put(Settlement s) { settlements.put(s.id(), s); setDirty(); }
}
