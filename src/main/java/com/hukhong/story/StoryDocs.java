package com.hukhong.story;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** 30 Văn Kiện: chuỗi A (10) -> Thạch Hống, chuỗi B (10) -> Bạch Từ Âm, bộ C (10) Di thư Tiền Nhân. Bậc: 0 Hạ, 1 Trung, 2 Thượng, 3 Đại. */
public final class StoryDocs {
    private StoryDocs() {}

    public record Doc(String id, char chain, int minTier, int maxTier) {
        public String itemId() { return "van_kien_" + id; }
        public String textKey() { return "hukhong.story.doc." + id; }
        public String itemKey() { return "item.hukhong." + itemId(); }
    }

    public static final int CLUES_FOR_LAIR = 3;
    /** Tỉ lệ rơi theo bậc Sứ Đồ (S4 R15): Hạ 6% · Trung 14% · Thượng 28% · Đại 55%. */
    public static final double[] CHANCE = {0.06, 0.14, 0.28, 0.55};

    public static final List<Doc> ALL = List.of(
            new Doc("a1", 'a', 0, 0), new Doc("a2", 'a', 0, 1), new Doc("a3", 'a', 1, 1),
            new Doc("a4", 'a', 1, 2), new Doc("a5", 'a', 2, 2), new Doc("a6", 'a', 3, 3),
            new Doc("a7", 'a', 1, 2), new Doc("a8", 'a', 2, 2), new Doc("a9", 'a', 2, 3), new Doc("a10", 'a', 3, 3),
            new Doc("b1", 'b', 0, 0), new Doc("b2", 'b', 0, 1), new Doc("b3", 'b', 1, 1),
            new Doc("b4", 'b', 1, 2), new Doc("b5", 'b', 2, 2), new Doc("b6", 'b', 3, 3),
            new Doc("b7", 'b', 1, 2), new Doc("b8", 'b', 2, 2), new Doc("b9", 'b', 2, 3), new Doc("b10", 'b', 3, 3),
            // Bộ C "Di thư Tiền Nhân": không rơi từ Sứ Đồ (roll bỏ qua), trao qua StoryHooks.complete (Bia Đá, Động Phủ, Thạch Khắc...)
            new Doc("c1", 'c', 0, 3), new Doc("c2", 'c', 0, 3), new Doc("c3", 'c', 0, 3), new Doc("c4", 'c', 0, 3), new Doc("c5", 'c', 0, 3),
            new Doc("c6", 'c', 0, 3), new Doc("c7", 'c', 0, 3), new Doc("c8", 'c', 0, 3), new Doc("c9", 'c', 0, 3), new Doc("c10", 'c', 0, 3));

    /** Số Văn Kiện của bộ {@code set} ('a'/'b'/'c') mà người chơi đã thu. */
    public static int countSet(net.minecraft.server.level.ServerPlayer p, char set) {
        return p.getData(StoryRegistry.STORY.get()).chainCount(set);
    }

    /** Tổng số Văn Kiện trong bộ. */
    public static int sizeOf(char set) {
        int n = 0;
        for (Doc d : ALL) if (d.chain() == set) n++;
        return n;
    }

    public static Doc get(String id) {
        for (Doc d : ALL) if (d.id().equals(id)) return d;
        return null;
    }

    /** Hạ <=25 · Trung <=45 · Thượng <=60 · Đại 61-65. */
    public static int tierOf(int level) { return level <= 25 ? 0 : level <= 45 ? 1 : level <= 60 ? 2 : 3; }

    public static int lairBit(char chain) { return chain == 'a' ? 1 : 2; }

    /** Tung rơi cho Sứ Đồ cấp {@code level}; null nếu không rơi. */
    public static Doc roll(RandomSource r, int level) {
        int tier = tierOf(level);
        if (r.nextDouble() >= CHANCE[tier]) return null;
        List<Doc> ok = new ArrayList<>();
        for (Doc d : ALL) if (d.chain() != 'c' && tier >= d.minTier() && tier <= d.maxTier()) ok.add(d);
        return ok.isEmpty() ? null : ok.get(r.nextInt(ok.size()));
    }
}
