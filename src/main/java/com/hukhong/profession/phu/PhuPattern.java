package com.hukhong.profession.phu;

import com.hukhong.phapkhi.PhapKhiKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Hinh hoc net phu van, dung chung client (ve, kiem tra ong) va server (cham lai). Khong phu thuoc Minecraft.
 * Net: 6..14 diem theo bac (seed = id loai phu, co dinh). Ong: +-R px, R tang theo cap nghe.
 */
public final class PhuPattern {
    private PhuPattern() {}

    /** Khung ve logic (px), toa do goc = goc tren-trai khung. */
    public static final int CANVAS = 200, MIN = 24, MAX = 176;
    /** Lay mau moi 64 ms (<= 200 mau cho 12 s). */
    public static final int SAMPLE_MS = 64, MAX_SAMPLES = 200;
    /** Lech ong lien tuc > 1 s = hong. */
    public static final int OUT_LIMIT_MS = 1000;
    /** Sai so thoi gian client -> server. */
    public static final int TIME_TOL_MS = 300;

    public static PhapKhiKind kindOf(String recipeId) {
        for (PhapKhiKind k : PhapKhiKind.values()) if (recipeId.equals("phu_" + k.id)) return k;
        return null;
    }

    static int clampTier(int tier) { return Math.max(1, Math.min(5, tier)); }

    /** So diem net: bac 1..5 -> 6, 8, 10, 12, 14. */
    public static int nodes(int tier) { return 6 + 2 * (clampTier(tier) - 1); }

    /** Thoi gian ve toi da (ms): bac 1 = 6 s, bac 5 = 12 s. */
    public static int limitMs(int tier) { return 6000 + 1500 * (clampTier(tier) - 1); }

    /** Ban kinh ong R (px): 10 o cap 0, tang 0.5 moi cap, toi da 20 o cap 20. */
    public static double radius(int level) { return 10 + Math.min(Math.max(level, 0), 20) / 2.0; }

    /** Net phu van (toa do trong khung CANVAS), co dinh theo id loai phu. */
    public static List<double[]> path(String kindId, int tier) {
        Random r = new Random(kindId.hashCode());
        int n = nodes(tier);
        List<double[]> pts = new ArrayList<>(n);
        double x = MIN + 20 + r.nextDouble() * (MAX - MIN - 40);
        double y = MIN + 20 + r.nextDouble() * (MAX - MIN - 40);
        double ang = r.nextDouble() * Math.PI * 2;
        pts.add(new double[]{x, y});
        for (int i = 1; i < n; i++) {
            ang += (r.nextDouble() * 2 - 1) * 1.9;
            double step = 26 + r.nextDouble() * 22;
            double nx = x + Math.cos(ang) * step, ny = y + Math.sin(ang) * step;
            if (nx < MIN || nx > MAX || ny < MIN || ny > MAX) {
                // di ra khoi khung: quay ve tam
                double center = Math.atan2(CANVAS / 2.0 - y, CANVAS / 2.0 - x);
                ang = center + (r.nextDouble() * 2 - 1) * 0.6;
                nx = x + Math.cos(ang) * step;
                ny = y + Math.sin(ang) * step;
                nx = Math.max(MIN, Math.min(MAX, nx));
                ny = Math.max(MIN, Math.min(MAX, ny));
            }
            x = nx;
            y = ny;
            pts.add(new double[]{x, y});
        }
        return pts;
    }

    /** Khoang cach tu diem toi net (polyline). */
    public static double distToPath(double px, double py, List<double[]> path) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i + 1 < path.size(); i++) {
            double[] a = path.get(i), b = path.get(i + 1);
            best = Math.min(best, distToSeg(px, py, a[0], a[1], b[0], b[1]));
        }
        return best;
    }

    /** Khoang cach tu diem toi polyline mau (xs, ys), n diem. */
    public static double distToTrace(double px, double py, int[] xs, int[] ys) {
        double best = Double.MAX_VALUE;
        if (xs.length == 1) return Math.hypot(px - xs[0], py - ys[0]);
        for (int i = 0; i + 1 < xs.length; i++) {
            best = Math.min(best, distToSeg(px, py, xs[i], ys[i], xs[i + 1], ys[i + 1]));
        }
        return best;
    }

    public static double distToSeg(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0 : ((px - ax) * dx + (py - ay) * dy) / len2;
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }
}
