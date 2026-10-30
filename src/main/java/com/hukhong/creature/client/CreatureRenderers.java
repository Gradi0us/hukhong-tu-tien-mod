package com.hukhong.creature.client;

import com.hukhong.HuKhong;
import com.hukhong.client.renderer.HuRenderers;
import com.hukhong.creature.CreatureKind;
import com.hukhong.creature.CreatureRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** GeckoLib renderer cho 14 sinh vat F4 (geo/anim/texture sinh boi tools/gen_c7.py; glowmask neu co). */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class CreatureRenderers {
    private CreatureRenderers() {}

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        for (CreatureKind k : CreatureKind.values())
            e.registerEntityRenderer((EntityType) CreatureRegistry.type(k), c -> (net.minecraft.client.renderer.entity.EntityRenderer) make(c, k.id));
    }

    private static <T extends Entity & GeoAnimatable> GeoEntityRenderer<T> make(EntityRendererProvider.Context c, String id) {
        return HuRenderers.glow(new GeoEntityRenderer<T>(c, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, id))));
    }
}
