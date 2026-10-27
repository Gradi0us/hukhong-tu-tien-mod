package com.hukhong.client.renderer;

import com.hukhong.HuKhong;
import com.hukhong.entity.PoisonSpit;
import com.hukhong.entity.V3Entities;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderer cho quai/boss v3 (GeckoLib) + nuoc bot doc (khong ve model, chi hat tu server). */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class MobBuffRenderers {
    private MobBuffRenderers() {}

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        geo(e, V3Entities.HU_ANH.get(), "hu_anh");
        geo(e, V3Entities.HU_GIAP.get(), "hu_giap");
        geo(e, V3Entities.HU_TRUNG.get(), "hu_trung");
        geo(e, V3Entities.HU_TRIEU_SU.get(), "hu_trieu_su");
        geo(e, V3Entities.SA_HAT_VUONG.get(), "sa_hat_vuong");
        geo(e, V3Entities.BANG_NHAN.get(), "bang_nhan");
        geo(e, V3Entities.DOC_MAU.get(), "doc_mau");
        e.registerEntityRenderer(V3Entities.POISON_SPIT.get(), SpitRenderer::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void geo(EntityRenderersEvent.RegisterRenderers e, net.minecraft.world.entity.EntityType type, String id) {
        e.registerEntityRenderer(type, c -> HuRenderers.glow(new GeoEntityRenderer(c,
                new DefaultedEntityGeoModel(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, id)))));
    }

    /** Khong ve gi (hat nuoc bot do server gui). */
    private static final class SpitRenderer extends EntityRenderer<PoisonSpit> {
        SpitRenderer(EntityRendererProvider.Context c) { super(c); }

        @Override public ResourceLocation getTextureLocation(PoisonSpit e) {
            return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
        }
    }
}
