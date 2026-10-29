package com.hukhong.story.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/** Tiến trình tuyến chính: chương hiện tại (0 = chưa bắt đầu, coi như 1) và các cờ truyện. */
public record QuestData(int chapter, List<String> flags) {
    public static final QuestData DEFAULT = new QuestData(0, List.of());
    public static final int LAST_CHAPTER = 10;

    public static final Codec<QuestData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("chapter", 0).forGetter(QuestData::chapter),
            Codec.STRING.listOf().optionalFieldOf("flags", List.of()).forGetter(QuestData::flags)
    ).apply(i, QuestData::new));

    public static final StreamCodec<ByteBuf, QuestData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, QuestData::chapter,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), QuestData::flags,
            QuestData::new);

    public int current() { return Math.max(1, chapter); }

    public boolean has(String f) { return flags.contains(f); }

    public QuestData withoutFlag(String f) {
        if (!has(f)) return this;
        List<String> l = new ArrayList<>(flags);
        l.remove(f);
        return new QuestData(chapter, List.copyOf(l));
    }

    /** Gia tri sau tien to trong cac co dang "prefix...:value" (dung cho chuoi nhiem vu phu), "" neu khong co. */
    public String valueOf(String prefix) { return flagValue(prefix); }

    public QuestData withFlag(String f) {
        if (has(f)) return this;
        List<String> l = new ArrayList<>(flags);
        l.add(f);
        return new QuestData(chapter, List.copyOf(l));
    }

    /** Bước hiện tại của luồng truyện (cờ "step:ID"), "" nếu chưa có. */
    public String step() { return flagValue("step:"); }

    /** Tiến độ bước hiện tại (đếm kill...), cờ "prog:N". */
    public int prog() {
        String v = flagValue("prog:");
        try { return v.isEmpty() ? 0 : Integer.parseInt(v); } catch (NumberFormatException e) { return 0; }
    }

    private String flagValue(String prefix) {
        for (String f : flags) if (f.startsWith(prefix)) return f.substring(prefix.length());
        return "";
    }

    private QuestData replaceFlag(String prefix, String value) {
        List<String> l = new ArrayList<>();
        for (String f : flags) if (!f.startsWith(prefix)) l.add(f);
        if (value != null) l.add(prefix + value);
        return new QuestData(chapter, List.copyOf(l));
    }

    public QuestData withStep(String id) { return replaceFlag("step:", id.isEmpty() ? null : id).replaceFlag("prog:", null); }

    public QuestData withProg(int n) { return replaceFlag("prog:", n <= 0 ? null : Integer.toString(n)); }

    /** Chương chỉ tiến, không lùi. */
    public QuestData withChapter(int c) { return new QuestData(Math.max(chapter, Math.min(LAST_CHAPTER, c)), flags); }

    /** Chương thô (0 = màn mở đầu). */
    public int raw() { return chapter; }
}
