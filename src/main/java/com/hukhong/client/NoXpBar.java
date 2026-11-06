package com.hukhong.client;

import com.hukhong.HuKhong;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** 2026-11-06: ẩn thanh XP và số cấp XP vanilla (hệ XP vanilla đã bỏ). */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class NoXpBar {
    private NoXpBar() {}

    @SubscribeEvent
    public static void onLayer(RenderGuiLayerEvent.Pre e) {
        if (e.getName().equals(VanillaGuiLayers.EXPERIENCE_BAR) || e.getName().equals(VanillaGuiLayers.EXPERIENCE_LEVEL)) e.setCanceled(true);
    }
}
