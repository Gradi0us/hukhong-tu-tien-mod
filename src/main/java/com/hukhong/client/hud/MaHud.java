package com.hukhong.client.hud;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientCache;
import com.hukhong.client.ClientConfig;
import com.hukhong.core.Path;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * HUD Ma Tu: thanh dọc Ác Tính cạnh thanh Ô Uế Lực (vạch 80 / 95, rung khi gần ngưỡng) + lớp cảnh báo Tẩu Hỏa 3 tầng.
 * Hiệu ứng toàn màn hình (viền đỏ, tint, rung) tôn trọng ClientConfig.screenEffectsOff(); chữ đếm ngược luôn hiện (R26).
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class MaHud {
    private MaHud() {}

    private static final int BAR_X = 160, BAR_Y = 6, BAR_W = 6, BAR_H = 36;

    @SubscribeEvent
    public static void onRegister(RegisterGuiLayersEvent e) {
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "ac_tinh_bar"), MaHud::renderBar);
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "ma_warning"), MaHud::renderWarning);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) { ClientMaState.tick(); }

    private static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && !mc.options.hideGui && ClientCache.cultivation().paths().contains(Path.MA);
    }

    private static void renderBar(GuiGraphics g, DeltaTracker dt) {
        if (!ClientConfig.showHud() || !active()) return;
        Minecraft mc = Minecraft.getInstance();
        int x = BAR_X, y = BAR_Y, BAR_W = -6; // thanh Ác Tính đã chuyển xuống dưới thanh Linh Khí (LinhKhiHud)
        if (ClientMaState.huyetTe() > 0)
            g.drawString(mc.font, Component.translatable("hukhong.hud.huyet_te", ClientMaState.huyetTe()), x + BAR_W + 6, y, 0xFFCC4455, true);
        if (ClientMaState.lost())
            g.drawString(mc.font, Component.translatable("hukhong.ma.lost_control"), x + BAR_W + 6, y + 11, 0xFFFF5555, true);
    }

    private static void renderWarning(GuiGraphics g, DeltaTracker dt) {
        if (!active()) return;
        int tier = ClientMaState.tier();
        if (tier <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth(), h = g.guiHeight();
        float t = mc.player.tickCount + dt.getGameTimeDeltaPartialTick(false);
        if (!ClientConfig.screenEffectsOff()) {
            if (tier == 3) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(t * 0.5f);
                g.fill(0, 0, w, h, ((int) (70 + 60 * pulse) << 24) | 0x300000); // đỏ đen toàn màn
            }
            int a = tier == 1 ? 0x38 : tier == 2 ? 0x70 : 0xA0;
            int edge = Math.max(w, h) / (tier == 1 ? 10 : 6);
            int c = (a << 24) | 0xB00000, clear = 0x00B00000;
            g.fillGradient(0, 0, w, edge, c, clear);
            g.fillGradient(0, h - edge, w, h, clear, c);
            // viền trái/phải: dải mỏng
            int s = edge / 2;
            for (int i = 0; i < s; i += 2) {
                int al = (int) (a * (1f - (float) i / s));
                g.fill(i, edge, i + 2, h - edge, (al << 24) | 0xB00000);
                g.fill(w - i - 2, edge, w - i, h - edge, (al << 24) | 0xB00000);
            }
        }
        if (tier == 3 && ClientMaState.countdown() >= 0) { // đếm ngược luôn hiện
            String n = String.valueOf((ClientMaState.countdown() + 19) / 20);
            g.pose().pushPose();
            g.pose().translate(w / 2f, h / 2f - 30, 0);
            g.pose().scale(4f, 4f, 1f);
            g.drawCenteredString(mc.font, n, 0, 0, 0xFFFF3030);
            g.pose().popPose();
            g.drawCenteredString(mc.font, Component.translatable("hukhong.ma.warn3.bar", n), w / 2, h / 2 + 5, 0xFFFFAAAA);
        } else if (tier == 2) {
            g.drawCenteredString(mc.font, Component.translatable("hukhong.ma.warn2.title"), w / 2, h / 4, 0xFFFF6060);
        }
    }
}
