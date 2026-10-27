package com.hukhong.city.ambient;

import com.hukhong.HuKhong;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Dang ky entity dan cu trang tri (AmbientNpc). */
public final class AmbientRegistry {
    private AmbientRegistry() {}

    /** So skin nguoi vanilla co the chon (xem client renderer). */
    public static final int SKINS = 8;

    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, HuKhong.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<AmbientNpc>> AMBIENT = TYPES.register("ambient_npc",
            () -> EntityType.Builder.of(AmbientNpc::new, MobCategory.MISC).sized(0.6f, 1.8f).clientTrackingRange(8).build("ambient_npc"));

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> e.put(AMBIENT.get(), AmbientNpc.attrs().build()));
    }
}
