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
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class EndgameRenderers {
    private EndgameRenderers() {}

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        ResourceLocation boss = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "nguyen_thuy_tich_diet");
        ResourceLocation dak = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "da_khuyet");
        e.registerEntityRenderer(EndgameRegistry.BOSS.get(),
                c -> HuRenderers.glow(new GeoEntityRenderer<NguyenThuyTichDiet>(c, new DefaultedEntityGeoModel<>(boss))));
        e.registerEntityRenderer(EndgameRegistry.DA_KHUYET.get(),
                c -> HuRenderers.glow(new com.hukhong.client.boss.PhasedBossRenderer<DaKhuyet>(c, dak.getPath())));
    }
}
