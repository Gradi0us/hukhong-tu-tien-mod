package com.hukhong.onboarding;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Tiến độ onboarding: step = số gợi ý đã hiện (0..5), shownAt = gameTime lúc hiện gợi ý gần nhất. */
public record OnboardingData(int step, boolean disabled, boolean killed, long shownAt) {
    public static final int TOTAL = 5;
    public static final OnboardingData DEFAULT = new OnboardingData(0, false, false, 0L);

    public static final Codec<OnboardingData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("step", 0).forGetter(OnboardingData::step),
            Codec.BOOL.optionalFieldOf("disabled", false).forGetter(OnboardingData::disabled),
            Codec.BOOL.optionalFieldOf("killed", false).forGetter(OnboardingData::killed),
            Codec.LONG.optionalFieldOf("shown_at", 0L).forGetter(OnboardingData::shownAt)
    ).apply(i, OnboardingData::new));

    public OnboardingData withStep(int s, long now) { return new OnboardingData(s, disabled, killed, now); }
    public OnboardingData withDisabled(boolean d) { return new OnboardingData(step, d, killed, shownAt); }
    public OnboardingData withKilled() { return new OnboardingData(step, disabled, true, shownAt); }
}
