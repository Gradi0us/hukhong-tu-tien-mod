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
        if (com.hukhong.client.fx.ThoClient.titanControl()) return "tho_titan_" + id.substring(5); // kỹ năng trụ cấp 3
        var mc = Minecraft.getInstance();
        PhapElement e = mc.player == null ? null : PhapTu.rawElement(mc.player);
        return e == null ? id : "phap_" + e.id() + "_" + id.substring(5);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(PhapTuRegistry.TURRET.get(), TurretRenderer::new);
        e.registerEntityRenderer(PhapTuRegistry.KINH.get(), QuangKinhRenderer::new);
    }

    /** Model GeckoLib của Thổ Trụ: cấp 1 trụ đơn, cấp 2 Hợp Thể, cấp 3 Thái Sơn Thần Tháp (lõi đỏ khi có người điều khiển).
     *  Pha lê xoay + nhấp nhô, vòng đá bay quay (Hợp Thể: nhanh dần theo độ nạp laser). Model sinh bởi tools/gen_tho_tru.py. */
    static final class TurretModel extends software.bernie.geckolib.model.GeoModel<PhapTurret> {
        private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, p); }
        private static final ResourceLocation[] GEO = {rl("geo/entity/tho_tru_1.geo.json"), rl("geo/entity/tho_tru_2.geo.json"), rl("geo/entity/tho_tru_3.geo.json")};
        private static final ResourceLocation[] TEX = {rl("textures/entity/tho_tru_1.png"), rl("textures/entity/tho_tru_2.png"), rl("textures/entity/tho_tru_3.png")};
        private static final ResourceLocation TEX_CTL = rl("textures/entity/tho_tru_3_ctl.png"), ANIM = rl("animations/entity/tho_tru.animation.json");

        static int tier(PhapTurret t) { return t.isTitan() ? 2 : t.isMerged() ? 1 : 0; }
        @Override public ResourceLocation getModelResource(PhapTurret t) { return GEO[tier(t)]; }
        @Override public ResourceLocation getTextureResource(PhapTurret t) { return tier(t) == 2 && t.isControlled() ? TEX_CTL : TEX[tier(t)]; }
        @Override public ResourceLocation getAnimationResource(PhapTurret t) { return ANIM; }

        @Override
        public void setCustomAnimations(PhapTurret t, long id, software.bernie.geckolib.animation.AnimationState<PhapTurret> st) {
            float age = t.tickCount + st.getPartialTick();
            int tier = tier(t);
            boolean ctl = tier == 2 && t.isControlled();
            float ramp = tier == 1 ? t.rampFraction() : 0f;
            float spin = (tier == 2 ? (ctl ? 0.14f : 0.05f) : 0.07f) + 0.35f * ramp;
            getBone("crystal").ifPresent(b -> {
                b.setRotY(age * spin);
                b.setPosY((float) Math.sin(age * 0.1) * (tier == 2 ? 2.5f : 1.2f));
                float k = 1f + 0.3f * ramp + (ramp > 0 ? (float) Math.sin(age * (0.3 + ramp)) * 0.04f * (1 + ramp * 3) : 0f);
                b.setScaleX(k); b.setScaleY(k); b.setScaleZ(k);
            });
            float orb = (tier == 2 ? (ctl ? 0.11f : 0.04f) : 0.05f) + 0.12f * ramp;
            getBone("orbit").ifPresent(b -> { b.setRotY(age * orb); b.setPosY((float) Math.sin(age * 0.07) * 1.2f); });
            getBone("orbit2").ifPresent(b -> b.setRotY(-age * orb * 0.8f));
            getBone("halo").ifPresent(b -> b.setRotY(-age * 0.03f));
            var mc = Minecraft.getInstance();
            boolean pilot = ctl && com.hukhong.client.fx.ThoClient.titanControl() && mc.player != null && mc.player.distanceToSqr(t) < 14 * 14;
            getBone("upper").ifPresent(b -> b.setHidden(pilot)); // người đang lái: ẩn lõi + đá bay để không che tầm nhìn
        }
    }

    /** Renderer Thổ Trụ: model GeckoLib + lớp phát sáng; trồi lên từ đất 14 tick đầu (và khi Hợp Thể); khiên hổ phách khi có máu ảo.
     *  Gai Địa Thứ (isSpike) vẫn ghép từ block vanilla. */
    static final class TurretRenderer extends software.bernie.geckolib.renderer.GeoEntityRenderer<PhapTurret> {
        TurretRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new TurretModel());
            addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<>(this));
            shadowRadius = 0.8f;
        }

        private static net.minecraft.world.level.block.state.BlockState bs(net.minecraft.world.level.block.Block b) { return b.defaultBlockState(); }
        private static final net.minecraft.world.level.block.state.BlockState
                SPIKE = bs(net.minecraft.world.level.block.Blocks.POINTED_DRIPSTONE), AMBER = bs(net.minecraft.world.level.block.Blocks.SHROOMLIGHT),
                GILD = bs(net.minecraft.world.level.block.Blocks.GILDED_BLACKSTONE), GLASS = bs(net.minecraft.world.level.block.Blocks.YELLOW_STAINED_GLASS),
                DRIP = bs(net.minecraft.world.level.block.Blocks.DRIPSTONE_BLOCK);

        private void block(PoseStack ps, MultiBufferSource buf, net.minecraft.world.level.block.state.BlockState st, double x, double y, double z, float s, float rot, int light) {
            ps.pushPose();
            ps.translate(x, y, z);
            ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rot));
            ps.scale(s, s, s);
            ps.translate(-0.5, 0, -0.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(st, ps, buf, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }

        @Override protected float getDeathMaxRotation(PhapTurret t) { return 0f; }

        @Override
        public void render(PhapTurret t, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            float age = t.tickCount + pt;
            float rise = Math.min(1f, (age - t.riseStart) / 14f);
            float ease = 1 - (1 - rise) * (1 - rise) * (1 - rise);
            int glow = LightTexture.pack(15, 15);
            int tier = TurretModel.tier(t);
            shadowRadius = tier == 2 ? 3.2f : tier == 1 ? 1.4f : 0.8f;
            ps.pushPose();
            ps.translate(0, -(tier == 2 ? 6.0 : tier == 1 ? 3.6 : 2.6) * (1 - ease), 0);
            if (t.isSpike()) spike(age, ps, buf, light, glow);
            else super.render(t, yaw, pt, ps, buf, light);
            float sh = t.shieldFraction();
            boolean pilotView = t.isTitan() && t.isControlled() && com.hukhong.client.fx.ThoClient.titanControl();
            if (sh > 0 && !pilotView) { // máu ảo + giáp ảo: vỏ kính hổ phách bao trụ, co lại theo lượng máu ảo
                float k = t.isTitan() ? (float) com.hukhong.phaptu.PhapMath.THO_TITAN_SCALE : t.isMerged() ? (float) com.hukhong.phaptu.PhapMath.THO_MERGED_SCALE : 1f;
                float s = (1.55f + 0.35f * sh) * k + (float) Math.sin(age * 0.25) * 0.04f;
                block(ps, buf, GLASS, 0, 0.05, 0, s, age * 2f, glow);
                for (int i = 0; i < 4; i++) {
                    double a2 = age * 0.06 + Math.PI / 2 * i;
                    block(ps, buf, GILD, Math.cos(a2) * 1.0 * k, 0.9 * k + Math.sin(age * 0.1 + i) * 0.1, Math.sin(a2) * 1.0 * k, 0.32f * k, (float) Math.toDegrees(-a2), glow);
                }
            }
            ps.popPose();
        }

        /** Gai Địa Thứ Hộ Tháp: cụm 5 nhũ đá chĩa lên, chân đá vụn, mũi hổ phách sáng. */
        private void spike(float age, PoseStack ps, MultiBufferSource buf, int light, int glow) {
            block(ps, buf, DRIP, 0, 0, 0, 0.9f, 20, light);
            float[][] o = {{0, 0, 1.6f}, {0.38f, 0.1f, 1.1f}, {-0.35f, 0.25f, 1.0f}, {0.1f, -0.4f, 0.9f}, {-0.2f, -0.3f, 0.8f}};
            for (float[] q : o) {
                ps.pushPose();
                ps.translate(q[0], 0.6, q[1]);
                ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(q[1] * 30));
                ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-q[0] * 30));
                ps.scale(q[2], q[2] * 1.6f, q[2]);
                block(ps, buf, SPIKE, 0, 0, 0, 1f, 0, light);
                ps.popPose();
            }
            block(ps, buf, AMBER, 0, 2.3 + Math.sin(age * 0.2) * 0.05, 0, 0.22f, age * 6, glow);
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
