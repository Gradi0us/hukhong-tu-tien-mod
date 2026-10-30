package com.hukhong.poi;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Dia diem da dung (Overworld), ung vien da bo qua, va dia diem moi nguoi choi da kham pha. */
public class PoiSavedData extends SavedData {
    private static final String NAME = "hukhong_poi";
    private final Map<String, PoiInstance> inst = new LinkedHashMap<>();
    private final Set<String> skipped = new HashSet<>();
    private final Map<UUID, Set<String>> found = new HashMap<>();
    private Map<Long, PoiInstance.Act> actIdx;
    private Map<Long, PoiInstance> actOwner;

    public static PoiSavedData get(ServerLevel ow) {
        return ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(PoiSavedData::new, PoiSavedData::load, null), NAME);
    }

    public static PoiSavedData load(CompoundTag tag, HolderLookup.Provider reg) {
        PoiSavedData d = new PoiSavedData();
        for (Tag t : tag.getList("inst", Tag.TAG_COMPOUND)) { PoiInstance i = PoiInstance.load((CompoundTag) t); d.inst.put(i.key, i); }
        for (Tag t : tag.getList("skipped", Tag.TAG_STRING)) d.skipped.add(t.getAsString());
        CompoundTag f = tag.getCompound("found");
        for (String k : f.getAllKeys()) {
            Set<String> s = new HashSet<>();
            for (Tag t : f.getList(k, Tag.TAG_STRING)) s.add(t.getAsString());
            try { d.found.put(UUID.fromString(k), s); } catch (IllegalArgumentException ignored) { }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        ListTag l = new ListTag();
        for (PoiInstance i : inst.values()) l.add(i.save());
        tag.put("inst", l);
        ListTag s = new ListTag();
        for (String k : skipped) s.add(StringTag.valueOf(k));
        tag.put("skipped", s);
        CompoundTag f = new CompoundTag();
        for (var e : found.entrySet()) {
            ListTag fl = new ListTag();
            for (String k : e.getValue()) fl.add(StringTag.valueOf(k));
            f.put(e.getKey().toString(), fl);
        }
        tag.put("found", f);
        return tag;
    }

    // ---- instances ----
    public Iterable<PoiInstance> all() { return inst.values(); }
    public int count() { return inst.size(); }
    public PoiInstance get(String key) { return inst.get(key); }

    public void add(PoiInstance i) { inst.put(i.key, i); actIdx = null; setDirty(); }

    public boolean known(String key) { return inst.containsKey(key) || skipped.contains(key); }
    public void markSkipped(String key) { if (skipped.add(key)) setDirty(); }

    /** Cac dia diem (moi loai) trong ban kinh r. */
    public List<PoiInstance> near(double x, double z, double r) {
        List<PoiInstance> out = new ArrayList<>();
        double r2 = r * r;
        for (PoiInstance i : inst.values()) if (i.dist2(x, z) <= r2) out.add(i);
        return out;
    }

    /** Co dia diem nao cua defId (hoac bat ky neu defId=null) trong khoang minDist? */
    public boolean anyWithin(String defId, int x, int z, int minDist) {
        for (PoiInstance i : inst.values())
            if ((defId == null || i.defId.equals(defId)) && !PoiMath.farEnough(x, z, i.x, i.z, minDist)) return true;
        return false;
    }

    // ---- act index ----
    private void index() {
        actIdx = new HashMap<>();
        actOwner = new HashMap<>();
        for (PoiInstance i : inst.values()) for (PoiInstance.Act a : i.acts) { actIdx.put(a.key(), a); actOwner.put(a.key(), i); }
    }

    public PoiInstance.Act actAt(BlockPos p) {
        if (actIdx == null) index();
        return actIdx.get(p.asLong());
    }

    public PoiInstance ownerOf(BlockPos p) {
        if (actOwner == null) index();
        return actOwner.get(p.asLong());
    }

    public void invalidateIndex() { actIdx = null; }

    // ---- discovery ----
    public boolean hasFound(UUID u, String key) { Set<String> s = found.get(u); return s != null && s.contains(key); }

    public Set<String> foundBy(UUID u) { return found.getOrDefault(u, Set.of()); }

    public boolean markFound(UUID u, String key) {
        boolean added = found.computeIfAbsent(u, k -> new HashSet<>()).add(key);
        if (added) setDirty();
        return added;
    }
}
