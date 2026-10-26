package com.hukhong.society;

import com.hukhong.city.Tier;
import com.hukhong.core.Aptitude;
import com.hukhong.core.Path;

import java.util.Set;

/** Toán thuần của tông môn / Linh Khí Đài (test được, không phụ thuộc Minecraft runtime). */
public final class SectMath {
    private SectMath() {}

    public enum Rank {
        NGOAI, NOI, CHAN_TRUYEN, TRUONG_LAO;

        public String key() { return "hukhong.sect.rank." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    /** Mã kết quả xét tuyển. */
    public static final int OK = 0, LOW_LEVEL = 1, LOW_APT = 2, TOO_OLD = 3, WRONG_PATH = 4, ALREADY = 5, NOT_BUILT = 6;

    private static final int[][] RANK_LEVEL = {{20, 40, 60}, {30, 50, 70}, {40, 60, 80}};
    private static final int[][] RANK_CH = {{500, 3000, 12000}, {1500, 8000, 30000}, {4000, 20000, 70000}};
    private static final double[] RANK_MULT = {1, 1.5, 2, 2};
    private static final double[] BUFF_FACTOR = {0.25, 0.5, 1, 1};
    private static final int[] CYCLE_CAP = {400, 800, 1600};
    private static final double[] INVITE_BASE = {0.12, 0.06, 0.02};
    private static final double[] APT_MULT = {0.5, 0.8, 1, 1.5, 2.5, 4, 6};

    /** Bậc đệ tử theo cảnh giới + CH tích lũy (rút gọn: bỏ Đại Bỉ / chuỗi Truyền Thừa). */
    public static Rank rankFor(Tier t, int level, long totalCh) {
        Rank r = Rank.NGOAI;
        for (int i = 0; i < 3; i++)
            if (level >= RANK_LEVEL[t.ordinal()][i] && totalCh >= RANK_CH[t.ordinal()][i]) r = Rank.values()[i + 1];
        return r;
    }

    public static int rankChNeeded(Tier t, Rank next) { return RANK_CH[t.ordinal()][next.ordinal() - 1]; }

    public static int rankLevelNeeded(Tier t, Rank next) { return RANK_LEVEL[t.ordinal()][next.ordinal() - 1]; }

    /** Hệ số Linh Khí Đài theo bậc (R22). */
    public static double rankMult(Rank r) { return RANK_MULT[r.ordinal()]; }

    /** Hệ số buff tông theo bậc (S6 §2.1: 25% / 50% / 100% / 100%). */
    public static double buffFactor(Rank r) { return BUFF_FACTOR[r.ordinal()]; }

    /** Trần CH kiếm được mỗi Chu Kỳ. */
    public static int cycleCap(Tier t) { return CYCLE_CAP[t.ordinal()]; }

    /** R22: Bế Quan khoảng 0,1 E(L) mỗi giờ thực (~10 giờ/cấp). Trả EXP/giây. */
    public static double afkExpPerSecond(long expToNext, double mult) { return expToNext * 0.1 / 3600.0 * mult; }

    /** Xác suất mời mỗi lần kiểm (30 lần kiểm ~ nền x hệ số tư chất mỗi nửa giờ). */
    public static double inviteChance(Tier t, Aptitude apt) { return INVITE_BASE[t.ordinal()] * APT_MULT[apt.ordinal()] / 30.0; }

    public static Aptitude best(java.util.Collection<Aptitude> apts) {
        Aptitude b = Aptitude.D;
        for (Aptitude a : apts) if (a.ordinal() > b.ordinal()) b = a;
        return b;
    }

    /** Xét tuyển. invited: bỏ qua 1 mốc cảnh giới (-10), tuổi +5 (S6 §2.1). */
    public static int check(SectDef d, int level, Aptitude bestApt, int age, Set<Path> paths, boolean invited) {
        if (level < d.minLevel() - (invited ? 10 : 0)) return LOW_LEVEL;
        if (bestApt.ordinal() < d.minApt().ordinal()) return LOW_APT;
        if (age > d.maxAge() + (invited ? 5 : 0)) return TOO_OLD;
        if (d.banMa() && paths.contains(Path.MA)) return WRONG_PATH;
        if (d.multiPath() && paths.size() < 2) return WRONG_PATH;
        if (!d.paths().isEmpty() && java.util.Collections.disjoint(d.paths(), paths)) return WRONG_PATH;
        return OK;
    }

    public static int allowedCh(int earnedThisCycle, int add, Tier t) { return Math.max(0, Math.min(add, cycleCap(t) - earnedThisCycle)); }

    // ---- nhiệm vụ ----
    public static final String Q_KILL = "kill", Q_HERB = "herb";

    public static int questNeed(String q, Tier t) {
        if (Q_KILL.equals(q)) return 8 + 4 * t.ordinal();
        if (Q_HERB.equals(q)) return 10;
        return 0;
    }

    public static int questCh(String q, Tier t) {
        int base = Q_KILL.equals(q) ? 60 : 40;
        return (int) Math.round(base * (1 + 0.5 * t.ordinal()));
    }
}
