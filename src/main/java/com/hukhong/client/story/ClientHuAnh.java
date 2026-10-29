package com.hukhong.client.story;

/** Bản sao phía client của Hư Ảnh theo thành (chỉ số CityDef.ALL), 0..100. */
public final class ClientHuAnh {
    private ClientHuAnh() {}

    private static volatile int[] values = new int[10];

    public static int get(int cityIdx) { int[] v = values; return cityIdx < 0 || cityIdx >= v.length ? 0 : v[cityIdx]; }

    public static int average() {
        int[] v = values; if (v.length == 0) return 0;
        int s = 0; for (int x : v) s += x;
        return s / v.length;
    }

    public static void update(int[] v) { values = v; }
}
