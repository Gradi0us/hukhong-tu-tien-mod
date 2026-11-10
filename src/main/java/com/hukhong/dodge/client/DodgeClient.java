package com.hukhong.dodge.client;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import com.hukhong.dodge.DodgeNet;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client Lách Né: phím, thanh Thể Lực (ẩn khi đầy), hiệu ứng Né Hoàn Hảo (FOV + phủ xám/vignette — không làm chậm tick thật). */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class DodgeClient {
    private DodgeClient() {}

    public static final KeyMapping DODGE = new KeyMapping("key.hukhong.dodge", InputConstants.KEY_LALT, "key.categories.hukhong");

    private static final int PERFECT_TICKS = 14, FADE_TICKS = 20;
    private static float cur = -1f, max = 100f;
    private static int perfectLeft, fullFor;

    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent e) { e.register(DODGE); }

    public static void onStamina(float c, float m) { cur = c; max = Math.max(1f, m); }

    public static void onPerfect() { perfectLeft = PERFECT_TICKS; }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        while (DODGE.consumeClick())
            if (mc.screen == null) PacketDistributor.sendToServer(new DodgeNet.Dodge(p.input.forwardImpulse, p.input.leftImpulse));
        // Cạn Thể Lực -> không giữ được chạy nhanh (server cũng tắt; client tắt để khỏi giật)
        if (cur >= 0 && cur < 0.5f && p.isSprinting() && !p.isCreative()) p.setSprinting(false);
        if (perfectLeft > 0) perfectLeft--;
        fullFor = (cur < 0 || cur >= max - 0.01f) ? fullFor + 1 : 0;
    }

    @SubscribeEvent
    public static void onLayers(RegisterGuiLayersEvent e) {
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "stamina_hud"), DodgeClient::renderHud);
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "perfect_dodge_fx"), DodgeClient::renderFx);
    }

    /** Thanh nhỏ dưới tâm ngắm; mờ dần rồi ẩn khi đầy. */
    private static void renderHud(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !ClientConfig.staminaHud() || cur < 0) return;
        if (fullFor >= FADE_TICKS) return;
        float alpha = fullFor == 0 ? 1f : 1f - fullFor / (float) FADE_TICKS;
        int a = (int) (alpha * 255) & 0xFF;
        int w = 50, h = 3;
        int x = g.guiWidth() / 2 - w / 2, y = g.guiHeight() / 2 + 14;
        double r = Math.max(0, Math.min(1, cur / max));
        int fill = (int) Math.round(w * r);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, (a * 2 / 3) << 24);
        int col = ClientConfig.colorblind() ? 0xF0E442 : r < 0.25 ? 0xE0A030 : 0x7FE07F;
        if (fill > 0) g.fill(x, y, x + fill, y + h, (a << 24) | col);
    }

    private static float fxPhase(float partial) {
        if (perfectLeft <= 0) return 0f;
        float t = 1f - (perfectLeft - partial) / PERFECT_TICKS; // 0 -> 1
        return (float) Math.sin(Math.PI * Math.max(0, Math.min(1, t)));
    }

    private static boolean fxOn() { return ClientConfig.dodgeFx() && !ClientConfig.screenEffectsOff(); }

    /** "Chậm thời gian" giả: phủ xám (giảm bão hòa) + vignette tối 4 cạnh. */
    private static void renderFx(GuiGraphics g, DeltaTracker dt) {
        if (!fxOn() || perfectLeft <= 0) return;
        float k = fxPhase(dt.getGameTimeDeltaPartialTick(false));
        if (k <= 0) return;
        int w = g.guiWidth(), h = g.guiHeight();
        g.fill(0, 0, w, h, ((int) (k * 70) << 24) | 0x8A8F99);
        int edge = (int) (k * 160) << 24;
        int bw = w / 5, bh = h / 5;
        g.fillGradient(0, 0, w, bh, edge, 0);
        g.fillGradient(0, h - bh, w, h, 0, edge);
        for (int i = 0; i < bw; i += 4) { // gradient ngang thủ công (fillGradient chỉ dọc)
            int al = (int) ((k * 160) * (1f - i / (float) bw)) << 24;
            g.fill(i, 0, i + 4, h, al);
            g.fill(w - i - 4, 0, w - i, h, al);
        }
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov e) {
        if (!fxOn() || perfectLeft <= 0) return;
        float k = fxPhase((float) e.getPartialTick());
        e.setFOV(e.getFOV() * (1.0 - 0.08 * k));
    }
}
