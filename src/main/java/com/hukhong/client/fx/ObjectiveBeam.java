package com.hukhong.client.fx;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import com.hukhong.client.objective.ClientObjective;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Cột sáng mỏng, mờ kiểu beacon tại mục tiêu chính khi người chơi trong 256 block. Tôn trọng FX quality. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class ObjectiveBeam {
    private ObjectiveBeam() {}

    private static final double RANGE = 256;
    private static final int GOLD = 0xE8C060;

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        BlockPos t = ClientObjective.sameDimTarget();
        Minecraft mc = Minecraft.getInstance();
        if (t == null || mc.level == null || mc.player == null) return;
        double dx = t.getX() + 0.5 - mc.player.getX(), dz = t.getZ() + 0.5 - mc.player.getZ();
        if (dx * dx + dz * dz > RANGE * RANGE) return;
        MultiBufferSource.BufferSource bs = mc.renderBuffers().bufferSource();
        VertexConsumer vc = bs.getBuffer(RenderType.lightning());
        FxDraw g = new FxDraw(vc, e.getPoseStack().last().pose(), e.getCamera().getPosition());
        double y0 = Math.max(mc.level.getMinBuildHeight(), t.getY() - 2), y1 = Math.min(mc.level.getMaxBuildHeight(), t.getY() + 96);
        Vec3 a = new Vec3(t.getX() + 0.5, y0, t.getZ() + 0.5), b = new Vec3(t.getX() + 0.5, y1, t.getZ() + 0.5);
        float pulse = 0.85f + 0.15f * (float) Math.sin((mc.level.getGameTime() + e.getPartialTick().getGameTimeDeltaPartialTick(false)) / 8.0);
        switch (ClientConfig.fxQuality()) {
            case LOW -> g.ribbon(a, b, 0.18, 0.18, FxDraw.col(GOLD, 0.22f * pulse), FxDraw.col(GOLD, 0.05f));
            case MED -> {
                g.ribbon(a, b, 0.35, 0.35, FxDraw.col(GOLD, 0.10f * pulse), FxDraw.col(GOLD, 0.02f));
                g.ribbon(a, b, 0.15, 0.15, FxDraw.col(GOLD, 0.25f * pulse), FxDraw.col(GOLD, 0.05f));
            }
            default -> {
                g.ribbon(a, b, 0.5, 0.5, FxDraw.col(GOLD, 0.07f * pulse), FxDraw.col(GOLD, 0.01f));
                g.ribbon(a, b, 0.25, 0.25, FxDraw.col(GOLD, 0.16f * pulse), FxDraw.col(GOLD, 0.03f));
                g.ribbon(a, b, 0.08, 0.08, FxDraw.col(FxDraw.white(GOLD, 0.8f), 0.35f * pulse), FxDraw.col(GOLD, 0.06f));
            }
        }
        bs.endBatch(RenderType.lightning());
    }
}
