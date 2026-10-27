package com.hukhong.bicanh;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Bảng thưởng rương Bí Cảnh (thuần Java). Ánh xạ sang ItemStack ở {@link BiCanhLoot}. */
public final class BiCanhRewards {
    private BiCanhRewards() {}

    public enum Kind { HON_TINH, LINH_THAO, HUYET_THAM, HOA_LIEN, THANH_DIEP, HOI_XUAN_DAN, PHA_CANH_DAN, HON_COT, SKILL_SCROLL }

    /** sub: PHA_CANH_DAN = chỉ số đan (0..5); HON_COT = bậc Hồn Cốt (HonCotMath.TIER_*); SKILL_SCROLL = phẩm bí tịch (SkillGrade ordinal). */
    public record Entry(Kind kind, int count, int sub) {}

    public static final Kind[] HERBS = {Kind.LINH_THAO, Kind.HUYET_THAM, Kind.HOA_LIEN, Kind.THANH_DIEP};
    public static final int PILL_KINDS = 6;

    /** Xác suất rơi Hồn Cốt theo cấp (Bạch/Hoàng không có). */
    public static double boneChance(BiCanhTier t) {
        return switch (t) { case TU -> 0.12; case HAC -> 0.22; case HONG -> 0.30; default -> 0.0; };
    }

    public static double pillChance(BiCanhTier t) { return 0.15 + 0.05 * t.ordinal(); }

    static int between(Random r, int lo, int hi) { return lo + r.nextInt(hi - lo + 1); }

    /** Bậc Hồn Cốt: Tử -> 0; Hắc -> 0 (70%) / 1; Hồng -> 1 (60%) / 2. */
    static int boneTier(BiCanhTier t, Random r) {
        return switch (t) {
            case HAC -> r.nextDouble() < 0.7 ? 0 : 1;
            case HONG -> r.nextDouble() < 0.6 ? 1 : 2;
            default -> 0;
        };
    }

    public static List<Entry> roll(BiCanhTier t, Random r) {
        int i = t.ordinal();
        List<Entry> out = new ArrayList<>();
        out.add(new Entry(Kind.HON_TINH, between(r, 2 + i, 4 + 2 * i), 0));
        Kind h1 = HERBS[r.nextInt(HERBS.length)];
        Kind h2 = HERBS[r.nextInt(HERBS.length)];
        out.add(new Entry(h1, between(r, 2, 4 + i), 0));
        if (h2 != h1) out.add(new Entry(h2, between(r, 2, 3 + i), 0));
        out.add(new Entry(Kind.HOI_XUAN_DAN, between(r, 1, 2), 0));
        if (r.nextDouble() < pillChance(t)) out.add(new Entry(Kind.PHA_CANH_DAN, 1, Math.min(PILL_KINDS - 1, i + r.nextInt(2))));
        if (r.nextDouble() < boneChance(t)) out.add(new Entry(Kind.HON_COT, 1, boneTier(t, r)));
        if (r.nextDouble() < com.hukhong.skillbook.SkillBookRewards.biCanhChance(i))
            out.add(new Entry(Kind.SKILL_SCROLL, 1, com.hukhong.skillbook.SkillBookRewards.biCanhGrade(i, r.nextDouble()).ordinal()));
        return out;
    }
}
