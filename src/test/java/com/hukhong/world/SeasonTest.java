package com.hukhong.world;

import com.hukhong.world.calendar.LichMath;
import com.hukhong.world.season.Season;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeasonTest {
    @Test
    void fourSeasonsOfSevenLichDaysCycle() {
        assertEquals(Season.XUAN, Season.of(0));
        assertEquals(Season.XUAN, Season.of(7 * LichMath.DAY - 1));
        assertEquals(Season.HA, Season.of(7 * LichMath.DAY));
        assertEquals(Season.THU, Season.of(14 * LichMath.DAY));
        assertEquals(Season.DONG, Season.of(21 * LichMath.DAY));
        assertEquals(Season.XUAN, Season.of(28 * LichMath.DAY), "year wraps after 28 Lich days");
    }

    @Test
    void masksAreDistinctBits() {
        int all = 0;
        for (Season s : Season.values()) { assertEquals(0, all & s.mask()); all |= s.mask(); }
        assertEquals(0b1111, all);
    }

    @Test
    void tintStaysInRgbRange() {
        for (Season s : Season.values()) assertEquals(0, Season.tint(0xB4D8A0, s) >>> 24);
        assertNotEquals(Season.tint(0xB4D8A0, Season.THU), Season.tint(0xB4D8A0, Season.DONG));
    }
}
