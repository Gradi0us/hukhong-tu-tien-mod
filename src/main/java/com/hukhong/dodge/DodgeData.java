package com.hukhong.dodge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Trạng thái Thể Lực + Lách Né của người chơi. {@code stamina < 0} = đầy (chưa khởi tạo).
 * Mốc thời gian = gameTime của level.
 */
public record DodgeData(float stamina, long lastSpend, long dodgeStart, long cooldownUntil, boolean perfectUsed) {
    public static final DodgeData DEFAULT = new DodgeData(-1f, -1000L, -1000L, 0L, true);

    public static final Codec<DodgeData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("stamina").forGetter(DodgeData::stamina),
            Codec.LONG.optionalFieldOf("last_spend", -1000L).forGetter(DodgeData::lastSpend),
            Codec.LONG.optionalFieldOf("dodge_start", -1000L).forGetter(DodgeData::dodgeStart),
            Codec.LONG.optionalFieldOf("cooldown_until", 0L).forGetter(DodgeData::cooldownUntil),
            Codec.BOOL.optionalFieldOf("perfect_used", true).forGetter(DodgeData::perfectUsed)
    ).apply(i, DodgeData::new));

    public float current(float max) { return stamina < 0 ? max : Math.min(stamina, max); }

    public DodgeData withStamina(float s, long spend) { return new DodgeData(s, spend, dodgeStart, cooldownUntil, perfectUsed); }
}
