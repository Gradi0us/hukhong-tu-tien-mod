package com.hukhong.creature.pet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

/** Linh thu cua nguoi choi: 1 thu chien dau ("battle") + 1 linh sung ("spirit"). Luu UUID thuc the. */
public record PetData(Optional<UUID> battle, Optional<UUID> spirit) {
    public static final PetData EMPTY = new PetData(Optional.empty(), Optional.empty());
    public static final Codec<PetData> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.optionalFieldOf("battle").forGetter(PetData::battle),
            UUIDUtil.CODEC.optionalFieldOf("spirit").forGetter(PetData::spirit)).apply(i, PetData::new));

    public Optional<UUID> get(String slot) { return "battle".equals(slot) ? battle : "spirit".equals(slot) ? spirit : Optional.empty(); }

    public boolean taken(String slot) { return get(slot).isPresent(); }

    public PetData with(String slot, UUID id) {
        Optional<UUID> v = Optional.ofNullable(id);
        return "battle".equals(slot) ? new PetData(v, spirit) : "spirit".equals(slot) ? new PetData(battle, v) : this;
    }
}
