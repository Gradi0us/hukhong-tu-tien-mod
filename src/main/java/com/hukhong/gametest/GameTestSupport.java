package com.hukhong.gametest;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;

/** Khu bien ngau nhien cua vanilla trong GameTest (xem docs/research/RCA-v1-flaky-gametests.md). */
public final class GameTestSupport {
    private GameTestSupport() {}

    /** Bo: baby (5%, hitbox thap -> tia ky nang bay qua dau), leader_zombie_bonus (~0.5%, HP x2-5), chay nang (random moi tick ban ngay). */
    public static void deterministic(Mob m) {
        if (m instanceof Zombie z) {
            z.setBaby(false);
            if (z.isPassenger()) z.stopRiding();
            var hp = z.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) hp.removeModifier(ResourceLocation.withDefaultNamespace("leader_zombie_bonus"));
            z.setHealth(z.getMaxHealth());
        }
        m.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, true, false));
    }

    /** Bao kich la ngau nhien (DamageRules.resolve): tat de so sanh sat thuong chinh xac. */
    public static void noCrit(Player p) {
        var inst = p.getAttribute(com.hukhong.stats.ModAttributes.holder(com.hukhong.stats.ModAttributes.CRIT_CHANCE));
        if (inst != null) inst.setBaseValue(0);
    }
}
