package com.hukhong.client.endgame;

import com.hukhong.HuKhong;
import com.hukhong.client.renderer.HuRenderers;
import com.hukhong.endgame.DaKhuyet;
import com.hukhong.endgame.EndgameRegistry;
import com.hukhong.endgame.NguyenThuyTichDiet;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class EndgameRenderers {
    private EndgameRenderers() {}

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        ResourceLocation dak = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "da_khuyet");
        e.registerEntityRenderer(EndgameRegistry.BOSS.get(),
                c -> HuRenderers.glow(new GeoEntityRenderer<NguyenThuyTichDiet>(c, new NttdModel())));
        e.registerEntityRenderer(EndgameRegistry.DA_KHUYET.get(),
                c -> HuRenderers.glow(new com.hukhong.client.boss.PhasedBossRenderer<DaKhuyet>(c, dak.getPath()).withScale(0.5f)));
    }

    /** Model theo dạng (dữ liệu đồng bộ FORM): dạng 1 = nttd_form1 (hoá thân), dạng 2 = nguyen_thuy_tich_diet. */
    static final class NttdModel extends GeoModel<NguyenThuyTichDiet> {
        private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, p); }
        private static String id(NguyenThuyTichDiet e) { return e.form() == 1 ? "nttd_form1" : "nguyen_thuy_tich_diet"; }
        @Override public ResourceLocation getModelResource(NguyenThuyTichDiet e) { return rl("geo/entity/" + id(e) + ".geo.json"); }
        @Override public ResourceLocation getTextureResource(NguyenThuyTichDiet e) { return rl("textures/entity/" + id(e) + ".png"); }
        @Override public ResourceLocation getAnimationResource(NguyenThuyTichDiet e) { return rl("animations/entity/" + id(e) + ".animation.json"); }
    }
}
