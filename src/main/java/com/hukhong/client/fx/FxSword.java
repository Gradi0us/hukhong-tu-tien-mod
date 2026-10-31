package com.hukhong.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Hiệu ứng Bản Mệnh Kiếm phía client (additive, RenderType.lightning): cung kiếm khí lưỡi liềm của đòn đánh thường,
 * quầng sáng quanh thân kiếm và dải sáng sau kiếm. Chỉ nhận dữ liệu đã tính (không đọc CULTIVATION).
 */
public final class FxSword {
    private FxSword() {}

    private static final class Arc {
        Vec3 piv, fwd, w, nrm;
        double reach;
        int rgb, age, life = 15;
        final Random rnd = new Random();
    }

    private static final List<Arc> ARCS = new ArrayList<>();
    private static final double HALF_ANGLE = 1.15; // rad (~66 deg) - quạt hitbox server là +-72 deg

    /**
     * Cung kiếm khí: nằm trên đường tròn bán kính (0..reach) quanh piv, trong mặt phẳng (fwd, w); mở rộng ra tới reach
     * (đúng tầm quạt quét của server), sáng dần từ phía đầu cung (+w) sang phía cuối rồi mờ.
     */
    public static void arc(Vec3 piv, Vec3 fwd, Vec3 w, double reach, int rgb) {
        Arc a = new Arc();
        a.piv = piv; a.fwd = fwd; a.w = w; a.reach = reach; a.rgb = rgb;
        Vec3 n = w.cross(fwd);
        if (n.y < 0) n = n.scale(-1);
        a.nrm = n.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : n.normalize();
        if (ARCS.size() >= 24) ARCS.remove(0);
        ARCS.add(a);
    }

    public static boolean any() { return !ARCS.isEmpty(); }
    public static void clear() { ARCS.clear(); }

    static void tick() {
        for (int i = ARCS.size() - 1; i >= 0; i--) {
            Arc a = ARCS.get(i);
            if (a.age < 7) sparks(a);
            if (++a.age >= a.life) ARCS.remove(i);
        }
    }

    /** Hướng từ tâm tới điểm góc ang trên cung: (fwd nghiêng lên BULGE độ) * cos + w * sin. Hitbox server nằm ngang; hình ảnh nâng nhẹ để nhìn rõ từ sau lưng. */
    private static final double BULGE = Math.toRadians(30);

    private static Vec3 radial(Arc a, double ang) {
        Vec3 e2 = a.fwd.scale(Math.cos(BULGE)).add(a.nrm.scale(Math.sin(BULGE)));
        return e2.scale(Math.cos(ang)).add(a.w.scale(Math.sin(ang)));
    }

    private static double radiusAt(Arc a, double t) {
        double k = Math.min(1, Math.max(0, t / a.life));
        double x = Math.min(1, k * 1.5);
        return a.reach * (0.38 + 0.62 * (1 - (1 - x) * (1 - x)));
    }

    private static void sparks(Arc a) {
        float q = FxClient.q();
        double d = radiusAt(a, a.age + 1);
        int n = (int) (7 * q) + 1;
        for (int i = 0; i < n; i++) {
            double ang = (a.rnd.nextDouble() * 2 - 1) * HALF_ANGLE;
            Vec3 r = radial(a, ang);
            Vec3 p = a.piv.add(r.scale(d + a.rnd.nextDouble() * 0.2));
            Vec3 v = r.scale(0.05 + a.rnd.nextDouble() * 0.06).add(0, 0.015 + a.rnd.nextDouble() * 0.03, 0);
            FxClient.puff(FxParticle.GLOW, p.x, p.y, p.z, v.x, v.y, v.z, a.rnd.nextInt(3) == 0 ? 0xFFFFFF : a.rgb,
                    0.16f + a.rnd.nextFloat() * 0.14f, 12 + a.rnd.nextInt(10), 0f, true);
        }
        if (a.rnd.nextInt(2) == 0) {
            double ang = (a.rnd.nextDouble() * 2 - 1) * HALF_ANGLE;
            Vec3 r = radial(a, ang);
            Vec3 p = a.piv.add(r.scale(d));
            FxClient.puff(FxParticle.STAR, p.x, p.y, p.z, r.x * 0.03, 0.03, r.z * 0.03, 0xFFFFFF, 0.2f, 18, -0.002f, true);
        }
    }

    /** Hạt sáng nhỏ (đuôi kiếm đang bay). */
    public static void spark(Vec3 p, Vec3 v, int rgb, float size, int life) {
        FxClient.puff(FxParticle.GLOW, p.x, p.y, p.z, v.x, v.y, v.z, rgb, size, life, 0f, true);
    }

    static void render(FxDraw g, float pt) {
        float boost = com.hukhong.client.compat.ShaderCompat.shadersActive() ? 1.35f : 1f;
        for (Arc a : ARCS) renderArc(a, g, pt, boost);
    }

