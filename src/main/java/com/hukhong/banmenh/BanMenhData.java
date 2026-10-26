package com.hukhong.banmenh;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

/** Attachment người chơi: loại kiếm (rỗng = chưa roll), chủ, giai đoạn tiến hóa (0..6), seed roll (0 = chưa có). */
public record BanMenhData(String type, UUID owner, int stage, long seed) {
    public static final BanMenhData EMPTY = new BanMenhData("", null, 0, 0L);

    public static final Codec<BanMenhData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("type", "").forGetter(BanMenhData::type),
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(d -> Optional.ofNullable(d.owner())),
            Codec.INT.optionalFieldOf("stage", 0).forGetter(BanMenhData::stage),
            Codec.LONG.optionalFieldOf("seed", 0L).forGetter(BanMenhData::seed)
    ).apply(i, (t, o, s, sd) -> new BanMenhData(t, o.orElse(null), s, sd)));

    public boolean hasType() { return BanMenhType.byId(type) != null; }
    public BanMenhData withType(String t, UUID o) { return new BanMenhData(t, o, stage, seed); }
    public BanMenhData withStage(int s) { return new BanMenhData(type, owner, s, seed); }
    public BanMenhData withSeed(long s) { return new BanMenhData(type, owner, stage, s); }
}
