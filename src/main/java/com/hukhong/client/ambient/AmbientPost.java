package com.hukhong.client.ambient;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Chỉnh màu hậu kỳ nhẹ nhàng (giảm bão hòa, ám tím ở vùng tối, vignette) bằng PostChain riêng
 * (assets/hukhong/shaders/post/void_grade.json). Cường độ = trọng số đã làm mượt nên bật/tắt không giật. Lỗi → tự tắt, không crash.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class AmbientPost {
    private AmbientPost() {}

    private static final ResourceLocation CHAIN = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "shaders/post/void_grade.json");
    private static PostChain chain;
    private static int w, h;
    private static boolean failed;

    @SubscribeEvent
    public static void reload(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener((ResourceManagerReloadListener) rm -> invalidate());
    }

    /** Tự kiểm tra khi khởi động (-Dhukhong.ambient.selftest=1): nạp PostChain để bắt lỗi shader sớm. */
    static void selfTest() {
        Minecraft mc = Minecraft.getInstance();
        try {
            PostChain c = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), CHAIN);
            c.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
            c.setUniform("Strength", 0.5f);
            c.process(0f);
            mc.getMainRenderTarget().bindWrite(false);
            c.close();
            com.mojang.logging.LogUtils.getLogger().info("AMBIENT-SELFTEST post chain OK");
        } catch (Exception ex) {
            com.mojang.logging.LogUtils.getLogger().error("AMBIENT-SELFTEST post chain FAILED", ex);
        }
    }

    static void invalidate() {
        if (chain != null) { try { chain.close(); } catch (Exception ignored) { } }
        chain = null;
        failed = false;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || failed) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !ClientConfig.ambientPost() || !AmbientClient.active()) return;
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        float c = AmbientClient.corruption(pt), wr = AmbientClient.war(pt), d = AmbientClient.dust(pt);
        float bi = AmbientClient.inBiCanh() ? d * 0.3f : 0f;
        float s = Mth.clamp(Math.max(Math.max(c * 0.95f, wr * 0.85f), bi), 0f, 1f);
        if (s < 0.01f) return;
        try {
            int ww = mc.getWindow().getWidth(), hh = mc.getWindow().getHeight();
            if (chain == null) {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), CHAIN);
                chain.resize(ww, hh);
                w = ww; h = hh;
            } else if (ww != w || hh != h) {
                chain.resize(ww, hh);
                w = ww; h = hh;
            }
            float tot = Math.max(1e-4f, c + wr);
            float cw = c / tot, ww2 = wr / tot;
            float[] tint = {0.62f * cw + 1.0f * ww2, 0.38f * cw + 0.38f * ww2, 1.0f * cw + 0.55f * ww2};
            if (bi > 0f && c < 0.01f && wr < 0.01f) tint = AmbientSky.tierPalette(AmbientState.bicanhTier()).clone();
            float pulse = wr * (0.5f + 0.5f * Mth.sin((AmbientClient.ticks() + pt) * 0.09f));
            chain.setUniform("Strength", s);
            chain.setUniform("TintR", tint[0]);
            chain.setUniform("TintG", tint[1]);
            chain.setUniform("TintB", tint[2]);
            chain.setUniform("Pulse", pulse);
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.resetTextureMatrix();
            chain.process(pt);
            mc.getMainRenderTarget().bindWrite(false);
            RenderSystem.enableDepthTest();
        } catch (Exception ex) {
            failed = true;
            com.mojang.logging.LogUtils.getLogger().warn("Ambient post shader disabled: {}", ex.toString());
            chain = null;
        }
    }
}
