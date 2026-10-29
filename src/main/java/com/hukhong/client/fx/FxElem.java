package com.hukhong.client.fx;

import com.hukhong.client.fx.FxClient.Inst;
import com.hukhong.network.FxPayload;
import com.hukhong.vfx.FxType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

import static com.hukhong.client.fx.FxDraw.col;
import static com.hukhong.client.fx.FxDraw.white;

/**
 * VFX riêng từng nguyên tố của Pháp Tu. Mỗi kind là một HÌNH khác hẳn nhau (không chỉ đổi màu):
 * SPIKES gai đá (Thổ), HALOS vòng hào quang + sao lăng kính (Quang), ARC sét gấp khúc (Lôi),
 * TONGUES lưỡi lửa (Hỏa), TIDE bức sóng (Thủy), NEEDLES phi kiếm/kim bạc (Kim), BOULDER tảng đá khổng lồ (Thổ T).
 * Kind nằm ở byte cao của color. dir = vector (độ dài = tầm); dir ~ 0 thì toả tròn quanh pos bán kính scale.
 */
final class FxElem {
    private FxElem() {}

    static final int SPIKES = 0, HALOS = 1, ARC = 2, TONGUES = 3, TIDE = 4, NEEDLES = 5, BOULDER = 6;

    static int defLife(int kind) {
        return switch (kind) { case SPIKES -> 34; case HALOS -> 14; case ARC -> 8; case TONGUES -> 30; case TIDE -> 16; case NEEDLES -> 10; default -> 30; };
    }

    static void receive(FxPayload pl, Vec3 pos, Vec3 dir, float sc, int rgb, boolean dim) {
        int kind = pl.color() >>> 24;
        Inst i = FxClient.add(FxType.ELEM, pos, dir, sc, rgb, pl.life() > 0 ? pl.life() : defLife(kind), 0);
        i.dim = dim;
        i.kind = kind;
    }

    private static Vec3[] axes(Vec3 d) {
        Vec3 f = d.lengthSqr() < 1e-8 ? new Vec3(0, 0, 1) : d.normalize();
        Vec3 u = Math.abs(f.y) > 0.95 ? new Vec3(1, 0, 0) : f.cross(new Vec3(0, 1, 0)).normalize();
        return new Vec3[]{f, u, f.cross(u).normalize()};
    }

    private static boolean radial(Inst f) { return f.dir.lengthSqr() < 1e-4; }

    /** Vị trí phần tử thứ i/n: trên đoạn pos->pos+dir, hoặc trên vòng tròn khi toả tròn. */
    private static Vec3 slot(Inst f, int i, int n) {
        if (radial(f)) {
            double a = Math.PI * 2 * i / n, rr = f.scale * (f.kind == SPIKES ? 1.9 : 1.0); // gai đá mọc thành vành ngoài tầm mắt chủ
            return f.pos.add(Math.cos(a) * rr, 0, Math.sin(a) * rr);
        }
        return f.pos.add(f.dir.scale((i + 0.5) / n));
    }

    private static int count(Inst f) {
        return radial(f) ? 8 : Math.max(3, Math.min(24, (int) Math.ceil(f.dir.length())));
    }

    // ---------------------------------------------------------------- start / tick (hạt)

    static void start(Inst f) {
        f.rnd.setSeed(f.pos.hashCode() * 31L + f.kind);
    }

