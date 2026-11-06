package com.hukhong.client.phaptu;

import com.hukhong.phaptu.QuangKinh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Quang Ảnh Kính: 2 tấm kính hình thoi bắt chéo xoay chậm + lõi sáng; nứt dần (lõi tối đi) theo số đòn đã chịu. */
final class QuangKinhRenderer extends EntityRenderer<QuangKinh> {
    private static final BlockState GLASS = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
    private static final BlockState GLASS2 = Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
    private static final BlockState CORE = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState CORE_DIM = Blocks.GLOWSTONE.defaultBlockState();

    QuangKinhRenderer(EntityRendererProvider.Context ctx) { super(ctx); shadowRadius = 0.3f; }

    @Override
    public void render(QuangKinh e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        var br = Minecraft.getInstance().getBlockRenderer();
        float t = e.tickCount + pt;
        int full = LightTexture.FULL_BRIGHT;
        float hp = 1 - e.hits() / (float) QuangKinh.MAX_HITS;
        ps.pushPose();
        ps.translate(0, 1.1 + Math.sin(t * 0.08) * 0.12, 0);
        ps.mulPose(Axis.YP.rotationDegrees(t * 1.6f));
        for (int i = 0; i < 2; i++) { // 2 tấm kính thoi bắt chéo
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(i * 90));
            ps.mulPose(Axis.ZP.rotationDegrees(45));
            float s = 1.05f;
            ps.scale(s, s, 0.06f);
            ps.translate(-0.5, -0.5, -0.5);
            br.renderSingleBlock(i == 0 ? GLASS : GLASS2, ps, buf, full, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        ps.pushPose(); // lõi sáng, nhỏ dần khi nứt
        float c = 0.18f + 0.14f * hp;
        ps.mulPose(Axis.XP.rotationDegrees(t * 2.2f));
        ps.scale(c, c, c);
        ps.translate(-0.5, -0.5, -0.5);
        br.renderSingleBlock(hp > 0.4f ? CORE : CORE_DIM, ps, buf, full, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(QuangKinh e) { return TextureAtlas.LOCATION_BLOCKS; }
}
