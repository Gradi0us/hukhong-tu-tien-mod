package com.hukhong.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.hukhong.charcreate.CharRoll;
import java.util.Map;
import java.util.Set;

/** Dữ liệu tu sĩ (bất biến). Realm suy ra từ level. */
public record CultivationData(int level, long exp, int resource, int resourceMax,
                              Set<Path> paths, Map<Path, Aptitude> aptitude, boolean bottleneck,
                              String origin, CharRoll roll, boolean milestoneUnlocked) {

    public static final int MAX_LEVEL = 100;
    public static final CultivationData DEFAULT = new CultivationData(1, 0, 100, 100, Set.of(), Map.of(), false, "", null, false);

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
            Codec.BOOL.optionalFieldOf("bottleneck", false).forGetter(CultivationData::bottleneck),
            Codec.STRING.optionalFieldOf("origin", "").forGetter(CultivationData::origin),
            CharRoll.CODEC.optionalFieldOf("roll").forGetter(d -> java.util.Optional.ofNullable(d.roll())),
            Codec.BOOL.optionalFieldOf("milestone_unlocked", false).forGetter(CultivationData::milestoneUnlocked)
    ).apply(i, (lv, exp, res, resMax, paths, apt, bn, org, roll, mu) ->
            new CultivationData(lv, exp, res, resMax, paths, apt, bn, org, roll.orElse(null), mu)));

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivationData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /** Constructor tương thích cũ (chưa có xuất thân / roll). */
    public CultivationData(int level, long exp, int resource, int resourceMax,
                           Set<Path> paths, Map<Path, Aptitude> aptitude, boolean bottleneck) {
        this(level, exp, resource, resourceMax, paths, aptitude, bottleneck, "", null, false);
    }

    public CultivationData(int level, long exp, int resource, int resourceMax,
                           Set<Path> paths, Map<Path, Aptitude> aptitude, boolean bottleneck,
                           String origin, CharRoll roll) {
        this(level, exp, resource, resourceMax, paths, aptitude, bottleneck, origin, roll, false);
    }

    public CultivationData {
        origin = origin == null ? "" : origin;
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

    /** Khóa ở mốc 10/20/.../90 cho tới khi mở khóa mốc (Ấn Hồn Đài / lệnh test). */
    public boolean milestoneLocked() { return isMilestoneLevel(level) && !milestoneUnlocked; }

    /** Mốc Lõi R29: 10,20,...,90, 95, 99, 100 (cấp 100 = Thần Lõi, chọn tại Ấn Hồn Đài). */
    public static boolean isMilestoneLevel(int l) { return l <= MAX_LEVEL && (l % 10 == 0 || l == 95 || l == 99); }

    public CultivationData withResource(int r) {
        return new CultivationData(level, exp, r, resourceMax, paths, aptitude, bottleneck, origin, roll, milestoneUnlocked);
    }

    public CultivationData withResourceMax(int m) {
        return new CultivationData(level, exp, resource, m, paths, aptitude, bottleneck, origin, roll, milestoneUnlocked);
    }

    /** Bình Cảnh: cấp 9/19/.../89 (ngay trước mốc). */
    public boolean inBottleneck() { return level < 90 && level % 10 == 9; }

    public CultivationData withMilestoneUnlocked(boolean u) {
        return new CultivationData(level, exp, resource, resourceMax, paths, aptitude, bottleneck, origin, roll, u);
    }

    public CultivationData withLevel(int l) {
        int max = CultivationService.resourceMaxFor(l);
        return new CultivationData(l, 0, Math.min(resource, max), max, paths, aptitude, bottleneck, origin, roll, false);
    }

    public CultivationData withExp(long e) {
        return new CultivationData(level, e, resource, resourceMax, paths, aptitude, bottleneck, origin, roll, milestoneUnlocked);
    }

    public CultivationData withCharacter(Set<Path> p, Map<Path, Aptitude> apt, String org, CharRoll r) {
        return new CultivationData(level, exp, resource, resourceMax, p, apt, bottleneck, org, r, milestoneUnlocked);
    }
}
