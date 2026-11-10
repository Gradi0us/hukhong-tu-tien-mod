package com.hukhong.client.bicanh;

import com.hukhong.HuKhong;
import com.hukhong.bicanh.BiCanhRegistry;
import com.hukhong.bicanh.BiCanhThuVe;
import com.hukhong.client.boss.PhasedBossRenderer;
import com.hukhong.client.renderer.HuRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderer boss Bí Cảnh Thủ Vệ (geo/animation/texture sinh bởi tools/gen_bicanh_thu_ve.py). */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class BiCanhRenderers {
    private BiCanhRenderers() {}

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(BiCanhRegistry.THU_VE.get(), c -> HuRenderers.glow(new PhasedBossRenderer<BiCanhThuVe>(c, BiCanhThuVe.ID)));
    }
}
