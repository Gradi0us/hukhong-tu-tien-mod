package com.hukhong.cores;

import com.hukhong.core.Aptitude;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Roll thuần (S3 §2.2–2.6): phẩm P theo Điểm Vận, 3 ô thường cùng phẩm, ô Phó Mặc Số Phận. Tất định theo seed. */
public final class CoreRoller {
    private CoreRoller() {}

    /** Xác suất gốc (%) theo Lõi # (1..9): Bạch,Hoàng,Tử,Hắc,Hồng. */
    private static final int[][] BASE = {
            {85, 15, 0, 0, 0}, {60, 40, 0, 0, 0}, {25, 55, 20, 0, 0}, {10, 45, 45, 0, 0},
            {0, 20, 45, 35, 0}, {0, 10, 40, 50, 0}, {0, 0, 30, 50, 20}, {0, 0, 15, 50, 35}, {0, 0, 5, 40, 55}
    };
    private static final int[] CEIL = {1, 1, 2, 2, 3, 3, 4, 4, 4};

    /** Δ: [≤-2, -1, 0, +1, ≥+2, Mắc Kẹt] theo tư chất D..SSS (S3 §2.6.3). */
    private static final int[][] DELTA = {
            {10, 22, 25, 25, 17, 1}, {9, 21, 30, 26, 13, 1}, {7, 20, 38, 24, 10, 1}, {6, 19, 42, 23, 9, 1},
            {5, 18, 46, 22, 8, 1}, {5, 17, 48, 21, 8, 1}, {4, 16, 50, 21, 8, 1}
    };
    private static final int[] APT_LUCK = {-6, -3, 0, 3, 6, 10, 15};

    public static int aptitudeLuck(Aptitude a) { return APT_LUCK[a.ordinal()]; }

    public static int ceilTier(int milestone) { return CEIL[clampM(milestone) - 1]; }

    private static int clampM(int m) { return Math.max(1, Math.min(9, m)); }

    /** Phân bố phẩm (%) sau khi áp Điểm Vận. */
    public static int[] tierWeights(int milestone, int luck) {
        int m = clampM(milestone);
        int[] p = BASE[m - 1].clone();
        int ceil = CEIL[m - 1];
        int lowest = 0;
        while (lowest < ceil && BASE[m - 1][lowest] == 0) lowest++;
        for (int n = Math.abs(luck); n > 0; n--) {
            if (luck > 0) {
                int i = 0;
                while (i < ceil && p[i] == 0) i++;
                if (i >= ceil) break;
                p[i]--; p[ceil]++;
            } else {
                if (p[ceil] == 0 || lowest >= ceil) break;
                p[ceil]--; p[lowest]++;
            }
        }
        return p;
    }

    public static Pending0 rollTier(int milestone, int luck, Random rnd) {
        int[] w = tierWeights(milestone, luck);
        int r = rnd.nextInt(100), acc = 0, tier = 0;
        for (int i = 0; i < w.length; i++) {
            acc += w[i];
            if (r < acc) { tier = i; break; }
        }
        return new Pending0(tier);
    }

    public record Pending0(int tier) {}

    public static CoreSlots.Pending roll(int milestone, long seed, int luck, Aptitude apt, Set<String> pathIds,
                                         Set<ResourceLocation> owned, Map<ResourceLocation, CoreDef> all) {
        Random rnd = new Random(seed);
        int tier = rollTier(milestone, luck, rnd).tier();

        // 3 ô thường: cùng phẩm, thuộc đạo hoặc "any", hợp phẩm tối thiểu, ưu tiên chưa sở hữu.
        List<ResourceLocation> pool = filter(all, pathIds, tier, owned);
        if (pool.size() < 3) pool = filter(all, pathIds, tier, Set.of());
        if (pool.isEmpty()) pool = new ArrayList<>(all.keySet());
        List<ResourceLocation> picks = pickThree(pool, all, rnd);

        // Ô thứ 4: Δ phẩm + Mắc Kẹt.
        int[] d = DELTA[apt.ordinal()];
        int r = rnd.nextInt(100), acc = 0, k = d.length - 1;
        for (int i = 0; i < d.length; i++) {
            acc += d[i];
            if (r < acc) { k = i; break; }
        }
        boolean stuck = k == 5;
        int delta = switch (k) {
            case 0 -> -2 - (rnd.nextInt(4) == 0 ? 1 : 0);
            case 1 -> -1;
            case 3 -> 1;
            case 4 -> 2;
            default -> 0;
        };
        int fourthTier = Math.max(0, Math.min(Math.min(4, CEIL[clampM(milestone) - 1] + 1), tier + delta));

        // Lõi mù: 60% pool đạo/any, 40% toàn bộ.
        List<ResourceLocation> fp = rnd.nextInt(100) < 60 ? filter(all, pathIds, fourthTier, Set.of()) : new ArrayList<>();
        if (fp.isEmpty()) fp = new ArrayList<>(all.keySet());
        ResourceLocation fourth = fp.get(rnd.nextInt(fp.size()));

        return new CoreSlots.Pending(milestone, seed, luck, tier, picks, fourth, fourthTier, stuck);
    }

    private static List<ResourceLocation> filter(Map<ResourceLocation, CoreDef> all, Set<String> pathIds, int tier,
                                                 Set<ResourceLocation> owned) {
        List<ResourceLocation> out = new ArrayList<>();
        for (var e : all.entrySet()) {
            CoreDef c = e.getValue();
            if (owned.contains(e.getKey())) continue;
            if (c.minTier().ordinal() > tier) continue;
            boolean ok = c.path().equals("any");
            for (String p : pathIds) ok |= c.path().equals(p);
            if (pathIds.isEmpty()) ok = c.path().equals("any");
            if (ok) out.add(e.getKey());
        }
        return out;
    }

    /** 3 Lõi khác nhau, cố gắng ≥ 2 Hệ khác nhau. */
    private static List<ResourceLocation> pickThree(List<ResourceLocation> pool, Map<ResourceLocation, CoreDef> all,
                                                    Random rnd) {
        List<ResourceLocation> best = new ArrayList<>();
        for (int attempt = 0; attempt < 24; attempt++) {
            List<ResourceLocation> l = new ArrayList<>(pool);
            Collections.shuffle(l, rnd);
            best = new ArrayList<>(l.subList(0, Math.min(3, l.size())));
            Set<String> systems = new HashSet<>();
            for (ResourceLocation id : best) systems.add(all.get(id).system());
            if (systems.size() >= 2 || best.size() < 3) break;
        }
        while (best.size() < 3 && !pool.isEmpty()) best.add(pool.get(rnd.nextInt(pool.size()))); // pool nhỏ: lặp
        return best;
    }
}
