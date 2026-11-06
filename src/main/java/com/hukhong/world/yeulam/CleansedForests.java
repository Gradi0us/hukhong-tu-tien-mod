package com.hukhong.world.yeulam;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Các Đại Sâm Lâm bị Hư Không xâm thực đã được người chơi giải phóng (Lịch Luyện): không còn sinh quái Hư Hóa. */
public class CleansedForests extends SavedData {
    private static final String NAME = "hukhong_cleansed_forests";
    private final Set<Long> done = new HashSet<>();

    public static CleansedForests get(ServerLevel ow) {
        return ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(CleansedForests::new, CleansedForests::load, null), NAME);
    }

    private static long key(DaiSamLam.Region r) { return ((long) r.cx() << 32) ^ (r.cz() & 0xFFFFFFFFL); }

    public boolean has(DaiSamLam.Region r) { return r != null && done.contains(key(r)); }

    public void add(DaiSamLam.Region r) { if (r != null && done.add(key(r))) setDirty(); }

    public static CleansedForests load(CompoundTag tag, HolderLookup.Provider reg) {
        CleansedForests d = new CleansedForests();
        for (long v : tag.getLongArray("done")) d.done.add(v);
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        tag.put("done", new LongArrayTag(done.stream().mapToLong(Long::longValue).toArray()));
        return tag;
    }
}
