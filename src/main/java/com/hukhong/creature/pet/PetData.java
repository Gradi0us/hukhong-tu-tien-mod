package com.hukhong.creature.pet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;
import java.util.UUID;

/**
 * Linh thu cua nguoi choi: 1 thu chien dau ("battle") + 1 linh sung ("spirit"). Luu UUID thuc the.
 * Thu da chet giu ban chup ({@link Fallen}) de dem nguoc hoi sinh; cac truong nay co mac dinh nen du lieu cu van doc duoc.
 */
public record PetData(Optional<UUID> battle, Optional<UUID> spirit, Optional<Fallen> battleFallen, Optional<Fallen> spiritFallen) {

    /** Thu da chet: ban chup de hoi sinh (kind = id CreatureKind, branch = "" neu chua tien hoa). */
    public record Fallen(String kind, int stage, String branch, int level, int bond, long reviveAt, CompoundTag snapshot) {
        public static final Codec<Fallen> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("kind").forGetter(Fallen::kind),
                Codec.INT.optionalFieldOf("stage", 0).forGetter(Fallen::stage),
                Codec.STRING.optionalFieldOf("branch", "").forGetter(Fallen::branch),
                Codec.INT.optionalFieldOf("level", 1).forGetter(Fallen::level),
                Codec.INT.optionalFieldOf("bond", 0).forGetter(Fallen::bond),
                Codec.LONG.fieldOf("revive_at").forGetter(Fallen::reviveAt),
                CompoundTag.CODEC.fieldOf("snapshot").forGetter(Fallen::snapshot)).apply(i, Fallen::new));
    }

    public static final PetData EMPTY = new PetData(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    public static final Codec<PetData> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.optionalFieldOf("battle").forGetter(PetData::battle),
            UUIDUtil.CODEC.optionalFieldOf("spirit").forGetter(PetData::spirit),
            Fallen.CODEC.optionalFieldOf("battle_fallen").forGetter(PetData::battleFallen),
            Fallen.CODEC.optionalFieldOf("spirit_fallen").forGetter(PetData::spiritFallen)).apply(i, PetData::new));

    /** Ban cu (chi co battle/spirit). */
    public PetData(Optional<UUID> battle, Optional<UUID> spirit) { this(battle, spirit, Optional.empty(), Optional.empty()); }

    public Optional<UUID> get(String slot) { return "battle".equals(slot) ? battle : "spirit".equals(slot) ? spirit : Optional.empty(); }

    public Optional<Fallen> fallen(String slot) { return "battle".equals(slot) ? battleFallen : "spirit".equals(slot) ? spiritFallen : Optional.empty(); }

    /** Ô bị chiếm: đang có thú, hoặc thú đã chết đang chờ hồi sinh. */
    public boolean taken(String slot) { return get(slot).isPresent() || fallen(slot).isPresent(); }

    public PetData with(String slot, UUID id) {
        Optional<UUID> v = Optional.ofNullable(id);
        return "battle".equals(slot) ? new PetData(v, spirit, battleFallen, spiritFallen)
                : "spirit".equals(slot) ? new PetData(battle, v, battleFallen, spiritFallen) : this;
    }

    public PetData withFallen(String slot, Fallen f) {
        Optional<Fallen> v = Optional.ofNullable(f);
        return "battle".equals(slot) ? new PetData(battle, spirit, v, spiritFallen)
                : "spirit".equals(slot) ? new PetData(battle, spirit, battleFallen, v) : this;
    }
}
