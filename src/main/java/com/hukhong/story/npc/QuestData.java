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
    public static final int LAST_CHAPTER = 6;

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

    public QuestData withFlag(String f) {
        if (has(f)) return this;
        List<String> l = new ArrayList<>(flags);
        l.add(f);
        return new QuestData(chapter, List.copyOf(l));
    }

    /** Chương chỉ tiến, không lùi. */
    public QuestData withChapter(int c) { return new QuestData(Math.max(chapter, Math.min(LAST_CHAPTER, c)), flags); }
}
