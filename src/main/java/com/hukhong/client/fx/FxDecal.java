package com.hukhong.client.fx;

import com.hukhong.client.fx.FxClient.Inst;
import com.hukhong.network.FxPayload;
import com.hukhong.vfx.FxType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.hukhong.client.fx.FxDraw.col;
import static com.hukhong.client.fx.FxDraw.white;

/**
 * F2 - Decal mặt đất (và vài hình phẳng khác): rune nhiều vòng, vòng nở, nứt chữ thập, vũng, telegraph tròn/nón/đường
 * (vùng đỏ lấp đầy dần tới lúc nổ), vết rách không gian (RIFT), vỏ lục giác (SHELL). Mọi hình hai mặt, additive.
 * Kind nằm ở byte cao của color (xem {@link #RUNE}...). Nhịp: tụ lực -> bùng -> tan.
 */
final class FxDecal {
    private FxDecal() {}

    static final int RUNE = 0, RING = 1, CRACK = 2, POOL = 3, TELE_CIRCLE = 4, TELE_CONE = 5, TELE_LINE = 6;

    static final int DANGER = 0xFF2A2A;

    static int defLife(int kind) {
        return switch (kind) { case RUNE -> 60; case RING -> 18; case CRACK -> 50; case POOL -> 80; default -> 30; };
    }

    static int defColor(int kind) {
        return switch (kind) { case POOL -> 0xA01028; case CRACK -> 0xFF9A40; case TELE_CIRCLE, TELE_CONE, TELE_LINE -> DANGER; default -> 0xFFE9A0; };
    }

    static void receive(FxPayload pl, Vec3 pos, Vec3 dir, float sc, int rgb, boolean dim) {
        int kind = pl.color() >>> 24;
        if (rgb == 0) rgb = defColor(kind);
        int life = pl.life() > 0 ? pl.life() : defLife(kind);
        Inst i = FxClient.add(FxType.DECAL, pos, dir, sc, rgb, life, 0);
        i.dim = dim; i.kind = kind;
    }

    // ---------------------------------------------------------------- RIFT / SHELL receive

    static void receiveRift(FxPayload pl, Vec3 pos, Vec3 dir, float sc, int rgb, boolean dim) {
        Inst i = FxClient.add(FxType.RIFT, pos, dir, sc <= 0 ? 1.4f : sc, rgb, pl.life() > 0 ? pl.life() : 14, 0);
        i.dim = dim;
    }

    static void receiveShell(FxPayload pl, Vec3 pos, Vec3 dir, float sc, int rgb, boolean dim) {
        int owner = (int) dir.x;
        Inst i = FxClient.add(FxType.SHELL, pos, Vec3.ZERO, sc <= 0 ? 1.3f : sc, rgb, Math.max(20, pl.life() > 0 ? pl.life() : 160), 0);
        i.dim = dim; i.owner = owner >= 0 ? owner : -1;
        // thay shell cũ của cùng chủ
        if (i.owner >= 0) FxClient.ACTIVE.removeIf(o -> o != i && o.type == FxType.SHELL && o.owner == i.owner);
    }

    // ---------------------------------------------------------------- start / tick

    static void start(Inst f) {
        float q = FxClient.q();
        switch (f.type) {
            case DECAL -> startDecal(f, q);
            case SHELL -> buildShell(f, q);
            default -> {}
        }
    }

