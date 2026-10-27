package com.hukhong.client;

import com.hukhong.HuKhong;
import com.hukhong.city.Prebuild;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Chu + thanh tien do tren man hinh tai the gioi trong luc server tich hop dung thanh/tong/cang truoc khi vao. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class PrebuildOverlay {
    private PrebuildOverlay() {}

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post e) {
        String text = Prebuild.overlayText;
        if (text == null) return;
        Screen sc = e.getScreen();
        GuiGraphics g = e.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        int w = sc.width, y = sc.height / 2 + 50;
        g.drawCenteredString(font, text, w / 2, y, 0xFFFFFF);
        int bw = 200, x0 = w / 2 - bw / 2;
        g.fill(x0 - 1, y + 14, x0 + bw + 1, y + 24, 0xFF000000);
        g.fill(x0, y + 15, x0 + (int) (bw * Prebuild.overlayFraction), y + 23, 0xFF55FF55);
    }
}
