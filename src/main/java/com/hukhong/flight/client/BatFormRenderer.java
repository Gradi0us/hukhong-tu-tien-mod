package com.hukhong.flight.client;

import com.hukhong.HuKhong;
import com.hukhong.flight.FlightClientState;
import com.hukhong.flight.FlightMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Ma Tu "hoa doi": khi bay kieu MA (dong bo qua FlightClientState, khong doc CULTIVATION) thay model nguoi choi bang
 * con doi vanilla phong ~2.5x, nhuom do sam + lop phat sang (mat/canh do), vo canh. Chi anh huong render: hitbox giu nguyen,
 * goc nhin thu nhat khong ve than nen khong doi.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class BatFormRenderer {
    private BatFormRenderer() {}

    static final float SCALE = 2.5f;
    static final int DARK_RED = 0xFF7A0C12;
    static final int GLOW_RED = 0xB0FF2020;
    private static final ResourceLocation TEX = ResourceLocation.withDefaultNamespace("textures/entity/bat.png");

    private static EntityModelSet bakedFor;
    private static ModelPart root;

    private static ModelPart root() {
        EntityModelSet set = Minecraft.getInstance().getEntityModels();
        if (root == null || bakedFor != set) { root = set.bakeLayer(ModelLayers.BAT); bakedFor = set; }
        return root;
    }

    /** Dang o dang doi? (thuan, dung chung cho test). */
    public static boolean isBatForm(int entityId) {
        var info = FlightClientState.get(entityId);
        return info != null && info.mode() == FlightMode.MA;
    }

    @SubscribeEvent
    public static void onRender(RenderPlayerEvent.Pre e) {
        if (!isBatForm(e.getEntity().getId())) return;
        e.setCanceled(true);
        var p = e.getEntity();
        PoseStack ps = e.getPoseStack();
        float pt = e.getPartialTick();
        float age = p.tickCount + pt;
        ModelPart r = root();
        r.getAllParts().forEach(ModelPart::resetPose);
        ModelPart body = r.getChild("body"), head = r.getChild("head");
        ModelPart rw = body.getChild("right_wing"), lw = body.getChild("left_wing");
        float flap = Mth.cos(age * 1.1f) * (float) Math.PI * 0.28f;
        rw.yRot = flap; lw.yRot = -flap;
        rw.getChild("right_wing_tip").yRot = flap * 0.6f; lw.getChild("left_wing_tip").yRot = -flap * 0.6f;
        body.xRot = (float) Math.PI / 4f + Mth.cos(age * 0.1f) * 0.15f;
        head.xRot = p.getViewXRot(pt) * ((float) Math.PI / 180f) * 0.5f;
        float bob = Mth.sin(age * 0.55f) * 0.05f;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180f - Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot)));
        ps.scale(-SCALE, -SCALE, SCALE);
        ps.translate(0.0, -1.501 + bob, 0.0);
        VertexConsumer vc = e.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(TEX));
        r.render(ps, vc, e.getPackedLight(), OverlayTexture.NO_OVERLAY, DARK_RED);
        VertexConsumer glow = e.getMultiBufferSource().getBuffer(RenderType.eyes(TEX));
        r.render(ps, glow, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, GLOW_RED);
        ps.popPose();
    }
}
