package com.hukhong.client.story;

import com.hukhong.HuKhong;
import com.hukhong.story.StoryRegistry;
import com.hukhong.story.SuDo;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class StoryRenderers {
    private StoryRenderers() {}

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        for (int i = 0; i < 3; i++) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, SuDo.IDS[i]);
            e.registerEntityRenderer(StoryRegistry.variant(i), c -> com.hukhong.client.renderer.HuRenderers.glow(new GeoEntityRenderer<SuDo>(c, new DefaultedEntityGeoModel<>(id))));
        }
    }
}
