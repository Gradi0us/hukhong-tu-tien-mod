package com.hukhong.profession;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Nghề của người chơi: nghề (NONE = chưa chọn), cấp nghề (1..trần), kinh nghiệm trong cấp hiện tại. */
public record ProfessionData(Profession profession, int level, long xp) {
    public static final ProfessionData DEFAULT = new ProfessionData(Profession.NONE, 1, 0L);

    public static final Codec<ProfessionData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Profession.CODEC.optionalFieldOf("profession", Profession.NONE).forGetter(ProfessionData::profession),
            Codec.INT.optionalFieldOf("level", 1).forGetter(ProfessionData::level),
            Codec.LONG.optionalFieldOf("xp", 0L).forGetter(ProfessionData::xp)
    ).apply(i, ProfessionData::new));
}