    static void tick(Inst f) {
        float q = FxClient.q();
        Random r = f.rnd;
        switch (f.kind) {
            case SPIKES -> {
                int n = count(f);
                int span = Math.max(1, f.life / 3);
                if (f.age <= span) {
                    int i = radial(f) ? f.age % n : Math.min(n - 1, f.age * n / span);
                    Vec3 p = slot(f, i, n);
                    for (int k = 0; k < 3 * q; k++)
                        FxClient.puffA(FxParticle.SHARD, p.x + (r.nextDouble() - .5) * .5, p.y + .1, p.z + (r.nextDouble() - .5) * .5,
                                (r.nextDouble() - .5) * .12, .12 + r.nextDouble() * .18, (r.nextDouble() - .5) * .12, 0x6A5030, .1f, 16, .05f, false, .9f);
                    FxClient.puffA(FxParticle.GLOW, p.x, p.y + .25, p.z, 0, .02, 0, white(f.rgb, .15f), 1.1f * Math.min(2f, f.scale), 22, -.002f, false, .28f);
                }
            }
            case TONGUES -> {
                if (f.age % 2 == 0) {
                    int n = Math.max(1, (int) (3 * q));
                    for (int k = 0; k < n; k++) {
                        Vec3 p = radial(f) ? f.pos.add((r.nextDouble() * 2 - 1) * f.scale, 0.1, (r.nextDouble() * 2 - 1) * f.scale)
                                : f.pos.add(f.dir.scale(r.nextDouble())).add((r.nextDouble() - .5) * .8, .1, (r.nextDouble() - .5) * .8);
                        FxClient.puffA(FxParticle.STAR, p.x, p.y, p.z, (r.nextDouble() - .5) * .06, .08 + r.nextDouble() * .12, (r.nextDouble() - .5) * .06,
                                r.nextBoolean() ? 0xFF8A20 : 0xFFD040, .16f, 24, -.003f, true, .95f);
                    }
                }
            }
            case TIDE -> {
                Vec3[] ax = axes(f.dir);
                float k = (f.age + 1f) / f.life;
                Vec3 head = f.pos.add(f.dir.scale(k));
                int n = (int) (6 * q);
                for (int i = 0; i < n; i++) {
                    double w = (r.nextDouble() * 2 - 1) * f.scale;
                    Vec3 p = head.add(ax[1].scale(w)).add(0, .6 + r.nextDouble() * .9, 0);
                    FxClient.puffA(r.nextInt(3) == 0 ? FxParticle.GLOW : FxParticle.STAR, p.x, p.y, p.z, ax[0].x * .12, .1, ax[0].z * .12,
                            r.nextInt(3) == 0 ? 0xFFFFFF : f.rgb, .2f, 18, .04f, true, .9f);
                }
            }
            case NEEDLES -> {
                if (f.age < 3 && q >= .5f)
                    for (int i = 0; i < 5; i++)
                        FxClient.puffA(FxParticle.SHARD, f.pos.x, f.pos.y, f.pos.z, (r.nextDouble() - .5) * .3, (r.nextDouble() - .5) * .2, (r.nextDouble() - .5) * .3, 0xFFFFFF, .14f, 10, 0f, true, 1f);
            }
            case BOULDER -> {
                int hit = f.life * 2 / 3;
                if (f.age == hit) {
                    float R = f.scale;
                    for (int i = 0; i < 40 * q; i++) {
                        double a = r.nextDouble() * Math.PI * 2, s = .15 + r.nextDouble() * .45 * (R / 4);
                        FxClient.puffA(FxParticle.GLOW, f.pos.x, f.pos.y + .3, f.pos.z, Math.cos(a) * s, .02 + r.nextDouble() * .06, Math.sin(a) * s, 0x7A6444, 1.4f + r.nextFloat(), 34, 0f, false, .3f);
                    }
                    for (int i = 0; i < 36 * q; i++) {
                        double a = r.nextDouble() * Math.PI * 2, s = .1 + r.nextDouble() * .4;
                        FxClient.puffA(FxParticle.SHARD, f.pos.x, f.pos.y + .4, f.pos.z, Math.cos(a) * s, .35 + r.nextDouble() * .5, Math.sin(a) * s, 0x6A5030, .14f + r.nextFloat() * .1f, 30, .08f, false, 1f);
                    }
                }
                if (f.age < hit && f.age % 2 == 0) {
                    double y = f.pos.y + fallH(f, f.age);
                    FxClient.puffA(FxParticle.GLOW, f.pos.x + (r.nextDouble() - .5) * f.scale, y, f.pos.z + (r.nextDouble() - .5) * f.scale, 0, .02, 0, 0x7A6040, 1.2f, 14, 0f, true, .3f);
                }
            }
            default -> { }
        }
    }

    private static double fallH(Inst f, float age) {
        int hit = f.life * 2 / 3;
        float k = Mth.clamp(age / hit, 0, 1);
        return (1 - k * k) * (14 + f.scale * 1.5);
    }

    // ---------------------------------------------------------------- render

    static void render(Inst f, FxDraw g, float pt) {
        float t = f.age + pt;
        float k = Mth.clamp(t / f.life, 0, 1);
        float fade = Mth.clamp(Math.min(t / 2f, (f.life - t) / Math.max(3f, f.life * .35f)), 0, 1);
        switch (f.kind) {
            case SPIKES -> spikes(f, g, t, fade);
            case HALOS -> halos(f, g, t, k, fade);
            case ARC -> arcs(f, g, k);
            case TONGUES -> tongues(f, g, t, fade);
            case TIDE -> tide(f, g, t, k, fade);
            case NEEDLES -> needles(f, g, t, fade);
            case BOULDER -> boulder(f, g, t);
            default -> { }
        }
    }

