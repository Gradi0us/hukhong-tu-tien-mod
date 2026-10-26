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
                c -> new GeoEntityRenderer<HuThi>(c, new DefaultedEntityGeoModel<>(rl("hu_thi"))));
        e.registerEntityRenderer(ModEntities.HU_COT.get(),
                c -> new GeoEntityRenderer<HuCot>(c, new DefaultedEntityGeoModel<>(rl("hu_cot"))));
        e.registerEntityRenderer(ModEntities.HU_CHU.get(),
                c -> new GeoEntityRenderer<HuChu>(c, new DefaultedEntityGeoModel<>(rl("hu_chu"))));
    }

    private static ResourceLocation rl(String id) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, id); }
}
