package com.hukhong.client.fx;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import com.hukhong.client.fx.PhapBeamClient.Entry;
import com.hukhong.phaptu.PhapElement;
import com.hukhong.phaptu.beam.PhapBeamMath;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Map;
import java.util.Random;

import static com.hukhong.client.fx.FxDraw.col;
import static com.hukhong.client.fx.FxDraw.white;

/**
 * Dựng hình học của Tinh Hà Quang Trụ (cộng sáng, toạ độ thế giới trừ vị trí camera; PoseStack mới của sự kiện, KHÔNG nhân modelView):
 * nạp lực (vòng rune, ruy băng hội tụ, rune đất, quả cầu) và tia (lõi trắng, vầng sáng, vòng chạy dọc tia, xoắn ốc, xoáy thiên hà,
 * điểm chạm). Mỗi nguyên tố một phối màu + chi tiết riêng. Có Iris: hạt bù ở {@link PhapBeamClient}.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
final class PhapBeamRender {
    private PhapBeamRender() {}

    private static final double L = PhapBeamMath.RANGE;

    // ------------------------------------------------------------------ màu

    private static int main(PhapElement e) {
        return switch (e) { case LOI -> 0x7A63FF; case HOA -> 0xFF5A1E; case THUY -> 0x20D8C8; default -> 0xFFD34D; };
    }

    private static int core(PhapElement e) {
        return switch (e) { case LOI -> 0xEDE6FF; case HOA -> 0xFFE6B0; case THUY -> 0xE6FFFF; default -> 0xFFF8E0; };
    }

    private static int accent(PhapElement e) {
        return switch (e) { case LOI -> 0x4FC3FF; case HOA -> 0xFF2A00; case THUY -> 0x7FF0FF; default -> 0xFF7AD0; };
    }

    // ------------------------------------------------------------------ hình học

    static Vec3 perp(Vec3 d) {
        Vec3 ref = Math.abs(d.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        return d.cross(ref).normalize();
    }

    /** Hệ số bán kính theo khoảng cách từ đầu trượng: nhỏ ở gốc, loe ra trong ~5 block. */
    static double profile(double s) {
        double t = Mth.clamp(s / 5.0, 0, 1);
        return 0.22 + 0.78 * t * t * (3 - 2 * t);
    }

    private static double fade(double s, double now) {
        double t = Mth.clamp((s / L - 0.8) / 0.2, 0, 1);
        return (1 - t * t * (3 - 2 * t)) * (0.9 + 0.1 * Math.sin(s * 0.8 - now * 3));
    }

    private static Vec3 at(Vec3 c, Vec3 u, Vec3 v, double ang, double r) {
        return c.add(u.scale(Math.cos(ang) * r)).add(v.scale(Math.sin(ang) * r));
    }

    /** Vòng dải phẳng trong mặt (u, v) quanh c, r0..r1; skipEvery>0 => đứt nét. Hai mặt. */
    private static void ring(FxDraw g, Vec3 c, Vec3 u, Vec3 v, double r0, double r1, int segs, double rot, int cIn, int cOut, int skipEvery) {
        for (int i = 0; i < segs; i++) {
            if (skipEvery > 0 && i % skipEvery == skipEvery - 1) continue;
            double a0 = rot + Math.PI * 2 * i / segs, a1 = rot + Math.PI * 2 * (i + 1) / segs;
            g.quad2(at(c, u, v, a0, r0), at(c, u, v, a0, r1), at(c, u, v, a1, r1), at(c, u, v, a1, r0), cIn, cOut, cOut, cIn);
        }
    }

    /** Đĩa quạt tâm đậm -> mép nhạt trong mặt (u, v). */
    private static void disc(FxDraw g, Vec3 c, Vec3 u, Vec3 v, double r, int segs, int ci, int co) {
        for (int i = 0; i < segs; i++) {
            double a0 = Math.PI * 2 * i / segs, a1 = Math.PI * 2 * (i + 1) / segs;
            g.quad2(c, at(c, u, v, a0, r), at(c, u, v, a1, r), c, ci, co, co, ci);
        }
    }

    /** Đĩa hướng camera. */
    private static void billboard(FxDraw g, Vec3 c, double r, int segs, int ci, int co) {
        Vec3 view = g.cam.subtract(c);
        if (view.lengthSqr() < 1e-6) return;
        view = view.normalize();
        Vec3 u = perp(view);
        disc(g, c, u, view.cross(u), r, segs, ci, co);
    }

    /** Ống phát sáng dọc tia: bán kính rBase x profile, alpha dọc theo (mờ cuối), tuỳ chọn sọc xoắn. */
    private static void tube(FxDraw g, Vec3 o, Vec3 d, Vec3 u, Vec3 v, double rBase, int rgb, float alpha, int segs, int lenSegs,
                             double now, boolean stripe) {
        for (int j = 0; j < lenSegs; j++) {
            double s0 = L * j / lenSegs, s1 = L * (j + 1) / lenSegs;
            double r0 = rBase * profile(s0), r1 = rBase * profile(s1);
            float a0 = alpha * (float) fade(s0, now), a1 = alpha * (float) fade(s1, now);
            Vec3 c0 = o.add(d.scale(s0)), c1 = o.add(d.scale(s1));
            for (int i = 0; i < segs; i++) {
                double ang0 = Math.PI * 2 * i / segs, ang1 = Math.PI * 2 * (i + 1) / segs;
                float k0 = 1f, k1 = 1f;
                if (stripe) {
                    k0 = (float) (0.45 + 0.55 * Math.pow(Math.sin(ang0 * 1.5 + s0 * 0.25 - now * 0.35) * 0.5 + 0.5, 2));
                    k1 = (float) (0.45 + 0.55 * Math.pow(Math.sin(ang1 * 1.5 + s0 * 0.25 - now * 0.35) * 0.5 + 0.5, 2));
                }
                g.quad2(at(c0, u, v, ang0, r0), at(c0, u, v, ang1, r0), at(c1, u, v, ang1, r1), at(c1, u, v, ang0, r1),
                        col(rgb, a0 * k0), col(rgb, a0 * k1), col(rgb, a1 * k1), col(rgb, a1 * k0));
            }
        }
    }

    // ------------------------------------------------------------------ sự kiện vẽ

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (PhapBeamClient.ENTRIES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource bs = mc.renderBuffers().bufferSource();
        VertexConsumer vc = bs.getBuffer(RenderType.lightning());
        FxDraw g = new FxDraw(vc, e.getPoseStack().last().pose(), e.getCamera().getPosition());
        boolean any = false;
        for (Map.Entry<Integer, Entry> me : PhapBeamClient.ENTRIES.entrySet()) {
            Entity en = mc.level.getEntity(me.getKey());
            Entry st = me.getValue();
            if (en == null || en.position().distanceToSqr(g.cam) > 160 * 160) continue;
            g.gain = en == mc.player ? 1f : 0.5f;
            draw(g, mc, en, st, pt);
            any = true;
        }
        g.gain = 1f;
        if (any) bs.endBatch(RenderType.lightning());
    }

    private static void draw(FxDraw g, Minecraft mc, Entity en, Entry st, float pt) {
        double now = mc.level.getGameTime() + pt;
        double age = now - st.start;
        PhapElement el = st.el;
        Vec3 eye = en.getEyePosition(pt), look = en.getViewVector(pt);
        Vec3 org = PhapBeamClient.origin(en, pt);
        Vec3 dir = eye.add(look.scale(L)).subtract(org).normalize();
        Vec3 u = perp(dir), v = dir.cross(u);
        boolean firstPerson = en == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON;
        switch (st.phase) {
            case PhapBeamClient.CHARGING -> charge(g, en, el, org, dir, u, v, look, pt, now, Mth.clamp((float) (age / PhapBeamMath.CHARGE_TICKS), 0f, 1f), false, firstPerson);
            case PhapBeamClient.READY -> charge(g, en, el, org, dir, u, v, look, pt, now, 1f, true, firstPerson);
            case PhapBeamClient.FIRING -> beam(g, mc, en, el, org, dir, u, v, now, (float) Math.min(1, age / 3.0), 1f, firstPerson);
            case PhapBeamClient.FADE -> {
                float f = 1f - (float) ((now - st.fadeStart) / PhapBeamClient.FADE_TICKS);
                if (f > 0) beam(g, mc, en, el, org, dir, u, v, now, 1f, f * f, firstPerson);
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ nạp lực / sẵn sàng

    private static void charge(FxDraw g, Entity en, PhapElement el, Vec3 org, Vec3 dir, Vec3 u, Vec3 v, Vec3 look, float pt, double now,
                               float k, boolean ready, boolean firstPerson) {
        int mc_ = main(el), cc = core(el), ac = accent(el);
        float q = ClientConfig.fxDensity();
        float a = (ready ? 0.55f : 0.15f + 0.4f * k) * (firstPerson ? 0.75f : 1f);

        // rune đất dưới chân: hai vòng ngược chiều + sao 6 cánh
        Vec3 feet = en.getPosition(pt).add(0, 0.05, 0);
        double gr = 2.0 * (0.5 + 0.5 * k);
        g.flatRing(feet, gr - 0.12, gr, 40, now * 0.03, col(mc_, a * 0.9f), col(mc_, a * 0.2f), 0);
        g.flatRing(feet, gr * 0.62 - 0.1, gr * 0.62, 32, -now * 0.05, col(cc, a), col(mc_, a * 0.3f), 4);
        g.disc(feet, gr, 28, col(mc_, a * 0.30f), col(mc_, 0f));
        int pts = 6;
        for (int i = 0; i < pts; i++) {
            double a0 = now * 0.03 + Math.PI * 2 * i / pts, a1 = now * 0.03 + Math.PI * 2 * ((i + 2) % pts) / pts;
            Vec3 p0 = feet.add(Math.cos(a0) * gr * 0.62, 0, Math.sin(a0) * gr * 0.62), p1 = feet.add(Math.cos(a1) * gr * 0.62, 0, Math.sin(a1) * gr * 0.62);
            g.flatGlow(p0, p1, 0.03, cc, a);
        }

        // rune quay trước mặt: vuông góc hướng bắn, 3 vòng cách nhau
        if (!firstPerson || k > 0.4f) {
            double fa = a * (firstPerson ? 0.55f : 1f);
            for (int i = 0; i < 3; i++) {
                Vec3 c = org.add(dir.scale(1.5 + 0.55 * i));
                double r = (1.3 - 0.32 * i) * (0.6 + 0.4 * k);
                double rot = (i % 2 == 0 ? 1 : -1) * now * (0.05 + 0.02 * i);
                ring(g, c, u, v, r - 0.07, r, 36, rot, col(cc, (float) fa), col(mc_, (float) fa * 0.5f), i == 1 ? 3 : 0);
                if (i == 0) {
                    int n = 5 + (el.ordinal() % 3);
                    for (int s = 0; s < n; s++) {
                        double a0 = rot + Math.PI * 2 * s / n, a1 = rot + Math.PI * 2 * ((s + 2) % n) / n;
                        g.glowLine(at(c, u, v, a0, r * 0.95), at(c, u, v, a1, r * 0.95), 0.025, mc_, (float) fa);
                    }
                }
            }
        }

        // ruy băng hội tụ vào đầu trượng
        int strands = Math.max(3, Math.round(7 * q));
        for (int i = 0; i < strands; i++) {
            Vec3 prev = null;
            double ph = Math.PI * 2 * i / strands;
            for (int j = 0; j <= 10; j++) {
                double f = 1 - j / 10.0;
                double ff = Mth.clamp(f * (1.0 - 0.25 * Math.sin(now * 0.2 + i)), 0, 1);
                Vec3 p = org.add(dir.scale(f * 2.4)).add(u.scale(Math.cos(ph + f * 5.5 - now * 0.3) * ff * 2.8))
                        .add(v.scale(Math.sin(ph + f * 5.5 - now * 0.3) * ff * 2.8));
                if (prev != null) {
                    float al = a * (float) Math.sqrt(1 - f) * 1.4f;
                    g.ribbon(prev, p, 0.03 + 0.03 * (1 - f), 0.03 + 0.03 * (1 - f + 0.1), col(i % 2 == 0 ? cc : mc_, al), col(i % 2 == 0 ? cc : mc_, al));
                }
                prev = p;
            }
        }

        // quả cầu ở đầu trượng
        double pulse = 1 + 0.12 * Math.sin(now * (ready ? 0.9 : 0.5));
        double r = (0.1 + 0.28 * k) * pulse;
        billboard(g, org, r * 2.6, 14, col(mc_, 0.25f * (0.4f + k)), col(mc_, 0f));
        billboard(g, org, r * 1.4, 14, col(cc, 0.6f), col(mc_, 0.05f));
        billboard(g, org, r * 0.7, 12, col(0xFFFFFF, 0.95f), col(cc, 0.3f));
        if (ready) { // gai sao quay
            for (int i = 0; i < 4; i++) {
                double an = now * 0.1 + Math.PI * i / 2;
                Vec3 dv = u.scale(Math.cos(an)).add(v.scale(Math.sin(an)));
                g.glowLine(org.subtract(dv.scale(r * 3.2)), org.add(dv.scale(r * 3.2)), 0.03, ac, 0.8f);
            }
        }
    }

    // ------------------------------------------------------------------ tia

    private static void beam(FxDraw g, Minecraft mc, Entity en, PhapElement el, Vec3 o, Vec3 d, Vec3 u, Vec3 v, double now,
                             float grow, float fadeK, boolean firstPerson) {
        float q = ClientConfig.fxDensity();
        boolean hi = q >= 0.9f;
        int segs = q >= 0.9f ? 16 : q >= 0.6f ? 12 : 8;
        int lenSegs = q >= 0.9f ? 12 : q >= 0.6f ? 8 : 6;
        int mc_ = main(el), cc = core(el), ac = accent(el);
        double rad = PhapBeamMath.RADIUS * (0.94 + 0.06 * Math.sin(now * 1.7)) * grow * (0.35 + 0.65 * fadeK);
        float fk = fadeK * grow;

        // thứ tự trong -> ngoài: lõi ghi độ sâu trước, các vầng ngoài (gần camera hơn) vẫn qua được depth test
        tube(g, o, d, u, v, rad * 0.24, 0xFFFFFF, 0.95f * fk, segs, lenSegs, now, false);
        tube(g, o, d, u, v, rad * 0.42, cc, 0.55f * fk, segs, lenSegs, now, false);
        tube(g, o, d, u, v, rad * 0.70, mc_, 0.24f * fk, segs, lenSegs, now, el == PhapElement.HOA || el == PhapElement.QUANG);
        tube(g, o, d, u, v, rad, mc_, 0.12f * fk, segs, lenSegs, now, false);
        if (hi) tube(g, o, d, u, v, rad * 1.3, ac, 0.05f * fk, segs, lenSegs, now, true);
        if (el == PhapElement.QUANG && hi) { // quầng lăng kính
            tube(g, o, d, u, v, rad * 1.18, 0xFF5A8A, 0.045f * fk, segs, lenSegs, now, true);
            tube(g, o, d, u, v, rad * 1.30, 0x5AFF9A, 0.040f * fk, segs, lenSegs, now + 11, true);
            tube(g, o, d, u, v, rad * 1.42, 0x5A8AFF, 0.035f * fk, segs, lenSegs, now + 23, true);
        }
        if (el == PhapElement.HOA && hi) tube(g, o, d, u, v, rad * 1.15, 0xFF3010, 0.07f * fk, segs, lenSegs, now * 1.6, true);

        // vòng chạy dọc tia
        int rings = Math.max(4, Math.round((el == PhapElement.THUY ? 12 : 8) * q));
        for (int i = 0; i < rings; i++) {
            double s = (now * 1.8 + i * L / rings) % L;
            double r = rad * profile(s) * 1.1 + (el == PhapElement.THUY ? 0.12 * Math.sin(s * 0.8 + now * 0.3) : 0.0);
            float al = 0.5f * fk * (float) fade(s, now);
            Vec3 c = o.add(d.scale(s));
            ring(g, c, u, v, r, r + 0.13 + 0.08 * profile(s), 20, now * 0.08, col(i % 2 == 0 ? cc : mc_, al), col(mc_, al * 0.3f), 0);
        }

        // xoắn ốc
        int strands = hi ? 3 : 2;
        for (int st = 0; st < strands; st++) {
            Vec3 prev = null;
            double step = hi ? 1.0 : 1.6;
            for (double s = 0.8; s <= L; s += step) {
                double ang = s * 0.5 - now * 0.5 + Math.PI * 2 * st / strands;
                double r = rad * profile(s) * 1.04;
                Vec3 p = o.add(d.scale(s)).add(u.scale(Math.cos(ang) * r)).add(v.scale(Math.sin(ang) * r));
                if (prev != null) {
                    float al = 0.6f * fk * (float) fade(s, now);
                    int cs = el == PhapElement.QUANG ? (st == 0 ? 0xFF7AD0 : st == 1 ? 0x7AFFB0 : 0x7AB0FF) : st % 2 == 0 ? cc : ac;
                    g.ribbon(prev, p, 0.07 * profile(s), 0.07 * profile(s), col(cs, al), col(cs, al));
                }
                prev = p;
            }
        }

        // Lôi: hồ quang đứt gãy dọc tia
        if (el == PhapElement.LOI) {
            int arcs = Math.max(3, Math.round(7 * q));
            Random rnd = new Random((long) (now * 1.6) * 7919L);
            for (int a = 0; a < arcs; a++) {
                double s0 = rnd.nextDouble() * (L - 8);
                double ang = rnd.nextDouble() * Math.PI * 2;
                Vec3 prev = null;
                for (int j = 0; j < 8; j++) {
                    double s = s0 + j * 0.9;
                    double r = rad * profile(s) * (0.5 + rnd.nextDouble() * 1.0);
                    double an = ang + (rnd.nextDouble() - 0.5) * 1.6;
                    Vec3 p = o.add(d.scale(s)).add(u.scale(Math.cos(an) * r)).add(v.scale(Math.sin(an) * r));
                    if (prev != null) g.glowLine(prev, p, 0.035, a % 2 == 0 ? 0xB8A8FF : 0x8FE0FF, 0.9f * fk);
                    prev = p;
                }
            }
        }

        // xoáy thiên hà ở gốc
        float vs = firstPerson ? 0.5f : 1f;
        Vec3 vc = o.add(d.scale(0.7));
        double vr = (1.5 + 0.2 * Math.sin(now * 0.6)) * vs * grow;
        disc(g, vc, u, v, vr, 24, col(0xFFFFFF, 0.5f * fk * vs), col(mc_, 0f));
        for (int arm = 0; arm < 4; arm++) {
            Vec3 prev = null;
            for (int j = 0; j <= 10; j++) {
                double rr = vr * (0.08 + 0.92 * j / 10.0);
                double ang = arm * Math.PI / 2 + rr * 2.2 / Math.max(0.5, vs) - now * 0.4;
                Vec3 p = at(vc, u, v, ang, rr);
                if (prev != null) {
                    float al = (1 - j / 11f) * 0.8f * fk * vs;
                    g.ribbon(prev, p, 0.07 * (1 - j / 12.0) * vs, 0.07 * (1 - (j + 1) / 12.0) * vs, col(arm % 2 == 0 ? cc : mc_, al), col(arm % 2 == 0 ? cc : mc_, al * 0.6f));
                }
                prev = p;
            }
        }
        ring(g, vc, u, v, vr * 0.95, vr, 28, now * 0.1, col(cc, 0.5f * fk * vs), col(mc_, 0.2f * fk * vs), 3);
        billboard(g, o, 0.5 * vs, 12, col(0xFFFFFF, 0.9f * fk), col(cc, 0.1f));

        // điểm chạm
        BlockHitResult hit = PhapBeamClient.clip(mc, en, o, o.add(d.scale(L)));
        if (hit != null) {
            Vec3 h = hit.getLocation();
            Vec3 n = new Vec3(hit.getDirection().getStepX(), hit.getDirection().getStepY(), hit.getDirection().getStepZ());
            Vec3 hu = perp(n), hv = n.cross(hu);
            Vec3 hp = h.add(n.scale(0.04));
            float pul = 0.8f + 0.2f * (float) Math.sin(now * 0.9);
            disc(g, hp, hu, hv, 3.2 * grow, 28, col(cc, 0.45f * fk * pul), col(mc_, 0f));
            double ex = 0.8 + ((now * 0.12) % 1.0) * 3.2;
            ring(g, hp, hu, hv, ex - 0.14, ex, 32, 0, col(cc, 0.6f * fk * (float) (1 - (ex - 0.8) / 3.2)), col(mc_, 0.2f * fk), 0);
            ring(g, hp, hu, hv, 1.5, 1.62, 24, now * 0.07, col(white(mc_, 0.5f), 0.55f * fk), col(mc_, 0.2f * fk), 3);
            billboard(g, h.add(n.scale(0.3)), 1.6 * grow * pul, 16, col(0xFFFFFF, 0.8f * fk), col(mc_, 0f));
            billboard(g, h.add(n.scale(0.3)), 2.6 * grow, 16, col(mc_, 0.25f * fk), col(mc_, 0f));
        }
    }
}
