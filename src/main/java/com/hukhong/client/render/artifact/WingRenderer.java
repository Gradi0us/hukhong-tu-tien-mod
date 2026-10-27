package com.hukhong.client.render.artifact;

import com.hukhong.artifact.ArtifactMath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/**
 * Cánh Đạo tu dùng chung cho FlightLayer (đang bay) và ArtifactLayer (Pháp Bảo). Vẽ trong không gian layer người chơi
 * (y xuống). stage 0 = cánh cơ bản cũ (wings.png); stage 1..6 = số đôi cánh + texture + lớp phát sáng theo giai đoạn.
 */
public final class WingRenderer {
    private WingRenderer() {}

    /** pairs: số đôi; stage: giai đoạn Pháp Bảo (0 = cánh cơ bản); flying: dang rộng + vỗ mạnh, ngược lại xếp gọn. */
    public static void render(PoseStack ps, MultiBufferSource buf, int light, int pairs, int stage, boolean flying, float age) {
        if (stage > 0) {
            if (stage >= 3) light = LightTexture.FULL_BRIGHT;
            draw(ps, buf.getBuffer(RenderType.entityCutoutNoCull(ArtifactTextures.wings(stage))), light, pairs, stage, flying, age);
            if (stage >= 4)
                draw(ps, buf.getBuffer(RenderType.eyes(ArtifactTextures.wingsGlow(stage))), LightTexture.FULL_BRIGHT, pairs, stage, flying, age);
        } else {
            draw(ps, buf.getBuffer(RenderType.entityCutoutNoCull(ArtifactTextures.WINGS_BASIC)),
                    LightTexture.FULL_BRIGHT, pairs, 0, flying, age);
        }
    }

    private static void draw(PoseStack ps, VertexConsumer vc, int light, int pairs, int stage, boolean flying, float age) {
        float flap = (float) Math.sin(age * (flying ? 0.25 : 0.08)) * (flying ? 12f : 3f);
        float spread = flying ? 28f : 12f;
        float grow = 1.0f + 0.07f * stage;
        float fold = flying ? 1f : 0.8f;
        for (int pair = 0; pair < pairs; pair++) {
            for (int side = -1; side <= 1; side += 2) {
                ps.pushPose();
                ps.translate(side * 0.12, 0.1 + pair * 0.12, 0.2);
                ps.mulPose(Axis.YP.rotationDegrees(-side * (spread + pair * 14f + flap)));
                ps.mulPose(Axis.ZP.rotationDegrees(side * pair * 10f));
                float w = (1.2f - pair * 0.1f) * grow * fold, top = -0.45f * fold * (0.9f + 0.1f * grow), bot = 0.85f * fold * grow;
                Matrix4f m = ps.last().pose();
                float u0 = side > 0 ? 0f : 1f, u1 = side > 0 ? 1f : 0f;
                float x1 = side * w;
                v(vc, ps, m, 0, top, u0, 0f, light);
                v(vc, ps, m, x1, top, u1, 0f, light);
                v(vc, ps, m, x1, bot, u1, 1f, light);
                v(vc, ps, m, 0, bot, u0, 1f, light);
                ps.popPose();
            }
        }
    }

    private static void v(VertexConsumer vc, PoseStack ps, Matrix4f m, float x, float y, float u, float v, int light) {
        vc.addVertex(m, x, y, 0f).setColor(255, 255, 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(ps.last(), 0f, 0f, 1f);
    }

    public static int pairsFor(int stage) { return ArtifactMath.wingPairs(stage); }
}
