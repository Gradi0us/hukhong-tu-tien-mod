package com.hukhong.effect;

import com.hukhong.HuKhong;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** hukhong:hu_hoa - giam 50% hoi tai nguyen (code hoi tai nguyen doc qua id nay). */
public final class ModEffects {
    private ModEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, HuKhong.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> HU_HOA = EFFECTS.register("hu_hoa",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x7B2FBF) {});

    /** He so hoi tai nguyen: 0.5 neu dang Hu Hoa, nguoc lai 1.0. */
    public static double regenMultiplier(LivingEntity e) {
        return e.hasEffect(HU_HOA) ? 0.5 : 1.0;
    }

    public static void register(IEventBus bus) { EFFECTS.register(bus); }
}
