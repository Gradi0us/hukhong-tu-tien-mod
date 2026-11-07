package com.hukhong.client.story;

import com.hukhong.HuKhong;
import com.hukhong.story.npc.StoryNpc;
import com.hukhong.story.npc.StoryNpcRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** NPC tuyến chính: model GeckoLib riêng từng nhân vật (StoryNpc.modelKey), NPC phụ dùng npc_default. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class NpcRenderers {
    private NpcRenderers() {}

    static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, p); }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(StoryNpcRegistry.STORY_NPC.get(), c -> new GeoEntityRenderer<StoryNpc>(c, new GeoModel<StoryNpc>() {
            @Override public ResourceLocation getModelResource(StoryNpc n) { return rl("geo/entity/" + StoryNpc.modelKey(n.npcId()) + ".geo.json"); }
            @Override public ResourceLocation getTextureResource(StoryNpc n) { return rl("textures/entity/" + StoryNpc.modelKey(n.npcId()) + ".png"); }
            @Override public ResourceLocation getAnimationResource(StoryNpc n) { return rl("animations/entity/" + StoryNpc.modelKey(n.npcId()) + ".animation.json"); }
        }) {
            { this.shadowRadius = 0.5f; }
        });
    }
}
