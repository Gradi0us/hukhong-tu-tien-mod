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

        private static net.minecraft.world.level.block.state.BlockState bs(net.minecraft.world.level.block.Block b) { return b.defaultBlockState(); }
        private static final net.minecraft.world.level.block.state.BlockState
                DS_BRICK = bs(net.minecraft.world.level.block.Blocks.DEEPSLATE_BRICKS), DS_TILE = bs(net.minecraft.world.level.block.Blocks.DEEPSLATE_TILES),
                BASALT = bs(net.minecraft.world.level.block.Blocks.POLISHED_BASALT), CH_DS = bs(net.minecraft.world.level.block.Blocks.CHISELED_DEEPSLATE),
                GILD = bs(net.minecraft.world.level.block.Blocks.GILDED_BLACKSTONE), AMBER = bs(net.minecraft.world.level.block.Blocks.SHROOMLIGHT),
                GOLD = bs(net.minecraft.world.level.block.Blocks.GOLD_BLOCK), TUFF = bs(net.minecraft.world.level.block.Blocks.CHISELED_TUFF_BRICKS),
                CRYING = bs(net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN), LANTERN = bs(net.minecraft.world.level.block.Blocks.OCHRE_FROGLIGHT),
                GLASS = bs(net.minecraft.world.level.block.Blocks.YELLOW_STAINED_GLASS);

        @Override
        public void render(PhapTurret t, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            float age = t.tickCount + pt;
            // trồi lên từ lòng đất trong 14 tick đầu (và lại trồi khi Hợp Thể)
            float rise = Math.min(1f, (age - t.riseStart) / 14f);
            float ease = 1 - (1 - rise) * (1 - rise) * (1 - rise);
            int glow = LightTexture.pack(15, 15);
            ps.pushPose();
            ps.translate(0, -2.4 * (1 - ease), 0);
            if (t.isSpike()) spike(t, age, ps, buf, light, glow);
            else if (t.isTitan()) titan(t, age, ps, buf, light, glow);
            else if (t.isMerged()) merged(t, age, ps, buf, light, glow);
            else single(t, age, ps, buf, light, glow);
            float sh = t.shieldFraction();
            boolean pilotView = t.isTitan() && t.isControlled() && com.hukhong.client.fx.ThoClient.titanControl();
            if (sh > 0 && !pilotView) { // máu ảo + giáp ảo: vỏ kính hổ phách bao trụ, co lại theo lượng máu ảo
                float k = t.isTitan() ? (float) com.hukhong.phaptu.PhapMath.THO_TITAN_SCALE : t.isMerged() ? (float) com.hukhong.phaptu.PhapMath.THO_MERGED_SCALE : 1f;
                float s = (1.55f + 0.35f * sh) * k + (float) Math.sin(age * 0.25) * 0.04f;
                block(ps, buf, GLASS, 0, 0.05, 0, s, age * 2f, glow);
                for (int i = 0; i < 4; i++) { // 4 phiến giáp xoay quanh
                    double a2 = age * 0.06 + Math.PI / 2 * i;
                    block(ps, buf, GILD, Math.cos(a2) * 1.0 * k, 0.9 * k + Math.sin(age * 0.1 + i) * 0.1, Math.sin(a2) * 1.0 * k, 0.32f * k, (float) Math.toDegrees(-a2), glow);
                }
            }
            ps.popPose();
            super.render(t, yaw, pt, ps, buf, light);
        }

        /** Trụ đơn (bắn từng phát): tháp obelisk đá sâu, 4 gai, lõi hổ phách lơ lửng với 3 mảnh đá quay quanh. */
        private void single(PhapTurret t, float age, PoseStack ps, MultiBufferSource buf, int light, int glow) {
            block(ps, buf, DS_TILE, 0, 0, 0, 1.15f, 0, light);
            block(ps, buf, CH_DS, 0, 0.9, 0, 0.82f, 45, light);
            block(ps, buf, BASALT, 0, 1.55, 0, 0.55f, 0, light);
            for (int i = 0; i < 4; i++) { // gai đá nghiêng chĩa ra 4 góc
                double a = Math.PI / 2 * i + Math.PI / 4;
                ps.pushPose();
                ps.translate(Math.cos(a) * 0.72, 0, Math.sin(a) * 0.72);
                ps.mulPose(com.mojang.math.Axis.YP.rotation((float) -a));
                ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-18));
                block(ps, buf, SPIKE, 0, 0, 0, 0.75f, 0, light);
                ps.popPose();
            }
            double bob = Math.sin(age * 0.12) * 0.08;
            float spin = age * 4f;
            block(ps, buf, AMBER, 0, 2.25 + bob, 0, 0.46f, spin, glow);
            block(ps, buf, GOLD, 0, 2.30 + bob, 0, 0.3f, -spin * 1.6f, glow);
            for (int i = 0; i < 3; i++) {
                double a = age * 0.09 + Math.PI * 2 / 3 * i;
                block(ps, buf, DS_BRICK, Math.cos(a) * 0.75, 2.2 + bob + Math.sin(age * 0.15 + i) * 0.15, Math.sin(a) * 0.75, 0.22f, spin, light);
            }
        }

        private static final net.minecraft.world.level.block.state.BlockState
                MAGMA = bs(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK), OBSID = bs(net.minecraft.world.level.block.Blocks.OBSIDIAN),
                POL_BS = bs(net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE_BRICKS), LODE = bs(net.minecraft.world.level.block.Blocks.LODESTONE),
                RAW_GOLD = bs(net.minecraft.world.level.block.Blocks.RAW_GOLD_BLOCK), PEARL = bs(net.minecraft.world.level.block.Blocks.PEARLESCENT_FROGLIGHT);

        /** Trụ cấp 3 - Thái Sơn Thần Tháp: pháo đài đá khổng lồ ~6 khối: bệ 3 tầng, 4 tháp canh có dung nham, thân đài, đỉnh có ghế điều khiển,
         *  lõi núi lửa xoay + 12 tảng đá bay vòng quanh. Có người nhập: lõi chuyển đỏ rực, vòng đá quay nhanh. */
        private void titan(PhapTurret t, float age, PoseStack ps, MultiBufferSource buf, int light, int glow) {
            boolean ctl = t.isControlled();
            var mc = Minecraft.getInstance();
            boolean pilot = ctl && com.hukhong.client.fx.ThoClient.titanControl() && mc.player != null
                    && mc.player.distanceToSqr(t) < 14 * 14; // người đang nhập trụ: ẩn phần trên để không che tầm nhìn
            // bệ 3 tầng
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) block(ps, buf, (x + z) % 2 == 0 ? DS_TILE : TUFF, x * 1.4, 0, z * 1.4, 1.4f, 0, light);
            block(ps, buf, POL_BS, 0, 1.3, 0, 3.0f, 0, light);
            block(ps, buf, CH_DS, 0, 4.2, 0, 2.0f, 45, light);
            // 4 tháp canh góc: đá đen + mạch dung nham + đèn đỉnh
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 2 * i + Math.PI / 4, x = Math.cos(a) * 2.6, z = Math.sin(a) * 2.6;
                block(ps, buf, BASALT, x, 0, z, 1.0f, 0, light);
                block(ps, buf, MAGMA, x, 1.0, z, 0.9f, 0, glow);
                block(ps, buf, BASALT, x, 1.9, z, 0.9f, 0, light);
                block(ps, buf, GILD, x, 2.8, z, 0.85f, 45, light);
                block(ps, buf, OBSID, x, 3.6, z, 0.5f, 45, light);
                block(ps, buf, ctl ? MAGMA : LANTERN, x, 4.15 + Math.sin(age * 0.15 + i) * 0.08, z, 0.42f, age * 4f, glow);
                for (int j = 0; j < 2; j++) { // gai đá chĩa ra ngoài
                    ps.pushPose();
                    ps.translate(x * 1.18, 0.2 + j * 1.4, z * 1.18);
                    ps.mulPose(com.mojang.math.Axis.YP.rotation((float) -a));
                    ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-35));
                    block(ps, buf, SPIKE, 0, 0, 0, 1.1f, 0, light);
                    ps.popPose();
                }
            }
            // thân đài + vành vàng
            block(ps, buf, LODE, 0, 5.5, 0, 1.3f, age * 0.5f, light);
            for (int i = 0; i < 8; i++) {
                double a = Math.PI / 4 * i;
                block(ps, buf, GILD, Math.cos(a) * 1.5, 5.6, Math.sin(a) * 1.5, 0.45f, (float) Math.toDegrees(-a), light);
            }
            if (pilot) return;
            // lõi núi lửa lơ lửng trên đỉnh
            double bob = Math.sin(age * 0.07) * 0.18;
            float spin = age * (ctl ? 9f : 3f);
            float cs = 1.5f + (float) Math.sin(age * 0.2) * 0.06f;
            block(ps, buf, ctl ? MAGMA : RAW_GOLD, 0, 7.6 + bob, 0, cs, spin, glow);
            block(ps, buf, ctl ? AMBER : PEARL, 0, 7.85 + bob, 0, cs * 0.62f, -spin * 1.5f, glow);
            block(ps, buf, CRYING, 0, 8.0 + bob, 0, cs * 0.3f, spin * 2.2f, glow);
            int n = 12;
            for (int i = 0; i < n; i++) { // 2 vòng tảng đá bay ngược chiều
                boolean inner = i % 2 == 0;
                double a = age * (ctl ? 0.11 : 0.04) * (inner ? 1 : -1) + Math.PI * 2 / n * i;
                double r = inner ? 2.6 : 3.6;
                block(ps, buf, inner ? DS_BRICK : GILD, Math.cos(a) * r, 7.4 + bob + Math.sin(a * 3 + age * 0.05) * 0.4, Math.sin(a) * r,
                        inner ? 0.55f : 0.4f, (float) Math.toDegrees(a) * 2, light);
            }
        }

        /** Gai Địa Thứ Hộ Tháp: cụm 5 nhũ đá chĩa lên, chân đá vụn, mũi hổ phách sáng. */
        private void spike(PhapTurret t, float age, PoseStack ps, MultiBufferSource buf, int light, int glow) {
            block(ps, buf, bs(net.minecraft.world.level.block.Blocks.DRIPSTONE_BLOCK), 0, 0, 0, 0.9f, 20, light);
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

        /** Trụ Hợp Thể (laser theo thời gian): đàn tế lớn - bệ tuff, 4 cột đá đen viền vàng, tinh thể khóc xoay giữa, vòng đá bay quanh, nạp càng lâu càng rực. */
        private void merged(PhapTurret t, float age, PoseStack ps, MultiBufferSource buf, int light, int glow) {
            float ramp = t.rampFraction();
            block(ps, buf, TUFF, 0, 0, 0, 1.9f, 0, light);
            block(ps, buf, DS_BRICK, 0, 0.75, 0, 1.4f, 45, light);
            for (int i = 0; i < 4; i++) { // 4 cột góc
                double a = Math.PI / 2 * i + Math.PI / 4;
                double x = Math.cos(a) * 1.25, z = Math.sin(a) * 1.25;
                block(ps, buf, BASALT, x, 0, z, 0.55f, 0, light);
                block(ps, buf, BASALT, x, 0.55, z, 0.55f, 0, light);
                block(ps, buf, BASALT, x, 1.1, z, 0.5f, 0, light);
                block(ps, buf, GILD, x, 1.6, z, 0.45f, 45, light);
                block(ps, buf, LANTERN, x, 2.05 + Math.sin(age * 0.2 + i) * 0.05, z, 0.22f + 0.12f * ramp, age * 6f, glow);
            }
            double bob = Math.sin(age * 0.08) * 0.1;
            float spin = age * (3f + 22f * ramp);
            float cs = 0.7f + 0.35f * ramp + (float) Math.sin(age * (0.3 + ramp)) * 0.04f * (1 + ramp * 3);
            block(ps, buf, CRYING, 0, 2.5 + bob, 0, cs, spin, glow);
            block(ps, buf, AMBER, 0, 2.6 + bob, 0, cs * 0.62f, -spin * 1.4f, glow);
            block(ps, buf, GOLD, 0, 2.65 + bob, 0, cs * 0.3f, spin * 2f, glow);
            int n = 8;
            for (int i = 0; i < n; i++) { // vòng đá bay, nhanh dần theo độ nạp
                double a = age * (0.05 + 0.12 * ramp) + Math.PI * 2 / n * i;
                double r = 1.7 - 0.35 * ramp;
                block(ps, buf, i % 2 == 0 ? DS_TILE : GILD, Math.cos(a) * r, 2.5 + bob + Math.sin(a * 2) * 0.25, Math.sin(a) * r, 0.24f, (float) Math.toDegrees(a), light);
            }
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
