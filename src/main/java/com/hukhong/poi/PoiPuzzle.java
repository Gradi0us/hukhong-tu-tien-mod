package com.hukhong.poi;

import java.util.Random;

/**
 * Do tuan tu (Dong Phu: 4 can gat theo ngu hanh, Ngu Hanh Tran Dai: 5 tru). Thu tu dung = vong tuong sinh quay tu phan tu khoi dau theo seed;
 * layout = vi tri thuc te (xao theo seed) cua tung phan tu tren tuong.
 * Phan tu: 0 Moc, 1 Hoa, 2 Tho, 3 Kim, 4 Thuy (tuong sinh Moc-Hoa-Tho-Kim-Thuy). Dong Phu 4 can bo Kim.
 */
public final class PoiPuzzle {
    private PoiPuzzle() {}

    public static final String[] ELEM_KEYS = {"moc", "hoa", "tho", "kim", "thuy"};

    /** Danh sach phan tu tham gia (n=4: bo Kim). */
    public static int[] elements(int n) { return n >= 5 ? new int[]{0, 1, 2, 3, 4} : new int[]{0, 1, 2, 4}; }

    /** layout[i] = chi so can/tru chua phan tu thu i trong elements(n) (hoan vi theo seed). */
    public static int[] layout(int seed, int n) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        Random r = new Random(seed * 31L + n);
        for (int i = n - 1; i > 0; i--) { int j = r.nextInt(i + 1); int t = p[i]; p[i] = p[j]; p[j] = t; }
        return p;
    }

    /** Phan tu khoi dau (chi so trong elements(n)). */
    public static int start(int seed, int n) { return Math.floorMod(seed, n); }

    /** Thu tu can phai bam: moi phan tu (tu start, thuan tuong sinh) -> can/tru chua no. */
    public static int[] slotOrder(int seed, int n) {
        int[] lay = layout(seed, n);
        int s = start(seed, n);
        int[] out = new int[n];
        for (int k = 0; k < n; k++) out[k] = lay[(s + k) % n];
        return out;
    }

    /** Phan tu (0..4) tai can/tru slot. */
    public static int elementAt(int seed, int n, int slot) {
        int[] lay = layout(seed, n);
        int[] el = elements(n);
        for (int i = 0; i < n; i++) if (lay[i] == slot) return el[i];
        return 0;
    }

    /** Ket qua bam: progress moi; solved; reset (sai thu tu -> ve 0). */
    public record Step(int progress, boolean solved, boolean reset) {}

    public static Step press(int[] order, int progress, int slot) {
        if (progress >= order.length) return new Step(progress, true, false);
        if (order[progress] == slot) {
            int np = progress + 1;
            return new Step(np, np >= order.length, false);
        }
        return new Step(0, false, true);
    }
}
