package com.hukhong.client.ambient;

import com.hukhong.HuKhong;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

/**
 * Bầu trời & sương: lớp phủ gradient tím thẫm/đỏ tím chồng lên trời (làm dịu mặt trời/mặt trăng/mây), màu sương, sương dày hơn,
 * chớp tím xa (không sát thương). Mọi trọng số lấy từ {@link AmbientClient} nên chuyển cảnh mượt.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class AmbientSky {
    private AmbientSky() {}

    private static final float[] CORR_SKY = {0.20f, 0.05f, 0.36f}, CORR_HORIZON = {0.46f, 0.16f, 0.62f};
    private static final float[] WAR_SKY = {0.30f, 0.03f, 0.13f}, WAR_HORIZON = {0.62f, 0.14f, 0.26f};
    private static final float[] CORR_FOG = {0.21f, 0.08f, 0.35f}, WAR_FOG = {0.34f, 0.07f, 0.16f};

    /** Bảng màu Bí Cảnh theo cấp: {bụi r,g,b, sương r,g,b}. */
    private static final float[][] TIER = {
            {0.85f, 0.92f, 1.00f, 0.10f, 0.13f, 0.20f}, // Bạch
            {1.00f, 0.85f, 0.45f, 0.17f, 0.13f, 0.06f}, // Hoàng
            {0.75f, 0.55f, 1.00f, 0.13f, 0.07f, 0.20f}, // Tử
            {0.60f, 0.52f, 0.70f, 0.05f, 0.04f, 0.07f}, // Hắc
            {1.00f, 0.42f, 0.42f, 0.20f, 0.04f, 0.06f}, // Hồng
    };

    public static float[] tierPalette(int tier) { return TIER[Mth.clamp(tier < 0 ? 2 : tier, 0, TIER.length - 1)]; }

    // ---- chớp
    private static final float[] PATTERN = {1f, 0.2f, 0.85f, 0.5f, 0.3f, 0.16f, 0.08f};
    private static int flashIdx = -1;
    private static float flash, flashO;
    private static float flashAz, flashRed;
    private static int thunderIn = -1;
    private static final float[][] BOLT = new float[3][];
    private static float boltAz;

    static void tick(Minecraft mc, ClientLevel lv) {
        flashO = flash;
        RandomSource rnd = lv.random;
        float c = AmbientClient.rawCorruption(), w = AmbientClient.rawWar();
        if (flashIdx < 0 && AmbientClient.master(1f) > 0.2f && lv.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            double chance = (c * 0.5 + w * 0.8) / (20.0 * 14.0);
            if ((c > 0.35f || w > 0.3f) && rnd.nextDouble() < chance || AmbientState.takeFlash()) startFlash(rnd, w > c);
        }
        if (flashIdx >= 0) {
            flash = PATTERN[flashIdx] * (flashIdx == 0 ? 1f : 0.85f + 0.15f * rnd.nextFloat());
            if (++flashIdx >= PATTERN.length) flashIdx = -1;
        } else flash = Math.max(0f, flash - 0.08f);
        if (thunderIn >= 0 && --thunderIn < 0) AmbientSounds.thunder(mc, 0.55f * AmbientClient.master(1f), 0.8f + rnd.nextFloat() * 0.25f);
    }

    private static void startFlash(RandomSource rnd, boolean red) {
        flashIdx = 0;
        flashAz = rnd.nextFloat() * Mth.TWO_PI;
        flashRed = red ? 1f : 0f;
        boltAz = flashAz;
        thunderIn = 28 + rnd.nextInt(50);
        // đường sét: 3 nhánh {elev, az} xen kẽ
        float e = 0.95f, a = flashAz;
        float[] main = new float[24];
        for (int i = 0; i < 12; i++) {
            main[i * 2] = e; main[i * 2 + 1] = a;
            e -= 0.07f + rnd.nextFloat() * 0.03f;
            a += (rnd.nextFloat() - 0.5f) * 0.09f;
        }
        BOLT[0] = main;
        for (int b = 1; b < 3; b++) {
            int from = 2 + rnd.nextInt(5);
            float[] br = new float[10];
            float be = main[from * 2], ba = main[from * 2 + 1], dir = rnd.nextBoolean() ? 1f : -1f;
            for (int i = 0; i < 5; i++) {
                br[i * 2] = be; br[i * 2 + 1] = ba;
                be -= 0.05f + rnd.nextFloat() * 0.03f;
                ba += dir * (0.035f + rnd.nextFloat() * 0.04f);
            }
            BOLT[b] = br;
        }
    }

    static float flash(float pt) { return Mth.lerp(pt, flashO, flash); }

    // ---------------------------------------------------------------- màu sương / khoảng nhìn
    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !AmbientClient.active()) return;
        float pt = (float) e.getPartialTick();
        float day = dayFactor(mc.level, pt);
        float dim = 0.55f + 0.45f * day;
        if (AmbientClient.inBiCanh()) {
            float[] p = tierPalette(AmbientState.bicanhTier());
            float k = Math.min(1f, AmbientClient.dust(pt));
            mixFog(e, p[3], p[4], p[5], k * 0.95f);
            return;
        }
        if (!AmbientClient.overworld()) return;
        float c = AmbientClient.corruption(pt), w = AmbientClient.war(pt);
        float t = Math.max(c, w);
        if (t < 0.004f) return;
        float tot = c + w;
        float r = (CORR_FOG[0] * c + WAR_FOG[0] * w) / tot, g = (CORR_FOG[1] * c + WAR_FOG[1] * w) / tot, b = (CORR_FOG[2] * c + WAR_FOG[2] * w) / tot;
        float f = flash(pt);
        mixFog(e, Math.min(1f, r * dim + f * 0.25f), Math.min(1f, g * dim + f * 0.08f), Math.min(1f, b * dim + f * 0.35f), Math.min(1f, t) * 0.93f);
    }

    private static void mixFog(ViewportEvent.ComputeFogColor e, float r, float g, float b, float k) {
        e.setRed(Mth.lerp(k, e.getRed(), r));
        e.setGreen(Mth.lerp(k, e.getGreen(), g));
        e.setBlue(Mth.lerp(k, e.getBlue(), b));
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !AmbientClient.active() || !AmbientClient.overworld()) return;
        if (e.getType() != FogType.NONE) return;
        float pt = (float) e.getPartialTick();
        float t = Math.max(AmbientClient.corruption(pt), AmbientClient.war(pt) * 0.8f);
        if (t < 0.004f) return;
        float far = e.getFarPlaneDistance(), near = e.getNearPlaneDistance();
        e.setFarPlaneDistance(Math.max(28f, Mth.lerp(Math.min(1f, t), far, far * 0.5f)));
        e.setNearPlaneDistance(Mth.lerp(Math.min(1f, t), near, near * 0.15f));
        e.setCanceled(true);
    }

    static float dayFactor(ClientLevel lv, float pt) {
        float tod = lv.getTimeOfDay(pt);
        return Mth.clamp(Mth.cos(tod * Mth.TWO_PI) * 2f + 0.5f, 0f, 1f);
    }

    // ---------------------------------------------------------------- vòm trời phủ
    @SubscribeEvent
    public static void renderSky(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel lv = mc.level;
        if (lv == null || !AmbientClient.active() || !AmbientClient.overworld()) return;
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        float c = AmbientClient.corruption(pt), w = AmbientClient.war(pt);
        float f = flash(pt);
        float amount = Math.min(1f, Math.max(c, w));
        if (amount < 0.005f && f < 0.01f) return;
        float tot = Math.max(1e-4f, c + w);
        float wc = c / tot, ww = w / tot;
        float day = dayFactor(lv, pt);
        float dim = 0.5f + 0.5f * day;
        float[] zen = new float[3], hor = new float[3];
        for (int i = 0; i < 3; i++) {
            zen[i] = (CORR_SKY[i] * wc + WAR_SKY[i] * ww) * dim;
            hor[i] = (CORR_HORIZON[i] * wc + WAR_HORIZON[i] * ww) * dim;
        }
        Matrix4f mv = e.getModelViewMatrix();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        PoseStack ps = new PoseStack();
        ps.mulPose(mv);
        Matrix4f m = ps.last().pose();

        if (amount >= 0.005f) {
            RenderSystem.defaultBlendFunc();
            dome(m, zen, hor, amount * 0.82f, 0f, 0f, 0f);
        }
        if (f > 0.01f) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            float[] fc = flashRed > 0.5f ? new float[]{1f, 0.35f, 0.4f} : new float[]{0.72f, 0.5f, 1f};
            dome(m, fc, fc, 0f, f * 0.5f, flashAz, 1f);
            bolt(m, fc, f);
        }
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static final int RINGS = 14, SEG = 28;
    private static final float R = 100f, E0 = -0.35f;

    /**
     * Vòm gradient. Chế độ phủ (flashAmt=0): alpha = a * (0.55..1 theo độ cao), mờ dần dưới đường chân trời.
     * Chế độ chớp (flashAmt>0): quầng quanh phương vị az + nền mờ.
     */
    private static void dome(Matrix4f m, float[] zen, float[] hor, float a, float flashAmt, float az, float flashMode) {
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < RINGS; i++) {
            float e1 = Mth.lerp(i / (float) RINGS, E0, Mth.HALF_PI), e2 = Mth.lerp((i + 1) / (float) RINGS, E0, Mth.HALF_PI);
            for (int j = 0; j < SEG; j++) {
                float a1 = j * Mth.TWO_PI / SEG, a2 = (j + 1) * Mth.TWO_PI / SEG;
                vert(bb, m, e1, a1, zen, hor, a, flashAmt, az, flashMode);
                vert(bb, m, e1, a2, zen, hor, a, flashAmt, az, flashMode);
                vert(bb, m, e2, a2, zen, hor, a, flashAmt, az, flashMode);
                vert(bb, m, e2, a1, zen, hor, a, flashAmt, az, flashMode);
            }
        }
        MeshData md = bb.build();
        if (md != null) BufferUploader.drawWithShader(md);
    }

    private static void vert(BufferBuilder bb, Matrix4f m, float el, float az, float[] zen, float[] hor, float a, float flashAmt, float flashAz, float mode) {
        float ce = Mth.cos(el);
        float x = ce * Mth.sin(az) * R, y = Mth.sin(el) * R, z = ce * Mth.cos(az) * R;
        float h = Mth.clamp(Mth.sin(Math.max(el, 0f)), 0f, 1f);
        float al;
        float r = Mth.lerp(h, hor[0], zen[0]), g = Mth.lerp(h, hor[1], zen[1]), b = Mth.lerp(h, hor[2], zen[2]);
        if (mode > 0.5f) {
            float d = Mth.abs(Mth.wrapDegrees((az - flashAz) * Mth.RAD_TO_DEG)) / 60f;
            float lowBias = 1f - Mth.clamp(el / 1.1f, 0f, 1f) * 0.6f;
            al = flashAmt * (0.25f + 0.75f * (float) Math.exp(-d * d * 1.6)) * lowBias;
            if (el < 0) al *= 1f + el / (-E0) ;
        } else {
            al = a * Mth.lerp(h, 0.55f, 1f);
            if (el < 0) al *= Math.max(0f, 1f + el / (-E0));
        }
        bb.addVertex(m, x, y, z).setColor(Mth.clamp(r, 0f, 1f), Mth.clamp(g, 0f, 1f), Mth.clamp(b, 0f, 1f), Mth.clamp(al, 0f, 1f));
    }

    private static void bolt(Matrix4f m, float[] col, float f) {
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        boolean any = false;
        for (int bi = 0; bi < 3; bi++) {
            float[] p = BOLT[bi];
            if (p == null) continue;
            float width = bi == 0 ? 0.012f : 0.006f;
            for (int i = 0; i + 1 < p.length / 2; i++) {
                float e1 = p[i * 2], a1 = p[i * 2 + 1], e2 = p[i * 2 + 2], a2 = p[i * 2 + 3];
                float al1 = Mth.clamp(f * (bi == 0 ? 1f : 0.6f) * (1f - i * 0.05f), 0f, 1f);
                bolt(bb, m, e1, a1, e2, a2, width, col, al1);
                any = true;
            }
        }
        if (!any) return;
        MeshData md = bb.build();
        if (md != null) BufferUploader.drawWithShader(md);
    }

    private static void bolt(BufferBuilder bb, Matrix4f m, float e1, float a1, float e2, float a2, float w, float[] col, float al) {
        for (float s : new float[]{1f, 2.6f}) { // lõi + quầng
            float ww = w * s, aa = s > 1.5f ? al * 0.25f : al;
            float r = s > 1.5f ? col[0] : Math.min(1f, col[0] + 0.35f), g = s > 1.5f ? col[1] : Math.min(1f, col[1] + 0.35f), b = s > 1.5f ? col[2] : Math.min(1f, col[2] + 0.2f);
            pt(bb, m, e1, a1 - ww, r, g, b, aa);
            pt(bb, m, e1, a1 + ww, r, g, b, aa);
            pt(bb, m, e2, a2 + ww, r, g, b, aa);
            pt(bb, m, e2, a2 - ww, r, g, b, aa);
        }
    }

    private static void pt(BufferBuilder bb, Matrix4f m, float el, float az, float r, float g, float b, float a) {
        float ce = Mth.cos(el);
        bb.addVertex(m, ce * Mth.sin(az) * (R - 2f), Mth.sin(el) * (R - 2f), ce * Mth.cos(az) * (R - 2f)).setColor(r, g, b, a);
    }
}
