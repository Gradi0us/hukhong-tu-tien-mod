package com.hukhong.gates;

import net.minecraft.world.entity.LivingEntity;

/** Mọi nguồn EXP/thưởng tu tiên nhân hệ số này. */
public final class RewardGate {
    private RewardGate() {}

    public static boolean canReward(LivingEntity victim) { return !SpawnerMarks.is(victim) && !com.hukhong.society.SectTrial.isTrialMob(victim); }

    /** 0..1. Gọi đúng 1 lần mỗi lần giết (ghi nhận vào KillRateGate). */
    public static float reward(LivingEntity victim) {
        return canReward(victim) ? KillRateGate.multiplier(victim) : 0f;
    }
}
