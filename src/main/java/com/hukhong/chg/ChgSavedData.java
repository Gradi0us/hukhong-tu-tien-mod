package com.hukhong.chg;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Arrays;

/** Trạng thái thế giới của CHG: hang đã dựng (+ độ cao sàn), lần xuất hiện gần nhất, lần sự kiện gần nhất. */
public class ChgSavedData extends SavedData {
    private static final String NAME = "hukhong_chg";
    private final int[] floorY = new int[ChgKit.COUNT];       // Integer.MIN_VALUE = chưa dựng
    private final long[] lastSpawn = new long[ChgKit.COUNT];  // Long.MIN_VALUE/2 = chưa từng
    private final long[] lastEvent = new long[ChgKit.COUNT];

    public ChgSavedData() {
        Arrays.fill(floorY, Integer.MIN_VALUE);
        Arrays.fill(lastSpawn, Long.MIN_VALUE / 2);
        Arrays.fill(lastEvent, Long.MIN_VALUE / 2);
    }

    public static ChgSavedData get(ServerLevel ow) {
        return ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(ChgSavedData::new, ChgSavedData::load, null), NAME);
    }

    public static ChgSavedData load(CompoundTag tag, HolderLookup.Provider reg) {
        ChgSavedData d = new ChgSavedData();
        int[] y = tag.getIntArray("floorY");
        for (int i = 0; i < Math.min(y.length, d.floorY.length); i++) d.floorY[i] = y[i];
        long[] s = tag.getLongArray("lastSpawn"), e = tag.getLongArray("lastEvent");
        for (int i = 0; i < Math.min(s.length, d.lastSpawn.length); i++) d.lastSpawn[i] = s[i];
        for (int i = 0; i < Math.min(e.length, d.lastEvent.length); i++) d.lastEvent[i] = e[i];
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        tag.putIntArray("floorY", floorY);
        tag.putLongArray("lastSpawn", lastSpawn);
        tag.putLongArray("lastEvent", lastEvent);
        return tag;
    }

    public boolean built(int idx) { return floorY[idx - 1] != Integer.MIN_VALUE; }
    public int floorY(int idx) { return floorY[idx - 1]; }
    public void setBuilt(int idx, int y) { floorY[idx - 1] = y; setDirty(); }
    public long lastSpawn(int idx) { return lastSpawn[idx - 1]; }
    public void markSpawn(int idx, long t) { lastSpawn[idx - 1] = t; setDirty(); }
    public long lastEvent(int idx) { return lastEvent[idx - 1]; }
    public void markEvent(int idx, long t) { lastEvent[idx - 1] = t; setDirty(); }
}
