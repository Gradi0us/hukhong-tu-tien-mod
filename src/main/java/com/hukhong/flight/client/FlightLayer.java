package com.hukhong.flight.client;

import com.hukhong.client.render.artifact.WingRenderer;
import com.hukhong.flight.FlightClientState;
import com.hukhong.flight.FlightMode;
import com.hukhong.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Layer người chơi: kiếm dưới chân (Kiếm) và cánh thiên sứ (Đạo). */
public final class FlightLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
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
        else if (info.mode() == FlightMode.DAO) {
            // Pháp Bảo (stage >= 1) do ArtifactLayer vẽ cả lúc bay; chỉ giữ cánh cơ bản cho người chưa có Pháp Bảo.
            if (p.getData(com.hukhong.artifact.ArtifactAttachments.ARTIFACT.get()).dao() == 0)
                WingRenderer.render(ps, buf, LightTexture.FULL_BRIGHT, info.pairs(), 0, true, age);
        }
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
}