    private static void renderArc(Arc a, FxDraw g, float pt, float boost) {
        float t = a.age + pt;
        int segs = (int) (26 * (0.6f + 0.4f * FxClient.q()));
        for (int tr = 0; tr < 3; tr++) {
            double tt = t - tr * 1.5;
            if (tt < 0) break;
            double k = tt / a.life;
            if (k >= 1) continue;
            float peak = Math.min(1f, (float) tt / 1.2f) * (float) Math.pow(1 - k, 0.9)
                    * (tr == 0 ? 1f : tr == 1 ? 0.4f : 0.18f) * boost;
            double d = radiusAt(a, tt) * (1 - 0.04 * tr);
            double reveal = Math.min(1, tt / 3.0);
            double aEnd = HALF_ANGLE - 2 * HALF_ANGLE * reveal;
            double thick = 0.9 * (0.55 + 0.45 * (1 - k)) * (d / a.reach);
            double hgt = 0.38;
            int rim = FxDraw.white(a.rgb, 0.5f), body = a.rgb;
            Vec3 po = null, pi = null, pu = null, pdn = null;
            double ptp = 0;
            for (int i = 0; i <= segs; i++) {
                double ang = HALF_ANGLE + (aEnd - HALF_ANGLE) * i / segs;
                double s = (ang + HALF_ANGLE) / (2 * HALF_ANGLE);
                double tp = Math.pow(Math.max(0, Math.sin(Math.PI * s)), 0.75);
                Vec3 r = radial(a, ang);
                Vec3 o = a.piv.add(r.scale(d));
                Vec3 in = o.subtract(r.scale(thick * tp));
                Vec3 up = o.add(a.nrm.scale(hgt * tp)), dn = o.subtract(a.nrm.scale(hgt * tp * 0.7));
                float ca = peak * (float) (0.55 + 0.45 * tp);
                ca *= (float) Math.min(1.0, Math.max(0.25, (Math.sqrt(o.distanceToSqr(g.cam)) - 1.0) / 2.5)); // sát camera (góc nhìn thứ nhất): dịu bớt, khỏi che màn hình
                if (po != null) {
                    int ro = FxDraw.col(rim, 0.95f * ca), ri = FxDraw.col(body, 0.22f * ca);
                    g.quad2(po, o, in, pi, ro, ro, ri, ri);
                    int fo = FxDraw.col(rim, 0.55f * ca), fi = FxDraw.col(body, 0f);
                    g.quad2(po, o, up, pu, fo, fo, fi, fi);
                    g.quad2(po, o, dn, pdn, fo, fo, fi, fi);
                    // viền sáng hướng camera, rộng tối thiểu theo kích thước màn hình để luôn thấy
                    double wb = Math.max(0.05, 0.0045 * Math.sqrt(o.distanceToSqr(g.cam))) * (0.4 + 0.6 * tp);
                    double wa = Math.max(0.05, 0.0045 * Math.sqrt(po.distanceToSqr(g.cam))) * (0.4 + 0.6 * ptp);
                    int c3 = FxDraw.col(a.rgb, 0.16f * ca), c2 = FxDraw.col(a.rgb, 0.5f * ca), c1 = FxDraw.col(0xFFFFFF, Math.min(1f, ca));
                    g.ribbon(po, o, wa * 3.4, wb * 3.4, c3, c3);
                    g.ribbon(po, o, wa * 1.8, wb * 1.8, c2, c2);
                    g.ribbon(po, o, wa * 0.7, wb * 0.7, c1, c1);
                }
                po = o; pi = in; pu = up; pdn = dn; ptp = tp;
            }
        }
    }

    // ---------------------------------------------------------------- kiếm

    /** Quầng sáng bám thân kiếm (tâm center, trục dir đơn vị, dài len): vành mờ -> lõi trắng, đầu mũi thon + đốm sáng. */
    public static void blade(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3 center, Vec3 dir, double len, int rgb, float alpha) {
        FxDraw g = new FxDraw(vc, m, cam);
        Vec3 a = center.subtract(dir.scale(len * 0.5)), b = center.add(dir.scale(len * 0.5));
        double w = len * 0.16;
        g.ribbon(a, b, w * 1.6, w * 0.5, FxDraw.col(rgb, alpha * 0.22f), FxDraw.col(rgb, alpha * 0.30f));
        g.ribbon(a, b, w * 0.9, w * 0.25, FxDraw.col(rgb, alpha * 0.40f), FxDraw.col(rgb, alpha * 0.60f));
        int core = FxDraw.white(rgb, 0.8f);
        g.ribbon(a, b, w * 0.35, w * 0.1, FxDraw.col(core, alpha * 0.5f), FxDraw.col(core, alpha * 0.95f));
        Vec3 up = dir.cross(b.subtract(cam));
        if (up.lengthSqr() > 1e-8) {
            up = up.normalize();
            int tc = FxDraw.col(core, alpha * 0.7f), tz = FxDraw.col(rgb, 0f);
            double r = len * 0.16;
            g.quad2(b.add(up.scale(r)), b.add(dir.scale(r * 1.4)), b.subtract(up.scale(r)), b.subtract(dir.scale(r * 0.4)), tz, tc, tz, tc);
        }
    }

    /** Dải sáng quét giữa hai mốc thời gian của thân kiếm (đuôi sáng): tip/base mỗi mốc, alpha mỗi mốc. */
    public static void sheet(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3 tipA, Vec3 baseA, Vec3 tipB, Vec3 baseB, int rgb, float aA, float aB) {
        FxDraw g = new FxDraw(vc, m, cam);
        int core = FxDraw.white(rgb, 0.55f);
        int ta = FxDraw.col(core, aA * 0.75f), tb = FxDraw.col(core, aB * 0.75f), ba = FxDraw.col(rgb, 0f);
        g.quad2(tipA, tipB, baseB, baseA, ta, tb, ba, ba);
    }
}
