package com.hukhong.skillbook;

import com.hukhong.core.Path;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * Danh sách kỹ năng đã học + cấp Cường Hóa + gán phím theo từng đạo (trang). Bất biến.
 * levels: id -> cấp (kỹ năng học từ bí tịch luôn có mặt; kỹ năng nền chỉ có mặt khi đã được nâng).
 * slots: "KIEM:0" -> id kỹ năng gán vào phím 0..3 (Q/E/R/T) của trang đó; thiếu = mặc định của đạo.
 */
public record SkillBookData(Map<String, Integer> levels, Map<String, String> slots) {
    public static final SkillBookData EMPTY = new SkillBookData(Map.of(), Map.of());

    public static final Codec<SkillBookData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("levels", Map.of()).forGetter(SkillBookData::levels),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("slots", Map.of()).forGetter(SkillBookData::slots)
    ).apply(i, SkillBookData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkillBookData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), SkillBookData::levels,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8), SkillBookData::slots,
            SkillBookData::new);

    public SkillBookData {
        levels = Map.copyOf(levels);
        slots = Map.copyOf(slots);
    }

    public static String slotKey(Path p, int slot) { return p.name() + ":" + slot; }

    /** Đã học kỹ năng từ bí tịch (không tính kỹ năng nền). */
    public boolean learned(String id) { return BookSkill.byId(id) != null && levels.containsKey(id); }

    /** Cấp hiện tại (kỹ năng nền chưa nâng = 1). */
    public int level(String id) { return SkillBookMath.clampLevel(levels.getOrDefault(id, 1)); }

    public SkillBookData withLevel(String id, int lv) {
        Map<String, Integer> m = new HashMap<>(levels);
        m.put(id, SkillBookMath.clampLevel(lv));
        return new SkillBookData(m, slots);
    }

    public String slot(Path p, int slot) {
        String s = slots.get(slotKey(p, slot));
        return s == null || s.isEmpty() ? null : s;
    }

    /** Gán id vào phím (null/"" = bỏ gán). Một kỹ năng chỉ chiếm 1 phím trong cùng trang. */
    public SkillBookData withSlot(Path p, int slot, String id) {
        Map<String, String> m = new HashMap<>(slots);
        if (id == null || id.isEmpty()) {
            m.remove(slotKey(p, slot));
        } else {
            for (int i = 0; i < 4; i++) if (id.equals(m.get(slotKey(p, i)))) m.remove(slotKey(p, i));
            m.put(slotKey(p, slot), id);
        }
        return new SkillBookData(levels, m);
    }
}
