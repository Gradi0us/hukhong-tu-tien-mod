package com.hukhong.cores;

import com.hukhong.HuKhong;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/** Đăng ký datapack registry Lõi (đồng bộ xuống client). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CoreRegistry {
    private CoreRegistry() {}

    @SubscribeEvent
    public static void onNewRegistry(DataPackRegistryEvent.NewRegistry e) {
        e.dataPackRegistry(CoreDef.REGISTRY_KEY, CoreDef.CODEC, CoreDef.CODEC);
    }

    public static CoreDef get(RegistryAccess access, ResourceLocation id) {
        return access.registry(CoreDef.REGISTRY_KEY).map(r -> r.get(id)).orElse(null);
    }

    /** Sắp xếp theo id để roll tất định. */
    public static Map<ResourceLocation, CoreDef> all(RegistryAccess access) {
        Map<ResourceLocation, CoreDef> out = new LinkedHashMap<>();
        access.registry(CoreDef.REGISTRY_KEY).ifPresent(r -> r.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(en -> out.put(en.getKey().location(), en.getValue())));
        return out;
    }
}
