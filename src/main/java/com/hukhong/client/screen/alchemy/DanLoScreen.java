package com.hukhong.client.screen.alchemy;

import com.hukhong.HuKhong;
import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.alchemy.DanLoMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.lwjgl.glfw.GLFW;

/** Màn Đan Lô: thanh nhiệt dọc + dải mục tiêu; W/Up tăng lửa, S/Down giảm lửa. */
public class DanLoScreen extends AbstractContainerScreen<DanLoMenu> {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/gui/dan_lo.png");
    private static final int GX = 87, GY = 15, GW = 10, GH = 48;

    public DanLoScreen(DanLoMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageHeight = 166;
        inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("hukhong.alchemy.start"), b -> press(0))
                .bounds(leftPos + 112, topPos + 48, 56, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> press(1))
                .bounds(leftPos + 103, topPos + 14, 14, 14).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> press(2))
                .bounds(leftPos + 103, topPos + 50, 14, 14).build());
    }

    private void press(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP) { press(1); return true; }
        if (key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_DOWN) { press(2); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int heat = menu.data.get(0), lo = menu.data.get(3), hi = menu.data.get(4);
        boolean brewing = menu.data.get(2) == 1;
        int x = leftPos + GX, y = topPos + GY;
        g.fill(x, y, x + GW, y + GH, 0xFF1A1420);
        // dải mục tiêu
        int yHi = y + GH - hi * GH / 100, yLo = y + GH - lo * GH / 100;
        g.fill(x, yHi, x + GW, yLo, 0xFF2E7D32);
        // mức nhiệt
        int yh = y + GH - Math.min(100, heat) * GH / 100;
        boolean ok = heat >= lo && heat <= hi;
        g.fill(x + 1, yh, x + GW - 1, y + GH, brewing ? (ok ? 0xFFFFC107 : 0xFFE64A19) : 0xFF6D4C41);
        g.fill(x - 2, yh, x + GW + 2, yh + 1, 0xFFFFFFFF);
        // tiến độ
        int total = Math.max(1, menu.data.get(6));
        int pw = 40 * menu.data.get(1) / total;
        g.fill(leftPos + 112, topPos + 30, leftPos + 152, topPos + 36, 0xFF333333);
        g.fill(leftPos + 112, topPos + 30, leftPos + 112 + pw, topPos + 36, 0xFF42A5F5);
        g.drawString(font, Component.translatable("hukhong.alchemy.band", menu.data.get(7) * 100 / total), leftPos + 112, topPos + 66, 0xFFFFFF, false);
        g.drawString(font, Component.translatable(menu.data.get(5) > 0 ? "hukhong.alchemy.burning" : "hukhong.alchemy.cold"),
                leftPos + 26, topPos + 48, menu.data.get(5) > 0 ? 0xFFB74D : 0x9E9E9E, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }

    @EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
    public static final class Reg {
        private Reg() {}

        @SubscribeEvent
        public static void onRegister(RegisterMenuScreensEvent e) {
            e.register(AlchemyRegistry.DAN_LO_MENU.get(), DanLoScreen::new);
        }
    }
}
