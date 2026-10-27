package com.hukhong.client.render.artifact;

import com.hukhong.artifact.ArtifactAttachments;
import com.hukhong.artifact.ArtifactData;
import com.hukhong.artifact.ArtifactMath;
import com.hukhong.flight.FlightClientState;
import com.hukhong.flight.FlightMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/**
 * Layer pháp khí theo đạo (stage 1..6, đồng bộ qua attachment):
 * Đạo = Pháp Bảo (cánh nhiều đôi + lớp sáng), Thể = Bá Thể (lớp da vàng kim + gân sáng), Ma = Ma Hồn (sừng + mây tối).
 */
public final class ArtifactLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public ArtifactLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, float limbSwing,
                       float limbSwingAmount, float partialTick, float age, float netHeadYaw, float headPitch) {
        if (p.isInvisible() || p.isSpectator()) return;
        ArtifactData a = p.getData(ArtifactAttachments.ARTIFACT.get());
        if (a.dao() > 0) renderWings(ps, buf, light, p, a.dao(), age);
        if (a.the() > 0) renderBaThe(ps, buf, light, a.the(), age);
        if (a.ma() > 0) {
            renderHorns(ps, buf, light, a.ma(), age);
            renderCloud(ps, buf, light, a.ma(), age);
        }
    }

    // ------------------------------------------------------------------ Đạo: Pháp Bảo

    private void renderWings(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, int stage, float age) {
        var info = FlightClientState.get(p.getId());
        boolean flying = info != null && info.mode() == FlightMode.DAO;
        WingRenderer.render(ps, buf, light, ArtifactMath.wingPairs(stage), stage, flying, age);
    }

    // ------------------------------------------------------------------ Thể: Bá Thể

    private void renderBaThe(PoseStack ps, MultiBufferSource buf, int light, int stage, float age) {
        int alpha = Math.min(235, 70 + stage * 28);
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(ArtifactTextures.theSkin(stage)));
        getParentModel().renderToBuffer(ps, vc, light, OverlayTexture.NO_OVERLAY, (alpha << 24) | 0xFFFFFF);
        if (stage >= 4) {
            float pulse = 0.55f + 0.35f * (float) Math.sin(age * 0.12);
            int ga = (int) (255 * Math.min(1f, pulse));
            VertexConsumer glow = buf.getBuffer(RenderType.eyes(ArtifactTextures.theGlow(stage)));
            getParentModel().renderToBuffer(ps, glow, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, (ga << 24) | 0xFFFFFF);
        }
    }

    // ------------------------------------------------------------------ Ma: Ma Hồn

    private static final int[] HORN_SEGS = {0, 2, 3, 3, 4, 5, 6};
    private static final float[] HORN_SIZE = {0, 0.045f, 0.05f, 0.055f, 0.06f, 0.065f, 0.07f};

    private void renderHorns(PoseStack ps, MultiBufferSource buf, int light, int stage, float age) {
        if (stage >= 5) light = LightTexture.FULL_BRIGHT;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(ArtifactTextures.horn(stage)));
        int pairs = stage >= 4 ? 2 : 1;
        for (int pair = 0; pair < pairs; pair++) {
            for (int side = -1; side <= 1; side += 2) {
                ps.pushPose();
                getParentModel().head.translateAndRotate(ps);
                // đôi chính ở thái dương, đôi phụ (>=4) nhỏ hơn, lệch ra sau
                float bx = side * (pair == 0 ? 0.2f : 0.13f);
                float bz = pair == 0 ? -0.04f : 0.1f;
                ps.translate(bx, -0.48f, bz);
                ps.mulPose(Axis.ZP.rotationDegrees(side * (pair == 0 ? 14f : 30f)));
                ps.mulPose(Axis.XP.rotationDegrees(pair == 0 ? -4f : -26f));
                float size = HORN_SIZE[stage] * (pair == 0 ? 1f : 0.7f);
                int segs = Math.max(2, HORN_SEGS[stage] - pair);
                for (int s = 0; s < segs; s++) {
                    float t = (float) s / segs;
                    float half = size * (1.15f - t * 0.95f);
                    float len = 0.085f + 0.01f * stage;
                    box(vc, ps, half, len / 2f, half, -len / 2f, light, 255);
                    ps.translate(0, -len * 0.92f, 0);
                    ps.mulPose(Axis.ZP.rotationDegrees(side * (9f + stage)));
                    ps.mulPose(Axis.XP.rotationDegrees(-5f));
                }
                ps.popPose();
            }
        }
    }

    /** Hộp quanh (0, cy, 0) nửa kích thước hx/hy/hz; UV phủ toàn texture trên mỗi mặt (texture sừng là gradient gốc->đỉnh). */
    private static void box(VertexConsumer vc, PoseStack ps, float hx, float hy, float hz, float cy, int light, int a) {
        Matrix4f m = ps.last().pose();
        float x0 = -hx, x1 = hx, y0 = cy - hy, y1 = cy + hy, z0 = -hz, z1 = hz;
        // y xuống: y1 là phía gốc (thấp hơn), y0 là phía đỉnh. v: 0 = đỉnh, 1 = gốc.
        quad(vc, ps, m, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0, 0, -1, light, a);
        quad(vc, ps, m, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, 0, 0, 1, light, a);
        quad(vc, ps, m, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0, light, a);
        quad(vc, ps, m, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0, light, a);
    }

    private static void quad(VertexConsumer vc, PoseStack ps, Matrix4f m,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz, int light, int a) {
        vert(vc, ps, m, ax, ay, az, 0, 0, nx, ny, nz, light, a);
        vert(vc, ps, m, bx, by, bz, 1, 0, nx, ny, nz, light, a);
        vert(vc, ps, m, cx, cy, cz, 1, 1, nx, ny, nz, light, a);
        vert(vc, ps, m, dx, dy, dz, 0, 1, nx, ny, nz, light, a);
    }

    private static void vert(VertexConsumer vc, PoseStack ps, Matrix4f m, float x, float y, float z, float u, float v,
                             float nx, float ny, float nz, int light, int a) {
        vc.addVertex(m, x, y, z).setColor(255, 255, 255, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(ps.last(), nx, ny, nz);
    }

    private void renderCloud(PoseStack ps, MultiBufferSource buf, int light, int stage, float age) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(ArtifactTextures.cloud(stage)));
        int n = 3 + stage * 2;
        int alpha = Math.min(210, 90 + stage * 20);
        int fl = Math.max(light & 0xFFFF, 0x00A0) | (light & 0xFFFF0000); // tối nhưng không đen hẳn
        for (int i = 0; i < n; i++) {
            float ang = age * (0.03f + 0.004f * (i % 3)) * (i % 2 == 0 ? 1 : -1) + i * 6.2831853f / n;
            float r = 0.38f + 0.06f * (i % 4) + 0.04f * stage;
            float band = (i % 3) * 0.22f;
            float y = 1.5f - 0.04f - band - 0.03f * (float) Math.sin(age * 0.07 + i);
            float size = 0.45f + 0.06f * stage + 0.05f * (i % 3);
            ps.pushPose();
            ps.translate(Math.cos(ang) * r, y, Math.sin(ang) * r);
            ps.mulPose(Axis.YP.rotationDegrees(age * 2.2f + i * 37f));
            Matrix4f m = ps.last().pose();
            // mặt phẳng ngang (xz), pháp tuyến hướng lên (y xuống => -y)
            float h = size;
            vc.addVertex(m, -h, 0, -h).setColor(255, 255, 255, alpha).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fl).setNormal(ps.last(), 0, -1, 0);
            vc.addVertex(m, h, 0, -h).setColor(255, 255, 255, alpha).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fl).setNormal(ps.last(), 0, -1, 0);
            vc.addVertex(m, h, 0, h).setColor(255, 255, 255, alpha).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fl).setNormal(ps.last(), 0, -1, 0);
            vc.addVertex(m, -h, 0, h).setColor(255, 255, 255, alpha).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fl).setNormal(ps.last(), 0, -1, 0);
            ps.popPose();
        }
    }
}
