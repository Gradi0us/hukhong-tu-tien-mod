package com.hukhong.client.fx;

import com.hukhong.client.fx.FxClient.Inst;
import com.hukhong.vfx.FxType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.hukhong.client.fx.FxDraw.col;
import static com.hukhong.client.fx.FxDraw.white;

/** Dựng + vẽ từng loại hiệu ứng. start = một lần khi bắt đầu; tick = hạt mỗi tick; render = hình additive. */
final class FxEffects {
    private FxEffects() {}

    private static float ease(float k) { return 1f - (1f - k) * (1f - k); }
    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static Vec3 mix(Vec3 a, Vec3 b, double t) { return a.add(b.subtract(a).scale(t)); }

    /** Trục (dir chuẩn hoá, right, up) cho hiệu ứng có hướng. */
    private static Vec3[] axes(Vec3 dir) {
        Vec3 d = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
        Vec3 r = FxDraw.UP.cross(d);
        r = r.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : r.normalize();
        Vec3 u = d.cross(r).normalize();
        return new Vec3[]{d, r, u};
    }

    // ================================================================ start

    static void start(Inst f) {
        float q = FxClient.q();
        switch (f.type) {
            case RIBBON -> {}
            case DECAL, SHELL -> FxDecal.start(f);
            case ELEM -> FxElem.start(f);
            case GIANT_SWORD -> FxGiantSword.start(f);
            case SWORD_CRESCENT -> {
                f.tilt = (float) (f.rnd.nextDouble() * 1.8 - 0.9);
                Vec3[] ax = axes(f.dir);
                for (int i = 0; i < 5 * q; i++) { // Kiếm: giảm ~70% hạt
                    double a = (f.rnd.nextDouble() * 2 - 1) * 1.2;
                    Vec3 p = f.pos.add(ax[0].scale(Math.cos(a) * 1.6 * f.scale)).add(ax[1].scale(Math.sin(a) * 2.4 * f.scale));
                    Vec3 v = ax[0].scale(0.25 + f.rnd.nextDouble() * 0.3).add(ax[1].scale(Math.sin(a) * 0.1));
                    FxClient.puff(FxParticle.SHARD, p.x, p.y, p.z, v.x, v.y, v.z, f.rgb, 0.4f, 12, 0f, true);
                }
            }
            case LIGHTNING_BOLT -> {
                buildBolt(f);
                FxClient.add(FxType.SHOCKWAVE, f.pos, Vec3.ZERO, 3f, f.rgb, 12, 0);
                FxClient.add(FxType.SCREEN_TINT, f.pos, Vec3.ZERO, 0.45f, 0xDDEEFF, 8, 0);
                for (int i = 0; i < 26 * q; i++) {
                    double a = f.rnd.nextDouble() * Math.PI * 2, s = 0.1 + f.rnd.nextDouble() * 0.45;
                    FxClient.puff(FxParticle.STAR, f.pos.x, f.pos.y + 0.2, f.pos.z, Math.cos(a) * s, 0.15 + f.rnd.nextDouble() * 0.4, Math.sin(a) * s, 0xD8E8FF, 0.3f, 16, 0.03f, true);
                }
                FxClient.puff(FxParticle.GLOW, f.pos.x, f.pos.y + 0.8, f.pos.z, 0, 0, 0, 0xC8DCFF, 4.5f, 8, 0f, false);
            }
            case GROUND_CRACK -> buildCracks(f);
            case SWORD_RAIN -> {
                int n = Math.max(2, (int) ((6 + 2.5 * f.scale) * q * 0.35));
                f.sx = new float[n]; f.sz = new float[n]; f.sd = new float[n]; f.landed = new boolean[n];
                for (int i = 0; i < n; i++) {
                    double a = f.rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(f.rnd.nextDouble()) * f.scale;
                    f.sx[i] = (float) (Math.cos(a) * r); f.sz[i] = (float) (Math.sin(a) * r);
                    f.sd[i] = f.rnd.nextFloat() * Math.max(6, f.life - 20);
                }
            }
            case SWORD_DOMAIN -> {}
            case TALISMAN_CIRCLE -> FxClient.add(FxType.SHOCKWAVE, f.pos, Vec3.ZERO, f.scale * 0.9f, f.rgb, 12, 0);
            case SHOCKWAVE -> {
                for (int i = 0; i < 20 * q; i++) {
                    double a = f.rnd.nextDouble() * Math.PI * 2, r = f.scale * 0.35;
                    FxClient.puff(FxParticle.GLOW, f.pos.x + Math.cos(a) * r, f.pos.y + 0.15, f.pos.z + Math.sin(a) * r,
                            Math.cos(a) * f.scale * 0.04, 0.03, Math.sin(a) * f.scale * 0.04, f.rgb, 0.45f, 14, 0f, true);
                }
            }
            case LIGHT_PILLAR -> {
                for (int i = 0; i < 22 * q; i++) {
                    double a = f.rnd.nextDouble() * Math.PI * 2, r = f.scale * 0.5 * f.rnd.nextDouble();
                    FxClient.puff(FxParticle.STAR, f.pos.x + Math.cos(a) * r, f.pos.y + 0.1, f.pos.z + Math.sin(a) * r,
                            0, 0.2 + f.rnd.nextDouble() * 0.4, 0, f.rnd.nextInt(3) == 0 ? 0xFFFFFF : f.rgb, 0.35f, 24, 0f, true);
                }
            }
            case DEMON_SILHOUETTE -> FxClient.puff(FxParticle.GLOW, f.pos.x, f.pos.y + 1.5 * f.scale, f.pos.z, 0, 0.02, 0, f.rgb, 6f * f.scale, 14, 0f, false);
            case BLOOD_VORTEX -> {}
            default -> {}
        }
    }

