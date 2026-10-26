package com.hukhong.story;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/** Tiến trình truyện: mode (0 chưa chọn, 1 truyện, 2 tự do), Văn Kiện đã thu, cờ gợi ý hang ổ CHG (bit0 = chuỗi A, bit1 = chuỗi B). */
public record StoryData(int mode, List<String> docs, int lair) {
    public static final int MODE_UNSET = 0, MODE_STORY = 1, MODE_FREE = 2;
    public static final StoryData DEFAULT = new StoryData(MODE_UNSET, List.of(), 0);

    public static final Codec<StoryData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("mode", 0).forGetter(StoryData::mode),
            Codec.STRING.listOf().optionalFieldOf("docs", List.of()).forGetter(StoryData::docs),
            Codec.INT.optionalFieldOf("lair", 0).forGetter(StoryData::lair)
    ).apply(i, StoryData::new));

    public static final StreamCodec<ByteBuf, StoryData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StoryData::mode,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), StoryData::docs,
            ByteBufCodecs.VAR_INT, StoryData::lair,
            StoryData::new);

    public boolean has(String id) { return docs.contains(id); }

    public StoryData withMode(int m) { return new StoryData(m, docs, lair); }

    public StoryData withDoc(String id) {
        if (has(id)) return this;
        List<String> l = new ArrayList<>(docs);
        l.add(id);
        return new StoryData(mode, List.copyOf(l), lair);
    }

    public StoryData withLair(int bit) { return new StoryData(mode, docs, lair | bit); }

    public int chainCount(char chain) {
        int n = 0;
        for (String d : docs) if (!d.isEmpty() && d.charAt(0) == chain) n++;
        return n;
    }
}
