package com.hukhong.entity;

import com.hukhong.core.CultivationService;
import com.hukhong.core.CultivationData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 2026-11-06: EXP theo cap nguoi choi - cap 1 ~9 con/cap, cap 100 ~50 con/cap (w 1.1); chenh cap bi chan, 1 lan giet <= 1.5 thanh. */
class HuHoaExpTest {
    static final double W = 1.1;

    @Test
    void earlyLevelsAreFast() {
        for (int lv = 1; lv <= 10; lv++) {
            double kills = 1.0 / (W * HuHoa.levelExpMult(lv));
            assertTrue(kills >= 8 && kills <= 13, "L" + lv + " kills=" + kills);
        }
    }

    @Test
    void lateLevelsAreNotTooFast() {
        for (int lv = 90; lv <= 100; lv++) {
            double kills = 1.0 / (W * HuHoa.levelExpMult(lv));
            assertTrue(kills >= 40 && kills <= 55, "L" + lv + " kills=" + kills);
        }
    }

    @Test
    void higherLevelMobsAreCapped() {
        int lv = 40;
        long req = CultivationService.expToNext(lv);
        long same = HuHoa.exp(1.0, lv, lv), far = HuHoa.exp(1.0, lv + 60, lv);
        assertEquals(same * 1.5, far, 2.0); // truoc day quai cao hon 60 cap cho ~ (100/40)^2.15 = 7 lan
        assertTrue(HuHoa.exp(500.0, lv, lv) <= req * HuHoa.MAX_BARS_PER_KILL + 1);
    }

    @Test
    void weakMobsGiveLittle() {
        assertTrue(HuHoa.exp(1.0, 1, 1) >= 1);
        assertTrue(HuHoa.exp(1.0, 1, 30) <= HuHoa.exp(1.0, 30, 30) * 0.11 + 1);
    }

    @Test
    void levelRequirementGrowsSoLateLevelsStillTakeLongerThanEarly() {
        assertTrue(CultivationService.expToNext(50) > CultivationService.expToNext(10));
        assertTrue(CultivationData.MAX_LEVEL >= 100);
    }
}
