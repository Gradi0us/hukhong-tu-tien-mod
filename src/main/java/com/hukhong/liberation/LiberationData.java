package com.hukhong.liberation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Luu cac dau truong Tam Hu Hoa da dung (khong dung lai) va so vung da giai phong. Khoa Tam = BlockPos(x, 0, z).asLong(). */
public class LiberationData extends SavedData {
    private static final String NAME = "hukhong_liberation";

    private final Set<Long> arenas = new HashSet<>();
    private int liberated;

    public static LiberationData get(ServerLevel ow) {
        return ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(LiberationData::new, LiberationData::load, null), NAME);
    }

    public static LiberationData load(CompoundTag tag, HolderLookup.Provider reg) {
        LiberationData d = new LiberationData();
        for (long l : tag.getLongArray("arenas")) d.arenas.add(l);
        d.liberated = tag.getInt("liberated");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        tag.putLongArray("arenas", arenas.stream().mapToLong(Long::longValue).toArray());
        tag.putInt("liberated", liberated);
        return tag;
    }

    public static long key(BlockPos heart) { return new BlockPos(heart.getX(), 0, heart.getZ()).asLong(); }

    public boolean hasArena(BlockPos heart) { return arenas.contains(key(heart)); }
    public void markArena(BlockPos heart) { if (arenas.add(key(heart))) setDirty(); }
    public void clearArena(BlockPos heart) { if (arenas.remove(key(heart))) setDirty(); }
    public int liberated() { return liberated; }
    public void addLiberated() { liberated++; setDirty(); }
}
