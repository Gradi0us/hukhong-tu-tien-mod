package com.hukhong.resource;

import com.hukhong.entity.HuHoa;

/** Toan thuan cho linh thao: nua mua, tang truong/heo, pham chat theo vung + Linh Khi Trieu. */
public final class HerbMath {
    private HerbMath() {}

    public static final int MAX_AGE = 2, MAX_QUALITY = 3;

    /** Linh thao nao trong mua {@code season} (ordinal) co the nua? Linh Khi Trieu bo qua rang buoc mua. */
    public static boolean blooms(int seasonMask, int season, boolean trieu) {
        return trieu || (seasonMask & (1 << season)) != 0;
    }

    /** Tuoi sau mot lan random tick. growRoll/wiltRoll la ket qua tung xuc xac (true = trung). */
    public static int nextAge(int age, boolean bloom, boolean growRoll, boolean wiltRoll) {
        if (bloom) return growRoll ? Math.min(MAX_AGE, age + 1) : age;
        return wiltRoll ? Math.max(0, age - 1) : age;
    }

    /** Ngoai mua, cay hoang (khong do nguoi trong) o tuoi 0 se tan. */
    public static boolean witherAway(int age, boolean bloom, boolean cultivated, boolean roll) {
        return !bloom && !cultivated && age == 0 && roll;
    }

    /** Mau so xuc xac tang truong: 1/n. Linh Khi Trieu nhanh x3. */
    public static int growDenom(boolean trieu) { return trieu ? 3 : 9; }

    /** Pham chat 0..3 (Ha/Trung/Thuong/Cuc) theo cap vung (cap quai tuong ung khoang cach / vung xam thuc); Trieu +1. */
    public static int quality(int zoneLevel, boolean trieu) {
        int q = zoneLevel < 15 ? 0 : zoneLevel < 30 ? 1 : zoneLevel < 60 ? 2 : 3;
        return Math.min(MAX_QUALITY, q + (trieu ? 1 : 0));
    }

    /** Cap vung tai khoang cach d toi spawn (cung duong cong voi quai). */
    public static int zoneLevelAt(double distToSpawn) { return HuHoa.levelFor(distToSpawn, 0, 0); }

    /** So thao thu duoc: 1, +1 neu pham Thuong tro len, +1 (xac suat 50%) neu Cuc. */
    public static int dropCount(int quality, boolean bonusRoll) {
        return 1 + (quality >= 2 ? 1 : 0) + (quality >= 3 && bonusRoll ? 1 : 0);
    }
}
