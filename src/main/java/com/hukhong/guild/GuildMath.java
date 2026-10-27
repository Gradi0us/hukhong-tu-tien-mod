package com.hukhong.guild;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Toan Hiep Hoi Mao Hiem Gia (thuan Java, kiem thu duoc): 6 bac, diem/cap can de thang bac, giam gia, nhiem vu ngay.
 * Bac: 0 Dong, 1 Bac, 2 Bach Ngan, 3 Vang, 4 Tu Kim, 5 Dau Vuong.
 */
public final class GuildMath {
    private GuildMath() {}

    public static final int RANKS = 6, SLOTS = 5, MAX_ACTIVE = 3;
    private static final int[] POINTS = {0, 100, 300, 800, 2000, 5000};
    private static final int[] LEVEL = {0, 10, 25, 45, 65, 85};
    public static final int SECT_PCT = 70;

    public static final int K_KILL = 0, K_HERB = 1, K_COURIER = 2, K_CLEANSE = 3, K_HUNT = 4, K_BICANH = 5;

    /** lvLo..lvHi: dai cap Hu Hoa (KILL); tier: bac Bi Canh (BICANH) hoac khoang cach tin su (COURIER). */
    public record Quest(int kind, int amount, int lvLo, int lvHi, int tier, long ha, int points, boolean bonus) {}

    /** Thu thach thang bac: kind HUNT (diet boss) hoac BICANH (clear bac tier). */
    public record Trial(int kind, int amount, int tier) {}

    public static int clampRank(int r) { return Math.max(0, Math.min(RANKS - 1, r)); }
    public static int pointsFor(int rank) { return POINTS[clampRank(rank)]; }
    public static int levelFor(int rank) { return LEVEL[clampRank(rank)]; }

    /** Du diem + cap de lam thu thach len bac rank+1. */
    public static boolean canTrial(int rank, int points, int level) {
        return rank >= 0 && rank < RANKS - 1 && points >= POINTS[rank + 1] && level >= LEVEL[rank + 1];
    }

    public static int discountPct(int rank) { return 5 * clampRank(rank); }

    /** Gia sau giam gia theo bac (toi thieu 1). */
    public static long discounted(long price, int rank) {
        if (price <= 0) return price;
        return Math.max(1, price * (100 - discountPct(rank)) / 100);
    }

    /** Thuong cho thanh vien tong mon: 70%. */
    public static long scale(long v, boolean sectMember) {
        if (!sectMember || v <= 0) return v;
        return Math.max(1, v * SECT_PCT / 100);
    }

    public static Trial trial(int nextRank) {
        return switch (clampRank(nextRank)) {
            case 1 -> new Trial(K_HUNT, 1, 0);
            case 2 -> new Trial(K_HUNT, 2, 0);
            case 3 -> new Trial(K_BICANH, 1, 1);
            case 4 -> new Trial(K_BICANH, 1, 2);
            default -> new Trial(K_BICANH, 1, 3);
        };
    }

    /** Loai nhiem vu mo theo bac. */
    public static List<Integer> pool(int rank) {
        List<Integer> p = new ArrayList<>(List.of(K_KILL, K_HERB, K_COURIER));
        if (rank >= 1) p.add(K_CLEANSE);
        if (rank >= 2) p.add(K_HUNT);
        if (rank >= 3) p.add(K_BICANH);
        return p;
    }

    /** Bac Bi Canh hop voi cap nguoi choi. */
    public static int bicanhTierFor(int level) { return level < 20 ? 0 : level < 45 ? 1 : level < 65 ? 2 : level < 85 ? 3 : 4; }

    /** Danh sach nhiem vu ngay: tat dinh theo (bac, ngay, cap luc lam moi, seed nguoi choi). */
    public static List<Quest> daily(int rank, long day, int level, long seed) {
        rank = clampRank(rank);
        Random r = new Random(seed * 0x9E3779B97F4A7C15L + day * 1_000_003L + rank * 31L + level);
        List<Integer> pool = pool(rank);
        List<Integer> kinds = new ArrayList<>(List.of(K_KILL, K_HERB));
        List<Integer> rest = new ArrayList<>(pool);
        rest.removeAll(kinds);
        while (kinds.size() < SLOTS) {
            if (rest.isEmpty()) rest = new ArrayList<>(pool);
            kinds.add(rest.remove(r.nextInt(rest.size())));
        }
        List<Quest> out = new ArrayList<>();
        for (int k : kinds) out.add(make(k, rank, level, r));
        return out;
    }

    private static Quest make(int kind, int rank, int level, Random r) {
        int lv = Math.max(1, level);
        boolean bonus = r.nextInt(4) == 0;
        return switch (kind) {
            case K_KILL -> {
                int n = 6 + 3 * rank + r.nextInt(5);
                yield new Quest(kind, n, Math.max(1, lv - 20), lv + 20, 0, (long) n * (2 + lv / 8), 4 + n / 2, bonus);
            }
            case K_HERB -> {
                int n = 8 + 2 * rank + r.nextInt(5);
                yield new Quest(kind, n, 0, 0, 0, 2L * n + lv, 4 + n / 3, bonus);
            }
            case K_COURIER -> {
                int dist = 70 + r.nextInt(71);
                yield new Quest(kind, 1, 0, 0, dist, 30L + 2L * lv + dist / 2, 10 + 2 * rank, bonus);
            }
            case K_CLEANSE -> {
                int n = 1 + rank / 2 + r.nextInt(2);
                yield new Quest(kind, n, 0, 0, 0, (long) n * (40 + lv), 8 * n + 6, bonus);
            }
            case K_HUNT -> new Quest(kind, rank >= 3 ? 2 : 1, 0, 0, 0, 100L + 6L * lv, 25 + 5 * rank, bonus);
            default -> {
                int t = bicanhTierFor(lv);
                yield new Quest(K_BICANH, 1, 0, 0, t, 200L + 250L * t, 40 + 15 * t, bonus);
            }
        };
    }
}
