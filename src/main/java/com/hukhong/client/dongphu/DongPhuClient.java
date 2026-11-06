package com.hukhong.client.dongphu;

import com.hukhong.HuKhong;
import com.hukhong.dongphu.DongPhuRegistry;
import com.hukhong.dongphu.DongPhuTranBanBE;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Renderer Động Phủ Trận Bàn: đĩa ngọc + vòng rune + lõi; lớp phát sáng (glowmask) cho rune và tinh thể. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class DongPhuClient {
    private DongPhuClient() {}

    static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, p); }

    static final class Model extends GeoModel<DongPhuTranBanBE> {
        @Override public ResourceLocation getModelResource(DongPhuTranBanBE e) { return rl("geo/entity/dong_phu_tran_ban.geo.json"); }
        @Override public ResourceLocation getTextureResource(DongPhuTranBanBE e) { return rl("textures/entity/dong_phu_tran_ban.png"); }
        @Override public ResourceLocation getAnimationResource(DongPhuTranBanBE e) { return rl("animations/entity/dong_phu_tran_ban.animation.json"); }
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(DongPhuRegistry.TRAN_BAN_BE.get(), ctx -> {
            GeoBlockRenderer<DongPhuTranBanBE> r = new GeoBlockRenderer<>(new Model());
            r.addRenderLayer(new AutoGlowingGeoLayer<>(r));
            return r;
        });
    }
}
