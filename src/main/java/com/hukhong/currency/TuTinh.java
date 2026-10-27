package com.hukhong.currency;

import java.util.Random;

/**
 * Tu Tinh (tien te): Ha pham = 1, Trung pham = 100 Ha, Thuong pham = 100 Trung (10000 Ha). Thuan Java, kiem thu duoc.
 * Moi gia tri tinh bang "Ha" (long).
 */
public final class TuTinh {
    private TuTinh() {}

    public static final long TRUNG = 100L, THUONG = 10_000L;

    public static long value(long ha, long trung, long thuong) { return ha + trung * TRUNG + thuong * THUONG; }

    /** Dang chuan: {ha (0..99), trung (0..99), thuong}. */
    public static long[] canonical(long v) {
        v = Math.max(0, v);
        return new long[]{v % TRUNG, (v / TRUNG) % TRUNG, v / THUONG};
    }

    /** Ke hoach tra tien: so dong bo ra + tien thoi (gia tri Ha, se doi ra dang chuan). */
    public record Plan(long ha, long trung, long thuong, long change) {}

    /**
     * Tra price Ha tu vi (ha, trung, thuong). Dung dong nho truoc; neu thieu thi dung dong lon va tra lai tien thoi.
     * null neu khong du tien. Bao toan: value(truoc) - price == value(sau) (sau = con lai + canonical(change)).
     */
    public static Plan pay(long ha, long trung, long thuong, long price) {
        if (price < 0 || value(ha, trung, thuong) < price) return null;
        long rem = price;
        long uh = Math.min(ha, rem);
        rem -= uh;
        long ut = 0, uth = 0, change = 0;
        if (rem > 0) {
            ut = Math.min(trung, (rem + TRUNG - 1) / TRUNG);
            long paid = ut * TRUNG;
            if (paid >= rem) { change = paid - rem; rem = 0; } else rem -= paid;
        }
        if (rem > 0) {
            uth = Math.min(thuong, (rem + THUONG - 1) / THUONG);
            long paid = uth * THUONG;
            change = paid - rem;
        }
        return new Plan(uh, ut, uth, change);
    }

    // ---- nguon thu (gia tri Ha) ----

    /** Quai Hu Hoa thuong: cap 1..100 -> ~1..12 Ha. */
    public static long mobDrop(int level, Random r) {
        int lv = Math.max(1, level);
        return 1 + lv / 12 + r.nextInt(1 + lv / 20 + 1);
    }

    /** Xac suat roi tien cua quai thuong. */
    public static double mobDropChance() { return 0.55; }

    /** Boss (Hu Vuong, Su Do...): vai Trung pham, tang theo cap. mult: 1 thuong, 2 Chap Hanh Gia. */
    public static long bossDrop(int level, int mult, Random r) {
        int lv = Math.max(1, level);
        return ((long) (2 + lv / 8) * TRUNG + r.nextInt(50)) * Math.max(1, mult);
    }

    /** Ruong Bi Canh theo bac (0..4): tu vai Trung toi Thuong pham. */
    public static long bicanhChest(int tier, Random r) {
        int t = Math.max(0, Math.min(4, tier));
        long base = new long[]{3, 8, 20, 45, 100}[t] * TRUNG;
        return base + r.nextInt(30 * (t + 1));
    }

    /** Thuong nhiem vu tong mon. */
    public static long sectQuest(int tier, boolean finalStep) { return (15L + 25L * tier) * (finalStep ? 4 : 1); }

    /** Phi Bo Quan tra bang Tu Tinh thay vi Cong Hien: 3 Ha moi 1 diem Cong Hien. */
    public static long altarFee(int feeCh) { return 3L * Math.max(0, feeCh); }

    /** Gia quy doi tu "luc bao" cu: 1 ngoc = 10 Ha. */
    public static long fromEmerald(int emeralds) { return 10L * emeralds; }
}
