package com.hukhong.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Hồ sơ ký ức của người chơi: bitmask 8 ký ức đã nhập (bit id-1), các loại địa điểm đã nhận ký ức lần đầu, cờ hoàn thành.
 * Lưu bằng attachment (copyOnDeath) — không phụ thuộc QuestData.
 */
public record MemoryData(int mask, List<String> poiKinds, boolean completed) {
    public static final MemoryData EMPTY = new MemoryData(0, List.of(), false);

    public static final Codec<MemoryData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("mask", 0).forGetter(MemoryData::mask),
            Codec.STRING.listOf().optionalFieldOf("poi", List.of()).forGetter(MemoryData::poiKinds),
            Codec.BOOL.optionalFieldOf("done", false).forGetter(MemoryData::completed)
    ).apply(i, MemoryData::new));

    private static int bit(int id) { return 1 << (id - 1); }

    public boolean has(int id) { return MemoryLore.byId(id) != null && (mask & bit(id)) != 0; }

    public MemoryData with(int id) { return new MemoryData(mask | bit(id), poiKinds, completed); }

    public int count() { return Integer.bitCount(mask); }

    public boolean looted(String kind) { return poiKinds.contains(kind); }

    public MemoryData withLooted(String kind) {
        if (looted(kind)) return this;
        List<String> l = new ArrayList<>(poiKinds);
        l.add(kind);
        return new MemoryData(mask, List.copyOf(l), completed);
    }

    public MemoryData done() { return new MemoryData(mask, poiKinds, true); }
}
