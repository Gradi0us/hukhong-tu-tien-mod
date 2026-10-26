package com.hukhong.cores;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Lõi đã chọn + phiên Ấn Hồn đang chờ (roll lưu trước) + cờ Mắc Kẹt (stuckAt = mốc, 0 = không). */
public record CoreSlots(List<Picked> picked, Optional<Pending> pending, int stuckAt) {
    public static final CoreSlots EMPTY = new CoreSlots(List.of(), Optional.empty(), 0);

    public record Picked(int milestone, ResourceLocation core, int tier) {
        public static final Codec<Picked> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("milestone").forGetter(Picked::milestone),
                ResourceLocation.CODEC.fieldOf("core").forGetter(Picked::core),
                Codec.INT.fieldOf("tier").forGetter(Picked::tier)
        ).apply(i, Picked::new));

        public CoreTier coreTier() { return CoreTier.of(tier); }
    }

    /** Kết quả roll đã chốt (seed + toàn bộ kết quả) — thoát/relog không reroll. */
    public record Pending(int milestone, long seed, int luck, int tier, List<ResourceLocation> ids,
                          ResourceLocation fourth, int fourthTier, boolean stuckRoll) {
        public static final Codec<Pending> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("milestone").forGetter(Pending::milestone),
                Codec.LONG.fieldOf("seed").forGetter(Pending::seed),
                Codec.INT.fieldOf("luck").forGetter(Pending::luck),
                Codec.INT.fieldOf("tier").forGetter(Pending::tier),
                ResourceLocation.CODEC.listOf().fieldOf("ids").forGetter(Pending::ids),
                ResourceLocation.CODEC.fieldOf("fourth").forGetter(Pending::fourth),
                Codec.INT.fieldOf("fourth_tier").forGetter(Pending::fourthTier),
                Codec.BOOL.fieldOf("stuck_roll").forGetter(Pending::stuckRoll)
        ).apply(i, Pending::new));
    }

    public static final Codec<CoreSlots> CODEC = RecordCodecBuilder.create(i -> i.group(
            Picked.CODEC.listOf().optionalFieldOf("picked", List.of()).forGetter(CoreSlots::picked),
            Pending.CODEC.optionalFieldOf("pending").forGetter(CoreSlots::pending),
            Codec.INT.optionalFieldOf("stuck_at", 0).forGetter(CoreSlots::stuckAt)
    ).apply(i, CoreSlots::new));

    /** Gửi client: KHÔNG kèm pending (tránh lộ ô úp). */
    public static final StreamCodec<RegistryFriendlyByteBuf, CoreSlots> SYNC_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC).map(s -> s, CoreSlots::withoutPending);

    public CoreSlots withoutPending() { return new CoreSlots(picked, Optional.empty(), stuckAt); }

    public CoreSlots withPending(Pending p) { return new CoreSlots(picked, Optional.of(p), stuckAt); }

    public CoreSlots withPicked(Picked p) {
        List<Picked> l = new ArrayList<>(picked);
        l.removeIf(x -> x.milestone() == p.milestone());
        l.add(p);
        return new CoreSlots(l, Optional.empty(), stuckAt == p.milestone() ? 0 : stuckAt);
    }

    public CoreSlots withStuck(int milestone) { return new CoreSlots(picked, Optional.empty(), milestone); }

    public CoreSlots withStuckCleared() { return new CoreSlots(picked, pending, 0); }
}
