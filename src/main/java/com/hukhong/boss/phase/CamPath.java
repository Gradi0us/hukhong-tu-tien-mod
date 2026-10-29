package com.hukhong.boss.phase;

import java.util.List;

/**
 * Toán thuần (không phụ thuộc MC) cho đường camera cutscene: nội suy Hermite (tiếp tuyến Catmull-Rom, thời gian không đều)
 * giữa các keyframe. Toạ độ keyframe tương đối boss: dx = phải, dy = lên, dz = phía trước (khung xoay theo hướng boss lúc bắt đầu).
 */
public final class CamPath {
    private CamPath() {}

    /** Một keyframe: thời điểm t (tick), vị trí tương đối, lệch góc nhìn (độ) so với hướng nhìn vào boss, FOV (độ), lookH = độ cao điểm nhìn trên chân boss. */
    public record Key(int t, float dx, float dy, float dz, float yaw, float pitch, float fov, float lookH) {}

    /** Kết quả nội suy tại một thời điểm. */
    public record Pose(float dx, float dy, float dz, float yaw, float pitch, float fov, float lookH) {}

    private static final int N = 7;

    private static float[] vec(Key k) { return new float[]{k.dx, k.dy, k.dz, k.yaw, k.pitch, k.fov, k.lookH}; }

    /** Mẫu tại thời điểm t (tick, có thể lẻ). Ngoài khoảng thì kẹp về keyframe đầu/cuối. Danh sách phải sắp theo t tăng dần. */
    public static Pose sample(List<Key> keys, float t) {
        if (keys.isEmpty()) return new Pose(0, 2, 8, 0, 0, 70, 1.5f);
        int n = keys.size();
        if (n == 1 || t <= keys.get(0).t) return of(vec(keys.get(0)));
        if (t >= keys.get(n - 1).t) return of(vec(keys.get(n - 1)));
        int i = 0;
        while (i < n - 2 && t >= keys.get(i + 1).t) i++;
        Key a = keys.get(i), b = keys.get(i + 1);
        float dt = Math.max(1e-3f, b.t - a.t);
        float u = (t - a.t) / dt;
        float u2 = u * u, u3 = u2 * u;
        float h00 = 2 * u3 - 3 * u2 + 1, h10 = u3 - 2 * u2 + u, h01 = -2 * u3 + 3 * u2, h11 = u3 - u2;
        float[] pa = vec(a), pb = vec(b), ma = tangent(keys, i), mb = tangent(keys, i + 1), out = new float[N];
        for (int c = 0; c < N; c++) out[c] = h00 * pa[c] + h10 * dt * ma[c] + h01 * pb[c] + h11 * dt * mb[c];
        return of(out);
    }

    /** Tiếp tuyến (đơn vị / tick) tại keyframe i: sai phân trung tâm, đầu/cuối dùng sai phân một phía. */
    private static float[] tangent(List<Key> keys, int i) {
        int n = keys.size();
        Key p = keys.get(Math.max(0, i - 1)), q = keys.get(Math.min(n - 1, i + 1));
        float span = Math.max(1e-3f, q.t - p.t);
        float[] a = vec(p), b = vec(q), m = new float[N];
        for (int c = 0; c < N; c++) m[c] = (b[c] - a[c]) / span;
        return m;
    }

    private static Pose of(float[] v) { return new Pose(v[0], v[1], v[2], v[3], v[4], v[5], v[6]); }

    /** Hệ số mờ vào/ra 0..1 cho thanh đen & chữ: tăng trong {@code in} tick đầu, giảm trong {@code out} tick cuối. */
    public static float envelope(float t, float total, float in, float out) {
        float a = Math.min(1f, Math.max(0f, t / Math.max(1e-3f, in)));
        float b = Math.min(1f, Math.max(0f, (total - t) / Math.max(1e-3f, out)));
        return Math.min(a, b);
    }
}