    /** Gai đá: kim tự tháp 4 mặt nhọn trồi lên lần lượt, chân nâu thẫm -> mũi hổ phách. */
    private static void spikes(Inst f, FxDraw g, float t, float fade) {
        int n = count(f);
        Random r = new Random(f.pos.hashCode());
        Vec3[] ax = axes(radial(f) ? new Vec3(1, 0, 0) : f.dir);
        for (int i = 0; i < n; i++) {
            float start = radial(f) ? (i % n) * .6f : i * (f.life / 3f) / n;
            float rise = Mth.clamp((t - start) / 3f, 0, 1);
            float sink = Mth.clamp((f.life - t) / (f.life * .35f), 0, 1);
            float h = (float) (f.scale * (radial(f) ? .55 : 1.0) * (1.0 + r.nextDouble() * 1.1)) * rise * sink;
            double hw = f.scale * (.2 + r.nextDouble() * .15);
            Vec3 b = slot(f, i, n).add(ax[1].scale((r.nextDouble() - .5) * f.scale * .8)).add(0, .05, 0);
            Vec3 lean = new Vec3((r.nextDouble() - .5) * .5, 0, (r.nextDouble() - .5) * .5).scale(h);
            if (h < .05f) continue;
            Vec3 tip = b.add(lean).add(0, h, 0);
            Vec3 a = b.add(-hw, 0, -hw), bb = b.add(hw, 0, -hw), c = b.add(hw, 0, hw), d = b.add(-hw, 0, hw);
            int dark = col(0x2E2214, .8f * fade), mid = col(mix(0x4A3A22, f.rgb, .55), .85f * fade);
            g.tri(a, bb, tip, mid);
            g.tri(bb, c, tip, dark);
            g.tri(c, d, tip, mid);
            g.tri(d, a, tip, dark);
            int top = col(white(f.rgb, .45f), .95f * fade);
            g.ribbon(b.add(0, h * .5, 0), tip, hw * .25, 0.01, top, top);
        }
        g.disc(f.pos.add(0, .06, 0), f.scale * 1.4, 16, col(f.rgb, .18f * fade), col(f.rgb, 0));
    }

    /** Vòng hào quang: vành vuông góc tia lan dọc tia; đầu tia có sao 4 cánh trắng-vàng (lăng kính). */
    private static void halos(Inst f, FxDraw g, float t, float k, float fade) {
        Vec3 axis = radial(f) ? new Vec3(0, 1, 0) : f.dir.normalize();
        double L = radial(f) ? 0 : f.dir.length();
        int n = radial(f) ? 3 : Math.max(3, (int) (L / 2.2));
        Vec3[] pa = axes(axis);
        for (int i = 0; i < n; i++) {
            float appear = (float) i / n * f.life * .6f;
            float a = Mth.clamp((t - appear) / 3f, 0, 1) * Mth.clamp((f.life - t) / 5f, 0, 1);
            if (a <= 0) continue;
            double rad = f.scale * (.35 + .5 * (i + 1.0) / n) * (1 + .25 * (t - appear) / f.life);
            Vec3 c = radial(f) ? f.pos.add(0, .3 + .55 * i, 0) : f.pos.add(axis.scale(L * (i + 1.0) / n));
            for (int j = 0; j < 20; j++) {
                double a0 = Math.PI * 2 * j / 20, a1 = Math.PI * 2 * (j + 1) / 20;
                Vec3 o0 = pa[1].scale(Math.cos(a0)).add(pa[2].scale(Math.sin(a0))), o1 = pa[1].scale(Math.cos(a1)).add(pa[2].scale(Math.sin(a1)));
                int cin = col(0xFFFFFF, .85f * a), cmid = col(f.rgb, .75f * a), cout = col(f.rgb, 0f);
                g.quad2(c.add(o0.scale(rad * .86)), c.add(o0.scale(rad)), c.add(o1.scale(rad)), c.add(o1.scale(rad * .86)), cin, cmid, cmid, cin);
                g.quad2(c.add(o0.scale(rad)), c.add(o0.scale(rad * 1.18)), c.add(o1.scale(rad * 1.18)), c.add(o1.scale(rad)), cmid, cout, cout, cmid);
            }
        }
        Vec3 head = f.pos.add(radial(f) ? new Vec3(0, .6, 0) : f.dir);
        Vec3 cam = g.cam.subtract(head).normalize();
        Vec3 u = cam.cross(new Vec3(0, 1, 0));
        u = u.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : u.normalize();
        Vec3 v = cam.cross(u).normalize();
        double sz = f.scale * 1.1 * Mth.clamp(1 - k, 0, 1) * fade + .05;
        int cw = col(0xFFFFFF, .95f * fade), cg = col(f.rgb, 0f);
        for (Vec3 dd : new Vec3[]{u, v}) {
            Vec3 o = dd == u ? v : u;
            g.quad2(head.add(dd.scale(sz)), head.add(o.scale(0.04)), head.subtract(dd.scale(sz)), head.subtract(o.scale(0.04)), cg, cw, cg, cw);
        }
        Vec3 d1 = u.add(v).normalize(), d2 = u.subtract(v).normalize();
        g.quad2(head.add(d1.scale(sz * .6)), head.add(d2.scale(0.03)), head.subtract(d1.scale(sz * .6)), head.subtract(d2.scale(0.03)), cg, cw, cg, cw);
        g.quad2(head.add(d2.scale(sz * .6)), head.add(d1.scale(0.03)), head.subtract(d2.scale(sz * .6)), head.subtract(d1.scale(0.03)), cg, cw, cg, cw);
    }

