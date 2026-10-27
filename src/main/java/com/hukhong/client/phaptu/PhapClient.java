package com.hukhong.client.phaptu;

import com.hukhong.HuKhong;
import com.hukhong.artifact.ArtifactAttachments;
import com.hukhong.client.ClientConfig;
import com.hukhong.phaptu.PhapElement;
import com.hukhong.phaptu.PhapTu;
import com.hukhong.phaptu.PhapTuRegistry;
import com.hukhong.phaptu.PhapTurret;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Vector3f;

/** Phía client của Pháp Tu: icon kỹ năng theo nguyên tố, renderer Tháp Thổ, hào quang Pháp Khí. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class PhapClient {
    private PhapClient() {}

    /** Icon HUD: "phap_1" -> "phap_quang_1" theo nguyên tố của người chơi; id khác giữ nguyên. */
    public static String icon(String id) {
        if (id == null || !id.startsWith("phap_")) return id;
        var mc = Minecraft.getInstance();
        PhapElement e = mc.player == null ? null : PhapTu.rawElement(mc.player);
        return e == null ? id : "phap_" + e.id() + "_" + id.substring(5);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(PhapTuRegistry.TURRET.get(), TurretRenderer::new);
    }

    /** Tháp Thổ: khối "bệ phóng" xoay chậm, bay nhẹ, phát sáng đủ cho dễ thấy. */
    static final class TurretRenderer extends EntityRenderer<PhapTurret> {
        private final ItemStack stack = new ItemStack(Items.DISPENSER);

        TurretRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
            shadowRadius = 0.5f;
        }

        @Override
        public void render(PhapTurret t, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            ps.pushPose();
            float age = t.tickCount + pt;
            ps.translate(0, 0.75 + Math.sin(age * 0.1) * 0.04, 0);
            ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(age * 4f));
            ps.scale(1.5f, 1.5f, 1.5f);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                    LightTexture.pack(12, 12), OverlayTexture.NO_OVERLAY, ps, buf, t.level(), t.getId());
            ps.popPose();
            super.render(t, yaw, pt, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(PhapTurret t) {
            return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
        }
    }

    /** Pháp Khí: vòng hạt nguyên tố quanh người, nhiều hơn theo giai đoạn. */
    @SubscribeEvent
    public static void aura(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused() || ClientConfig.particles() == ClientConfig.ParticleLevel.OFF) return;
        if (level.getGameTime() % (ClientConfig.particles() == ClientConfig.ParticleLevel.LOW ? 6 : 3) != 0) return;
        var cam = mc.gameRenderer.getMainCamera().getPosition();
        RandomSource r = level.random;
        for (AbstractClientPlayer p : level.players()) {
            if (p.isInvisible() || p.isSpectator() || p.distanceToSqr(cam) > 32 * 32) continue;
            if (p == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            int stage = p.getData(ArtifactAttachments.ARTIFACT.get()).phap();
            PhapElement el = PhapTu.rawElement(p);
            if (stage <= 0 || el == null) continue;
            var dust = new DustParticleOptions(new Vector3f(((el.rgb >> 16) & 255) / 255f, ((el.rgb >> 8) & 255) / 255f, (el.rgb & 255) / 255f), 0.9f);
            int n = 1 + stage / 2;
            double base = level.getGameTime() * 0.12;
            for (int i = 0; i < n; i++) {
                double a = base + i * (Math.PI * 2 / n), rad = 0.7 + 0.05 * stage;
                level.addParticle(dust, p.getX() + Math.cos(a) * rad, p.getY() + 0.9 + Math.sin(base * 0.7 + i) * 0.5,
                        p.getZ() + Math.sin(a) * rad, 0, 0.01, 0);
            }
        }
    }
}
