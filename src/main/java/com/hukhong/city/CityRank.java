package com.hukhong.city;

/** Chức vị thành (S6 §3.2, rút gọn: một loại điểm "Cống Hiến Thành" thay DV + ĐPT). Toàn bộ là hàm thuần. */
public final class CityRank {
    private CityRank() {}

    public enum Rank {
        CU_DAN, HO_VE, DOI_TRUONG, THONG_LINH, PHO_THANH_CHU, THANH_CHU;

        public String key() { return "hukhong.city.rank." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    private static final int[] BASE_POINTS = {0, 100, 300, 700, 1200, 2000};
    /** Cảnh giới tối thiểu theo [tier][rank]. */
    private static final int[][] LEVELS = {{0, 10, 20, 30, 40, 50}, {0, 30, 40, 50, 60, 70}, {0, 50, 60, 70, 80, 90}};
    private static final int BASE_CAP = 200;

    public static int pointsFor(Rank r, Tier t) { return (int) Math.round(BASE_POINTS[r.ordinal()] * t.mult); }

    public static int levelFor(Rank r, Tier t) { return LEVELS[t.ordinal()][r.ordinal()]; }

    /** Chức cao nhất thỏa điểm + cảnh giới. Thành Chủ chỉ khi lordFree (ghế trống hoặc chính mình); nếu không dừng ở Phó Thành Chủ. */
    public static Rank rankFor(int points, int level, Tier t, boolean lordFree) {
        Rank best = Rank.CU_DAN;
        for (Rank r : Rank.values()) {
            if (r == Rank.THANH_CHU && !lordFree) continue;
            if (points >= pointsFor(r, t) && level >= levelFor(r, t)) best = r;
        }
        return best;
    }

    /** Chức kế tiếp, hoặc null nếu đã Thành Chủ. */
    public static Rank next(Rank r) { return r == Rank.THANH_CHU ? null : Rank.values()[r.ordinal() + 1]; }

    /** Trần điểm kiếm được mỗi Chu Kỳ (7 ngày game). */
    public static int cycleCap(Tier t) { return (int) Math.round(BASE_CAP * t.mult); }

    /** Phần điểm thực sự được cộng sau khi áp trần. */
    public static int allowed(int earnedThisCycle, int add, Tier t) {
        return Math.max(0, Math.min(add, cycleCap(t) - earnedThisCycle));
    }

    /** Điểm cho một lần hạ quái Hư Hóa cấp huLevel. */
    public static int killPoints(int huLevel) { return 1 + Math.max(0, huLevel) / 5; }

    public static final int HERBS_PER_DONATION = 8;
    public static final int DONATION_POINTS = 20;
    public static final long CYCLE_TICKS = 7L * 24000L;
}
