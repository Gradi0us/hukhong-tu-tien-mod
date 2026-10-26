package com.hukhong.age;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Đồng hồ tuổi duy nhất: onlineTicks (chỉ tăng khi online và không AFK). 24.000 tick = 1 tháng. */
public record AgeData(long onlineTicks, int startAgeYears) {
    public static final long TICKS_PER_MONTH = 24_000L;
    public static final long TICKS_PER_YEAR = TICKS_PER_MONTH * 12;
    public static final AgeData DEFAULT = new AgeData(0, 16);

    public static final Codec<AgeData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.optionalFieldOf("online_ticks", 0L).forGetter(AgeData::onlineTicks),
            Codec.INT.optionalFieldOf("start_age", 16).forGetter(AgeData::startAgeYears)
    ).apply(i, AgeData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AgeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, AgeData::onlineTicks,
            ByteBufCodecs.VAR_INT, AgeData::startAgeYears,
            AgeData::new);

    public int ageYears() { return startAgeYears + (int) (onlineTicks / TICKS_PER_YEAR); }

    public AgeData withOnlineTicks(long t) { return new AgeData(t, startAgeYears); }
}
