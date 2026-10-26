package com.hukhong.bicanh;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Cổng đang mở ở Overworld + mốc sinh cổng kế tiếp (theo gameTime Overworld). */
public class BiCanhSaved extends SavedData {
    public record Portal(BlockPos pos, int tier, long expire) {}

    private static final String NAME = "hukhong_bicanh";
    public final List<Portal> portals = new ArrayList<>();
    public long nextSpawn = -1;

    public static BiCanhSaved get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(BiCanhSaved::new, BiCanhSaved::load, null), NAME);
    }

    public static BiCanhSaved load(CompoundTag tag, HolderLookup.Provider reg) {
        BiCanhSaved d = new BiCanhSaved();
        d.nextSpawn = tag.getLong("next");
        for (Tag t : tag.getList("portals", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.portals.add(new Portal(new BlockPos(c.getInt("x"), c.getInt("y"), c.getInt("z")), c.getInt("tier"), c.getLong("exp")));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        tag.putLong("next", nextSpawn);
        ListTag l = new ListTag();
        for (Portal p : portals) {
            CompoundTag c = new CompoundTag();
            c.putInt("x", p.pos().getX()); c.putInt("y", p.pos().getY()); c.putInt("z", p.pos().getZ());
            c.putInt("tier", p.tier()); c.putLong("exp", p.expire());
            l.add(c);
        }
        tag.put("portals", l);
        return tag;
    }

    public void add(Portal p) { portals.add(p); setDirty(); }
    public void remove(BlockPos pos) { if (portals.removeIf(p -> p.pos().equals(pos))) setDirty(); }
    public void setNext(long t) { nextSpawn = t; setDirty(); }
}
