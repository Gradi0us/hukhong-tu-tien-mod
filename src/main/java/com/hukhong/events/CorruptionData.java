package com.hukhong.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Vùng Hư Hóa lan: tập chunk bị nhiễm + các Tâm Hư Hóa (nơi boss thế giới xuất hiện). */
public class CorruptionData extends SavedData {
    private static final String NAME = "hukhong_corruption";
    public static final int MAX_CHUNKS = 600;

    private final Set<Long> chunks = new HashSet<>();
    private final List<BlockPos> hearts = new ArrayList<>();
    private final Set<Long> reverting = new HashSet<>();
    private long nextInvasionAt = -1;
    private boolean seeded;

    public static CorruptionData get(ServerLevel ow) {
        return ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(CorruptionData::new, CorruptionData::load, null), NAME);
    }

    public static CorruptionData load(CompoundTag tag, HolderLookup.Provider reg) {
        CorruptionData d = new CorruptionData();
        for (long l : tag.getLongArray("chunks")) d.chunks.add(l);
        int[] h = tag.getIntArray("hearts");
        for (int i = 0; i + 2 < h.length; i += 3) d.hearts.add(new BlockPos(h[i], h[i + 1], h[i + 2]));
        for (long l : tag.getLongArray("reverting")) d.reverting.add(l);
        d.nextInvasionAt = tag.contains("nextInv") ? tag.getLong("nextInv") : -1;
        d.seeded = tag.getBoolean("seeded");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        tag.putLongArray("chunks", chunks.stream().mapToLong(Long::longValue).toArray());
        int[] h = new int[hearts.size() * 3];
        for (int i = 0; i < hearts.size(); i++) { h[3 * i] = hearts.get(i).getX(); h[3 * i + 1] = hearts.get(i).getY(); h[3 * i + 2] = hearts.get(i).getZ(); }
        tag.putIntArray("hearts", h);
        tag.putLongArray("reverting", reverting.stream().mapToLong(Long::longValue).toArray());
        tag.putLong("nextInv", nextInvasionAt);
        tag.putBoolean("seeded", seeded);
        return tag;
    }

    public boolean isCorrupted(int cx, int cz) { return chunks.contains(ChunkPos.asLong(cx, cz)); }
    public boolean isCorrupted(BlockPos p) { return isCorrupted(p.getX() >> 4, p.getZ() >> 4); }
    public int size() { return chunks.size(); }
    public Set<Long> chunks() { return chunks; }
    public List<BlockPos> hearts() { return hearts; }
    /** Chunk đã tẩy nhưng còn khối Hư Không cần hoàn nguyên. */
    public Set<Long> reverting() { return reverting; }
    public void finishRevert(long l) { if (reverting.remove(l)) setDirty(); }
    public boolean seeded() { return seeded; }
    public void markSeeded() { seeded = true; setDirty(); }
    public long nextInvasionAt() { return nextInvasionAt; }
    public void setNextInvasionAt(long t) { nextInvasionAt = t; setDirty(); }

    public boolean add(int cx, int cz) {
        if (chunks.size() >= MAX_CHUNKS) return false;
        boolean r = chunks.add(ChunkPos.asLong(cx, cz));
        if (r) { reverting.remove(ChunkPos.asLong(cx, cz)); setDirty(); }
        return r;
    }

    public void addHeart(BlockPos p) { hearts.add(p); add(p.getX() >> 4, p.getZ() >> 4); setDirty(); }
    public void removeHeart(BlockPos p) { if (hearts.remove(p)) setDirty(); }

    /** Tẩy n chunk gần (x,z) nhất; trả số chunk đã tẩy. Chunk chứa Tâm Hư Hóa không bị tẩy. */
    public int cleanseNear(int x, int z, int n) {
        Set<Long> keep = new HashSet<>();
        for (BlockPos h : hearts) keep.add(ChunkPos.asLong(h.getX() >> 4, h.getZ() >> 4));
        List<Long> sorted = new ArrayList<>(chunks);
        sorted.removeAll(keep);
        int cx = x >> 4, cz = z >> 4;
        sorted.sort((a, b) -> Long.compare(d2(a, cx, cz), d2(b, cx, cz)));
        int c = 0;
        for (long l : sorted) { if (c >= n) break; chunks.remove(l); c++; }
        if (c > 0) setDirty();
        return c;
    }

    /** Xoá mọi chunk trong bán kính r (chunk) quanh vị trí (khi diệt boss). */
    public int cleanseRadius(BlockPos p, int r) {
        int cx = p.getX() >> 4, cz = p.getZ() >> 4, c = 0;
        var it = chunks.iterator();
        while (it.hasNext()) { long l = it.next(); if (d2(l, cx, cz) <= (long) r * r) { it.remove(); reverting.add(l); c++; } }
        if (c > 0) setDirty();
        return c;
    }

    private static long d2(long packed, int cx, int cz) {
        long dx = (int) packed - cx, dz = (int) (packed >> 32) - cz;
        return dx * dx + dz * dz;
    }
}
