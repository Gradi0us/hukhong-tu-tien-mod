package com.hukhong.world.yeulam;

import com.hukhong.HuKhong;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Feature cây cổ thụ Đại Sâm Lâm (configured/placed/biome_modifier ở data/hukhong). */
public final class YeuLamRegistry {
    private YeuLamRegistry() {}

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, HuKhong.MODID);
    public static final DeferredHolder<Feature<?>, CoThuFeature> CO_THU = FEATURES.register("co_thu", () -> new CoThuFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus bus) { FEATURES.register(bus); }
}
