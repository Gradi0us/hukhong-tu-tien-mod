package com.hukhong.client.fx;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import com.hukhong.network.FxPayload;
import com.hukhong.vfx.FxType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Quản lý hiệu ứng VFX phía client: nhận FxPayload, tick, vẽ (additive) sau particle, phủ màu màn hình. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class FxClient {
    private FxClient() {}

    private static final int MAX_ACTIVE = 72;
    static final List<Inst> ACTIVE = new ArrayList<>();

    /** Một hiệu ứng đang chạy. */
    static final class Inst {
        FxType type; Vec3 pos, dir; float scale; int rgb, life, age, delay; boolean started; final Random rnd = new Random();
        List<Vec3[]> segs; List<Float> segW;          // sét / nứt đất
        float[] sx, sz, sd; boolean[] landed;           // mưa kiếm
        float tilt;
        Inst(FxType t, Vec3 p, Vec3 d, float s, int rgb, int life) {
            type = t; pos = p; dir = d; scale = s <= 0 ? 1f : s; this.rgb = rgb; this.life = life;
        }
    }

    static float q() {
        return switch (ClientConfig.fxQuality()) { case LOW -> 0.4f; case MED -> 0.7f; default -> 1f; };
    }

    static int defColor(FxType t) {
        return switch (t) {
            case SWORD_CRESCENT -> 0x9FE8FF; case SWORD_RAIN -> 0xBFEFFF; case SWORD_DOMAIN -> 0x7FD8FF;
            case TALISMAN_CIRCLE -> 0xFFD866; case LIGHT_PILLAR -> 0xFFE9A0; case GROUND_CRACK -> 0xFF8A30;
            case SHOCKWAVE -> 0xFFC070; case BLOOD_VORTEX -> 0xC01028; case DEMON_SILHOUETTE -> 0x8A0A1E;
            case LIGHTNING_BOLT -> 0xB8D8FF; case TELEGRAPH -> 0xA050FF; case STORM_CLOUD -> 0x1C1C2C;
            case SCREEN_TINT -> 0xFFFFFF; case BEAM -> 0xFFE9A0; default -> 0xFFE9A0;
        };
    }

    static int defLife(FxType t) {
        return switch (t) {
            case SWORD_CRESCENT -> 11; case SWORD_RAIN -> 46; case SWORD_DOMAIN -> 80; case TALISMAN_CIRCLE -> 50;
            case LIGHT_PILLAR -> 32; case GROUND_CRACK -> 50; case SHOCKWAVE -> 18; case BLOOD_VORTEX -> 40;
            case DEMON_SILHOUETTE -> 40; case LIGHTNING_BOLT -> 14; case TELEGRAPH -> 30; case STORM_CLOUD -> 80;
            case SCREEN_TINT -> 14; case BEAM -> 9; default -> 30;
        };
    }

    // ---------------------------------------------------------------- nhận

    public static void receive(FxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        FxType t = FxType.of(p.fx());
        Vec3 pos = new Vec3(p.x(), p.y(), p.z()), dir = new Vec3(p.dx(), p.dy(), p.dz());
        int rgb = p.color() == 0 ? defColor(t) : p.color() & 0xFFFFFF;
        float sc = p.scale() <= 0 ? 1f : p.scale();
        if (t == FxType.BREAKTHROUGH) { breakthrough(pos, sc, rgb); return; }
        if (t == FxType.UPGRADE_BURST) { upgrade(pos, sc, rgb); return; }
        add(t, pos, dir, sc, rgb, p.life() > 0 ? p.life() : defLife(t), 0);
    }

    static Inst add(FxType t, Vec3 pos, Vec3 dir, float sc, int rgb, int life, int delay) {
        Inst i = new Inst(t, pos, dir, sc, rgb, life);
        i.delay = delay;
        if (ACTIVE.size() >= MAX_ACTIVE) ACTIVE.remove(0);
        ACTIVE.add(i);
        return i;
    }

    private static void breakthrough(Vec3 pos, float s, int rgb) {
        add(FxType.LIGHT_PILLAR, pos, Vec3.ZERO, 2.2f * s, rgb, 70, 0);
        add(FxType.TALISMAN_CIRCLE, pos, Vec3.ZERO, 5.5f * s, rgb, 90, 0);
        int n = Math.max(2, (int) (5 * q()));
        for (int k = 0; k < n; k++) add(FxType.SHOCKWAVE, pos, Vec3.ZERO, (4f + 3.2f * k) * s, k % 2 == 0 ? rgb : 0xFFFFFF, 22, 4 * k);
        add(FxType.SCREEN_TINT, pos, Vec3.ZERO, 0.55f, 0xFFFFFF, 26, 0);
        for (int i = 0; i < 70 * q(); i++) { // bùng khí
            double a = Math.random() * Math.PI * 2, sp = 0.15 + Math.random() * 0.5;
            puff(FxParticle.GLOW, pos.x, pos.y + 0.6, pos.z, Math.cos(a) * sp, 0.05 + Math.random() * 0.4, Math.sin(a) * sp, rgb, 0.5f, 40, -0.004f, true);
        }
        for (int i = 0; i < 50 * q(); i++) {
            double a = Math.random() * Math.PI * 2, r = 0.5 + Math.random() * 3.5;
            puff(FxParticle.STAR, pos.x + Math.cos(a) * r, pos.y + 0.1, pos.z + Math.sin(a) * r, 0, 0.12 + Math.random() * 0.25, 0, 0xFFFFFF, 0.35f, 50, -0.002f, true);
        }
    }

    private static void upgrade(Vec3 pos, float s, int rgb) {
        add(FxType.LIGHT_PILLAR, pos, Vec3.ZERO, 0.6f * s, rgb, 38, 0);
        add(FxType.TALISMAN_CIRCLE, pos, Vec3.ZERO, 1.7f * s, rgb, 44, 0);
        add(FxType.SHOCKWAVE, pos, Vec3.ZERO, 2.4f * s, rgb, 16, 0);
        add(FxType.SHOCKWAVE, pos, Vec3.ZERO, 3.6f * s, 0xFFFFFF, 18, 5);
        int n = (int) (44 * q());
        for (int i = 0; i < n; i++) {
            double a = i * 0.55, r = 0.4 + (i % 7) * 0.12;
            puff(FxParticle.STAR, pos.x + Math.cos(a) * r, pos.y + 0.1 + i * 0.03, pos.z + Math.sin(a) * r, 0, 0.09 + (i % 5) * 0.03, 0, i % 3 == 0 ? 0xFFFFFF : rgb, 0.3f, 36, 0f, true);
        }
    }

    // ---------------------------------------------------------------- hạt

    static int cap() { return (int) (700 * q()); }

    static void puff(SpriteSet set, double x, double y, double z, double vx, double vy, double vz, int rgb, float size, int life, float grav, boolean shrink) {
        puffA(set, x, y, z, vx, vy, vz, rgb, size, life, grav, shrink, 1f);
    }

    static void puffA(SpriteSet set, double x, double y, double z, double vx, double vy, double vz, int rgb, float size, int life, float grav, boolean shrink, float a) {
        Minecraft mc = Minecraft.getInstance();
        if (set == null || mc.level == null || ClientConfig.particles() == ClientConfig.ParticleLevel.OFF) return;
        if (FxParticle.COUNT.get() >= cap()) return;
        mc.particleEngine.add(new FxParticle(mc.level, x, y, z, vx, vy, vz, set, rgb, size, life, grav, shrink).a(a));
    }

    // ---------------------------------------------------------------- tick

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { ACTIVE.clear(); return; }
        if (mc.isPaused()) return;
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Inst f = ACTIVE.get(i);
            if (f.delay > 0) { f.delay--; continue; }
            if (!f.started) { f.started = true; FxEffects.start(f); }
            FxEffects.tick(f);
            if (++f.age >= f.life) ACTIVE.remove(i);
        }
    }

    // ---------------------------------------------------------------- vẽ

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource bs = mc.renderBuffers().bufferSource();
        VertexConsumer vc = bs.getBuffer(RenderType.lightning());
        FxDraw g = new FxDraw(vc, e.getModelViewMatrix(), e.getCamera().getPosition());
        for (Inst f : ACTIVE) {
            if (f.delay > 0 || !f.started) continue;
            if (f.pos.distanceToSqr(g.cam) > 200 * 200) continue;
            FxEffects.render(f, g, pt);
        }
        bs.endBatch(RenderType.lightning());
    }

    /** Phủ màu toàn màn (chớp sét, đột phá). Tôn trọng R26. */
    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Pre e) {
        if (ClientConfig.screenEffectsOff() || ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        float best = 0; int rgb = 0xFFFFFF;
        for (Inst f : ACTIVE) {
            if (f.type != FxType.SCREEN_TINT || !f.started) continue;
            float k = (f.age + pt) / f.life;
            double d = mc.player.position().distanceTo(f.pos);
            float a = f.scale * (1f - k) * (1f - k) * (float) Math.max(0, 1 - d / 40.0);
            if (a > best) { best = a; rgb = f.rgb; }
        }
        if (best <= 0.01f) return;
        e.getGuiGraphics().fill(0, 0, e.getGuiGraphics().guiWidth(), e.getGuiGraphics().guiHeight(),
                ((int) (Math.min(0.6f, best) * 255) << 24) | rgb);
    }
}
