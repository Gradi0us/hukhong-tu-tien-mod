package com.hukhong.guild;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Du lieu Hiep Hoi cua nguoi choi (dong bo xuong client, giu khi chet).
 * prog[slot]: -1 chua nhan, 0..need dang lam, DONE da nop. trial: -1 chua nhan thu thach, >=0 tien do.
 * day/genLevel: ngay (gameTime/24000) + cap luc lam moi danh sach nhiem vu.
 */
public record GuildData(int rank, int points, long day, int genLevel, List<Integer> prog, int trial, int done) {
    public static final int NONE = -1, DONE = -2;
    public static final GuildData EMPTY = new GuildData(0, 0, -1, 1, List.of(NONE, NONE, NONE, NONE, NONE), NONE, 0);

    public static final Codec<GuildData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("rank", 0).forGetter(GuildData::rank),
            Codec.INT.optionalFieldOf("points", 0).forGetter(GuildData::points),
            Codec.LONG.optionalFieldOf("day", -1L).forGetter(GuildData::day),
            Codec.INT.optionalFieldOf("genLevel", 1).forGetter(GuildData::genLevel),
            Codec.INT.listOf().optionalFieldOf("prog", EMPTY.prog()).forGetter(GuildData::prog),
            Codec.INT.optionalFieldOf("trial", NONE).forGetter(GuildData::trial),
            Codec.INT.optionalFieldOf("done", 0).forGetter(GuildData::done)
    ).apply(i, GuildData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuildData> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public int progOf(int slot) { return slot >= 0 && slot < prog.size() ? prog.get(slot) : NONE; }

    public int activeCount() {
        int n = 0;
        for (int p : prog) if (p >= 0) n++;
        return n;
    }

    public GuildData withProg(int slot, int v) {
        List<Integer> l = new ArrayList<>(prog);
        while (l.size() < GuildMath.SLOTS) l.add(NONE);
        l.set(slot, v);
        return new GuildData(rank, points, day, genLevel, List.copyOf(l), trial, done);
    }

    public GuildData withPoints(int p) { return new GuildData(rank, Math.max(0, p), day, genLevel, prog, trial, done); }
    public GuildData withRank(int r) { return new GuildData(GuildMath.clampRank(r), points, day, genLevel, prog, NONE, done); }
    public GuildData withTrial(int t) { return new GuildData(rank, points, day, genLevel, prog, t, done); }
    public GuildData withDone(int d) { return new GuildData(rank, points, day, genLevel, prog, trial, d); }

    /** Sang ngay moi: xoa tien do nhiem vu ngay (thu thach giu nguyen). */
    public GuildData refreshed(long newDay, int level) {
        return new GuildData(rank, points, newDay, Math.max(1, level), EMPTY.prog(), trial, done);
    }
}
