package com.hukhong.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Map;
import java.util.Set;

/** Dữ liệu tu sĩ (bất biến). Realm suy ra từ level. */
public record CultivationData(int level, long exp, int resource, int resourceMax,
                              Set<Path> paths, Map<Path, Aptitude> aptitude, boolean bottleneck) {

    public static final int MAX_LEVEL = 100;
    public static final CultivationData DEFAULT = new CultivationData(1, 0, 100, 100, Set.of(), Map.of(), false);

    public static final Codec<CultivationData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("level", 1).forGetter(CultivationData::level),
            Codec.LONG.optionalFieldOf("exp", 0L).forGetter(CultivationData::exp),
            Codec.INT.optionalFieldOf("resource", 100).forGetter(CultivationData::resource),
            Codec.INT.optionalFieldOf("resource_max", 100).forGetter(CultivationData::resourceMax),
            Path.CODEC.listOf().optionalFieldOf("paths", java.util.List.of())
                    .xmap(l -> (Set<Path>) Set.copyOf(l), s -> s.stream().sorted().toList())
                    .forGetter(CultivationData::paths),
            Codec.unboundedMap(Path.CODEC, Aptitude.CODEC).optionalFieldOf("aptitude", Map.of())
                    .forGetter(CultivationData::aptitude),
            Codec.BOOL.optionalFieldOf("bottleneck", false).forGetter(CultivationData::bottleneck)
    ).apply(i, CultivationData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivationData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public CultivationData {
        level = Math.max(1, Math.min(MAX_LEVEL, level));
        exp = Math.max(0, exp);
        paths = Set.copyOf(paths);
        aptitude = Map.copyOf(aptitude);
        resourceMax = Math.max(1, resourceMax);
        resource = Math.max(0, Math.min(resourceMax, resource));
    }

    public int realmIndex() { return Realm.indexOf(level); }

    /** Đạo chính (đầu tiên theo thứ tự enum) hoặc null nếu chưa nhập đạo. */
    public Path primaryPath() {
        return paths.stream().sorted().findFirst().orElse(null);
    }

    /** Khóa ở mốc 10/20/.../90 cho tới khi chọn Lõi (TODO Ấn Hồn Đài). */
    public boolean milestoneLocked() { return level < MAX_LEVEL && level % 10 == 0; }

    public CultivationData withLevel(int l) {
        int max = CultivationService.resourceMaxFor(l);
        return new CultivationData(l, 0, Math.min(resource, max), max, paths, aptitude, bottleneck);
    }

    public CultivationData withExp(long e) {
        return new CultivationData(level, e, resource, resourceMax, paths, aptitude, bottleneck);
    }
}
