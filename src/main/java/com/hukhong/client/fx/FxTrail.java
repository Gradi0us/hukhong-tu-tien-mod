package com.hukhong.client.fx;

import com.hukhong.client.ClientConfig;
import com.hukhong.client.fx.FxClient.Inst;
import com.hukhong.network.FxPayload;
import com.hukhong.vfx.FxType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.hukhong.client.fx.FxDraw.col;
import static com.hukhong.client.fx.FxDraw.white;

/**
 * F1 - Ribbon / trail: một điểm đầu chạy theo cung (hoặc xoắn ốc) từ pos tới pos+dir, để lại dải sáng mờ dần,
 * màu chuyển trắng-nóng (đầu) -> màu chủ đạo -> trong suốt (đuôi). Chỉ dùng additive, không hạt nặng.
 * Mã hoá gói (xem Fx.ribbon): scale = nửa rộng dải; color = rgb | arcByte<<24; life = flight | delay<<10 | wave<<18 | mode<<23.
 * Client cũng gọi trực tiếp {@link #follow} để vệt bám theo điểm di chuyển bất kỳ (thực thể, đạn).
 */
final class FxTrail {
    private FxTrail() {}

    /** Số tick đuôi còn thấy được sau khi điểm đầu đi qua. */
    static final int TAIL = 14;

    /** Điểm đã đi qua: vị trí + thời điểm (tick, lẻ). */
    static final class Pts {
        final List<Vec3> p = new ArrayList<>();
        final List<Float> t = new ArrayList<>();
        void add(Vec3 v, float time, int cap) {
            p.add(v); t.add(time);
            while (p.size() > cap) { p.remove(0); t.remove(0); }
        }
    }

    static void receive(FxPayload pl, Vec3 pos, Vec3 dir, float sc, int rgb, boolean dim) {
        int life = pl.life();
        int flight = Math.max(2, life & 0x3FF), delay = (life >> 10) & 0xFF, wave = (life >> 18) & 31, mode = (life >> 23) & 3;
        int ab = pl.color() >>> 24;
        Inst i = FxClient.add(FxType.RIBBON, pos, dir, sc <= 0 ? 0.12f : sc, rgb, flight + TAIL, delay);
        i.dim = dim; i.flight = flight; i.mode = mode; i.amp = wave * 0.1f;
        i.arc = mode == 1 ? ab / 255f * (float) (Math.PI * 2) : (ab == 0 ? 0 : (ab - 128) / 8f);
        i.data = new Pts();
    }

    /** Vệt bám theo điểm do client điều khiển: gọi mỗi tick qua {@link Pts#add}. Trả Inst để huỷ sớm. */
    static Inst follow(Vec3 start, float width, int rgb, int life) {
        Inst i = FxClient.add(FxType.RIBBON, start, Vec3.ZERO, width, rgb, life, 0);
        i.flight = 0; i.mode = 2; i.data = new Pts(); i.started = true;
        return i;
    }

    /** Vị trí điểm đầu ở thời điểm age (tick, lẻ). */
    static Vec3 head(Inst f, float age) {
        if (f.mode == 2) return f.pos;
        float u = Math.max(0f, Math.min(1f, age / Math.max(1, f.flight)));
        float e = u * u * (3 - 2 * u) * 0.5f + u * 0.5f; // ease nhẹ: không dừng đột ngột
        Vec3 d = f.dir;
        Vec3 base = f.pos.add(d.scale(e));
        double len = d.length();
        if (len < 1e-4) return base;
        Vec3 n = d.scale(1 / len);
        Vec3 up = Math.abs(n.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = n.cross(up).normalize();
        Vec3 lift = side.cross(n).normalize(); // gần với "lên" nhưng vuông góc trục
        double env = Math.sin(Math.PI * u);
        if (f.mode == 1) {
            double th = u * 2.5 * Math.PI * 2 + f.arc;
            return base.add(side.scale(Math.cos(th) * f.amp)).add(lift.scale(Math.sin(th) * f.amp));
        }
        Vec3 off = lift.scale(f.arc * env);
        if (f.amp > 0.001f) off = off.add(side.scale(Math.sin(u * Math.PI * 3) * f.amp * env));
        return base.add(off);
    }

    static void tick(Inst f) {
        Pts pts = (Pts) f.data;
        if (pts == null) return;
        int cap = ClientConfig.fxTrailPoints() * 3;
        if (f.mode == 2) { pts.add(f.pos, f.age, cap); return; }
        int sub = FxClient.q() >= 0.7f ? 3 : 2;
        if (f.age <= f.flight) {
            for (int s = 1; s <= sub; s++) {
                float a = f.age + (float) s / sub;
                pts.add(head(f, a), a, cap);
            }
            Vec3 h = head(f, f.age + 1);
            FxClient.puffA(FxParticle.GLOW, h.x, h.y, h.z, 0, 0, 0, white(f.rgb, 0.5f), 0.3f + f.scale * 1.4f, 5, 0f, true, 0.7f);
            if (f.age % 2 == 0 && FxClient.q() >= 0.7f)
                FxClient.puffA(FxParticle.STAR, h.x, h.y, h.z, (f.rnd.nextDouble() - 0.5) * 0.06, 0.02, (f.rnd.nextDouble() - 0.5) * 0.06, f.rgb, 0.18f, 10, 0f, true, 0.8f);
        }
    }

    static void render(Inst f, FxDraw g, float pt) {
        Pts pts = (Pts) f.data;
        if (pts == null || pts.p.size() < 2) return;
        float t = f.age + pt;
        int n = pts.p.size();
        int hot = white(f.rgb, 0.85f), mid = white(f.rgb, 0.25f);
        double w0 = f.scale;
        Vec3 prev = null; float prevF = 0;
        List<Vec3> pp = new ArrayList<>(pts.p);
        List<Float> tt = new ArrayList<>(pts.t);
        // điểm đầu theo thời gian thực (mượt giữa các tick)
        if (f.mode != 2 && t <= f.flight + 0.001f) { pp.add(head(f, t)); tt.add(t); }
        else if (f.mode == 2) { pp.add(f.pos); tt.add(t); }
        for (int i = 0; i < pp.size(); i++) {
            float age = t - tt.get(i);
            float fade = 1f - age / TAIL;
            if (fade <= 0.02f) { prev = null; continue; }
            fade = fade * fade * (3 - 2 * fade); // smoothstep
            Vec3 cur = pp.get(i);
            if (prev != null && cur.distanceToSqr(prev) > 1e-6) {
                float fa = Math.min(1f, prevF), fb = Math.min(1f, fade);
                double wa = w0 * (0.15 + 0.85 * fa), wb = w0 * (0.15 + 0.85 * fb);
                int ca = fa > 0.7f ? hot : mid, cb = fb > 0.7f ? hot : mid;
                g.ribbon(prev, cur, wa * 3.0, wb * 3.0, col(f.rgb, 0.10f * fa), col(f.rgb, 0.10f * fb));
                g.ribbon(prev, cur, wa * 1.5, wb * 1.5, col(f.rgb, 0.32f * fa), col(f.rgb, 0.32f * fb));
                g.ribbon(prev, cur, wa * 0.55, wb * 0.55, col(ca, 0.95f * fa), col(cb, 0.95f * fb));
            }
            prev = cur; prevF = fade;
        }
    }
}