    // ================================================================ tick

    static void tick(Inst f) {
        float q = FxClient.q();
        switch (f.type) {
            case RIBBON -> FxTrail.tick(f);
            case DECAL, SHELL -> FxDecal.tick(f);
            case ELEM -> FxElem.tick(f);
            case GIANT_SWORD -> FxGiantSword.tick(f);
            case STORM_CLOUD -> {
                int n = Math.max(1, (int) (3 * q));
                for (int i = 0; i < n; i++) {
                    double a = f.rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(f.rnd.nextDouble()) * f.scale;
                    FxClient.puffA(FxParticle.GLOW, f.pos.x + Math.cos(a) * r, f.pos.y + 30 + f.rnd.nextDouble() * 4, f.pos.z + Math.sin(a) * r,
                            (f.rnd.nextDouble() - 0.5) * 0.05, 0, (f.rnd.nextDouble() - 0.5) * 0.05, f.rgb, 9f + f.rnd.nextFloat() * 5f, 50, 0f, false, 0.55f);
                }
                if (f.age % 9 == 0 && f.rnd.nextInt(2) == 0) { // lóe sáng trong mây
                    double a = f.rnd.nextDouble() * Math.PI * 2, r = f.rnd.nextDouble() * f.scale;
                    FxClient.puff(FxParticle.GLOW, f.pos.x + Math.cos(a) * r, f.pos.y + 29, f.pos.z + Math.sin(a) * r, 0, 0, 0, 0x8FA8FF, 7f, 5, 0f, false);
                }
            }
            case TELEGRAPH -> {
                if (f.age % 3 == 0) {
                    double a = f.rnd.nextDouble() * Math.PI * 2, r = f.scale * (0.5 + 0.5 * f.rnd.nextDouble());
                    FxClient.puff(FxParticle.STAR, f.pos.x + Math.cos(a) * r, f.pos.y + 0.1, f.pos.z + Math.sin(a) * r, 0, 0.08 + 0.12 * f.age / f.life, 0, f.rgb, 0.25f, 14, 0f, true);
                }
            }
            case SWORD_RAIN -> {
                if (f.landed == null) return;
                for (int i = 0; i < f.landed.length; i++) {
                    if (!f.landed[i] && f.age >= f.sd[i] + 5) {
                        f.landed[i] = true;
                        double x = f.pos.x + f.sx[i], z = f.pos.z + f.sz[i];
                        for (int j = 0; j < 1.2 * q + 0.3; j++) {
                            double a = f.rnd.nextDouble() * Math.PI * 2;
                            FxClient.puff(FxParticle.SHARD, x, f.pos.y + 0.2, z, Math.cos(a) * 0.12, 0.12 + f.rnd.nextDouble() * 0.12, Math.sin(a) * 0.12, f.rgb, 0.3f, 12, 0.02f, true);
                        }
                        
                    }
                }
            }
            case SWORD_DOMAIN -> {
                if (f.age % 7 == 0) {
                    double a = f.rnd.nextDouble() * Math.PI * 2, r = f.scale * (0.85 + 0.15 * f.rnd.nextDouble());
                    FxClient.puff(FxParticle.SHARD, f.pos.x + Math.cos(a) * r, f.pos.y + 0.2, f.pos.z + Math.sin(a) * r, 0, 0.1 + f.rnd.nextDouble() * 0.1, 0, f.rgb, 0.4f, 16, 0f, true);
                }
            }
            case BLOOD_VORTEX -> {
                double a = f.age * 0.9, k = (double) f.age / f.life;
                for (int arm = 0; arm < 2; arm++) {
                    double aa = a + arm * Math.PI, r = f.scale * (1.0 - 0.7 * k);
                    FxClient.puff(FxParticle.GLOW, f.pos.x + Math.cos(aa) * r, f.pos.y + 0.2 + k * 3.5 * f.scale * 0.5, f.pos.z + Math.sin(aa) * r, 0, 0.05, 0, f.rgb, 0.6f, 14, 0f, true);
                }
            }
            case TALISMAN_CIRCLE -> {
                if (f.age % 3 == 0) {
                    double a = f.rnd.nextDouble() * Math.PI * 2, r = f.scale * f.rnd.nextDouble();
                    FxClient.puff(FxParticle.STAR, f.pos.x + Math.cos(a) * r, f.pos.y + 0.1, f.pos.z + Math.sin(a) * r, 0, 0.06 + f.rnd.nextDouble() * 0.08, 0, f.rgb, 0.28f, 22, 0f, true);
                }
            }
            case BEAM -> {
                if (f.age == 0) {
                    Vec3 d = f.dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : f.dir.normalize();
                    int n = (int) (18 * q);
                    for (int i = 0; i < n; i++) {
                        double t = f.rnd.nextDouble() * f.scale;
                        FxClient.puff(FxParticle.STAR, f.pos.x + d.x * t, f.pos.y + d.y * t, f.pos.z + d.z * t,
                                (f.rnd.nextDouble() - 0.5) * 0.1, (f.rnd.nextDouble() - 0.5) * 0.1, (f.rnd.nextDouble() - 0.5) * 0.1, f.rgb, 0.3f, 14, 0f, true);
                    }
                    Vec3 e = f.pos.add(d.scale(f.scale));
                    FxClient.puff(FxParticle.GLOW, e.x, e.y, e.z, 0, 0, 0, 0xFFFFFF, 2.2f, 7, 0f, false);
                }
            }
            default -> {}
        }
    }

