package com.hukhong.artifact;

import com.hukhong.core.Path;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Giai đoạn pháp khí theo đạo (0..6, bất biến): Đạo = Pháp Bảo (cánh), Thể = Bá Thể (kim thân + khí),
 * Ma = Ma Hồn (sừng + mây tối), Pháp = Pháp Khí (vòng nguyên tố quanh người). Kiếm dùng Bản Mệnh Kiếm riêng nên không có ở đây.
 */
public record ArtifactData(int dao, int the, int ma, int phap) {
    public static final ArtifactData EMPTY = new ArtifactData(0, 0, 0, 0);

    public static final Codec<ArtifactData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("dao", 0).forGetter(ArtifactData::dao),
            Codec.INT.optionalFieldOf("the", 0).forGetter(ArtifactData::the),
            Codec.INT.optionalFieldOf("ma", 0).forGetter(ArtifactData::ma),
            Codec.INT.optionalFieldOf("phap", 0).forGetter(ArtifactData::phap)
    ).apply(i, ArtifactData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArtifactData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ArtifactData::dao,
            ByteBufCodecs.VAR_INT, ArtifactData::the,
            ByteBufCodecs.VAR_INT, ArtifactData::ma,
            ByteBufCodecs.VAR_INT, ArtifactData::phap,
            ArtifactData::new);

    public ArtifactData {
        dao = clamp(dao);
        the = clamp(the);
        ma = clamp(ma);
        phap = clamp(phap);
    }

    /** Tương thích cũ (chưa có Pháp Khí). */
    public ArtifactData(int dao, int the, int ma) { this(dao, the, ma, 0); }

    private static int clamp(int v) { return Math.max(0, Math.min(ArtifactMath.MAX_STAGE, v)); }

    public int get(Path p) {
        return switch (p) {
            case DAO -> dao;
            case THE -> the;
            case MA -> ma;
            case PHAP -> phap;
            default -> 0;
        };
    }

    public ArtifactData with(Path p, int stage) {
        return switch (p) {
            case DAO -> new ArtifactData(stage, the, ma, phap);
            case THE -> new ArtifactData(dao, stage, ma, phap);
            case MA -> new ArtifactData(dao, the, stage, phap);
            case PHAP -> new ArtifactData(dao, the, ma, stage);
            default -> this;
        };
    }
}
