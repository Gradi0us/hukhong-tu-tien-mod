package com.hukhong.client.treasure;

import com.hukhong.HuKhong;
import com.hukhong.treasure.BaoRuongBE;
import com.hukhong.treasure.TreasureRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Renderer Bảo Rương: model/texture theo cấp, lớp phát sáng (glowmask) cho ngọc và vòng tinh thể. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class TreasureClient {
    private TreasureClient() {}

    static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, p); }

    static final class Model extends GeoModel<BaoRuongBE> {
        @Override public ResourceLocation getModelResource(BaoRuongBE e) { return rl("geo/entity/bao_ruong_" + e.tier() + ".geo.json"); }
        @Override public ResourceLocation getTextureResource(BaoRuongBE e) { return rl("textures/entity/bao_ruong_" + e.tier() + ".png"); }
        @Override public ResourceLocation getAnimationResource(BaoRuongBE e) { return rl("animations/entity/bao_ruong.animation.json"); }
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(TreasureRegistry.BAO_RUONG_BE.get(), ctx -> {
            GeoBlockRenderer<BaoRuongBE> r = new GeoBlockRenderer<>(new Model());
            r.addRenderLayer(new AutoGlowingGeoLayer<>(r));
            return r;
        });
    }
}
