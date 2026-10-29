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

    /** Bit cờ trong FxPayload.life: hiệu ứng của người chơi khác (vẽ 50% độ đậm). */
    public static final int DIM_BIT = 1 << 30;
    /** Hệ số độ đậm của hiệu ứng đang start/tick (dùng trong puffA). */
    static float GAIN = 1f;
    static final List<Inst> ACTIVE = new ArrayList<>();

    /** Một hiệu ứng đang chạy. */
    static final class Inst {
        FxType type; Vec3 pos, dir; float scale; int rgb, life, age, delay; boolean started; final Random rnd = new Random();
        List<Vec3[]> segs; List<Float> segW;          // sét / nứt đất
        float[] sx, sz, sd; boolean[] landed;           // mưa kiếm
        float tilt;
        int owner = -1;          // >=0: bám theo thực thể (Kiếm Vực đi cùng chủ)
        int stop = -1;           // tick bắt đầu tắt dần (Kiếm Vực khi tắt kỹ năng)
        float roll;              // SWORD_QI: góc nghiêng của dải kiếm khí (độ)
        boolean dim;             // hiệu ứng của người chơi khác
        int kind, mode, flight;  // RIBBON/DECAL: biến thể
        float amp, arc;
        Object data;             // trạng thái riêng (vệt, hình học dựng sẵn)
        Inst(FxType t, Vec3 p, Vec3 d, float s, int rgb, int life) {
            type = t; pos = p; dir = d; scale = s <= 0 ? 1f : s; this.rgb = rgb; this.life = life;
        }
    }

    static float q() {
        return ClientConfig.fxDensity();
    }

    static int defColor(FxType t) {
        return switch (t) {
            case SWORD_CRESCENT, SWORD_QI -> 0x9FE8FF; case SWORD_RAIN -> 0xBFEFFF; case SWORD_DOMAIN -> 0x7FD8FF;
            case TALISMAN_CIRCLE -> 0xFFD866; case LIGHT_PILLAR -> 0xFFE9A0; case GROUND_CRACK -> 0xFF8A30;
            case SHOCKWAVE -> 0xFFC070; case BLOOD_VORTEX -> 0xC01028; case DEMON_SILHOUETTE -> 0x8A0A1E;
            case LIGHTNING_BOLT -> 0xB8D8FF; case TELEGRAPH -> 0xA050FF; case STORM_CLOUD -> 0x1C1C2C;
            case SCREEN_TINT -> 0xFFFFFF; case BEAM -> 0xFFE9A0; default -> 0xFFE9A0;
        };
    }

    static int defLife(FxType t) {
        return switch (t) {
            case SWORD_CRESCENT -> 11; case SWORD_QI -> 11; case SWORD_RAIN -> 46; case SWORD_DOMAIN -> 80; case TALISMAN_CIRCLE -> 50;
            case LIGHT_PILLAR -> 32; case GROUND_CRACK -> 50; case SHOCKWAVE -> 18; case BLOOD_VORTEX -> 40;
            case DEMON_SILHOUETTE -> 40; case LIGHTNING_BOLT -> 14; case TELEGRAPH -> 30; case STORM_CLOUD -> 80;
            case SCREEN_TINT -> 14; case BEAM -> 9; default -> 30;
        };
    }

    // ---------------------------------------------------------------- nhận

    public static void receive(FxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        boolean dim = (p.life() & DIM_BIT) != 0;
        if (dim) p = new FxPayload(p.fx(), p.x(), p.y(), p.z(), p.dx(), p.dy(), p.dz(), p.scale(), p.color(), p.life() & ~DIM_BIT);
        FxType t = FxType.of(p.fx());
        Vec3 pos = new Vec3(p.x(), p.y(), p.z()), dir = new Vec3(p.dx(), p.dy(), p.dz());
        if (t == FxType.SWORD_FLY) {
            com.hukhong.client.render.orbit.OrbitState.addSkill(pos, dir, p.life(), p.color() >>> 24, p.scale());
            return;
        }
        if (t == FxType.SWORD_STAB) { stab(pos, dir, p); return; }
        if (t == FxType.SWORD_SLASH) {
            com.hukhong.client.render.orbit.OrbitState.addSlash((int) p.dx(), p.dy(), p.dz(), p.color() >>> 24, p.life() > 0 ? p.life() : 5, p.scale());
            return;
        }
        if (t == FxType.SWORD_DOMAIN && p.dx() > 0.5f) { domain(p); return; }
        int rgb = p.color() == 0 ? defColor(t) : p.color() & 0xFFFFFF;
        float sc = p.scale() <= 0 ? 1f : p.scale();
        if (t == FxType.BREAKTHROUGH) { breakthrough(pos, sc, rgb); return; }
        if (t == FxType.UPGRADE_BURST) { upgrade(pos, sc, rgb); return; }
        if (t == FxType.CAMERA_SHAKE) { FxHit.shake(pos, sc, p.life() > 0 ? p.life() : 10, p.dx() > 0 ? p.dx() : 24f); return; }
        if (t == FxType.RIBBON) { FxTrail.receive(p, pos, dir, sc, rgb, dim); return; }
        if (t == FxType.ELEM) { FxElem.receive(p, pos, dir, sc, rgb, dim); return; }
        if (t == FxType.DECAL) { FxDecal.receive(p, pos, dir, sc, rgb, dim); return; }
        if (t == FxType.GIANT_SWORD) { FxGiantSword.receive(p, pos, dir, sc, rgb, dim); return; }
        if (t == FxType.RIFT) { FxDecal.receiveRift(p, pos, dir, sc, rgb, dim); return; }
        if (t == FxType.SHELL) { FxDecal.receiveShell(p, pos, dir, sc, rgb, dim); return; }
        Inst in = add(t, pos, dir, sc, rgb, p.life() > 0 ? p.life() : defLife(t), 0);
        in.dim = dim;
        if (t == FxType.SWORD_QI) in.roll = (p.color() >>> 24) / 1.4167f - 90f;
    }

    /** Kiếm Vực: một thể duy nhất theo chủ (dx = id chủ); gói làm tươi giữ nó sáng, scale <= 0 = tắt dần. */
    private static void domain(FxPayload p) {
        int owner = (int) p.dx();
        int rgb = p.color() == 0 ? defColor(FxType.SWORD_DOMAIN) : p.color() & 0xFFFFFF;
        for (Inst i : ACTIVE) {
            if (i.type != FxType.SWORD_DOMAIN || i.owner != owner) continue;
            if (p.scale() <= 0) { if (i.stop < 0) i.stop = i.age; return; }
            i.stop = -1; i.scale = p.scale(); i.rgb = rgb;
            i.age = Math.min(i.age, 6);
            i.life = i.age + Math.max(8, p.life());
            return;
        }
        if (p.scale() <= 0) return;
        Inst i = add(FxType.SWORD_DOMAIN, new Vec3(p.x(), p.y(), p.z()), Vec3.ZERO, p.scale(), rgb, Math.max(8, p.life()) + 4, 0);
        i.owner = owner;
    }

    /** Kiếm cắm đất: kiếm rơi mũi chúc xuống, nằm lại vài nhịp rồi mờ; chạm đất có bụi + vết nứt nhỏ rồi tan. */
    private static void stab(Vec3 from, Vec3 dir, FxPayload p) {
        int delay = p.color() >>> 24, flight = Math.max(2, p.life());
        com.hukhong.client.render.orbit.OrbitState.addSkill(from, dir, flight, delay, p.scale(), STAB_HOLD);
        if (q() >= 0.7f) add(FxType.GROUND_CRACK, from.add(dir).add(0, 0.02, 0), Vec3.ZERO, 0.8f, 0xA8BCC8, 40, delay + flight);
    }

    static final int STAB_HOLD = 30;

    /** Vệt kiếm khí lưỡi liềm nhỏ từ a tới b (đòn đánh thường tầm xa). */
    public static void qi(Vec3 a, Vec3 b, float radius, int rgb, int delay, int life) {
        Vec3 d = b.subtract(a);
        if (d.lengthSqr() < 0.25) return;
        Inst i = add(FxType.SWORD_QI, a.add(d.normalize().scale(0.8)), d.subtract(d.normalize().scale(0.8)), radius, rgb, Math.max(3, life), delay);
        i.roll = 20f;
    }

    static Inst add(FxType t, Vec3 pos, Vec3 dir, float sc, int rgb, int life, int delay) {
        Inst i = new Inst(t, pos, dir, sc, rgb, life);
        i.delay = delay;
        if (ACTIVE.size() >= ClientConfig.fxMaxActive()) ACTIVE.remove(0);
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

    static int cap() { return ClientConfig.fxParticleBudget(); }

    static void puff(SpriteSet set, double x, double y, double z, double vx, double vy, double vz, int rgb, float size, int life, float grav, boolean shrink) {
        puffA(set, x, y, z, vx, vy, vz, rgb, size, life, grav, shrink, 1f);
    }

    static void puffA(SpriteSet set, double x, double y, double z, double vx, double vy, double vz, int rgb, float size, int life, float grav, boolean shrink, float a) {
        Minecraft mc = Minecraft.getInstance();
        if (set == null || mc.level == null || ClientConfig.particles() == ClientConfig.ParticleLevel.OFF) return;
        if (FxParticle.COUNT.get() >= cap()) return;
        if (GAIN < 0.99f) { // hiệu ứng người khác: bỏ ~nửa hạt, phần còn lại mờ hơn
            if (mc.level.random.nextFloat() > GAIN) return;
            a *= GAIN + 0.2f;
        }
        if (mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(x, y, z) > (double) ClientConfig.fxLod() * ClientConfig.fxLod() * 1.5) return;
        mc.particleEngine.add(new FxParticle(mc.level, x, y, z, vx, vy, vz, set, rgb, size, life, grav, shrink).a(a));
    }

    // ---------------------------------------------------------------- tick

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { ACTIVE.clear(); FxHit.clear(); FxGiantSword.clear(); return; }
        if (mc.isPaused()) return;
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Inst f = ACTIVE.get(i);
            if (f.delay > 0) { f.delay--; continue; }
            if (f.owner >= 0) follow(mc, f, 1f);
            GAIN = f.dim ? 0.5f : 1f;
            if (!f.started) { f.started = true; FxEffects.start(f); }
            FxEffects.tick(f);
            GAIN = 1f;
            if (++f.age >= f.life || (f.stop >= 0 && f.age >= f.stop + 8)) ACTIVE.remove(i);
        }
    }

    /** Bám thực thể chủ (nội suy theo partial tick) - toạ độ thế giới. */
    private static void follow(Minecraft mc, Inst f, float pt) {
        var en = mc.level.getEntity(f.owner);
        if (en != null) f.pos = en.getPosition(pt);
    }

    // ---------------------------------------------------------------- vẽ

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (ACTIVE.isEmpty() && !FxHit.hasFlash() && !FxGiantSword.any()) return;
        Minecraft mc = Minecraft.getInstance();
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource bs = mc.renderBuffers().bufferSource();
        VertexConsumer vc = bs.getBuffer(RenderType.lightning());
        FxDraw g = new FxDraw(vc, e.getPoseStack().last().pose(), e.getCamera().getPosition());
        double lod = ClientConfig.fxLod();
        for (Inst f : ACTIVE) {
            if (f.delay > 0 || !f.started) continue;
            if (f.owner >= 0) follow(mc, f, pt);
            double dsq = f.pos.distanceToSqr(g.cam);
            if (dsq > 200 * 200) continue;
            if (dsq > lod * lod && f.scale < 4f) continue; // LOD: hiệu ứng nhỏ ở xa không vẽ
            g.gain = f.dim ? 0.5f : 1f;
            FxEffects.render(f, g, pt);
        }
        g.gain = 1f;
        FxHit.renderFlash(g, pt);
        bs.endBatch(RenderType.lightning());
        FxGiantSword.renderItems(e, mc, bs, pt);
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
