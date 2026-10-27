package com.hukhong.skill;

import com.hukhong.core.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BanMenhSkillTest {
    @Test
    void lockedBelowUnlockLevelAndOnePerPath() {
        for (Path p : Path.values()) {
            if (p == Path.PHAP) continue; // Pháp Tu: phím R là kỹ năng nguyên tố, không có Bản Mệnh
            assertNull(BanMenhSkill.of(p, BanMenhSkill.UNLOCK_LEVEL - 1));
            assertNotNull(BanMenhSkill.of(p, BanMenhSkill.UNLOCK_LEVEL));
        }
        assertNull(BanMenhSkill.of(null, 100));
    }

    @Test
    void coefScalesWithLevelAndIsCapped() {
        BanMenhSkill k = BanMenhSkill.NHAT_KIEM;
        assertEquals(3.0, k.coef(40), 1e-9);
        assertEquals(3.0, k.coef(1), 1e-9);           // dưới mở: kẹp
        assertTrue(k.coef(80) > k.coef(60));
        assertEquals(6.0, k.coef(100), 1e-9);
        for (BanMenhSkill s : BanMenhSkill.values()) assertTrue(s.coef(100) <= 13.0 + 1e-9);
    }

    @Test
    void rangeGrowsToCap() {
        assertEquals(8.0, BanMenhSkill.NHAT_KIEM.range(40), 1e-9);
        assertEquals(20.0, BanMenhSkill.NHAT_KIEM.range(100), 1e-9);
        assertEquals(20.0, BanMenhSkill.NHAT_KIEM.range(500), 1e-9);
    }
}
