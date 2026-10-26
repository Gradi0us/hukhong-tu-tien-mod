package com.hukhong.client.renderer;

import com.hukhong.HuKhong;
import com.hukhong.entity.HuChu;
import com.hukhong.entity.HuCot;
import com.hukhong.entity.HuThi;
import com.hukhong.entity.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class HuRenderers {
    private HuRenderers() {}

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.HU_THI.get(),
                c -> glow(new GeoEntityRenderer<HuThi>(c, new DefaultedEntityGeoModel<>(rl("hu_thi")))));
        e.registerEntityRenderer(ModEntities.HU_COT.get(),
                c -> glow(new GeoEntityRenderer<HuCot>(c, new DefaultedEntityGeoModel<>(rl("hu_cot")))));
        e.registerEntityRenderer(ModEntities.HU_CHU.get(),
                c -> glow(new GeoEntityRenderer<HuChu>(c, new DefaultedEntityGeoModel<>(rl("hu_chu")))));
    }

    /** Thêm glowmask nếu có <id>_glowmask.png (kiểm tra lười + cache, thiếu file thì bỏ qua). */
    private static <T extends net.minecraft.world.entity.Entity & software.bernie.geckolib.animatable.GeoAnimatable> GeoEntityRenderer<T> glow(GeoEntityRenderer<T> r) {
        r.addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<T>(r) {
            private final java.util.Map<ResourceLocation, Boolean> has = new java.util.HashMap<>();

            @Override
            public void render(com.mojang.blaze3d.vertex.PoseStack ps, T a, software.bernie.geckolib.cache.object.BakedGeoModel m,
                               net.minecraft.client.renderer.RenderType rt, net.minecraft.client.renderer.MultiBufferSource b,
                               com.mojang.blaze3d.vertex.VertexConsumer vc, float pt, int light, int overlay) {
                ResourceLocation tex = getTextureResource(a);
                boolean ok = has.computeIfAbsent(tex, k -> net.minecraft.client.Minecraft.getInstance().getResourceManager()
                        .getResource(ResourceLocation.fromNamespaceAndPath(k.getNamespace(),
                                k.getPath().replace(".png", "_glowmask.png"))).isPresent());
                if (ok) super.render(ps, a, m, rt, b, vc, pt, light, overlay);
            }
        });
        return r;
    }

    private static ResourceLocation rl(String id) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, id); }
}