    // ================================================================ geometry build

    private static void buildBolt(Inst f) {
        f.segs = new ArrayList<>(); f.segW = new ArrayList<>();
        float q = FxClient.q();
        double H = 42;
        Vec3 top = f.pos.add((f.rnd.nextDouble() - 0.5) * 6, H, (f.rnd.nextDouble() - 0.5) * 6);
        int n = (int) (10 + 8 * q);
        Vec3[] pts = new Vec3[n + 1];
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n;
            Vec3 p = mix(top, f.pos, t);
            double j = i == 0 || i == n ? 0 : (1.4 + 1.6 * (1 - t)) ;
            pts[i] = p.add((f.rnd.nextDouble() - 0.5) * 2 * j, 0, (f.rnd.nextDouble() - 0.5) * 2 * j);
        }
        for (int i = 0; i < n; i++) { f.segs.add(new Vec3[]{pts[i], pts[i + 1]}); f.segW.add(1f); }
        int branches = (int) (2 + 5 * q);
        for (int b = 0; b < branches; b++) {
            int from = 2 + f.rnd.nextInt(Math.max(1, n - 4));
            Vec3 cur = pts[from];
            double ang = f.rnd.nextDouble() * Math.PI * 2;
            Vec3 step = new Vec3(Math.cos(ang) * 2.4, -H / n * 0.9, Math.sin(ang) * 2.4);
            int len = 4 + f.rnd.nextInt(4);
            for (int s = 0; s < len; s++) {
                Vec3 nx = cur.add(step).add((f.rnd.nextDouble() - 0.5) * 2.2, (f.rnd.nextDouble() - 0.5) * 0.8, (f.rnd.nextDouble() - 0.5) * 2.2);
                if (nx.y < f.pos.y) nx = new Vec3(nx.x, f.pos.y, nx.z);
                f.segs.add(new Vec3[]{cur, nx}); f.segW.add(0.45f - 0.05f * s);
                if (s == 2 && q > 0.9f && f.rnd.nextBoolean()) { // nhánh con
                    Vec3 sub = nx.add((f.rnd.nextDouble() - 0.5) * 4, -2.5, (f.rnd.nextDouble() - 0.5) * 4);
                    f.segs.add(new Vec3[]{nx, sub}); f.segW.add(0.22f);
                }
                cur = nx;
                if (cur.y <= f.pos.y) break;
            }
        }
    }

    private static void buildCracks(Inst f) {
        f.segs = new ArrayList<>(); f.segW = new ArrayList<>();
        float q = FxClient.q();
        boolean small = f.scale < 1.2f;
        int arms = Math.max(3, (int) Math.min(7 + 5 * q, 3 + 3 * f.scale * q));
        for (int a = 0; a < arms; a++) {
            double ang = Math.PI * 2 * a / arms + (f.rnd.nextDouble() - 0.5) * 0.5;
            Vec3 cur = f.pos;
            int len = 5 + f.rnd.nextInt(3);
            double step = f.scale * 1.1 / len;
            for (int s = 0; s < len; s++) {
                ang += (f.rnd.nextDouble() - 0.5) * 0.7;
                Vec3 nx = cur.add(Math.cos(ang) * step * (0.7 + f.rnd.nextDouble() * 0.6), 0, Math.sin(ang) * step * (0.7 + f.rnd.nextDouble() * 0.6));
                f.segs.add(new Vec3[]{cur, nx}); f.segW.add(1f - 0.12f * s);
                if (s >= 1 && f.rnd.nextInt(3) == 0) {
                    double ba = ang + (f.rnd.nextBoolean() ? 1 : -1) * (0.6 + f.rnd.nextDouble() * 0.6);
                    Vec3 bn = nx.add(Math.cos(ba) * step * 1.2, 0, Math.sin(ba) * step * 1.2);
                    f.segs.add(new Vec3[]{nx, bn}); f.segW.add(0.45f);
                }
                cur = nx;
            }
        }
        for (int i = 0; i < (small ? 4 : 14) * q; i++) {
            double a = f.rnd.nextDouble() * Math.PI * 2, r = f.scale * f.rnd.nextDouble();
            FxClient.puffA(FxParticle.GLOW, f.pos.x + Math.cos(a) * r, f.pos.y + 0.1, f.pos.z + Math.sin(a) * r, 0, 0.05 + f.rnd.nextDouble() * 0.12, 0,
                    f.rgb, small ? 0.5f : 0.4f, small ? 30 : 24, 0f, true, small ? 0.3f : 1f);
        }
        if (!small) FxClient.add(FxType.SHOCKWAVE, f.pos, Vec3.ZERO, f.scale * 0.9f, f.rgb, 14, 0);
    }

    // ================================================================ render

    static void render(Inst f, FxDraw g, float pt) {
        float t = f.age + pt;
        float k = clamp01(t / f.life);
        float fade = clamp01(Math.min(t / 4f, (f.life - t) / Math.max(4f, f.life * 0.3f)));
        Vec3 gp = f.pos.add(0, 0.07, 0);
        float q = FxClient.q();
        int c = f.rgb;
        switch (f.type) {
            case RIBBON -> FxTrail.render(f, g, pt);
            case DECAL, RIFT, SHELL -> FxDecal.render(f, g, pt);
            case ELEM -> FxElem.render(f, g, pt);
            case GIANT_SWORD -> FxGiantSword.render(f, g, pt);
            case SWORD_CRESCENT -> crescent(f, g, k, q);
            case SWORD_QI -> qi(f, g, k, q);
            case LIGHTNING_BOLT -> {
                if (f.segs == null) return;
                float a = f.age < 2 ? 1f : (f.age < 4 ? 0.35f : (f.age == 4 ? 1f : (float) Math.pow(1 - k, 1.5)));
                double w = 0.13 * Math.max(0.7, f.scale);
                for (int i = 0; i < f.segs.size(); i++) g.glowLine(f.segs.get(i)[0], f.segs.get(i)[1], w * f.segW.get(i), c, a);
                g.disc(gp, 3.2 * (0.6 + k), 20, col(0xFFFFFF, 0.5f * a), col(c, 0f));
            }
            case GROUND_CRACK -> {
                if (f.segs == null) return;
                float reveal = clamp01(k * 3.5f), a = clamp01((1 - k) * 2f) * (f.scale < 1.2f ? 0.6f : 1f);
                int shown = (int) (f.segs.size() * reveal);
                for (int i = 0; i < shown && i < f.segs.size(); i++)
                    g.flatGlow(f.segs.get(i)[0].add(0, 0.07, 0), f.segs.get(i)[1].add(0, 0.07, 0), 0.07 * f.segW.get(i) * Math.max(1, f.scale * 0.4), c, a);
                g.disc(gp, f.scale * 0.35, 18, col(white(c, 0.6f), 0.5f * a), col(c, 0));
            }
            case SHOCKWAVE -> {
                float r = f.scale * ease(k), a = (1 - k) * (1 - k);
                double wd = f.scale * 0.22 * (1 - k) + 0.1;
                g.flatRing(gp, Math.max(0, r - wd), r, (int) (20 + 28 * q), 0, col(c, 0f), col(white(c, 0.7f), a), 0);
                g.flatRing(gp, r, r + wd * 0.7, (int) (20 + 28 * q), 0, col(white(c, 0.7f), a), col(c, 0f), 0);
                g.cylinder(f.pos, r, 0, 0.9 * f.scale * (1 - k) + 0.2, (int) (18 + 24 * q), col(c, a * 0.5f), col(c, 0f));
            }
            case SWORD_DOMAIN -> domain(f, g, t, fade * (f.stop < 0 ? 1f : clamp01(1f - (t - f.stop) / 8f)), gp, q);
            case TALISMAN_CIRCLE -> talisman(f, g, t, fade, gp, q);
            case TELEGRAPH -> telegraph(f, g, t, k, gp, q);
            case LIGHT_PILLAR -> {
                float grow = clamp01(t / 5f), a = (1 - k) * (1 - k * 0.5f);
                double h = 16 * f.scale * (0.3 + 0.7 * grow), r = 0.45 * f.scale * (1 - 0.5 * k) + 0.1;
                int seg = (int) (10 + 6 * q);
                g.cylinder(f.pos, r * 2.2, 0, h, seg, col(c, 0.28f * a), col(c, 0f));
                g.cylinder(f.pos, r, 0, h, seg, col(white(c, 0.5f), 0.55f * a), col(c, 0f));
                g.cylinder(f.pos, r * 0.35, 0, h * 1.05, 8, col(0xFFFFFF, 0.9f * a), col(0xFFFFFF, 0.1f * a));
                g.disc(gp, r * 4, 18, col(white(c, 0.6f), 0.6f * a), col(c, 0f));
            }
            case SWORD_RAIN -> rain(f, g, t, gp, q);
            case BLOOD_VORTEX -> vortex(f, g, t, k, q);
            case DEMON_SILHOUETTE -> demon(f, g, k);
            case BEAM -> {
                Vec3 d = f.dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : f.dir.normalize();
                Vec3 e = f.pos.add(d.scale(f.scale));
                float a = (1 - k) * (1 - k);
                double w = 0.22 * (1 - 0.5 * k);
                g.glowLine(f.pos, e, w, c, a);
                Vec3[] ax = axes(d);
                int n = (int) (14 + 14 * q);
                for (int h = 0; h < 2; h++) {
                    Vec3 prev = f.pos;
                    for (int i = 1; i <= n; i++) {
                        double u = (double) i / n, ang = u * f.scale * 0.9 + t * 1.2 + h * Math.PI;
                        Vec3 p = f.pos.add(d.scale(f.scale * u)).add(ax[1].scale(Math.cos(ang) * 0.45)).add(ax[2].scale(Math.sin(ang) * 0.45));
                        g.ribbon(prev, p, 0.05, 0.05, col(white(c, 0.5f), 0.8f * a), col(white(c, 0.5f), 0.8f * a));
                        prev = p;
                    }
                }
                g.disc(e, 1.0 + 0.8 * k, 12, col(0xFFFFFF, 0.0f), col(c, 0f));
            }
            default -> {}
        }
    }

    /**
     * Dải kiếm khí lưỡi liềm: mặt cong nằm trong mặt phẳng (hướng bay, trục chém nghiêng theo roll), lồi ra phía trước,
     * hai sừng cong về sau; dọc theo cung có "vây" mờ dần lên/xuống để nhìn thấy từ mọi góc. Gradient: mép ngoài sáng
     * (trắng pha màu) -> mép trong trong suốt. Tối đa ~0.4 alpha, cộng sáng nhẹ - dịu mắt.
     */
    private static void qi(Inst f, FxDraw g, float k, float q) {
        Vec3 d = f.dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : f.dir;
        double travel = d.length();
        d = d.scale(1 / travel);
        Vec3 flat = new Vec3(d.x, 0, d.z);
        flat = flat.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0, flat.x);
        double sign = f.roll < 0 ? -1 : 1, tilt = Math.toRadians(Math.min(90, Math.abs(f.roll)));
        Vec3 w = right.scale(Math.cos(tilt) * sign).add(0, Math.sin(tilt), 0);
        w = w.subtract(d.scale(w.dot(d)));
        w = w.lengthSqr() < 1e-6 ? right : w.normalize();
        Vec3 nrm = w.cross(d);
        if (nrm.y < 0) nrm = nrm.scale(-1);
        nrm = nrm.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : nrm.normalize();

        float inA = clamp01(k * 8f), outA = (float) Math.pow(1 - k, 1.1);
        float peak = 0.42f * inA * outA;
        double R = f.scale * (0.55 + 0.45 * ease(Math.min(1f, k * 2.2f)));
        int n = (int) (12 + 10 * q);
        double A = 1.2;
        int rim = white(f.rgb, 0.65f), body = f.rgb;
        for (int tr = 0; tr < 3; tr++) { // bản chính + 2 bóng đuôi
            float kk = k - tr * 0.07f;
            if (kk < 0) break;
            float ta = peak * (tr == 0 ? 1f : tr == 1 ? 0.45f : 0.2f);
            Vec3 cen = f.pos.add(d.scale(travel * ease(Math.min(1f, kk))));
            double Rt = R * (1 - 0.1 * tr);
            Vec3 po = null, pn = null, pi = null, pdn = null;
            double hgt = Rt * 0.22;
            for (int i = 0; i <= n; i++) {
                double a = -A + 2 * A * i / n;
                double tp = Math.pow(Math.cos(a * Math.PI / (2 * A)), 0.8);
                Vec3 o = cen.add(d.scale(Math.cos(a) * Rt * 0.9 - Rt * 0.55)).add(w.scale(Math.sin(a) * Rt * 1.5));
                Vec3 in = o.subtract(d.scale(0.5 * Rt * tp));
                Vec3 up = o.add(nrm.scale(hgt * tp)), dn = o.subtract(nrm.scale(hgt * tp));
                if (po != null) {
                    g.quad2(po, o, in, pi, col(rim, ta * 0.95f), col(rim, ta * 0.95f), col(body, 0f), col(body, 0f));
                    g.quad2(po, o, up, pn, col(rim, ta * 0.6f), col(rim, ta * 0.6f), col(body, 0f), col(body, 0f));
                    g.quad2(po, o, dn, pdn, col(rim, ta * 0.6f), col(rim, ta * 0.6f), col(body, 0f), col(body, 0f));
                }
                po = o; pi = in; pn = up; pdn = dn;
            }
        }
    }

    private static void crescent(Inst f, FxDraw g, float k, float q) {
        Vec3[] ax = axes(f.dir);
        double ct = Math.cos(f.tilt), st = Math.sin(f.tilt);
        Vec3 rt = ax[1].scale(ct).add(ax[2].scale(st));
        float a = (float) Math.pow(1 - k, 1.2) * 0.4f; // dịu hơn
        int n = (int) (10 + 12 * q);
        for (int layer = 0; layer < 2; layer++) {
            double lag = layer == 0 ? 0 : 0.35;
            double travel = f.scale * (0.5 + 4.0 * ease(Math.max(0f, k - (float) lag * 0.4f)));
            double R = f.scale * (1.5 + 1.2 * k) * (layer == 0 ? 1 : 0.85);
            float la = a * (layer == 0 ? 1f : 0.45f);
            Vec3 cen = f.pos.add(ax[0].scale(travel));
            double amax = 1.25;
            Vec3 po = null, pi = null;
            for (int i = 0; i <= n; i++) {
                double an = -amax + 2 * amax * i / n;
                double tp = Math.cos(an * Math.PI / (2 * amax));
                Vec3 o = cen.add(ax[0].scale(Math.cos(an) * R)).add(rt.scale(Math.sin(an) * R * 1.55));
                Vec3 in = o.subtract(ax[0].scale(0.62 * R * tp));
                if (po != null) g.quad(po, o, in, pi, col(white(f.rgb, 0.8f), la), col(white(f.rgb, 0.8f), la), col(f.rgb, la * 0.05f), col(f.rgb, la * 0.05f));
                po = o; pi = in;
            }
            // viền sáng ngoài
            Vec3 prev = null;
            for (int i = 0; i <= n; i++) {
                double an = -amax + 2 * amax * i / n;
                double tp = Math.cos(an * Math.PI / (2 * amax));
                Vec3 o = cen.add(ax[0].scale(Math.cos(an) * R)).add(rt.scale(Math.sin(an) * R * 1.55));
                if (prev != null) g.ribbon(prev, o, 0.09 * tp * f.scale + 0.02, 0.09 * tp * f.scale + 0.02, col(0xFFFFFF, la), col(0xFFFFFF, la));
                prev = o;
            }
        }
    }

    private static void domain(Inst f, FxDraw g, float t, float fade, Vec3 gp, float q) {
        double R = f.scale;
        float a = fade * 0.35f;
        int seg = (int) (28 + 40 * q);
        double rot = t * 0.05;
        int c = f.rgb, w = white(c, 0.7f);
        g.flatRing(gp, R * 0.97, R * 1.02, seg, 0, col(c, 0.1f * a), col(w, 0.9f * a), 0);
        g.flatRing(gp, R * 1.02, R * 1.12, seg, 0, col(w, 0.9f * a), col(c, 0f), 0);
        g.flatRing(gp, R * 0.70, R * 0.74, seg, -rot, col(c, 0.8f * a), col(c, 0.8f * a), 4);
        g.flatRing(gp, R * 0.40, R * 0.43, seg, rot * 1.6, col(c, 0.8f * a), col(c, 0.8f * a), 3);
        g.disc(gp, R * 0.97, seg, col(c, 0.1f * a), col(c, 0.02f * a));
        int swords = (int) (8 + 4 * q);
        for (int i = 0; i < swords; i++) { // kiếm ấn toả ra
            double an = rot + Math.PI * 2 * i / swords;
            Vec3 d = new Vec3(Math.cos(an), 0, Math.sin(an)), s = new Vec3(-d.z, 0, d.x);
            Vec3 b = gp.add(d.scale(R * 0.45)), tip = gp.add(d.scale(R * 0.95));
            g.tri(b.add(s.scale(0.18 * R * 0.2)), b.subtract(s.scale(0.18 * R * 0.2)), tip, col(w, 0.85f * a));
        }
        double wallH = 1.2 + 0.4 * Math.sin(t * 0.4);
        g.cylinder(f.pos, R, 0, wallH, seg, col(c, 0.35f * a), col(c, 0f));
        for (int i = 0; i < swords; i++) { // kiếm khí xoay vòng quanh rìa
            double an = -rot * 3 + Math.PI * 2 * i / swords;
            Vec3 b = f.pos.add(Math.cos(an) * R, 0.2, Math.sin(an) * R);
            g.glowLine(b, b.add(0, 1.6 + 0.4 * Math.sin(t * 0.3 + i), 0), 0.04, c, 0.9f * a);
        }
    }

    private static void talisman(Inst f, FxDraw g, float t, float fade, Vec3 gp, float q) {
        double R = f.scale;
        float a = fade;
        int seg = (int) (24 + 36 * q), c = f.rgb, w = white(c, 0.7f);
        double rot = t * 0.04, y = Math.min(1, t / 6f);
        gp = gp.add(0, 0.05 * y, 0);
        g.disc(gp, R, seg, col(c, 0.14f * a), col(c, 0.02f * a));
        g.flatRing(gp, R * 0.96, R * 1.0, seg, 0, col(w, 0.95f * a), col(w, 0.95f * a), 0);
        g.flatRing(gp, R * 0.82, R * 0.84, seg, 0, col(c, 0.8f * a), col(c, 0.8f * a), 0);
        g.flatRing(gp, R * 0.86, R * 0.92, seg, rot, col(c, 0.7f * a), col(c, 0.7f * a), 3);
        for (int tri = 0; tri < 2; tri++) { // sao sáu cánh
            for (int i = 0; i < 3; i++) {
                double a0 = rot * (tri == 0 ? 1 : -1) + tri * Math.PI / 3 + Math.PI * 2 * i / 3, a1 = a0 + Math.PI * 2 / 3;
                Vec3 p0 = gp.add(Math.cos(a0) * R * 0.82, 0, Math.sin(a0) * R * 0.82), p1 = gp.add(Math.cos(a1) * R * 0.82, 0, Math.sin(a1) * R * 0.82);
                g.flatGlow(p0, p1, 0.025 * Math.max(1, R * 0.5), c, 0.9f * a);
            }
        }
        int glyph = 12;
        for (int i = 0; i < glyph; i++) { // ký tự bùa
            double an = -rot * 2 + Math.PI * 2 * i / glyph;
            Vec3 b = gp.add(Math.cos(an) * R * 0.88, 0, Math.sin(an) * R * 0.88);
            Vec3 d = new Vec3(Math.cos(an), 0, Math.sin(an));
            g.flatLine(b.add(d.scale(-0.08 * R)), b.add(d.scale(0.1 * R)), 0.012 * R + 0.01, col(w, 0.9f * a), col(w, 0.9f * a));
        }
        double h = (1.5 + 2.5 * y) * Math.min(1, R * 0.4 + 0.4);
        g.cylinder(f.pos, R * 0.98, 0, h, seg, col(c, 0.25f * a), col(c, 0f));
    }

    private static void telegraph(Inst f, FxDraw g, float t, float k, Vec3 gp, float q) {
        double R = f.scale;
        int seg = (int) (20 + 24 * q), c = f.rgb, w = white(c, 0.6f);
        float pulse = 0.75f + 0.25f * (float) Math.sin(t * 0.9);
        g.flatRing(gp, R * 0.93, R, seg, 0, col(w, 0.9f * pulse), col(c, 0.6f * pulse), 0);
        g.disc(gp, R * 0.93 * (0.15 + 0.85 * k * k), seg, col(w, 0.5f * k), col(c, 0.25f * k));
        g.disc(gp, R * 0.93, seg, col(c, 0.07f + 0.18f * k), col(c, 0.02f));
        for (int i = 0; i < 6; i++) {
            double an = t * 0.12 + Math.PI * 2 * i / 6;
            g.flatGlow(gp.add(Math.cos(an) * R * 0.3, 0, Math.sin(an) * R * 0.3), gp.add(Math.cos(an + 0.5) * R * 0.88, 0, Math.sin(an + 0.5) * R * 0.88), 0.03, c, 0.55f * (0.4f + k));
        }
        g.cylinder(f.pos, R, 0, 2.2 * k + 0.2, seg, col(c, 0.35f * k), col(c, 0f));
    }

    private static void rain(Inst f, FxDraw g, float t, Vec3 gp, float q) {
        if (f.sx == null) return;
        int c = f.rgb, w = white(c, 0.75f);
        float tail = clamp01((f.life - t) / 10f);
        for (int i = 0; i < f.sx.length; i++) {
            float lt = t - f.sd[i];
            if (lt < 0) continue;
            double fall = Math.min(1, lt / 5.0);
            double ty = f.pos.y + 18 * (1 - fall) * (1 - fall);
            Vec3 tip = new Vec3(f.pos.x + f.sx[i], ty, f.pos.z + f.sz[i]);
            float a = (lt < 5 ? 1f : tail) * 0.5f;
            Vec3 top = tip.add(0, 3.0, 0);
            if (lt < 5) g.ribbon(top, top.add(0, 6 * (1 - fall) + 1, 0), 0.04, 0.0, col(c, 0.5f), col(c, 0f)); // vệt rơi
            blade(g, tip, top, 0.2, c, w, a);
            if (lt >= 5 && lt < 12) {
                double r = 0.5 + (lt - 5) * 0.35;
                float ra = 1 - (lt - 5) / 7f;
                g.flatRing(new Vec3(tip.x, f.pos.y + 0.08, tip.z), r - 0.08, r, 14, 0, col(w, ra), col(c, 0f), 0);
            }
        }
    }

    private static void blade(FxDraw g, Vec3 tip, Vec3 top, double wd, int c, int w, float a) {
        Vec3 mid = mix(tip, top, 0.78);
        double[] ws = {2.2, 1.35, 1.0};
        float[] as = {0.12f, 0.3f, 0.95f};
        for (int i = 0; i < 3; i++) {
            int cc = i == 2 ? w : c;
            g.ribbon(tip, mid, 0, wd * ws[i], col(cc, as[i] * a), col(cc, as[i] * a));
            g.ribbon(mid, top, wd * ws[i], wd * ws[i] * 0.8, col(cc, as[i] * a), col(cc, as[i] * a * 0.7f));
        }
        Vec3 gd = mix(tip, top, 0.82); // chắn kiếm
        g.glowLine(gd.add(0.3, 0, 0.3), gd.subtract(0.3, 0, 0.3), 0.025, c, a);
    }

    private static void vortex(Inst f, FxDraw g, float t, float k, float q) {
        int arms = 3, n = (int) (14 + 12 * q), c = f.rgb;
        float a = clamp01(Math.min(t / 5f, (1 - k) * 2.2f));
        double H = 4.5 * Math.max(0.6, f.scale * 0.5);
        for (int arm = 0; arm < arms; arm++) {
            Vec3 prev = null;
            for (int i = 0; i <= n; i++) {
                double u = (double) i / n;
                double ang = t * 0.55 + arm * Math.PI * 2 / arms + u * 5.0;
                double r = f.scale * (1.0 - 0.75 * u) * (0.6 + 0.4 * Math.min(1, t / 8));
                Vec3 p = f.pos.add(Math.cos(ang) * r, 0.15 + u * H, Math.sin(ang) * r);
                if (prev != null) {
                    float wa = (float) (1 - u * 0.6) * a;
                    g.ribbon(prev, p, 0.16 * (1 - u * 0.5), 0.16 * (1 - u * 0.5), col(c, 0.5f * wa), col(c, 0.5f * wa));
                    g.ribbon(prev, p, 0.05, 0.05, col(white(c, 0.6f), 0.95f * wa), col(white(c, 0.6f), 0.95f * wa));
                }
                prev = p;
            }
        }
        Vec3 gp = f.pos.add(0, 0.07, 0);
        g.disc(gp, f.scale * 1.1, 24, col(c, 0.35f * a), col(c, 0.04f * a));
        g.flatRing(gp, f.scale * 1.0, f.scale * 1.08, 28, t * 0.1, col(white(c, 0.5f), 0.9f * a), col(white(c, 0.5f), 0.9f * a), 3);
    }

    private static final double[][] DEMON = {
            {-0.5, 0, 0.5, 0, 0.3, 1.6}, {-0.5, 0, 0.3, 1.6, -0.3, 1.6},
            {-0.22, 1.6, 0.22, 1.6, 0, 2.05},
            {-0.2, 1.95, -0.5, 2.7, -0.05, 2.05}, {0.2, 1.95, 0.5, 2.7, 0.05, 2.05},
            {0.3, 1.4, 2.0, 2.4, 1.3, 0.5}, {0.3, 1.4, 1.3, 0.5, 0.4, 0.4},
            {-0.3, 1.4, -2.0, 2.4, -1.3, 0.5}, {-0.3, 1.4, -1.3, 0.5, -0.4, 0.4}};

    private static void demon(Inst f, FxDraw g, float k) {
        Vec3 view = f.pos.subtract(g.cam);
        Vec3 right = FxDraw.UP.cross(view);
        if (right.lengthSqr() < 1e-8) return;
        right = right.normalize();
        float a = (float) Math.sin(Math.min(1, k * 1.6) * Math.PI * 0.5) * (1 - k * k);
        double s = f.scale * 1.2, lift = k * 1.2;
        Vec3 o = f.pos.add(0, lift, 0);
        int c = f.rgb;
        for (double[] tr : DEMON) {
            Vec3 p0 = o.add(right.scale(tr[0] * s)).add(0, tr[1] * s, 0), p1 = o.add(right.scale(tr[2] * s)).add(0, tr[3] * s, 0), p2 = o.add(right.scale(tr[4] * s)).add(0, tr[5] * s, 0);
            g.tri(p0, p1, p2, col(c, 0.5f * a));
            g.glowLine(p0, p1, 0.03 * s, c, 0.5f * a);
            g.glowLine(p1, p2, 0.03 * s, c, 0.5f * a);
        }
        for (int e = -1; e <= 1; e += 2) { // mắt
            Vec3 ep = o.add(right.scale(e * 0.11 * s)).add(0, 1.78 * s, 0);
            g.glowLine(ep.add(right.scale(-0.07 * s)), ep.add(right.scale(0.07 * s)), 0.018 * s, 0xFFE040, a);
        }
    }
}
