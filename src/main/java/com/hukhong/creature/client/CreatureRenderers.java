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
            e.registerEntityRenderer((EntityType) CreatureRegistry.type(k), c -> (net.minecraft.client.renderer.entity.EntityRenderer) (k.plant() ? makePlant(c, k.id) : make(c, k.id)));
    }

    /** Yêu Thú thực vật: bị Hư Không xâm thực thì dùng model + texture id_hu (tinh thể hư không mọc trên thân, vân nứt tím). */
    @SuppressWarnings("unchecked")
    private static <T extends Entity & GeoAnimatable> GeoEntityRenderer<T> makePlant(EntityRendererProvider.Context c, String id) {
        ResourceLocation base = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, id);
        ResourceLocation geoHu = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "geo/entity/" + id + "_hu.geo.json");
        ResourceLocation texHu = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/entity/" + id + "_hu.png");
        var model = new DefaultedEntityGeoModel<T>(base) {
            private boolean hu(T a) { return a instanceof com.hukhong.creature.LinhQuai q && q.corrupted(); }
            @Override public ResourceLocation getModelResource(T a) { return hu(a) ? geoHu : super.getModelResource(a); }
            @Override public ResourceLocation getTextureResource(T a) { return hu(a) ? texHu : super.getTextureResource(a); }
        };
        return HuRenderers.glow(new GeoEntityRenderer<T>(c, model));
    }

    /** Loài có model theo giai đoạn trưởng thành (gen_c14_pet_stages.py): giai đoạn -> hậu tố. */
    private static String stageSuffix(String id, int stage) {
        return switch (id) {
            case "ngoc_tho", "linh_ho" -> stage == 2 ? "_g2" : stage == 1 ? "_g1" : "_g0";
            case "thiet_boi_hung_con" -> stage >= 1 ? "_g1" : "_g0";
            case "thiet_boi_hung" -> stage == 2 ? "_g2" : "";
            default -> "";
        };
    }

    private static <T extends Entity & GeoAnimatable> GeoEntityRenderer<T> make(EntityRendererProvider.Context c, String id) {
        if (!stageSuffix(id, 2).isEmpty() || !stageSuffix(id, 1).isEmpty()) {
            var model = new DefaultedEntityGeoModel<T>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, id)) {
                private String sid(T a) { return id + (a instanceof com.hukhong.creature.LinhThu t && t.isTame() ? stageSuffix(id, t.growthStage()) : ""); }
                @Override public ResourceLocation getModelResource(T a) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "geo/entity/" + sid(a) + ".geo.json"); }
                @Override public ResourceLocation getTextureResource(T a) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/entity/" + sid(a) + ".png"); }
                @Override public ResourceLocation getAnimationResource(T a) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "animations/entity/" + sid(a) + ".animation.json"); }
            };
            return HuRenderers.glow(new GeoEntityRenderer<T>(c, model));
        }
        return HuRenderers.glow(new GeoEntityRenderer<T>(c, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, id))));
    }
}
