package com.hukhong.travel;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/** Cac diem da kham pha (id dang "c:<thanh/cang>", "s:<tong>", "g:<thanh>") - nguoi choi tung o trong 150 block. Dong bo client. */
public record TravelData(List<String> seen) {
    public static final TravelData EMPTY = new TravelData(List.of());
    public static final Codec<TravelData> CODEC = Codec.STRING.listOf().xmap(TravelData::new, TravelData::seen);
    public static final StreamCodec<ByteBuf, TravelData> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(TravelData::new, TravelData::seen);

    public boolean has(String id) { return seen.contains(id); }

    public TravelData with(String id) {
        if (has(id)) return this;
        List<String> l = new ArrayList<>(seen);
        l.add(id);
        return new TravelData(List.copyOf(l));
    }
}
