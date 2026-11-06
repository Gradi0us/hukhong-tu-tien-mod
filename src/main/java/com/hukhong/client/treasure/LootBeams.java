package com.hukhong.client.treasure;

import com.hukhong.HuKhong;
import com.hukhong.client.fx.FxDraw;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

/**
 * Cột sáng đồ rơi (2026-11-06): vật phẩm trên đất có cột sáng + vòng sáng dưới chân theo độ hiếm
 * (lục: đồ của mod, vàng: hiếm, lam: quý, tím: sử thi). Đồ quý vừa rơi: tiếng "ting" + bung hạt.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class LootBeams {
    private LootBeams() {}

    static final double RANGE = 40;
    static final int MAX = 96;
    static final int[] COL = {0, 0x9CFFB0, 0xFFE45A, 0x5AE8FF, 0xC86BFF};
    private static final Set<Integer> SEEN = new HashSet<>();

    /** 0 = không cột (đồ vanilla thường), 1..4 theo độ hiếm. */
    public static int level(ItemStack st) {
        if (st.isEmpty()) return 0;
        int l = st.getRarity().ordinal();
        if (HuKhong.MODID.equals(BuiltInRegistries.ITEM.getKey(st.getItem()).getNamespace())) l++;
        if (st.isEnchanted()) l++;
        return Math.min(4, l);
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        MultiBufferSource.BufferSource bs = mc.renderBuffers().bufferSource();
        VertexConsumer vc = bs.getBuffer(RenderType.lightning());
        FxDraw g = new FxDraw(vc, e.getPoseStack().last().pose(), e.getCamera().getPosition());
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        double time = mc.level.getGameTime() + pt;
        int n = 0;
        for (Entity en : mc.level.entitiesForRendering()) {
            if (!(en instanceof ItemEntity it) || n >= MAX) continue;
            if (it.distanceToSqr(mc.player) > RANGE * RANGE) continue;
            int lvl = level(it.getItem());
            if (lvl == 0) continue;
            n++;
            int rgb = COL[lvl];
            Vec3 base = it.getPosition(pt);
            float pulse = 0.8f + 0.2f * (float) Math.sin(time / 6.0 + it.getId());
            double h = 0.8 + 0.85 * lvl;
            Vec3 a = base.add(0, 0.05, 0), b = base.add(0, h, 0);
            g.ribbon(a, b, 0.32 + 0.05 * lvl, 0.05, FxDraw.col(rgb, 0.16f * pulse), FxDraw.col(rgb, 0f));
            g.ribbon(a, b, 0.09, 0.02, FxDraw.col(FxDraw.white(rgb, 0.6f), 0.55f * pulse), FxDraw.col(rgb, 0f));
            ring(g, base.add(0, 0.03, 0), 0.32 + 0.04 * lvl + 0.04 * Math.sin(time / 5.0), rgb, 0.35f * pulse);
            if (lvl >= 3) ring(g, base.add(0, 0.04, 0), 0.6 + 0.1 * ((time / 20.0) % 1), rgb, 0.25f * (1f - (float) ((time / 20.0) % 1)));
        }
        bs.endBatch(RenderType.lightning());
    }

    /** Vòng sáng mỏng nằm trên đất (24 đoạn). */
    static void ring(FxDraw g, Vec3 c, double r, int rgb, float alpha) {
        double w = 0.06;
        int seg = 24;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            Vec3 o0 = c.add(Math.cos(a0) * (r + w), 0, Math.sin(a0) * (r + w)), o1 = c.add(Math.cos(a1) * (r + w), 0, Math.sin(a1) * (r + w));
            Vec3 i0 = c.add(Math.cos(a0) * (r - w), 0, Math.sin(a0) * (r - w)), i1 = c.add(Math.cos(a1) * (r - w), 0, Math.sin(a1) * (r - w));
            int col = FxDraw.col(rgb, alpha);
            g.quad2(o0, o1, i1, i0, col, col, col, col);
            g.quad2(i0, i1, o1, o0, col, col, col, col); // hai mặt
        }
    }

    /** Lấp lánh quanh đồ quý + "ting" khi đồ quý xuất hiện gần người chơi. */
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel lv = mc.level;
        if (lv == null || mc.player == null || mc.isPaused()) return;
        if (lv.getGameTime() % 200 == 0) SEEN.removeIf(id -> lv.getEntity(id) == null);
        for (Entity en : lv.entitiesForRendering()) {
            if (!(en instanceof ItemEntity it) || it.distanceToSqr(mc.player) > RANGE * RANGE) continue;
            int lvl = level(it.getItem());
            if (lvl < 2) continue;
            int rgb = COL[lvl];
            var dust = new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 0.8f);
            if (lv.random.nextInt(6 - lvl) == 0)
                lv.addParticle(dust, it.getX() + (lv.random.nextDouble() - .5) * .5, it.getY() + 0.2 + lv.random.nextDouble() * .5, it.getZ() + (lv.random.nextDouble() - .5) * .5, 0, 0.03, 0);
            if (lvl >= 3 && SEEN.add(it.getId()) && it.distanceToSqr(mc.player) < 24 * 24) {
                lv.playLocalSound(it.getX(), it.getY(), it.getZ(), SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.NEUTRAL, 0.9f, lvl == 4 ? 0.7f : 1.2f, false);
                for (int k = 0; k < 14 + 6 * lvl; k++)
                    lv.addParticle(lvl == 4 ? ParticleTypes.WITCH : ParticleTypes.END_ROD, it.getX(), it.getY() + 0.3, it.getZ(),
                            (lv.random.nextDouble() - .5) * .3, lv.random.nextDouble() * .25, (lv.random.nextDouble() - .5) * .3);
            }
        }
    }
}
