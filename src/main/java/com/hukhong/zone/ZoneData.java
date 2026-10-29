package com.hukhong.zone;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Vung xam thuc dat san luc tao the gioi (chi du lieu). Chi muc tinh (INDEX) cho truy van nhanh, an toan da luong. */
public class ZoneData extends SavedData {
    private static final String NAME = "hukhong_zones";

    public static final class Zone {
        public final int id, cx, cz, radius, level, form;
        public final String name;
        public volatile boolean liberated;
        /** Ban kinh da tay sach tu tam ra (0 = chua tay). */
        public volatile int cleansed;

        public Zone(int id, int cx, int cz, int radius, int level, int form, String name) {
            this.id = id; this.cx = cx; this.cz = cz; this.radius = radius; this.level = level; this.form = form; this.name = name;
        }
        public int tier() { return ZoneMath.tier(radius); }
        public boolean covers(double x, double z) { return ZoneMath.covers(x, z, cx, cz, radius, cleansed); }
    }

    private final List<Zone> zones = new ArrayList<>();
    private boolean generated;
    private static volatile Zone[] INDEX = new Zone[0];

    public static ZoneData get(ServerLevel ow) {
        ZoneData d = ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(ZoneData::new, ZoneData::load, null), NAME);
        if (INDEX.length != d.zones.size()) d.rebuildIndex();
        return d;
    }

    public static ZoneData load(CompoundTag tag, HolderLookup.Provider reg) {
        ZoneData d = new ZoneData();
        d.generated = tag.getBoolean("generated");
        ListTag l = tag.getList("zones", Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.getCompound(i);
            Zone z = new Zone(c.getInt("id"), c.getInt("cx"), c.getInt("cz"), c.getInt("r"), c.getInt("lv"), c.getInt("form"), c.getString("name"));
            z.liberated = c.getBoolean("lib");
            z.cleansed = c.getInt("clean");
            d.zones.add(z);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        tag.putBoolean("generated", generated);
        ListTag l = new ListTag();
        for (Zone z : zones) {
            CompoundTag c = new CompoundTag();
            c.putInt("id", z.id); c.putInt("cx", z.cx); c.putInt("cz", z.cz); c.putInt("r", z.radius); c.putInt("lv", z.level);
            c.putInt("form", z.form); c.putString("name", z.name); c.putBoolean("lib", z.liberated); c.putInt("clean", z.cleansed);
            l.add(c);
        }
        tag.put("zones", l);
        return tag;
    }

    public boolean generated() { return generated; }
    public List<Zone> zones() { return zones; }
    public void add(Zone z) { zones.add(z); setDirty(); }
    public void markGenerated() { generated = true; setDirty(); rebuildIndex(); }
    public Zone byId(int id) { for (Zone z : zones) if (z.id == id) return z; return null; }
    public void rebuildIndex() { INDEX = zones.toArray(new Zone[0]); }
    public static void clearIndex() { INDEX = new Zone[0]; }

    // ----------------------------------------------------------- truy van nhanh (khong cham SavedData)
    /** Vung dang xam thuc (chua tay sach) chua (x,z), hoac null. */
    public static Zone at(double x, double z) {
        for (Zone zn : INDEX) if (zn.covers(x, z)) return zn;
        return null;
    }
    /** Cap quai tai (x,z) neu trong vung dang xam thuc, nguoc lai -1. */
    public static int levelAt(double x, double z) { Zone zn = at(x, z); return zn == null ? -1 : zn.level; }
    public static boolean coversChunk(int cx, int cz) { return at(cx * 16 + 8, cz * 16 + 8) != null; }
    /** (x,z) nam trong phan da tay sach cua vung da giai phong. */
    public static boolean cleansedAt(double x, double z) {
        for (Zone zn : INDEX) if (zn.liberated && Math.hypot(x - zn.cx, z - zn.cz) < Math.min(zn.cleansed, zn.radius)) return true;
        return false;
    }
    public static Zone[] snapshot() { return INDEX; }

    /** Them cac chunk vung xam thuc quanh (pcx,pcz) ban kinh r chunk vao out (toi da max). Dung dong bo hieu ung client. */
    public static void addNearbyChunks(int pcx, int pcz, int r, List<Long> out, int max) {
        if (INDEX.length == 0) return;
        for (int x = pcx - r; x <= pcx + r && out.size() < max; x++)
            for (int z = pcz - r; z <= pcz + r && out.size() < max; z++)
                if (coversChunk(x, z)) out.add(net.minecraft.world.level.ChunkPos.asLong(x, z));
    }
}
