package com.hukhong.client.fx;

import com.hukhong.banmenh.BanMenhKiemItem;
import com.hukhong.client.fx.FxClient.Inst;
import com.hukhong.client.render.orbit.OrbitRenderer;
import com.hukhong.network.FxPayload;
import com.hukhong.vfx.FxType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static com.hukhong.client.fx.FxDraw.col;
import static com.hukhong.client.fx.FxDraw.white;

/**
 * F3 - Cự kiếm: model item (Bản Mệnh Kiếm của người chơi gần nhất đang cầm, mặc định kiếm sắt) phóng to, sáng tối đa,
 * kèm viền/hào quang cộng sáng dọc thân. Nhịp: lơ lửng + tụ lực -> giáng nhanh 3 tick -> cắm lại rung tắt dần -> mờ.
 * Gói: pos = mũi kiếm khi cắm, scale = cỡ (1 cỡ ~ 1.3 block thân), color = rgb | impactTick<<24, life = tổng tick.
 * Va chạm (tick = impactTick): sóng xung kích, vết nứt, rung camera, chớp trắng.
 */
final class FxGiantSword {
    private FxGiantSword() {}

    static void receive(FxPayload pl, Vec3 pos, Vec3 dir, float sc, int rgb, boolean dim) {
        int impact = pl.color() >>> 24;
        if (impact <= 0) impact = 14;
        float size = sc <= 0 ? 6f : sc;
        int life = pl.life() > 0 ? pl.life() : impact + 34;
        Inst i = FxClient.add(FxType.GIANT_SWORD, pos, dir, size, rgb, life, 0);
        i.dim = dim; i.flight = impact;
        Minecraft mc = Minecraft.getInstance();
        ItemStack st = new ItemStack(Items.IRON_SWORD);
        if (mc.level != null) {
            double best = 40 * 40;
            for (Player p : mc.level.players()) {
                double d = p.distanceToSqr(pos);
                if (d >= best) continue;
                ItemStack h = OrbitRenderer.heldSword(p);
                if (!h.isEmpty()) { best = d; st = h.copyWithCount(1); }
            }
        }
        i.data = st;
        // vùng nguy hiểm dưới mũi kiếm: rune lấp sáng dần tới lúc giáng
        Inst r = FxClient.add(FxType.DECAL, pos, Vec3.ZERO, Math.max(2.5f, size * 0.55f), rgb, impact + 18, 0);
        r.dim = dim; r.kind = FxDecal.RUNE;
    }

    static boolean any() {
        for (Inst f : FxClient.ACTIVE) if (f.type == FxType.GIANT_SWORD) return true;
        return false;
    }

    static void clear() {}

    // ---- trạng thái theo tick

    /** Độ cao mũi kiếm so với điểm cắm. */
    private static double height(Inst f, float t) {
        int I = f.flight;
        double H = 6 + f.scale * 1.7;
        if (t < I - 3) return H + Math.sin(t * 0.3) * 0.12 * f.scale;
        if (t < I) {
            float u = (t - (I - 3)) / 3f;
            return H * (1 - u * u);
        }
        return 0;
    }

    static void start(Inst f) {}

    static void tick(Inst f) {
        int I = f.flight;
        float q = FxClient.q();
        if (f.age < I - 3 && f.age % 2 == 0) { // tụ lực: tinh quang rơi quanh thân
            double h = height(f, f.age);
            double a = f.rnd.nextDouble() * Math.PI * 2, r = f.scale * (0.1 + 0.2 * f.rnd.nextDouble());
            FxClient.puffA(FxParticle.STAR, f.pos.x + Math.cos(a) * r, f.pos.y + h + f.rnd.nextDouble() * f.scale, f.pos.z + Math.sin(a) * r,
                    0, -0.05, 0, f.rgb, 0.25f, 14, 0f, true, 0.8f);
        }
        if (f.age == I) {
            Vec3 p = f.pos;
            float s = f.scale;
            FxClient.add(FxType.SHOCKWAVE, p, Vec3.ZERO, s * 1.3f, f.rgb, 18, 0).dim = f.dim;
            FxClient.add(FxType.SHOCKWAVE, p, Vec3.ZERO, s * 2.1f, 0xFFFFFF, 22, 3).dim = f.dim;
            Inst c = FxClient.add(FxType.DECAL, p, new Vec3(4, 0, 0), s * 1.25f, f.rgb, 56, 0);
            c.kind = FxDecal.CRACK; c.dim = f.dim;
            Inst sc = FxClient.add(FxType.SCREEN_TINT, p, Vec3.ZERO, f.dim ? 0.15f : 0.35f, 0xFFFFFF, 8, 0);
            sc.dim = f.dim;
            FxHit.shake(p, Math.min(2.2f, 0.7f + s * 0.12f), 12, 36f);
            int n = (int) (26 * q * (f.dim ? 0.5 : 1));
            for (int i = 0; i < n; i++) {
                double a = f.rnd.nextDouble() * Math.PI * 2, sp = 0.15 + f.rnd.nextDouble() * 0.4;
                FxClient.puffA(FxParticle.STAR, p.x, p.y + 0.2, p.z, Math.cos(a) * sp, 0.12 + f.rnd.nextDouble() * 0.35, Math.sin(a) * sp, i % 3 == 0 ? 0xFFFFFF : f.rgb, 0.3f, 22, 0.03f, true, 0.9f);
            }
        }
    }

