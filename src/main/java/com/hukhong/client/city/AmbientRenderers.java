package com.hukhong.client.city;

import com.hukhong.HuKhong;
import com.hukhong.city.ambient.AmbientNpc;
import com.hukhong.city.ambient.AmbientRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Dan cu trang tri: mo hinh nguoi + skin nguoi choi mac dinh cua vanilla (chon theo SKIN), co giap cho linh. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class AmbientRenderers {
    private AmbientRenderers() {}

    private static final String[] SKINS = {"steve", "alex", "ari", "efe", "kai", "makena", "noor", "zuri"};

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(AmbientRegistry.AMBIENT.get(), c -> {
            var r = new HumanoidMobRenderer<AmbientNpc, HumanoidModel<AmbientNpc>>(c, new HumanoidModel<>(c.bakeLayer(ModelLayers.PLAYER)), 0.5f) {
                @Override public ResourceLocation getTextureLocation(AmbientNpc n) {
                    String s = SKINS[Math.floorMod(n.skin(), SKINS.length)];
                    return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/" + s + ".png");
                }
            };
            r.addLayer(new HumanoidArmorLayer<>(r, new HumanoidModel<>(c.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                    new HumanoidModel<>(c.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), c.getModelManager()));
            return r;
        });
    }
}
