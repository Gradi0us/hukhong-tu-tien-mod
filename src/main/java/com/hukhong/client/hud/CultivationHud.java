package com.hukhong.client.hud;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientCache;
import com.hukhong.client.ClientConfig;
import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import com.hukhong.core.Path;
import com.hukhong.core.Realm;
import com.hukhong.currency.TuTinhText;
import com.hukhong.currency.Wallet;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * HUD tu luyện. Texture ở textures/gui/hud/ (agent khác vẽ); thiếu file -> fill màu fallback.
 * Giả định kích thước texture thanh: 100x6 px. frame.png chưa dùng (chưa rõ kích thước).
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class CultivationHud {
    private CultivationHud() {}

    static final int BAR_W = 100, BAR_H = 6;
    private static final Map<ResourceLocation, Boolean> EXISTS = new HashMap<>();

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/gui/hud/" + name + ".png");
    }

    private static boolean exists(ResourceLocation rl) {
        return EXISTS.computeIfAbsent(rl, k -> Minecraft.getInstance().getResourceManager().getResource(k).isPresent());
    }

    @SubscribeEvent
    public static void onRegister(RegisterGuiLayersEvent e) {
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "cultivation_hud"),
                CultivationHud::render);
    }

    private static void bar(GuiGraphics g, int x, int y, double ratio, String bg, String fill, int fallbackArgb) {
        int w = (int) Math.round(BAR_W * Math.max(0, Math.min(1, ratio)));
        ResourceLocation bgT = tex(bg), fillT = tex(fill);
        if (exists(bgT)) g.blit(bgT, x, y, 0, 0, BAR_W, BAR_H, BAR_W, BAR_H);
        else g.fill(x, y, x + BAR_W, y + BAR_H, 0xAA000000);
        if (w <= 0) return;
        if (exists(fillT) && !ClientConfig.colorblind()) g.blit(fillT, x, y, 0, 0, w, BAR_H, BAR_W, BAR_H);
        else g.fill(x, y, x + w, y + BAR_H, fallbackArgb);
    }

    private static void render(GuiGraphics g, DeltaTracker dt) {
        if (!ClientConfig.showHud()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        CultivationData d = ClientCache.cultivation();
        Font font = mc.font;
        int x = 6, y = 6;

        String realm = Component.translatable(Realm.langKey(d.level())).getString();
        int sub = Realm.subStage(d.level());
        if (sub > 0) realm += " " + roman(sub);
        g.drawString(font, realm + " - " + Component.translatable("hukhong.hud.level", d.level()).getString(),
                x, y, 0xFFFFFFFF, true);
        y += 11;

        boolean full = d.level() >= CultivationData.MAX_LEVEL || d.milestoneLocked();
        double expRatio = full ? 1.0 : (double) d.exp() / CultivationService.barSize(d.level());
        if (d.inBottleneck() && !ClientConfig.colorblind()) g.setColor(1f, 0.35f, 0.15f, 1f); // thanh Chướng
        bar(g, x, y, expRatio, "exp_bar_bg", "exp_bar_fill", full ? 0xFFAAAAAA : d.inBottleneck() ? (ClientConfig.colorblind() ? 0xFFD55E00 : 0xFFD2501E) : (ClientConfig.colorblind() ? 0xFF009E73 : 0xFF7CFC00));
        g.setColor(1f, 1f, 1f, 1f);
        String expTxt = full ? "MAX" : d.exp() + "/" + CultivationService.barSize(d.level());
        g.drawString(font, expTxt, x + BAR_W + 4, y - 1, 0xFFDDDDDD, true);
        y += BAR_H + 3;

        Path path = d.primaryPath();
        int color = path == null ? 0xFF6FA8DC : ClientConfig.colorblind() ? colorblindPath(path) : path.argb;
        bar(g, x, y, (double) d.resource() / d.resourceMax(), "resource_bar_bg", "resource_bar_fill", color);
        g.drawString(font, d.resource() + "/" + d.resourceMax(), x + BAR_W + 4, y - 1, color, true);
        y += BAR_H + 4;

        int lifeHud = ClientCache.age().lifespan(d.realmIndex());
        int leftHud = lifeHud - ClientCache.age().ageYears();
        boolean showLife = d.level() >= 10 && lifeHud != com.hukhong.age.AgeMath.UNLIMITED;
        int ageCol = !showLife ? 0xFFE0C080 : leftHud <= 3 ? 0xFFFF5555 : leftHud <= 10 ? 0xFFFFD040 : 0xFFE0C080;
        g.drawString(font, showLife ? Component.translatable("hukhong.hud.age.life", ClientCache.age().ageYears(), lifeHud)
                : Component.translatable("hukhong.hud.age", ClientCache.age().ageYears()),
                x, y, ageCol, true);
        if (d.inBottleneck()) {
            g.drawString(font, Component.translatable("hukhong.hud.bottleneck"), x + 60, y, 0xFFFF5555, true);
        }
        // Dong tien (vi Tu Tinh) ngay duoi HUD tu luyen
        g.drawString(font, Component.translatable("hukhong.hud.tutinh", TuTinhText.of(Wallet.count(mc.player))),
                x, y + 11, 0xFFE0C080, true);
    }

    private static int colorblindPath(Path p) {
        return switch (p) { case KIEM -> 0xFF56B4E9; case DAO -> 0xFFF0E442; case THE -> 0xFFE69F00; case MA -> 0xFFCC79A7; case PHAP -> 0xFF009E73; };
    }

    private static String roman(int n) {
        return switch (n) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; default -> "V"; };
    }
}