    /** Cường độ viền sáng theo pha. */
    private static float glow(Inst f, float t) {
        int I = f.flight;
        if (t < I) return 0.25f + 0.55f * t / I; // tụ lực dần sáng
        float k = (t - I) / Math.max(1f, f.life - I);
        return (float) (0.9 * Math.pow(1 - k, 1.6) + 0.1);
    }

    private static float alpha(Inst f, float t) {
        float in = Math.min(1f, t / Math.max(3f, f.flight * 0.35f));
        float out = Math.min(1f, (f.life - t) / 12f);
        return Math.max(0f, Math.min(in, out));
    }

    static void render(Inst f, FxDraw g, float pt) {
        float t = f.age + pt;
        float a = alpha(f, t), gl = glow(f, t) * a;
        if (a <= 0.01f) return;
        double S = f.scale, h = height(f, t);
        Vec3 tip = f.pos.add(0, h, 0);
        Vec3 hilt = tip.add(0, S * 1.32, 0);
        int c = f.rgb;
        double w = 0.05 * S;
        g.ribbon(tip, hilt, w * 5, w * 5, col(c, 0.12f * gl), col(c, 0.04f * gl));  // hào quang
        g.glowLine(tip, tip.add(0, S * 1.05, 0), w * 0.9, c, gl);                     // lõi sáng dọc thân
        if (t > f.flight - 4 && t < f.flight + 6) { // vệt rơi
            g.ribbon(tip, tip.add(0, S * 2.6, 0), w * 3, w * 0.5, col(white(c, 0.6f), 0.5f * gl), col(c, 0f));
        }
    }

    // ---- vẽ model

    private static Quaternionf orient(Vec3 dir, Vec3 toCam) {
        Vector3f u = new Vector3f((float) dir.x, (float) dir.y, (float) dir.z).normalize();
        Vector3f c = new Vector3f((float) toCam.x, (float) toCam.y, (float) toCam.z);
        Vector3f n = c.sub(new Vector3f(u).mul(c.dot(u)));
        if (n.lengthSquared() < 1.0E-6f) n = u.cross(1, 0, 0, new Vector3f());
        n.normalize();
        Vector3f x = u.cross(n, new Vector3f());
        return new Quaternionf().setFromNormalized(new Matrix3f(x, u, n));
    }

    private static final class Fade implements MultiBufferSource {
        private final MultiBufferSource.BufferSource src; private final float alpha;
        Fade(MultiBufferSource.BufferSource src, float alpha) { this.src = src; this.alpha = alpha; }
        @Override public VertexConsumer getBuffer(RenderType rt) {
            return new Alpha(src.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS)), alpha);
        }
    }

    private static final class Alpha implements VertexConsumer {
        private final VertexConsumer d; private final float a;
        Alpha(VertexConsumer d, float a) { this.d = d; this.a = a; }
        @Override public VertexConsumer addVertex(float x, float y, float z) { d.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int al) { d.setColor(r, g, b, (int) (al * a)); return this; }
        @Override public VertexConsumer setUv(float u, float v) { d.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { d.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { d.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { d.setNormal(x, y, z); return this; }
    }

    /** Vẽ mesh kiếm bằng model item, sáng tối đa (không phụ thuộc ánh sáng thế giới). */
    static void renderItems(RenderLevelStageEvent e, Minecraft mc, MultiBufferSource.BufferSource bs, float pt) {
        ClientLevel level = mc.level;
        if (level == null) return;
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        boolean drew = false;
        for (Inst f : FxClient.ACTIVE) {
            if (f.type != FxType.GIANT_SWORD || f.delay > 0 || !f.started || !(f.data instanceof ItemStack st)) continue;
            float t = f.age + pt, a = alpha(f, t);
            if (a <= 0.02f) continue;
            double S = f.scale, h = height(f, t);
            // rung nhẹ khi vừa cắm
            double shake = t > f.flight ? Math.exp(-(t - f.flight) * 0.45) * 0.05 * S : 0;
            Vec3 center = f.pos.add(Math.sin(t * 5) * shake, h + S * 0.66, Math.cos(t * 7) * shake);
            Vec3 toCam = cam.subtract(center);
            float scale = (float) (S * (t < f.flight ? 0.85 + 0.15 * Math.min(1, t / (f.flight * 0.5)) : 1));
            MultiBufferSource src = a >= 0.97f ? bs : new Fade(bs, a);
            ps.pushPose();
            ps.translate(center.x - cam.x, center.y - cam.y, center.z - cam.z);
            ps.mulPose(orient(new Vec3(0, -1, 0), toCam));
            ps.mulPose(Axis.ZP.rotationDegrees(45f));
            ps.scale(scale, scale, scale);
            mc.getItemRenderer().renderStatic(st, ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.NO_OVERLAY, ps, src, level, f.age);
            ps.popPose();
            drew = true;
        }
        if (drew) bs.endBatch();
    }
}
