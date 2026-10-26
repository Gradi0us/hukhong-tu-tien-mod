package com.hukhong.client.chg;

import com.hukhong.HuKhong;
import com.hukhong.chg.ChapHanhGia;
import com.hukhong.chg.ChgKit;
import com.hukhong.chg.ChgRegistry;
import com.hukhong.client.renderer.HuRenderers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class ChgRenderers {
    private ChgRenderers() {}

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        for (int i = 1; i <= ChgKit.COUNT; i++) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "chg_" + i);
            e.registerEntityRenderer(ChgRegistry.type(i),
                    c -> HuRenderers.glow(new GeoEntityRenderer<ChapHanhGia>(c, new DefaultedEntityGeoModel<>(id))));
        }
    }
}
