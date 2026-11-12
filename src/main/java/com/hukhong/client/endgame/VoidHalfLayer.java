package com.hukhong.client.endgame;

import com.hukhong.HuKhong;
import com.hukhong.endgame.VoidMark;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Lớp vẽ dấu sau kết lên model người chơi (mọi skin, mọi người chơi):
 * <ul>
 *   <li>HALF_VOID (Hấp Thụ): nửa phải thân đen-tím vân sao ({@code void_half.png}) + đường nứt / mắt phải phát sáng
 *   ({@code void_half_glow.png}, RenderType eyes = luôn sáng).</li>
 *   <li>SEAL (Phong Ấn): ấn vàng phát sáng trên mu bàn tay phải ({@code seal_mark.png}).</li>
 *   <li>HALO (Siêu Độ): vầng sáng vàng nhạt sau lưng, hiện rõ khi đứng yên ({@code truth_halo.png}).</li>
 * </ul>
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class VoidHalfLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static ResourceLocation tex(String n) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/entity/" + n + ".png"); }
    static final ResourceLocation VOID = tex("void_half"), VOID_GLOW = tex("void_half_glow"), SEAL = tex("seal_mark"), HALO = tex("truth_halo");

    public VoidHalfLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers e) {
        for (PlayerSkin.Model m : e.getSkins()) {
            if (e.getSkin(m) instanceof PlayerRenderer r) r.addLayer(new VoidHalfLayer(r));
        }
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, float limb, float limbAmt, float pt, float age, float yawH, float pitch) {
        int k = VoidMarkClient.kind(p);
        if (k == VoidMark.NONE || p.isInvisible()) return;
        float a = VoidMarkClient.alpha(p, pt);
        if (a <= 0.01f) return;
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        int overlay = LivingEntityRenderer.getOverlayCoords(p, 0f);
        switch (k) {
            case VoidMark.HALF_VOID -> {
                m.renderToBuffer(ps, buf.getBuffer(RenderType.entityTranslucent(VOID)), light, overlay, FastColor.ARGB32.colorFromFloat(a, 1f, 1f, 1f));
                m.renderToBuffer(ps, buf.getBuffer(RenderType.eyes(VOID_GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, glow(a, p));
            }
            case VoidMark.SEAL -> m.renderToBuffer(ps, buf.getBuffer(RenderType.eyes(SEAL)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, glow(a * 0.9f, p));
            case VoidMark.HALO -> {
                float still = Math.max(0f, 1f - p.walkAnimation.speed(pt) * 3f); // rõ khi đứng yên
                if (still > 0.02f) halo(ps, buf, m, a * still, age);
            }
            default -> { }
        }
    }

    /** Vết nứt "thở" nhẹ. */
    private static int glow(float a, AbstractClientPlayer p) {
        float pulse = 0.75f + 0.25f * (float) Math.sin((p.tickCount + p.getId() * 7) * 0.12);
        float v = a * pulse;
        return FastColor.ARGB32.colorFromFloat(1f, v, v, v); // eyes: cộng sáng -> RGB quyết định độ sáng
    }

    /** Vầng sáng: quad 1.1 block sau gáy, xoay chậm, bám theo thân. */
    private static void halo(PoseStack ps, MultiBufferSource buf, PlayerModel<AbstractClientPlayer> m, float a, float age) {
        ps.pushPose();
        m.body.translateAndRotate(ps);
        ps.translate(0, -0.2, 0.32);
        ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(age * 0.8f));
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentEmissive(HALO));
        var pose = ps.last();
        float s = 0.55f;
        int c = FastColor.ARGB32.colorFromFloat(a * 0.8f, 1f, 0.95f, 0.75f);
        vert(vc, pose, -s, -s, 0, 0, c); vert(vc, pose, -s, s, 0, 1, c);
        vert(vc, pose, s, s, 1, 1, c); vert(vc, pose, s, -s, 1, 0, c);
        // mặt sau (để nhìn từ phía trước người chơi vẫn thấy viền sau vai)
        vert(vc, pose, s, -s, 1, 0, c); vert(vc, pose, s, s, 1, 1, c);
        vert(vc, pose, -s, s, 0, 1, c); vert(vc, pose, -s, -s, 0, 0, c);
        ps.popPose();
    }

    private static void vert(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float u, float v, int c) {
        vc.addVertex(pose, x, y, 0).setColor(c).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, -1);
    }

    /** Tay phải người thứ nhất (model đã được PlayerRenderer.renderRightHand đặt tư thế). */
    static void renderArm(PlayerModel<AbstractClientPlayer> m, int k, float a, PoseStack ps, MultiBufferSource buf, int light) {
        if (k == VoidMark.HALF_VOID) {
            int col = FastColor.ARGB32.colorFromFloat(a, 1f, 1f, 1f);
            VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(VOID));
            m.rightArm.render(ps, vc, light, OverlayTexture.NO_OVERLAY, col);
            m.rightSleeve.render(ps, vc, light, OverlayTexture.NO_OVERLAY, col);
            VertexConsumer g = buf.getBuffer(RenderType.eyes(VOID_GLOW));
            int gc = FastColor.ARGB32.colorFromFloat(1f, a, a, a);
            m.rightArm.render(ps, g, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, gc);
            m.rightSleeve.render(ps, g, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, gc);
        } else if (k == VoidMark.SEAL) {
            int gc = FastColor.ARGB32.colorFromFloat(1f, a * 0.9f, a * 0.9f, a * 0.9f);
            m.rightArm.render(ps, buf.getBuffer(RenderType.eyes(SEAL)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, gc);
        }
    }
}
