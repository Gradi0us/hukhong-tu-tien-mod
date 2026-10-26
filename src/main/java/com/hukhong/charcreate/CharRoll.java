package com.hukhong.charcreate;

import com.hukhong.core.Aptitude;
import com.hukhong.core.Path;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.function.ToIntFunction;

/** Kết quả roll lưu trước ở server (seed + kết quả). Relog không reroll. */
public record CharRoll(long seed, int slots, Map<Path, Aptitude> aptitude, Origin origin, int startAge) {
    public static final int[] START_AGES = {6, 8, 10, 12, 14, 16};
    private static final int[] APT_WEIGHTS = {8, 24, 28, 22, 13, 4, 1}; // D C B A S SS SSS

    public static final Codec<CharRoll> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("seed").forGetter(CharRoll::seed),
            Codec.INT.fieldOf("slots").forGetter(CharRoll::slots),
            Codec.unboundedMap(Path.CODEC, Aptitude.CODEC).fieldOf("aptitude").forGetter(CharRoll::aptitude),
            Origin.CODEC.fieldOf("origin").forGetter(CharRoll::origin),
            Codec.INT.fieldOf("start_age").forGetter(CharRoll::startAge)
    ).apply(i, CharRoll::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CharRoll> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public CharRoll { aptitude = Map.copyOf(aptitude); }

    public static CharRoll roll(long seed) {
        Random r = new Random(seed);
        int x = r.nextInt(100);
        int slots = x < 75 ? 1 : x < 95 ? 2 : 3;
        Origin origin = pick(r, Origin.values(), o -> o.weight);
        Map<Path, Aptitude> apt = new EnumMap<>(Path.class);
        for (Path p : Path.values()) {
            Aptitude a = pick(r, Aptitude.values(), v -> APT_WEIGHTS[v.ordinal()]);
            if (origin == Origin.NOBLE && a.ordinal() < Aptitude.C.ordinal()) a = Aptitude.C; // sàn C (R13)
            apt.put(p, a);
        }
        return new CharRoll(seed, slots, apt, origin, START_AGES[r.nextInt(START_AGES.length)]);
    }

    private static <T> T pick(Random r, T[] vals, ToIntFunction<T> w) {
        int total = 0;
        for (T v : vals) total += w.applyAsInt(v);
        int x = r.nextInt(total);
        for (T v : vals) {
            x -= w.applyAsInt(v);
            if (x < 0) return v;
        }
        return vals[vals.length - 1];
    }
}
