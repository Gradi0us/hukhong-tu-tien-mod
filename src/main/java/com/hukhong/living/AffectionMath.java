package com.hukhong.living;

/** Toan thien cam (-100..100): giam gia, ngung nhiem vu dac biet, bien do thay doi. */
public final class AffectionMath {
    private AffectionMath() {}

    public static final int MIN = -100, MAX = 100;
    public static final int QUEST_MIN = 10, SPECIAL_MIN = 50;

    public static int clamp(int v) { return Math.max(MIN, Math.min(MAX, v)); }

    /** % giam gia tai thuong nhan: duong = giam, am = phu thu. Thien cam theo phe cua thanh. */
    public static int discountPct(int faction) {
        if (faction >= 80) return 15;
        if (faction >= 60) return 10;
        if (faction >= 40) return 6;
        if (faction >= 20) return 3;
        if (faction <= -60) return -15;
        if (faction <= -30) return -8;
        return 0;
    }

    /** Gia sau khi dieu chinh theo thien cam, toi thieu 1. */
    public static long adjust(long price, int faction) {
        int pct = discountPct(faction);
        if (pct == 0 || price <= 1) return price;
        return Math.max(1, price * (100 - pct) / 100);
    }

    /** Muc: 0 Ket oan (<=-60), 1 Ghet (<=-20), 2 La, 3 Quen (>=20), 4 Than (>=50), 5 Tri ky (>=80). */
    public static int tier(int a) {
        if (a <= -60) return 0;
        if (a <= -20) return 1;
        if (a >= 80) return 5;
        if (a >= 50) return 4;
        if (a >= 20) return 3;
        return 2;
    }

    /** Qua tang: thuong +3, vat pham mod +5, hiem +8. */
    public static int giftDelta(boolean mod, boolean rare) { return rare ? 8 : mod ? 5 : 3; }

    public static boolean offersQuest(int npcAff) { return npcAff >= QUEST_MIN; }

    public static boolean offersSpecial(int npcAff) { return npcAff >= SPECIAL_MIN; }
}
