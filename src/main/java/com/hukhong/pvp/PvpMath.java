package com.hukhong.pvp;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Toan thuan cua Dau Truong Sinh Tu (PLAN-v2 s11.1): dot 5%, pari-mutuel, tien cuoc toi thieu, can bang cap, chong gian lan. Kiem thu duoc. */
public final class PvpMath {
    private PvpMath() {}

    public static final int BURN_PCT = 5, ROUNDS = 3, WINS_NEEDED = 2, MAX_PER_PAIR_DAY = 3, BALANCE_GAP = 10;
    public static final int ROUND_TICKS = 3 * 60 * 20, COUNTDOWN_TICKS = 100, BREAK_TICKS = 100;
    public static final long DAY_TICKS = 96000L;

    /** Dot 5% (lam tron xuong) cua tong. */
    public static long burn(long total) { return Math.max(0, total) * BURN_PCT / 100; }

    /** Tong tien cua mot tran (hai ben cuoc bang nhau). */
    public static long duelTotal(long stakeEach) { return 2 * Math.max(0, stakeEach); }

    /** Nguoi thang nhan phan con lai sau khi dot. */
    public static long duelPrize(long stakeEach) { long t = duelTotal(stakeEach); return t - burn(t); }

    /** Tien cuoc toi thieu (Ha) theo cap cao hon trong hai ben: 100 Ha moi bac cam gioi (cap/10 + 1). */
    public static long minStake(int level) { return 100L * (1 + Math.max(1, level) / 10); }

    /** He so sat thuong cua ben thap hon khi chenh cap > 10 (toi da x2); chenh <= 10 hoac khong can bang: 1. */
    public static double balanceMul(int levelDiff, boolean balance) {
        if (!balance || levelDiff <= BALANCE_GAP) return 1.0;
        return Math.min(2.0, 1.0 + 0.04 * (levelDiff - BALANCE_GAP));
    }

    /** Khoa dem tran theo cap (khong phu thuoc thu tu) + ngay Lich. */
    public static String pairKey(UUID a, UUID b, long gameTime) {
        String x = a.toString(), y = b.toString();
        return (x.compareTo(y) <= 0 ? x + "|" + y : y + "|" + x) + "@" + Math.floorDiv(gameTime, DAY_TICKS);
    }

    public static boolean pairAllowed(int playedToday) { return playedToday < MAX_PER_PAIR_DAY; }

    /**
     * Quyet dinh hiep het gio: ben con nhieu % mau hon thang. @return 1 (A), -1 (B), 0 (hoa, chenh duoi 0.5%).
     */
    public static int timeoutWinner(double hpA, double hpB) {
        if (Math.abs(hpA - hpB) < 0.005) return 0;
        return hpA > hpB ? 1 : -1;
    }

    /** Ket qua tran sau cac hiep: ai du WINS_NEEDED thang; neu het hiep thi so hiep thang, hoa -> so sanh tong % mau, van hoa -> 0. */
    public static int matchWinner(int winsA, int winsB, double hpSumA, double hpSumB) {
        if (winsA != winsB) return winsA > winsB ? 1 : -1;
        return timeoutWinner(hpSumA, hpSumB);
    }

    // ------------------------------------------------------------------ pari-mutuel

    public record Bet(UUID who, int side, long amount) {}

    /** pay: tien tra moi nguoi (gom ca hoan tien); burned: so bi dot. Tong pay + burned = tong cuoc. */
    public record Settlement(Map<UUID, Long> pay, long burned) {
        public long paid() { long s = 0; for (long v : pay.values()) s += v; return s; }
    }

    /**
     * Pari-mutuel: ben thang chia (tong cuoc - 5% dot) theo ti le tien da cuoc. Hoan tien day du (khong dot) neu hoa (winner == 0)
     * hoac ben thang khong co cuoc nao / ben thua khong co cuoc nao. Phan du lam tron xuong bi dot (cong vao burned).
     */
    public static Settlement settle(List<Bet> bets, int winner) {
        Map<UUID, Long> pay = new LinkedHashMap<>();
        long sumWin = 0, sumLose = 0, total = 0;
        for (Bet b : bets) {
            total += b.amount();
            if (b.side() == winner) sumWin += b.amount(); else sumLose += b.amount();
        }
        if (winner == 0 || sumWin == 0 || sumLose == 0) {
            for (Bet b : bets) pay.merge(b.who(), b.amount(), Long::sum);
            return new Settlement(pay, 0);
        }
        long net = total - burn(total);
        BigInteger bn = BigInteger.valueOf(net), bw = BigInteger.valueOf(sumWin);
        for (Bet b : bets) {
            if (b.side() != winner) continue;
            long share = bn.multiply(BigInteger.valueOf(b.amount())).divide(bw).longValueExact();
            pay.merge(b.who(), share, Long::sum);
        }
        long paid = 0;
        for (long v : pay.values()) paid += v;
        return new Settlement(pay, total - paid);
    }

    /** Tong cuoc theo tung ben (index 0 = A, 1 = B). */
    public static long[] sideTotals(List<Bet> bets) {
        long[] t = new long[2];
        for (Bet b : bets) t[b.side() > 0 ? 0 : 1] += b.amount();
        return t;
    }

    // ------------------------------------------------------------------ giai 8 nguoi

    /** Ghep 8 slot: nguoi that truoc, con lai NPC tan tu. Tra danh sach 8 phan tu; moi phan tu la UUID nguoi hoac null (= NPC). */
    public static List<UUID> seedBracket(List<UUID> humans) {
        List<UUID> out = new ArrayList<>(8);
        for (int i = 0; i < 8; i++) out.add(i < humans.size() ? humans.get(i) : null);
        return out;
    }

    /** Giai do thuan: moi tran la cap lien tiep trong slot (0v1, 2v3, ...). Tra so tran moi vong: 4, 2, 1. */
    public static int matchesInRound(int round) { return 4 >> Math.max(0, Math.min(2, round)); }

    /** Giai thuong: tong phi dang ky cua nguoi that tru 5% dot; huy khi dem so. */
    public static long bracketPrize(long totalFees) { return totalFees - burn(totalFees); }

    /** Dem so tran moi cap/ngay (tien ich cho PvpData). */
    public static int bump(Map<String, Integer> m, String key) { return m.merge(key, 1, Integer::sum); }

    public static Map<String, Integer> newCounter() { return new HashMap<>(); }
}
