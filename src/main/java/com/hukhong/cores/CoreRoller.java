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

    /** Xác suất gốc (%) theo mốc # (1..11): Bạch,Hoàng,Tử,Hắc,Hồng,Thất Thải,Cửu Thải. Mốc 11 (cấp 99) cố định 80/20. */
    private static final int[][] BASE = {
            {85, 15, 0, 0, 0, 0, 0}, {60, 40, 0, 0, 0, 0, 0}, {25, 55, 20, 0, 0, 0, 0}, {10, 45, 45, 0, 0, 0, 0},
            {0, 20, 45, 35, 0, 0, 0}, {0, 10, 40, 50, 0, 0, 0}, {0, 0, 30, 50, 20, 0, 0}, {0, 0, 15, 50, 35, 0, 0},
            {0, 0, 5, 40, 55, 0, 0}, {0, 0, 5, 40, 55, 0, 0}, {0, 0, 0, 0, 0, 80, 20}
    };
    /** Trần phẩm theo mốc (tăng theo cấp): Bạch..Hồng = 0..4; mốc 11 (cấp 99) tới Cửu Thải = 6. */
    private static final int[] CEIL = {1, 1, 2, 2, 3, 3, 4, 4, 4, 4, 6};
    public static final int MILESTONE_99 = 11;
    private static final int THAT_THAI = CoreTier.THAT_THAI.ordinal(), CUU_THAI = CoreTier.CUU_THAI.ordinal();

    /** Mỗi {@value #CAP_STEP_LEVELS} cấp, trần của "sàn phẩm" tăng 1 bậc. */
    public static final int CAP_STEP_LEVELS = 30;

    /** Cấp người chơi ứng với chỉ số mốc (1..9 -> x10, 10 -> 95, 11 -> 99, 12 -> 100). */
    public static int levelOfMilestone(int m) { return m <= 9 ? m * 10 : m == 10 ? 95 : m == 11 ? 99 : 100; }

    /** Trần của sàn phẩm tại cấp L: 1 + L/30 (Hoàng ở cấp < 30, Tử 30..59, Hắc 60..89, Hồng 90+). */
    public static int tierCapFor(int level) { return 1 + Math.max(0, level) / CAP_STEP_LEVELS; }

    /**
     * Sàn phẩm tại mốc m = min(phẩm tốt nhất từng roll, tierCapFor(cấp mốc)), không vượt trần phẩm của mốc đó.
     * Ví dụ: từng ra Tử ở cấp 40 -> cấp 50 và 60 tối thiểu Tử; cấp 20 (cap Hoàng) chỉ bị nâng tới Hoàng.
     */
    public static int floorTier(int bestRolled, int milestone) {
        int f = Math.min(Math.max(0, bestRolled), tierCapFor(levelOfMilestone(milestone)));
        return Math.min(f, ceilTier(milestone));
    }

    /** Δ: [≤-2, -1, 0, +1, ≥+2, Mắc Kẹt] theo tư chất D..SSS (S3 §2.6.3). */
    private static final int[][] DELTA = {
            {10, 22, 25, 25, 17, 1}, {9, 21, 30, 26, 13, 1}, {7, 20, 38, 24, 10, 1}, {6, 19, 42, 23, 9, 1},
            {5, 18, 46, 22, 8, 1}, {5, 17, 48, 21, 8, 1}, {4, 16, 50, 21, 8, 1}
    };
    private static final int[] APT_LUCK = {-6, -3, 0, 3, 6, 10, 15};

    public static int aptitudeLuck(Aptitude a) { return APT_LUCK[a.ordinal()]; }

    public static int ceilTier(int milestone) { return CEIL[clampM(milestone) - 1]; }

    private static int clampM(int m) { return Math.max(1, Math.min(CEIL.length, m)); }

    /** Phẩm cao nhất của ô thường/ô úp: mốc 99 tới Cửu Thải, còn lại tới Hồng. */
    private static int maxNormalTier(int m) { return m >= MILESTONE_99 ? CUU_THAI : 4; }

    /** Phân bố phẩm (%) sau khi áp Điểm Vận. */
    public static int[] tierWeights(int milestone, int luck) {
        int m = clampM(milestone);
        int[] p = BASE[m - 1].clone();
        if (m >= MILESTONE_99) return p; // cấp 99: đảm bảo >= Thất Thải, cố định 80% Thất Thải / 20% Cửu Thải (không theo Vận)
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

    public static Pending0 rollTier(int milestone, int luck, Random rnd) { return rollTier(milestone, luck, 0, rnd); }

    /** Roll phẩm rồi nâng lên sàn (bestRolled xem {@link #floorTier}); không bao giờ vượt trần mốc. */
    public static Pending0 rollTier(int milestone, int luck, int bestRolled, Random rnd) {
        int[] w = tierWeights(milestone, luck);
        int r = rnd.nextInt(100), acc = 0, tier = 0;
        for (int i = 0; i < w.length; i++) {
            acc += w[i];
            if (r < acc) { tier = i; break; }
        }
        int floor = floorTier(bestRolled, milestone);
        return new Pending0(Math.min(ceilTier(milestone), Math.max(tier, floor)));
    }

    public record Pending0(int tier) {}

    /** Mốc cấp 100 (R29): Thần Lõi. */
    public static final int THAN_MILESTONE = 12;

    /** Cấp 100: chỉ Thần Lõi (phẩm Kim) của các đạo người chơi; không ô úp / Mắc Kẹt. ids rỗng nếu chưa có đạo. */
    private static CoreSlots.Pending rollThan(int milestone, long seed, int luck, Set<String> pathIds,
                                              Map<ResourceLocation, CoreDef> all) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (var e : all.entrySet()) {
            CoreDef c = e.getValue();
            if (c.minTier() == CoreTier.KIM && pathIds.contains(c.path())) ids.add(e.getKey());
        }
        int kim = CoreTier.KIM.ordinal();
        ResourceLocation first = ids.isEmpty() ? ResourceLocation.fromNamespaceAndPath("hukhong", "none") : ids.get(0);
        return new CoreSlots.Pending(milestone, seed, luck, kim, ids, first, kim, false);
    }

    public static CoreSlots.Pending roll(int milestone, long seed, int luck, Aptitude apt, Set<String> pathIds,
                                         Set<ResourceLocation> owned, Map<ResourceLocation, CoreDef> all) {
        return roll(milestone, seed, luck, apt, pathIds, owned, all, 0);
    }

    /** bestRolled = phẩm chính tốt nhất người chơi từng được roll (CoreSlots.bestTier), dùng làm sàn. */
    public static CoreSlots.Pending roll(int milestone, long seed, int luck, Aptitude apt, Set<String> pathIds,
                                         Set<ResourceLocation> owned, Map<ResourceLocation, CoreDef> all, int bestRolled) {
        if (milestone >= THAN_MILESTONE) return rollThan(milestone, seed, luck, pathIds, all);
        // Lõi thường: loại Thần Lõi (min_tier Kim) khỏi mọi pool/fallback.
        Map<ResourceLocation, CoreDef> normal = new java.util.LinkedHashMap<>();
        all.forEach((k, v) -> { if (v.minTier() != CoreTier.KIM) normal.put(k, v); });
        all = normal;
        Random rnd = new Random(seed);
        int tier = rollTier(milestone, luck, bestRolled, rnd).tier();

        // 3 ô thường: cùng phẩm, thuộc đạo hoặc "any"; CHẶN Lõi trùng (đã sở hữu chỉ vào khi cạn kho).
        List<ResourceLocation> pool = filter(all, pathIds, tier, owned);
        if (pool.size() < 3) for (ResourceLocation id : all.keySet())
            if (!owned.contains(id) && !pool.contains(id)) pool.add(id);
        if (pool.size() < 3) for (ResourceLocation id : filter(all, pathIds, tier, Set.of()))
            if (!pool.contains(id)) pool.add(id);
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
        int fourthTier = Math.max(0, Math.min(Math.min(maxNormalTier(clampM(milestone)), CEIL[clampM(milestone) - 1] + 1), tier + delta));

        // Lõi mù: 60% pool đạo/any, 40% toàn bộ.
        List<ResourceLocation> fp = rnd.nextInt(100) < 60 ? filter(all, pathIds, fourthTier, owned) : new ArrayList<>();
        if (fp.isEmpty()) for (ResourceLocation id : all.keySet()) if (!owned.contains(id)) fp.add(id);
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