    private static void startDecal(Inst f, float q) {
        double R = f.scale;
        switch (f.kind) {
            case CRACK -> {
                int spokes = f.dir.x >= 3 ? (int) f.dir.x : 4;
                List<Vec3[]> segs = new ArrayList<>(); List<Float> w = new ArrayList<>();
                for (int a = 0; a < spokes; a++) {
                    double ang = Math.PI * 2 * a / spokes + (spokes == 4 ? Math.PI / 4 : 0) + (f.rnd.nextDouble() - 0.5) * 0.18;
                    Vec3 cur = f.pos;
                    int len = 6 + f.rnd.nextInt(3);
                    double step = R / len;
                    for (int s = 0; s < len; s++) {
                        ang += (f.rnd.nextDouble() - 0.5) * 0.45;
                        Vec3 nx = cur.add(Math.cos(ang) * step * (0.8 + f.rnd.nextDouble() * 0.4), 0, Math.sin(ang) * step * (0.8 + f.rnd.nextDouble() * 0.4));
                        segs.add(new Vec3[]{cur, nx}); w.add(1f - 0.11f * s);
                        if (s >= 1 && s < len - 1 && f.rnd.nextInt(3) == 0 && q >= 0.7f) {
                            double ba = ang + (f.rnd.nextBoolean() ? 1 : -1) * (0.55 + f.rnd.nextDouble() * 0.5);
                            Vec3 bn = nx.add(Math.cos(ba) * step * 1.6, 0, Math.sin(ba) * step * 1.6);
                            segs.add(new Vec3[]{nx, bn}); w.add(0.4f);
                        }
                        cur = nx;
                    }
                }
                f.segs = segs; f.segW = w;
                int n = (int) ((10 + 3 * R) * q * (f.dim ? 0.5 : 1)); // đá bay lên rồi rơi xuống
                for (int i = 0; i < n; i++) {
                    double a = f.rnd.nextDouble() * Math.PI * 2, r = R * (0.15 + 0.7 * f.rnd.nextDouble());
                    FxClient.puffA(FxParticle.SHARD, f.pos.x + Math.cos(a) * r, f.pos.y + 0.2, f.pos.z + Math.sin(a) * r,
                            Math.cos(a) * 0.05, 0.28 + f.rnd.nextDouble() * 0.32, Math.sin(a) * 0.05, f.rgb, 0.3f + f.rnd.nextFloat() * 0.25f, 26 + f.rnd.nextInt(10), 0.07f, false, 0.9f);
                }
            }
            case POOL -> {
                float[] bl = new float[28];
                for (int i = 0; i < bl.length; i++) bl[i] = 0.72f + f.rnd.nextFloat() * 0.5f;
                for (int i = 0; i < bl.length; i++) bl[i] = (bl[i] + bl[(i + 1) % bl.length] + bl[(i + bl.length - 1) % bl.length]) / 3f;
                f.data = bl;
            }
            default -> {}
        }
    }

