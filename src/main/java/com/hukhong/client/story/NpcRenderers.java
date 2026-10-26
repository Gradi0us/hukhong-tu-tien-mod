package com.hukhong.client.story;

import com.hukhong.HuKhong;
import com.hukhong.story.npc.StoryNpc;
import com.hukhong.story.npc.StoryNpcRegistry;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** NPC tuyến chính dùng mô hình dân làng vanilla + skin riêng nếu có (mặc định skin dân làng). */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class NpcRenderers {
    private NpcRenderers() {}

    private static final ResourceLocation TEX = ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(StoryNpcRegistry.STORY_NPC.get(), c ->
                new MobRenderer<StoryNpc, VillagerModel<StoryNpc>>(c, new VillagerModel<>(c.bakeLayer(ModelLayers.VILLAGER)), 0.5f) {
                    @Override public ResourceLocation getTextureLocation(StoryNpc n) { return TEX; }
                });
    }
}
