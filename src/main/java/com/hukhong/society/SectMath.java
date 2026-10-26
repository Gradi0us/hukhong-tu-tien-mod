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
    public static final int OK = 0, LOW_LEVEL = 1, LOW_APT = 2, TOO_OLD = 3, WRONG_PATH = 4, ALREADY = 5, NOT_BUILT = 6, NEED_INVITE = 7, NO_BAD_NAME = 8;

    private static final int[][] RANK_LEVEL = {{20, 40, 60}, {30, 50, 70}, {40, 60, 80}};
    private static final int[][] RANK_CH = {{500, 3000, 12000}, {1500, 8000, 30000}, {4000, 20000, 70000}};
    private static final double[] RANK_MULT = {1, 1.5, 2, 2};
    private static final double[] BUFF_FACTOR = {0.25, 0.5, 1, 1};
    private static final int[] CYCLE_CAP = {400, 800, 1600};
    private static final double[] INVITE_BASE = {0.12, 0.06, 0.02};
    private static final double[] APT_MULT = {0.5, 0.8, 1, 1.5, 2.5, 4, 6};

    /** Số lần thắng Đại Bỉ cần để giữ bậc (Nội 0, Chân Truyền 1, Trưởng Lão 2). */
    public static int daiBiNeeded(Rank r) { return Math.max(0, r.ordinal() - 1); }

    /** Bậc đệ tử: cảnh giới + CH tích lũy + số lần thắng Đại Bỉ (Chân Truyền cần 1, Trưởng Lão cần 2). */
    public static Rank rankFor(Tier t, int level, long totalCh, int daiBi) {
        Rank r = Rank.NGOAI;
        for (int i = 0; i < 3; i++)
            if (level >= RANK_LEVEL[t.ordinal()][i] && totalCh >= RANK_CH[t.ordinal()][i] && daiBi >= i) r = Rank.values()[i + 1];
        return r;
    }

    /** Bậc theo cảnh giới + CH, chưa tính Đại Bỉ (coi như đã qua mọi cửa ải). */
    public static Rank rankFor(Tier t, int level, long totalCh) { return rankFor(t, level, totalCh, 2); }

    /** Bậc mà Đại Bỉ lần kế mở khóa, hoặc null nếu chưa đủ cảnh giới/CH hay đã thắng đủ 2 lần. */
    public static Rank trialTarget(Tier t, int level, long totalCh, int daiBi) {
        if (daiBi < 0 || daiBi >= 2) return null;
        Rank target = Rank.values()[daiBi + 2];
        if (rankFor(t, level, totalCh, daiBi) != Rank.values()[daiBi + 1]) return null;
        return level >= rankLevelNeeded(t, target) && totalCh >= rankChNeeded(t, target) ? target : null;
    }

    /** Số quái phải hạ trong đấu trường Đại Bỉ. */
    public static int trialKills(Tier t, int daiBi) { return 8 + 4 * t.ordinal() + 6 * daiBi; }

    /** Giới hạn thời gian Đại Bỉ (tick). */
    public static int trialTicks(int daiBi) { return 1800 + 300 * daiBi; }

    /** Số ghế Linh Khí Đài mỗi tông. */
    public static int seats(Tier t) { return new int[]{3, 4, 6}[t.ordinal()]; }

    /** Phí Bế Quan mỗi giờ thực (trừ vào Cống Hiến khả dụng). */
    public static int altarFeePerHour(Tier t) { return new int[]{30, 60, 120}[t.ordinal()]; }

    public static final int FEE_PERIOD_SECONDS = 3600;

    /** Thu phí đúng 1 lần mỗi 3600 giây NGỒI cộng dồn (không reset khi đứng lên ngồi lại). */
    public static boolean feeDue(int sitSeconds) { return sitSeconds % FEE_PERIOD_SECONDS == 0; }

    /** Trả phí: ví mới, hoặc -1 nếu không đủ. */
    public static int chargeFee(int wallet, int fee) { return wallet >= fee ? wallet - fee : -1; }

    /** Tông ẩn: cần thư mời, và Ác Danh >= 1 sao. */
    public static int checkHidden(SectDef d, boolean invited, double acDanh) {
        if (!d.hidden()) return OK;
        if (!invited) return NEED_INVITE;
        return acDanh >= 1 ? OK : NO_BAD_NAME;
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

    /** Xét tuyển đầy đủ, gồm luật tông ẩn. */
    public static int check(SectDef d, int level, Aptitude bestApt, int age, Set<Path> paths, boolean invited, double acDanh) {
        int c = check(d, level, bestApt, age, paths, invited);
        return c != OK ? c : checkHidden(d, invited, acDanh);
    }

    public static int allowedCh(int earnedThisCycle, int add, Tier t) { return Math.max(0, Math.min(add, cycleCap(t) - earnedThisCycle)); }

    // ---- nhiệm vụ ----
    public static final String Q_KILL = "kill", Q_HERB = "herb", Q_CHAIN = "chain";
    public static final int CHAIN_STEPS = 3;

    /** Loại việc thật sự của nhiệm vụ: chuỗi luân phiên diệt / thảo dược. */
    public static String questKind(String q, int stage) {
        if (!Q_CHAIN.equals(q)) return q;
        return stage % 2 == 0 ? Q_KILL : Q_HERB;
    }

    public static int questNeed(String q, Tier t) { return questNeed(q, t, 0); }

    public static int questNeed(String q, Tier t, int stage) {
        String k = questKind(q, stage);
        int base = Q_KILL.equals(k) ? 8 + 4 * t.ordinal() : Q_HERB.equals(k) ? 10 : 0;
        return Q_CHAIN.equals(q) ? (int) Math.round(base * (1 + 0.5 * stage)) : base;
    }

    public static int questCh(String q, Tier t) { return questCh(q, t, 0); }

    public static int questCh(String q, Tier t, int stage) {
        String k = questKind(q, stage);
        int base = Q_KILL.equals(k) ? 60 : 40;
        double v = base * (1 + 0.5 * t.ordinal());
        return (int) Math.round(Q_CHAIN.equals(q) ? v * 1.5 * (1 + 0.5 * stage) : v);
    }

    /** Thưởng CH khi hoàn tất cả chuỗi. */
    public static int chainFinalCh(Tier t) { return (int) Math.round(200 * (1 + 0.5 * t.ordinal())); }
}
