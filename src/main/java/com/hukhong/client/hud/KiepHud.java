package com.hukhong.client.hud;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import com.hukhong.scaling.Difficulty;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Dòng "Kiếp Số" dưới HUD tu luyện: độ khó hiện tại + hệ số HP quái. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class KiepHud {
    private KiepHud() {}

    @SubscribeEvent
    public static void onRegister(RegisterGuiLayersEvent e) {
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "kiep_hud"), KiepHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker dt) {
        if (!ClientConfig.showHud()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        float k = Difficulty.clientKiep, m = Difficulty.clientMult;
        int col = m < 1.3f ? 0xFFE0C080 : m < 1.8f ? 0xFFFF9A40 : 0xFFFF5555;
        g.drawString(mc.font, Component.translatable("hukhong.hud.kiep", String.format("%.1f", k), String.format("%.2f", m)),
                6, 50, col, true); // cho dong Tu Tinh cu (da bo)
    }
}