    static void tick(Inst f) {
        float q = FxClient.q();
        double R = f.scale;
        if (f.type == FxType.SHELL) {
            if (f.age == f.life - 2) { // vỡ vụn khi hết hạn
                int n = (int) (22 * q);
                Vec3 c = f.pos.add(0, 0.95, 0);
                for (int i = 0; i < n; i++) {
                    double th = f.rnd.nextDouble() * Math.PI * 2, ph = Math.acos(f.rnd.nextDouble() * 2 - 1);
                    Vec3 d = new Vec3(Math.sin(ph) * Math.cos(th), Math.cos(ph), Math.sin(ph) * Math.sin(th));
                    Vec3 p = c.add(d.scale(R));
                    FxClient.puffA(FxParticle.SHARD, p.x, p.y, p.z, d.x * 0.1, d.y * 0.1, d.z * 0.1, f.rgb, 0.3f, 18, 0.01f, true, 0.9f);
                }
            }
            return;
        }
        if (f.type != FxType.DECAL) return;
        int L = f.life;
        switch (f.kind) {
            case RUNE -> {
                if (f.age % 3 == 0 && f.age < L * 0.7) { // tụ lực: sao bay lên từ vòng
                    double a = f.rnd.nextDouble() * Math.PI * 2;
                    FxClient.puffA(FxParticle.STAR, f.pos.x + Math.cos(a) * R * 0.95, f.pos.y + 0.1, f.pos.z + Math.sin(a) * R * 0.95,
                            0, 0.05 + f.rnd.nextDouble() * 0.08, 0, f.rgb, 0.22f, 20, 0f, true, 0.8f);
                }
                if (f.age == (int) (L * 0.3)) { // bùng
                    int n = (int) (14 * q);
                    for (int i = 0; i < n; i++) {
                        double a = Math.PI * 2 * i / Math.max(1, n);
                        FxClient.puffA(FxParticle.GLOW, f.pos.x + Math.cos(a) * R * 0.9, f.pos.y + 0.15, f.pos.z + Math.sin(a) * R * 0.9,
                                Math.cos(a) * 0.06, 0.04, Math.sin(a) * 0.06, white(f.rgb, 0.4f), 0.5f, 14, 0f, true, 0.9f);
                    }
                }
            }
            case TELE_CIRCLE, TELE_CONE, TELE_LINE -> {
                if (f.age % 4 == 0) {
                    double a = f.rnd.nextDouble() * Math.PI * 2, r = R * Math.sqrt(f.rnd.nextDouble()) * 0.9;
                    Vec3 p = f.kind == TELE_CIRCLE ? f.pos.add(Math.cos(a) * r, 0, Math.sin(a) * r) : f.pos.add(f.dir.multiply(1, 0, 1).scale(f.rnd.nextDouble() * (f.age + 1f) / L)).add((f.rnd.nextDouble() - 0.5) * R * 0.5, 0, (f.rnd.nextDouble() - 0.5) * R * 0.5);
                    FxClient.puffA(FxParticle.STAR, p.x, p.y + 0.1, p.z, 0, 0.06 + 0.12 * f.age / L, 0, f.rgb, 0.2f, 12, 0f, true, 0.7f);
                }
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- helpers (hai mặt)

    private static void ring(FxDraw g, Vec3 c, double r0, double r1, int segs, double rot, int ci, int co, int skipEvery) {
        for (int i = 0; i < segs; i++) {
            if (skipEvery > 0 && i % skipEvery == skipEvery - 1) continue;
            double a0 = rot + Math.PI * 2 * i / segs, a1 = rot + Math.PI * 2 * (i + 1) / segs;
            double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
            g.quad2(new Vec3(c.x + c0 * r0, c.y, c.z + s0 * r0), new Vec3(c.x + c0 * r1, c.y, c.z + s0 * r1),
                    new Vec3(c.x + c1 * r1, c.y, c.z + s1 * r1), new Vec3(c.x + c1 * r0, c.y, c.z + s1 * r0), ci, co, co, ci);
        }
    }

    private static void line(FxDraw g, Vec3 a, Vec3 b, double w, int c) {
        Vec3 d = b.subtract(a);
        Vec3 s = d.cross(FxDraw.UP);
        double l = s.lengthSqr();
        if (l < 1e-10) return;
        s = s.scale(w / Math.sqrt(l));
        g.quad2(a.add(s), a.subtract(s), b.subtract(s), b.add(s), c, c, c, c);
    }

    /** Quạt tròn từ tâm c, hướng ang0..ang1, bán kính r: tâm alpha ci, mép alpha co. */
    private static void fan(FxDraw g, Vec3 c, double r, double ang0, double ang1, int segs, int ci, int co) {
        for (int i = 0; i < segs; i++) {
            double a0 = ang0 + (ang1 - ang0) * i / segs, a1 = ang0 + (ang1 - ang0) * (i + 1) / segs;
            g.quad2(c, new Vec3(c.x + Math.cos(a0) * r, c.y, c.z + Math.sin(a0) * r),
                    new Vec3(c.x + Math.cos(a1) * r, c.y, c.z + Math.sin(a1) * r), c, ci, co, co, ci);
        }
    }

    private static float cl(float v) { return Math.max(0f, Math.min(1f, v)); }

    // ---------------------------------------------------------------- render

    static void render(Inst f, FxDraw g, float pt) {
        float t = f.age + pt, L = f.life, k = cl(t / L);
        float q = FxClient.q();
        switch (f.type) {
            case DECAL -> {
                Vec3 gp = f.pos.add(0, f.kind <= RING || f.kind >= TELE_CIRCLE ? 0.07 : 0.06, 0);
                switch (f.kind) {
                    case RUNE -> rune(f, g, gp, t, L, q);
                    case RING -> ringDecal(f, g, gp, k, q);
                    case CRACK -> crack(f, g, gp, k);
                    case POOL -> pool(f, g, gp, k);
                    case TELE_CIRCLE -> teleCircle(f, g, gp, t, L, q);
                    case TELE_CONE -> teleCone(f, g, gp, t, L, q);
                    case TELE_LINE -> teleLine(f, g, gp, t, L);
                    default -> {}
                }
            }
            case RIFT -> rift(f, g, k);
            case SHELL -> shell(f, g, t, L);
            default -> {}
        }
    }

    private static void rune(Inst f, FxDraw g, Vec3 gp, float t, float L, float q) {
        double R = f.scale;
        int c = f.rgb, w = white(c, 0.65f), seg = (int) (28 + 36 * q);
        float charge = cl(t / (L * 0.3f));
        float out = cl((L - t) / (L * 0.35f));
        float a = (float) Math.pow(charge, 0.7) * out;
        float burst = (float) Math.exp(-Math.pow((t - L * 0.3) / 2.2, 2)); // đỉnh sáng đúng lúc bùng
        float A = Math.min(1f, a * (0.75f + 0.5f * burst));
        double rot = t * 0.035;
        g.disc(gp, R, seg, col(c, 0.13f * A), col(c, 0.02f * A));
        ring(g, gp, R * 0.965, R, seg, 0, col(w, 0.95f * A), col(w, 0.95f * A), 0);
        ring(g, gp, R * 0.86, R * 0.9, seg, rot, col(c, 0.75f * A), col(c, 0.75f * A), 4);
        ring(g, gp, R * 0.70, R * 0.725, seg, -rot * 1.6, col(w, 0.8f * A), col(w, 0.8f * A), 3);
        ring(g, gp, R * 0.36, R * 0.385, seg, rot * 2.2, col(c, 0.7f * A), col(c, 0.7f * A), 5);
        int n = f.dir.x >= 3 ? (int) f.dir.x : 6, step = n <= 6 ? 2 : 3;
        double sr = R * 0.7, ro = -rot * 0.8;
        for (int i = 0; i < n; i++) { // sao nhiều cánh {n/step}
            double a0 = ro + Math.PI * 2 * i / n, a1 = ro + Math.PI * 2 * ((i + step) % n) / n;
            line(g, gp.add(Math.cos(a0) * sr, 0, Math.sin(a0) * sr), gp.add(Math.cos(a1) * sr, 0, Math.sin(a1) * sr), 0.03 + R * 0.006, col(c, 0.9f * A));
        }
        int tk = (int) (16 + 8 * q);
        for (int i = 0; i < tk; i++) { // ký tự
            double an = rot * 1.2 + Math.PI * 2 * i / tk;
            Vec3 d = new Vec3(Math.cos(an), 0, Math.sin(an));
            line(g, gp.add(d.scale(R * 0.74)), gp.add(d.scale(R * (i % 2 == 0 ? 0.84 : 0.8))), 0.022 + R * 0.003, col(w, 0.85f * A));
        }
        if (burst > 0.02f) { // vòng sóng khi bùng
            double br = R * (0.9 + 0.5 * cl((t - L * 0.3f) / 8f));
            ring(g, gp, br - R * 0.04, br, seg, 0, col(w, 0f), col(w, 0.8f * burst * out), 0);
        }
        g.cylinder(gp.add(0, -0.07, 0), R * 0.985, 0, (0.5 + 1.6 * charge) * Math.min(1.6, 0.6 + R * 0.12), seg, col(c, 0.2f * A), col(c, 0f));
    }

    private static void ringDecal(Inst f, FxDraw g, Vec3 gp, float k, float q) {
        double R = f.scale;
        int c = f.rgb, w = white(c, 0.7f), seg = (int) (28 + 28 * q);
        float e = 1f - (1f - cl(k * 2.6f)) * (1f - cl(k * 2.6f));
        double r = R * e, th = R * 0.16 * (1 - k) + 0.08;
        float a = (1f - k) * (1f - k);
        ring(g, gp, Math.max(0, r - th), r, seg, 0, col(c, 0f), col(w, 0.95f * a), 0);
        ring(g, gp, r, r + th * 0.5, seg, 0, col(w, 0.95f * a), col(c, 0f), 0);
        g.disc(gp, r, seg, col(c, 0.0f), col(c, 0.14f * a * (1 - k)));
    }

    private static void crack(Inst f, FxDraw g, Vec3 gp, float k) {
        if (f.segs == null) return;
        double R = f.scale;
        float reveal = cl(k * 5f), cool = cl(k * 1.6f);
        float a = cl((1 - k) * 2.2f);
        int hot = white(f.rgb, 0.85f * (1 - cool)), c = f.rgb;
        int shown = (int) (f.segs.size() * reveal);
        for (int i = 0; i < shown && i < f.segs.size(); i++) {
            Vec3 s0 = f.segs.get(i)[0].add(0, 0.07, 0), s1 = f.segs.get(i)[1].add(0, 0.07, 0);
            double wd = (0.035 + R * 0.012) * f.segW.get(i);
            line(g, s0, s1, wd * 2.8, col(c, 0.14f * a));
            line(g, s0, s1, wd, col(c, 0.65f * a));
            line(g, s0, s1, wd * 0.4, col(hot, a));
        }
        g.disc(gp, R * 0.3, 18, col(white(c, 0.7f), 0.55f * a * (1 - cool)), col(c, 0f));
    }

    private static void pool(Inst f, FxDraw g, Vec3 gp, float k) {
        float[] bl = (float[]) f.data;
        if (bl == null) return;
        double R = f.scale;
        float grow = 1f - (1f - cl(k / 0.2f)) * (1f - cl(k / 0.2f));
        float shrink = 1f - cl((k - 0.7f) / 0.3f) * 0.6f;
        float a = cl((1 - k) * 3f) ;
        int c = f.rgb, n = bl.length;
        double rot = k * 0.3;
        for (int i = 0; i < n; i++) {
            double a0 = rot + Math.PI * 2 * i / n, a1 = rot + Math.PI * 2 * (i + 1) / n;
            double r0 = R * grow * shrink * bl[i], r1 = R * grow * shrink * bl[(i + 1) % n];
            Vec3 p0 = gp.add(Math.cos(a0) * r0, 0, Math.sin(a0) * r0), p1 = gp.add(Math.cos(a1) * r1, 0, Math.sin(a1) * r1);
            g.quad2(gp, p0, p1, gp, col(c, 0.5f * a), col(c, 0.2f * a), col(c, 0.2f * a), col(c, 0.5f * a));
            Vec3 q0 = gp.add(Math.cos(a0) * r0 * 1.04, 0, Math.sin(a0) * r0 * 1.04), q1 = gp.add(Math.cos(a1) * r1 * 1.04, 0, Math.sin(a1) * r1 * 1.04);
            g.quad2(p0, q0, q1, p1, col(white(c, 0.45f), 0.55f * a), col(c, 0f), col(c, 0f), col(white(c, 0.45f), 0.55f * a));
        }
    }

    // ---------------------------------------------------------------- telegraph

    /** Nhịp telegraph: nhấp nháy nhanh dần, 2 tick cuối chớp trắng (báo nổ). Trả {lấp đầy 0..1, pulse, flash}. */
    private static float[] tele(float t, float L) {
        float k = cl(t / L);
        float fill = k * k * (3 - 2 * k);
        float fr = 0.5f + 3.5f * k;
        float pulse = 0.8f + 0.2f * (float) Math.sin(t * fr);
        float flash = t >= L - 2.5f ? cl((t - (L - 2.5f)) / 2.5f) : 0f;
        return new float[]{fill, pulse, flash};
    }

    private static void teleCircle(Inst f, FxDraw g, Vec3 gp, float t, float L, float q) {
        double R = f.scale;
        float[] s = tele(t, L);
        int c = f.rgb, w = white(c, 0.55f), seg = (int) (28 + 28 * q);
        float in = cl(t / 3f);
        ring(g, gp, R * 0.955, R, seg, 0, col(w, 0.95f * s[1] * in), col(c, 0.85f * s[1] * in), 0);
        g.disc(gp, R * 0.955, seg, col(c, (0.08f + 0.06f * s[0]) * in), col(c, (0.13f + 0.1f * s[0]) * in));
        double fr = R * 0.955 * (0.08 + 0.92 * s[0]);
        g.disc(gp, fr, seg, col(c, 0.26f + 0.2f * s[0]), col(c, 0.5f + 0.25f * s[0]));
        ring(g, gp, Math.max(0, fr - 0.12), fr, seg, 0, col(c, 0f), col(w, 0.85f), 0);
        for (int i = 0; i < 8; i++) { // vạch chéo cảnh báo
            double an = t * 0.05 + Math.PI * 2 * i / 8;
            line(g, gp.add(Math.cos(an) * R * 0.2, 0, Math.sin(an) * R * 0.2), gp.add(Math.cos(an) * R * 0.92, 0, Math.sin(an) * R * 0.92), 0.025, col(w, 0.35f * in));
        }
        if (s[2] > 0) g.disc(gp, R, seg, col(0xFFFFFF, 0.65f * s[2]), col(w, 0.5f * s[2]));
        g.cylinder(gp, R * 0.99, 0, 1.2 * s[0] + 0.15, seg, col(c, 0.28f * s[0] * in), col(c, 0f));
    }

    private static void teleCone(Inst f, FxDraw g, Vec3 gp, float t, float L, float q) {
        double R = f.scale, half = Math.toRadians(f.dir.y > 1 ? f.dir.y : 40);
        double face = Math.atan2(f.dir.z, f.dir.x);
        float[] s = tele(t, L);
        int c = f.rgb, w = white(c, 0.55f), seg = (int) (14 + 14 * q);
        float in = cl(t / 3f);
        double a0 = face - half, a1 = face + half;
        fan(g, gp, R, a0, a1, seg, col(c, 0.10f * in), col(c, 0.16f * in));
        double fr = R * (0.08 + 0.92 * s[0]);
        fan(g, gp, fr, a0, a1, seg, col(c, 0.3f + 0.15f * s[0]), col(c, 0.55f + 0.2f * s[0]));
        // viền
        Vec3 e0 = gp.add(Math.cos(a0) * R, 0, Math.sin(a0) * R), e1 = gp.add(Math.cos(a1) * R, 0, Math.sin(a1) * R);
        line(g, gp, e0, 0.05, col(w, 0.95f * s[1])); line(g, gp, e1, 0.05, col(w, 0.95f * s[1]));
        Vec3 prev = e0;
        for (int i = 1; i <= seg; i++) {
            double a = a0 + (a1 - a0) * i / seg;
            Vec3 p = gp.add(Math.cos(a) * R, 0, Math.sin(a) * R);
            line(g, prev, p, 0.05, col(w, 0.95f * s[1])); prev = p;
        }
        if (s[2] > 0) fan(g, gp, R, a0, a1, seg, col(0xFFFFFF, 0.7f * s[2]), col(w, 0.5f * s[2]));
    }

    private static void teleLine(Inst f, FxDraw g, Vec3 gp, float t, float L) {
        Vec3 d = f.dir.multiply(1, 0, 1);
        double len = d.length();
        if (len < 0.1) return;
        Vec3 u = d.scale(1 / len), sd = new Vec3(-u.z, 0, u.x).scale(f.scale * 0.5);
        float[] s = tele(t, L);
        int c = f.rgb, w = white(c, 0.55f);
        float in = cl(t / 3f);
        Vec3 a = gp, b = gp.add(d);
        g.quad2(a.add(sd), a.subtract(sd), b.subtract(sd), b.add(sd), col(c, 0.14f * in), col(c, 0.14f * in), col(c, 0.18f * in), col(c, 0.18f * in));
        Vec3 fe = gp.add(d.scale(0.04 + 0.96 * s[0]));
        g.quad2(a.add(sd), a.subtract(sd), fe.subtract(sd), fe.add(sd), col(c, 0.4f), col(c, 0.4f), col(c, 0.65f), col(c, 0.65f));
        line(g, a.add(sd), b.add(sd), 0.05, col(w, 0.95f * s[1])); line(g, a.subtract(sd), b.subtract(sd), 0.05, col(w, 0.95f * s[1]));
        line(g, b.add(sd), b.subtract(sd), 0.05, col(w, 0.95f * s[1]));
        if (s[2] > 0) g.quad2(a.add(sd), a.subtract(sd), b.subtract(sd), b.add(sd), col(0xFFFFFF, 0.65f * s[2]), col(0xFFFFFF, 0.65f * s[2]), col(w, 0.5f * s[2]), col(w, 0.5f * s[2]));
    }

    // ---------------------------------------------------------------- rift

    private static void rift(Inst f, FxDraw g, float k) {
        Vec3 d = f.dir;
        double len = d.length();
        if (len < 0.3) return;
        float open = k < 0.22f ? k / 0.22f : 1f - (float) Math.pow((k - 0.22f) / 0.78f, 1.6);
        open = cl(open);
        Vec3 n = d.scale(1 / len), up = Math.abs(n.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = n.cross(up).normalize(), lift = side.cross(n).normalize();
        int segs = 14, c = f.rgb, w = 0xFFFFFF;
        float a = cl((1 - k) * 2f);
        Vec3 pt = null, pb = null;
        for (int i = 0; i <= segs; i++) {
            double u = (double) i / segs, h = f.scale * open * Math.pow(Math.sin(Math.PI * u), 0.85);
            Vec3 ctr = f.pos.add(n.scale(len * u));
            Vec3 top = ctr.add(lift.scale(h)), bot = ctr.subtract(lift.scale(h));
            if (pt != null) {
                g.quad2(pt, top, ctr, f.pos.add(n.scale(len * (u - 1.0 / segs))), col(w, 0.9f * a), col(w, 0.9f * a), col(c, 0.55f * a), col(c, 0.55f * a));
                g.quad2(f.pos.add(n.scale(len * (u - 1.0 / segs))), ctr, bot, pb, col(c, 0.55f * a), col(c, 0.55f * a), col(w, 0.9f * a), col(w, 0.9f * a));
                g.glowLine(pt, top, 0.035, w, 0.9f * a); g.glowLine(pb, bot, 0.035, w, 0.9f * a);
            }
            pt = top; pb = bot;
        }
    }

    // ---------------------------------------------------------------- shell (hex)

    private static void buildShell(Inst f, float q) {
        int rows = q >= 0.7f ? 5 : 3, per = q >= 0.7f ? 8 : 6;
        Vec3[] v = new Vec3[rows * per];
        double R = f.scale;
        for (int j = 0; j < rows; j++) {
            double lat = Math.toRadians(-60 + 120.0 * j / Math.max(1, rows - 1));
            for (int i = 0; i < per; i++) {
                double lon = Math.PI * 2 * (i + (j % 2) * 0.5) / per;
                v[j * per + i] = new Vec3(Math.cos(lat) * Math.cos(lon) * R, Math.sin(lat) * R * 1.25, Math.cos(lat) * Math.sin(lon) * R);
            }
        }
        List<int[]> e = new ArrayList<>();
        for (int j = 0; j < rows; j++) for (int i = 0; i < per; i++) {
            int a = j * per + i;
            e.add(new int[]{a, j * per + (i + 1) % per});
            if (j + 1 < rows) {
                int n0 = (j + 1) * per + i, n1 = (j + 1) * per + (i + (j % 2 == 0 ? per - 1 : 1)) % per;
                e.add(new int[]{a, n0}); e.add(new int[]{a, n1});
            }
        }
        f.data = new Object[]{v, e};
    }

    @SuppressWarnings("unchecked")
    private static void shell(Inst f, FxDraw g, float t, float L) {
        if (f.data == null) return;
        Object[] d = (Object[]) f.data;
        Vec3[] v = (Vec3[]) d[0];
        List<int[]> e = (List<int[]>) d[1];
        int c = f.rgb, w = white(c, 0.7f);
        Vec3 ctr = f.pos.add(0, 0.95, 0);
        float form = cl(t / 7f), fray = cl((t - (L - 16)) / 14f);
        float base = 0.2f + 0.08f * (float) Math.sin(t * 0.35);
        float flash = (float) Math.exp(-Math.pow((t - 6) / 3.0, 2)); // chớp khi đóng kín
        for (int i = 0; i < e.size(); i++) {
            float local = cl(form * e.size() / (float) (i + 1) - 0.0f);
            if (i > e.size() * form + 1) continue;
            float h = (i * 0.618034f) % 1f;
            if (h < fray) continue; // nứt dần rồi rụng
            float a = (base + 0.7f * flash) * (1f - fray * 0.6f);
            int[] ed = e.get(i);
            Vec3 p0 = ctr.add(v[ed[0]]), p1 = ctr.add(v[ed[1]]);
            g.ribbon(p0, p1, 0.04, 0.04, col(c, 0.2f * a), col(c, 0.2f * a));
            g.ribbon(p0, p1, 0.014, 0.014, col(w, a), col(w, a));
        }
    }
}
