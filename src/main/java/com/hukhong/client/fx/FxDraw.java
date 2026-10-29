package com.hukhong.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Bộ vẽ hình học cộng sáng (RenderType.lightning): ribbon hướng camera, vòng phẳng, đa giác. Toạ độ thế giới. */
final class FxDraw {
    static final Vec3 UP = new Vec3(0, 1, 0);
    final VertexConsumer vc; final Matrix4f m; final Vec3 cam;
    /** Hệ số độ đậm chung (hiệu ứng của người chơi khác = 0.5). */
    float gain = 1f;

    FxDraw(VertexConsumer vc, Matrix4f m, Vec3 cam) { this.vc = vc; this.m = m; this.cam = cam; }

    static int col(int rgb, float a) {
        int ai = (int) (Math.max(0f, Math.min(1f, a)) * 255);
        return ai << 24 | (rgb & 0xFFFFFF);
    }

    static int white(int rgb, float t) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        r += (int) ((255 - r) * t); g += (int) ((255 - g) * t); b += (int) ((255 - b) * t);
        return r << 16 | g << 8 | b;
    }

    void v(Vec3 p, int c) {
        vc.addVertex(m, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z))
                .setColor(c >> 16 & 255, c >> 8 & 255, c & 255, gain >= 0.999f ? c >>> 24 : (int) ((c >>> 24) * gain));
    }

    void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int ca, int cb, int cc, int cd) { v(a, ca); v(b, cb); v(c, cc); v(d, cd); }

    /** Quad hai mặt (phòng khi bật cull): phát cả hai chiều quấn. */
    void quad2(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int ca, int cb, int cc, int cd) {
        v(a, ca); v(b, cb); v(c, cc); v(d, cd);
        v(d, cd); v(c, cc); v(b, cb); v(a, ca);
    }

    void tri(Vec3 a, Vec3 b, Vec3 c, int col) { v(a, col); v(b, col); v(c, col); v(c, col); }

    /** Dải hướng camera từ a->b, nửa rộng wa/wb. */
    void ribbon(Vec3 a, Vec3 b, double wa, double wb, int ca, int cb) {
        Vec3 d = b.subtract(a);
        Vec3 s = d.cross(a.subtract(cam));
        double l = s.lengthSqr();
        if (l < 1e-10) return;
        s = s.scale(1 / Math.sqrt(l));
        quad(a.add(s.scale(wa)), a.subtract(s.scale(wa)), b.subtract(s.scale(wb)), b.add(s.scale(wb)), ca, ca, cb, cb);
    }

    /** Đường phát sáng nhiều lớp (viền mờ -> lõi trắng). */
    void glowLine(Vec3 a, Vec3 b, double w, int rgb, float alpha) {
        ribbon(a, b, w * 3.2, w * 3.2, col(rgb, alpha * 0.10f), col(rgb, alpha * 0.10f));
        ribbon(a, b, w * 1.8, w * 1.8, col(rgb, alpha * 0.25f), col(rgb, alpha * 0.25f));
        ribbon(a, b, w, w, col(rgb, alpha * 0.6f), col(rgb, alpha * 0.6f));
        int core = white(rgb, 0.85f);
        ribbon(a, b, w * 0.4, w * 0.4, col(core, alpha), col(core, alpha));
    }

    /** Dải nằm phẳng trên mặt đất. */
    void flatLine(Vec3 a, Vec3 b, double w, int ca, int cb) {
        Vec3 d = b.subtract(a);
        Vec3 s = d.cross(UP);
        double l = s.lengthSqr();
        if (l < 1e-10) return;
        s = s.scale(w / Math.sqrt(l));
        quad(a.add(s), a.subtract(s), b.subtract(s), b.add(s), ca, ca, cb, cb);
    }

    void flatGlow(Vec3 a, Vec3 b, double w, int rgb, float alpha) {
        flatLine(a, b, w * 2.6, col(rgb, alpha * 0.14f), col(rgb, alpha * 0.14f));
        flatLine(a, b, w, col(rgb, alpha * 0.6f), col(rgb, alpha * 0.6f));
        int core = white(rgb, 0.8f);
        flatLine(a, b, w * 0.35, col(core, alpha), col(core, alpha));
    }

    /** Vòng phẳng r0..r1; gap>0 => đứt nét (gap = số đoạn cứ 2 bỏ 1). */
    void flatRing(Vec3 c, double r0, double r1, int segs, double rot, int ci, int co, int skipEvery) {
        for (int i = 0; i < segs; i++) {
            if (skipEvery > 0 && i % skipEvery == skipEvery - 1) continue;
            double a0 = rot + Math.PI * 2 * i / segs, a1 = rot + Math.PI * 2 * (i + 1) / segs;
            double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
            quad(new Vec3(c.x + c0 * r0, c.y, c.z + s0 * r0), new Vec3(c.x + c0 * r1, c.y, c.z + s0 * r1),
                    new Vec3(c.x + c1 * r1, c.y, c.z + s1 * r1), new Vec3(c.x + c1 * r0, c.y, c.z + s1 * r0), ci, co, co, ci);
        }
    }

    /** Hình trụ đứng (tường/cột) bán kính r, cao y0..y1, alpha đáy/đỉnh. */
    void cylinder(Vec3 c, double r, double y0, double y1, int segs, int cb, int ct) {
        for (int i = 0; i < segs; i++) {
            double a0 = Math.PI * 2 * i / segs, a1 = Math.PI * 2 * (i + 1) / segs;
            double x0 = Math.cos(a0) * r, z0 = Math.sin(a0) * r, x1 = Math.cos(a1) * r, z1 = Math.sin(a1) * r;
            quad(new Vec3(c.x + x0, c.y + y0, c.z + z0), new Vec3(c.x + x1, c.y + y0, c.z + z1),
                    new Vec3(c.x + x1, c.y + y1, c.z + z1), new Vec3(c.x + x0, c.y + y1, c.z + z0), cb, cb, ct, ct);
        }
    }

    /** Đĩa phẳng toả tròn (tâm đậm -> mép nhạt). */
    void disc(Vec3 c, double r, int segs, int ci, int co) { flatRing(c, 0.0, r, segs, 0, ci, co, 0); }
}
