package com.hukhong.client.hud;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientCache;
import com.hukhong.client.ClientConfig;
import com.hukhong.core.CultivationData;
import com.hukhong.core.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 2026-11-06: thanh thức ăn vanilla được thay bằng thanh Linh Khí (tài nguyên tu luyện / mana) ngay bên phải trên thanh công cụ.
 * Ma Tu: Ác Tính vẽ thành dải mỏng ngay dưới (vạch 80 / 95).
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class LinhKhiHud {
    private LinhKhiHud() {}

    public static final int W = 81, H = 9;

    @SubscribeEvent
    public static void onRegister(RegisterGuiLayersEvent e) {
        e.wrapLayer(VanillaGuiLayers.FOOD_LEVEL, orig -> (g, dt) -> render(g));
    }

    private static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !mc.gameMode.canHurtPlayer()) return;
        CultivationData d = ClientCache.cultivation();
        Font font = mc.font;
        int x = g.guiWidth() / 2 + 10, y = g.guiHeight() - 39;
        boolean ma = d.paths().contains(Path.MA);
        int manaH = ma ? 6 : H;

        Path path = d.primaryPath();
        int color = path == null ? 0xFF6FA8DC : path.argb | 0xFF000000;
        double r = d.resourceMax() <= 0 ? 0 : Math.max(0, Math.min(1, (double) d.resource() / d.resourceMax()));
        g.fill(x - 1, y - 1, x + W + 1, y + H + 1, 0xFF000000);
        g.fill(x, y, x + W, y + manaH, 0xFF1A1A2A);
        g.fill(x, y, x + (int) Math.round(W * r), y + manaH, color);
        g.fill(x, y, x + (int) Math.round(W * r), y + 1, 0x55FFFFFF); // ánh sáng mép trên
        String t = d.resource() + "/" + d.resourceMax();
        g.pose().pushPose();
        g.pose().translate(x + W / 2f, y + 1, 0);
        g.pose().scale(0.6f, 0.6f, 1f);
        g.drawCenteredString(font, t, 0, 0, 0xFFFFFFFF);
        g.pose().popPose();

        if (ma) {
            float ac = Math.max(0f, Math.min(100f, ClientMaState.acTinh()));
            int ay = y + manaH + 1, ah = H - manaH - 1;
            boolean cb = ClientConfig.colorblind();
            int ac0 = ac >= 95 ? (cb ? 0xFFD55E00 : 0xFFFF2020) : ac >= 80 ? (cb ? 0xFFE69F00 : 0xFFE0502A)
                    : ac >= 30 ? (cb ? 0xFFCC79A7 : 0xFFB03060) : (cb ? 0xFF56B4E9 : 0xFF8A4FB0);
            g.fill(x, ay, x + W, ay + ah, 0xFF221018);
            g.fill(x, ay, x + Math.round(W * ac / 100f), ay + ah, ac0);
            for (int th : new int[]{80, 95}) {
                int tx = x + Math.round(W * th / 100f);
                g.fill(tx, ay - 1, tx + 1, ay + ah + 1, th == 80 ? 0xFFFFD34F : 0xFFFFFFFF);
            }
            g.drawString(font, Component.translatable("hukhong.hud.ac_tinh").getString() + " " + Math.round(ac), x + W + 4, y, ac0, true);
        }
    }
}