    /** Sét gấp khúc: đường gãy ngẫu nhiên đổi mỗi 2 tick (nhấp nháy), có nhánh phụ; viền tím-xanh, lõi trắng. */
    private static void arcs(Inst f, FxDraw g, float k) {
        int seed = f.age / 2;
        float a = f.age < 2 ? 1f : (float) Math.pow(1 - k, 1.2);
        int arcs = radial(f) ? 5 : 1;
        for (int n = 0; n < arcs; n++) {
            Random r = new Random(f.pos.hashCode() * 17L + seed * 131L + n * 7919L);
            Vec3 to = radial(f) ? f.pos.add(Math.cos(n * 1.256 + seed) * f.scale, .3 + r.nextDouble() * .8, Math.sin(n * 1.256 + seed) * f.scale) : f.pos.add(f.dir);
            bolt(g, f.pos, to, f.scale, f.rgb, a, r, 0);
        }
    }

    private static void bolt(FxDraw g, Vec3 from, Vec3 to, float scale, int rgb, float a, Random r, int depth) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < .1) return;
        Vec3[] ax = axes(d);
        int segs = Math.max(4, (int) (len / .9));
        Vec3 prev = from;
        double amp = Math.min(.55, len / segs * .75) * (.7 + .3 * scale);
        for (int i = 1; i <= segs; i++) {
            double s = (double) i / segs;
            Vec3 p = i == segs ? to : from.add(d.scale(s)).add(ax[1].scale((r.nextDouble() - .5) * 2 * amp)).add(ax[2].scale((r.nextDouble() - .5) * 2 * amp));
            g.glowLine(prev, p, (depth == 0 ? .06 : .035) * Math.max(.8, scale), rgb, a);
            if (depth == 0 && i > 1 && i < segs && r.nextInt(3) == 0)
                bolt(g, prev, p.add(ax[1].scale((r.nextDouble() - .5) * len * .35)).add(ax[0].scale(len * .12)), scale * .6f, rgb, a * .8f, r, 1);
            prev = p;
        }
    }

    /** Lưỡi lửa: dải nhọn cong đung đưa, đáy đỏ -> giữa cam -> mũi vàng; nền đất cháy cam mờ. */
    private static void tongues(Inst f, FxDraw g, float t, float fade) {
        int n = radial(f) ? 12 : Math.max(5, (int) (f.dir.length() * 1.3));
        Random r = new Random(f.pos.hashCode());
        Vec3[] ax = axes(radial(f) ? new Vec3(1, 0, 0) : f.dir);
        float life = Mth.clamp(t / 4f, 0, 1) * Mth.clamp((f.life - t) / (f.life * .4f), 0, 1);
        for (int i = 0; i < n; i++) {
            Vec3 b;
            if (radial(f)) {
                double a = Math.PI * 2 * i / n + r.nextDouble() * .4, rr = f.scale * (.3 + .7 * r.nextDouble());
                b = f.pos.add(Math.cos(a) * rr, .05, Math.sin(a) * rr);
            } else b = f.pos.add(f.dir.scale((i + r.nextDouble()) / n)).add(ax[1].scale((r.nextDouble() - .5) * .9)).add(0, .05, 0);
            double h = (.7 + r.nextDouble() * .9) * Math.max(.7, Math.min(2.2, f.scale * (radial(f) ? .5 : 1))) * life * (.75 + .25 * Math.sin(t * 1.3 + i * 2.1));
            double w = .22 * Math.max(.7, Math.min(1.6, f.scale * .5));
            double ph = t * .5 + i, sway = .22 * h;
            Vec3 side = ax[1].scale(w);
            Vec3 prev0 = b.subtract(side), prev1 = b.add(side);
            for (int s = 1; s <= 3; s++) {
                double e = s / 3.0, ww = 1 - e;
                Vec3 c = b.add(Math.sin(ph + e * 2.2) * sway * e, h * e, Math.cos(ph * 1.1 + e * 2.0) * sway * e);
                Vec3 q0 = c.subtract(side.scale(ww)), q1 = c.add(side.scale(ww));
                int cb = s == 1 ? col(0xE02010, .8f * fade) : s == 2 ? col(0xFF7A18, .8f * fade) : col(0xFFE070, .9f * fade);
                int cp = s == 1 ? col(0xFF3010, .85f * fade) : s == 2 ? col(0xFF9A20, .85f * fade) : col(0xFFB030, .8f * fade);
                g.quad2(prev0, prev1, q1, q0, cp, cp, cb, cb);
                prev0 = q0;
                prev1 = q1;
            }
        }
        g.disc(f.pos.add(0, .06, 0), radial(f) ? f.scale * 1.05 : 1.2, 20, col(0xFF5A10, .32f * fade), col(0xC01800, 0f));
    }

    /** Bức sóng: tường cong có đỉnh gợn tiến dọc dir; chân mờ -> đỉnh bọt trắng. scale = nửa bề rộng. */
    private static void tide(Inst f, FxDraw g, float t, float k, float fade) {
        Vec3[] ax = axes(f.dir);
        Vec3 head = f.pos.add(f.dir.scale(Mth.clamp(k * 1.05f, 0, 1)));
        int N = 18;
        double W = f.scale, H = 1.3 + .35 * W;
        for (int i = 0; i < N; i++) {
            double w0 = -1 + 2.0 * i / N, w1 = -1 + 2.0 * (i + 1) / N;
            Vec3 b0 = head.add(ax[1].scale(w0 * W)).add(ax[0].scale(-(w0 * w0) * W * .45)).add(0, .05, 0);
            Vec3 b1 = head.add(ax[1].scale(w1 * W)).add(ax[0].scale(-(w1 * w1) * W * .45)).add(0, .05, 0);
            double h0 = H * (1 - .55 * w0 * w0) * (.85 + .15 * Math.sin(t * .9 + i * 1.7)), h1 = H * (1 - .55 * w1 * w1) * (.85 + .15 * Math.sin(t * .9 + (i + 1) * 1.7));
            int base = col(f.rgb, .12f * fade), mid = col(f.rgb, .55f * fade), foam = col(0xFFFFFF, .9f * fade);
            Vec3 m0 = b0.add(ax[0].scale(.35)).add(0, h0 * .7, 0), m1 = b1.add(ax[0].scale(.35)).add(0, h1 * .7, 0);
            Vec3 t0 = b0.add(ax[0].scale(.7)).add(0, h0, 0), t1 = b1.add(ax[0].scale(.7)).add(0, h1, 0);
            g.quad2(b0, b1, m1, m0, base, base, mid, mid);
            g.quad2(m0, m1, t1, t0, mid, mid, foam, foam);
        }
        g.disc(head.add(0, .06, 0), W * .9, 18, col(f.rgb, .2f * fade), col(f.rgb, 0));
    }

    /** Phi kim/kiếm bạc: thoi dài mỏng bay thẳng toả quạt, vệt sáng phía sau, mũi trắng. */
    private static void needles(Inst f, FxDraw g, float t, float fade) {
        int n = 7;
        Vec3[] ax = axes(radial(f) ? new Vec3(0, 0, 1) : f.dir);
        double L = radial(f) ? f.scale * 2 : f.dir.length();
        float flight = Mth.clamp(t / (f.life * .55f), 0, 1);
        float e = 1 - (1 - flight) * (1 - flight);
        if (radial(f)) n = 8;
        for (int i = 0; i < n; i++) {
            double fan = radial(f) ? Math.PI * 2 * i / n : (i - (n - 1) / 2.0) * .13;
            Vec3 dirv = radial(f) ? new Vec3(Math.cos(fan), .15, Math.sin(fan)).normalize() : ax[0].add(ax[1].scale(Math.tan(fan))).normalize();
            double lenb = f.scale * 1.15;
            double lead = L * e * (radial(f) ? .5 : (.55 + .45 * Math.abs(Math.cos(i))));
            Vec3 tip = f.pos.add(dirv.scale(lead + lenb)), tail = f.pos.add(dirv.scale(lead)), mid = f.pos.add(dirv.scale(lead + lenb * .35));
            Vec3 side = dirv.cross(g.cam.subtract(mid));
            side = side.lengthSqr() < 1e-8 ? ax[1] : side.normalize();
            double hw = .07 * f.scale;
            float a = fade * Mth.clamp((f.life - t) / 3f, 0, 1);
            int steel = col(0xC9D3DE, .95f * a), shine = col(0xFFFFFF, a), tr = col(0xAEBBC9, 0f);
            g.quad2(tail, mid.add(side.scale(hw)), tip, mid.subtract(side.scale(hw)), steel, shine, shine, steel);
            g.ribbon(tail.subtract(dirv.scale(L * .35 * e)), tail, .004, .03, tr, col(0xE8EEF5, .6f * a));
        }
    }

    /** Tảng đá khổng lồ: khối 20 mặt méo xoay, rơi tăng tốc rồi vỡ; bóng đổ nở dần trên đất. */
    private static void boulder(Inst f, FxDraw g, float t) {
        int hit = f.life * 2 / 3;
        float R = f.scale;
        float ground = Mth.clamp(t / hit, 0, 1);
        g.disc(f.pos.add(0, .07, 0), R * (.4 + .8 * ground), 24, col(0x2A1E10, (t > hit + 6 ? 0f : .5f * ground)), col(f.rgb, .05f));
        if (t > hit + 3) return;
        double y = f.pos.y + fallH(f, Math.min(t, hit));
        Vec3 c = new Vec3(f.pos.x, y + R * .8, f.pos.z);
        double ph = 1.618, A = 1 / Math.sqrt(1 + ph * ph);
        double[][] V = {{0, A, A * ph}, {0, -A, A * ph}, {0, A, -A * ph}, {0, -A, -A * ph}, {A, A * ph, 0}, {-A, A * ph, 0}, {A, -A * ph, 0}, {-A, -A * ph, 0}, {A * ph, 0, A}, {-A * ph, 0, A}, {A * ph, 0, -A}, {-A * ph, 0, -A}};
        int[][] F = {{0, 1, 8}, {0, 9, 1}, {0, 8, 4}, {0, 5, 9}, {0, 4, 5}, {1, 6, 8}, {1, 9, 7}, {1, 7, 6}, {2, 3, 11}, {2, 10, 3}, {2, 4, 10}, {2, 5, 4}, {2, 11, 5}, {3, 7, 11}, {3, 10, 6}, {3, 6, 7}, {4, 8, 10}, {5, 11, 9}, {6, 10, 8}, {7, 9, 11}};
        Random r = new Random(f.pos.hashCode());
        Vec3[] P = new Vec3[12];
        double rot = t * .12, cx = Math.cos(rot), sx = Math.sin(rot);
        for (int i = 0; i < 12; i++) {
            double s = R * (.8 + r.nextDouble() * .4);
            double x = V[i][0] * s, yy = V[i][1] * s, z = V[i][2] * s;
            P[i] = c.add(x * cx - z * sx, yy, x * sx + z * cx);
        }
        float a = t > hit ? 1 - (t - hit) / 3f : 1f;
        for (int i = 0; i < F.length; i++) {
            double shade = .55 + .45 * Math.abs(Math.sin(i * 2.399 + rot));
            g.tri(P[F[i][0]], P[F[i][1]], P[F[i][2]], col(mix(0x3A2A18, white(f.rgb, .2f), shade), .92f * a));
        }
    }

    private static int mix(int a, int b, double t) {
        int r = (int) ((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t), gg = (int) ((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t), bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | gg << 8 | bl;
    }
}
