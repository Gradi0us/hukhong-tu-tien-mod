package com.hukhong.client.render.artifact;

import com.hukhong.HuKhong;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Gắn ArtifactLayer vào cả hai model người chơi (default/slim). */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class ArtifactLayers {
    private ArtifactLayers() {}

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers e) {
        for (PlayerSkin.Model skin : e.getSkins()) {
            if (e.getSkin(skin) instanceof PlayerRenderer pr) pr.addLayer(new ArtifactLayer(pr));
        }
    }
}
