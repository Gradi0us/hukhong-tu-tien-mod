package com.hukhong.client.voidfx;

import com.hukhong.HuKhong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

/** Thanh máu boss mang khung Hư Không cho thanh có tên khóa "hukhong.*" (các thanh khác giữ vanilla). */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class VoidBossBar {
    private VoidBossBar() {}

    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/gui/void_bossbar.png");

    @SubscribeEvent
    public static void onBar(CustomizeGuiOverlayEvent.BossEventProgress e) {
        Component name = e.getBossEvent().getName();
        if (!(name.getContents() instanceof TranslatableContents tc) || !tc.getKey().startsWith("hukhong.")) return;
        GuiGraphics g = e.getGuiGraphics();
        int x = e.getX(), y = e.getY();
        g.blit(TEX, x, y, 0, 0, 182, 5, 182, 10);
        int w = (int) (e.getBossEvent().getProgress() * 183.0F);
        if (w > 0) g.blit(TEX, x, y, 0, 5, w, 5, 182, 10);
        g.drawCenteredString(Minecraft.getInstance().font, name, g.guiWidth() / 2, y - 9, 0xD9C8FF);
        e.setIncrement(19);
        e.setCanceled(true);
    }
}
