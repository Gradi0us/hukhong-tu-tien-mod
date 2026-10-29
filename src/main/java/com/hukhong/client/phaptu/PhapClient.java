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

    /** Tháp Thổ: trụ đá 3 tầng (đế gạch đá, thân chạm khắc, lõi hổ phách xoay) nhấp nhô nhẹ; lõi sáng khi vừa bắn. */
    static final class TurretRenderer extends EntityRenderer<PhapTurret> {
        private static final net.minecraft.world.level.block.state.BlockState BASE = net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState();
        private static final net.minecraft.world.level.block.state.BlockState MID = net.minecraft.world.level.block.Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        private static final net.minecraft.world.level.block.state.BlockState CORE = net.minecraft.world.level.block.Blocks.RAW_GOLD_BLOCK.defaultBlockState();
        private static final net.minecraft.world.level.block.state.BlockState SPIKE = net.minecraft.world.level.block.Blocks.POINTED_DRIPSTONE.defaultBlockState();

        TurretRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
            shadowRadius = 0.6f;
        }

        private void block(PoseStack ps, MultiBufferSource buf, net.minecraft.world.level.block.state.BlockState st, double x, double y, double z, float s, float rot, int light) {
            ps.pushPose();
            ps.translate(x, y, z);
            ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rot));
            ps.scale(s, s, s);
            ps.translate(-0.5, 0, -0.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(st, ps, buf, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }

        @Override
        public void render(PhapTurret t, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            float age = t.tickCount + pt;
            ps.pushPose();
            block(ps, buf, BASE, 0, 0, 0, 1.0f, 0, light);
            block(ps, buf, MID, 0, 0.95, 0, 0.72f, 45, light);
            for (int i = 0; i < 4; i++) { // 4 gai đá quanh chân trụ
                double a = Math.PI / 2 * i + 0.4;
                block(ps, buf, SPIKE, Math.cos(a) * 0.62, 0, Math.sin(a) * 0.62, 0.7f, 0, light);
            }
            double bob = Math.sin(age * 0.1) * 0.06;
            block(ps, buf, CORE, 0, 1.55 + bob, 0, 0.42f, age * 5f, LightTexture.pack(15, 15));
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
