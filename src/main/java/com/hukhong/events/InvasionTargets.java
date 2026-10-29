package com.hukhong.events;

import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import com.hukhong.story.world.StoryHooks;
import com.hukhong.travel.TravelRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/** Chon thanh bi tap kich (PLAN-v2 s3 + s11 #6): 60% thanh gan voi chuong cao nhat, 40% ngau nhien trong thanh da mo dich chuyen. */
public final class InvasionTargets {
    private InvasionTargets() {}

    public static final double CHAPTER_SHARE = 0.60;

    /** Thuan: chuong -> thanh (PLAN-v2 s3). */
    public static int cityForChapter(int chapter) {
        return switch (Math.max(0, Math.min(10, chapter))) {
            case 0, 1 -> 0;
            case 2 -> 1;
            case 3 -> 2;
            case 4 -> 3;
            case 5 -> 4;
            case 6 -> 5;
            case 7 -> 6;
            case 8 -> 7;
            case 9 -> 8;
            default -> 9;
        };
    }

    /**
     * Thuan: 60% thanh theo chuong (neu hop le), con lai (hoac khi thanh chuong khong hop le) ngau nhien trong {@code unlocked}.
     * @param eligible thanh da dung + khong RUIN. @return chi so thanh, hoac -1 neu khong co thanh nao.
     */
    public static int pick(RandomSource r, int chapterCity, List<Integer> unlocked, IntPredicate eligible) {
        boolean byChapter = r.nextDouble() < CHAPTER_SHARE;
        if (byChapter && eligible.test(chapterCity)) return chapterCity;
        List<Integer> pool = new ArrayList<>();
        for (int i : unlocked) if (eligible.test(i) && !pool.contains(i)) pool.add(i);
        if (pool.isEmpty()) {
            if (eligible.test(chapterCity)) return chapterCity;
            for (int i = 0; i < CityDef.ALL.size(); i++) if (eligible.test(i)) pool.add(i);
            if (pool.isEmpty()) return -1;
        }
        return pool.get(r.nextInt(pool.size()));
    }

    /** Chon thanh cho tap kich tu trang thai the gioi (nguoi choi truc tuyen). -1 neu khong co thanh hop le. */
    public static int choose(ServerLevel ow) {
        int maxCh = 0;
        List<Integer> unlocked = new ArrayList<>(List.of(0));
        for (ServerPlayer p : ow.players()) {
            if (p.isSpectator()) continue;
            maxCh = Math.max(maxCh, StoryHooks.chapter(p));
            var seen = p.getData(TravelRegistry.TRAVEL.get());
            for (CityDef d : CityDef.ALL) if (seen.has("c:" + d.id())) unlocked.add(d.index());
        }
        return pick(ow.random, cityForChapter(maxCh), unlocked, i -> eligible(ow, i));
    }

    public static boolean eligible(ServerLevel ow, int idx) {
        if (idx < 0 || idx >= CityDef.ALL.size()) return false;
        CityDef d = CityDef.ALL.get(idx);
        return CityService.builtCenter(ow, d) != null && !CityStates.isRuin(ow, d);
    }
}
