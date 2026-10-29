package com.hukhong.events;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvasionTargetsTest {
    @Test void chapterTableMatchesPlan() {
        assertEquals(0, InvasionTargets.cityForChapter(0));
        assertEquals(0, InvasionTargets.cityForChapter(1));
        assertEquals(1, InvasionTargets.cityForChapter(2));
        assertEquals(4, InvasionTargets.cityForChapter(5));
        assertEquals(6, InvasionTargets.cityForChapter(7));
        assertEquals(9, InvasionTargets.cityForChapter(10));
        assertEquals(9, InvasionTargets.cityForChapter(99));
    }

    @Test void sixtyPercentChapterFortyPercentRandomUnlocked() {
        RandomSource r = RandomSource.create(12345);
        List<Integer> unlocked = List.of(0, 2, 4, 6);
        int n = 20000, chapterHits = 0;
        int[] count = new int[10];
        for (int i = 0; i < n; i++) {
            int c = InvasionTargets.pick(r, 4, unlocked, x -> true);
            count[c]++;
            if (c == 4) chapterHits++;
        }
        // 60% chapter + 40% * (1/4 of unlocked) = 70%
        assertEquals(0.70, chapterHits / (double) n, 0.02);
        for (int c : new int[]{1, 3, 5, 7, 8, 9}) assertEquals(0, count[c], "city " + c + " not unlocked");
        assertEquals(0.10, count[0] / (double) n, 0.02);
    }

    @Test void ineligibleChapterCityFallsBackToUnlocked() {
        RandomSource r = RandomSource.create(1);
        for (int i = 0; i < 500; i++) {
            int c = InvasionTargets.pick(r, 9, List.of(0, 1), x -> x != 9);
            assertTrue(c == 0 || c == 1);
        }
        assertEquals(-1, InvasionTargets.pick(r, 3, List.of(0), x -> false));
    }

    @Test void ruinStateDefaults() {
        assertTrue(CityStates.defaultRuin(com.hukhong.city.CityDef.ALL.get(9)));
        assertTrue(CityStates.defaultRuin(com.hukhong.city.CityDef.ALL.get(3)));
        assertFalse(CityStates.defaultRuin(com.hukhong.city.CityDef.ALL.get(0)));
        assertFalse(CityStates.defaultRuin(com.hukhong.city.CityDef.ALL.get(5)));
    }
}
