package com.hukhong.flight.client;

import com.hukhong.HuKhong;
import com.hukhong.flight.FlightClientState;
import com.hukhong.flight.FlightMode;
import com.hukhong.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/** Layer người chơi: kiếm dưới chân (Kiếm) và cánh thiên sứ (Đạo). */
public final class FlightLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation WINGS =
            ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/entity/wings.png");
    private static ItemStack sword;

    public FlightLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, float limbSwing,
                       float limbSwingAmount, float partialTick, float age, float netHeadYaw, float headPitch) {
        var info = FlightClientState.get(p.getId());
        if (info == null) return;
        if (info.mode() == FlightMode.KIEM) renderSword(ps, buf, light, p, age);
        else if (info.mode() == FlightMode.DAO) renderWings(ps, buf, info.pairs(), age);
    }

    private void renderSword(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, float age) {
        if (sword == null) sword = new ItemStack(ModItems.BAN_MENH_KIEM.get());
        ps.pushPose();
        ps.translate(0.0, 1.5, 0.0);       // gót chân (không gian model: y xuống)
        ps.scale(-1f, -1f, 1f);             // trả về hệ y-lên
        ps.translate(0.0, Math.sin(age * 0.2) * 0.02, 0.0);
        ps.mulPose(Axis.YP.rotationDegrees(135f)); // mũi kiếm hướng phía trước
        ps.mulPose(Axis.XP.rotationDegrees(90f));  // nằm ngang
        ps.scale(1.6f, 1.6f, 1.6f);
        Minecraft.getInstance().getItemRenderer().renderStatic(sword, ItemDisplayContext.FIXED, light,
                OverlayTexture.NO_OVERLAY, ps, buf, p.level(), p.getId());
        ps.popPose();
    }

    private void renderWings(PoseStack ps, MultiBufferSource buf, int pairs, float age) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WINGS));
        int light = LightTexture.FULL_BRIGHT;
        float flap = (float) Math.sin(age * 0.25) * 12f;
        for (int pair = 0; pair < pairs; pair++) {
            for (int side = -1; side <= 1; side += 2) {
                ps.pushPose();
                ps.translate(side * 0.12, 0.1 + pair * 0.12, 0.2);
                ps.mulPose(Axis.YP.rotationDegrees(-side * (28f + pair * 14f + flap)));
                ps.mulPose(Axis.ZP.rotationDegrees(side * pair * 10f));
                float w = 1.2f - pair * 0.12f, top = -0.45f, bot = 0.85f;
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
}
