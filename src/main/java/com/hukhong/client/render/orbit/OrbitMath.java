package com.hukhong.client.render.orbit;

/** Toán thuần quỹ đạo kiếm quanh người (không phụ thuộc Minecraft để test được). Trục: x/z ngang, y lên, gốc = chân. */
public final class OrbitMath {
    private OrbitMath() {}

    public static final double SPEED = 0.16; // rad / tick

    /** Offset cục bộ (so với chân) của kiếm idx/count tại thời điểm time (tick, có phần lẻ). out = {x, y, z}. */
    public static void orbit(int idx, int count, double time, double[] out) {
        boolean two = count > 3;
        int ring = two ? (idx & 1) : 0;
        int n = two ? (ring == 0 ? (count + 1) / 2 : count / 2) : count;
        int k = two ? idx >> 1 : idx;
        double dir = ring == 0 ? 1 : -1;
        double a = dir * time * SPEED + k * 2 * Math.PI / n + ring * Math.PI / n;
        double r = two ? (ring == 0 ? 1.05 : 0.9) : 0.95;
        double h = two ? (ring == 0 ? 1.55 : 0.75) : 1.2;
        h += 0.1 * Math.sin(time * 0.1 + idx * 1.7);
        out[0] = Math.cos(a) * r;
        out[1] = h;
        out[2] = Math.sin(a) * r;
    }

    /** Hệ số phi kiếm k trong [0,1] tại f tick kể từ lúc phóng: đi (delay tick) rồi về (ret tick). Ngoài khoảng -> -1. */
    public static double strikeBlend(double f, int delay, int ret) {
        if (f < 0 || f >= delay + ret) return -1;
        if (f < delay) {
            double u = f / delay;
            return 1 - Math.pow(1 - u, 3); // easeOutCubic
        }
        double u = (f - delay) / ret;
        return 1 - u * u * (3 - 2 * u);   // smoothstep ngược
    }
}
