package com.hukhong.entity;

import com.hukhong.core.CultivationService;
import com.hukhong.core.CultivationData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Muc tieu EXP quai: cap 1-20 ~25-35 con/cap, cap cao ~1.5-2% yeu cau cap moi con (w trung binh 1.1). */
class HuHoaExpTest {
    static final double W = 1.1;

    @Test
    void earlyLevelsNeedTwentyFiveToThirtyFiveKills() {
        for (int lv = 1; lv <= 20; lv++) {
            double kills = 1.0 / (W * HuHoa.levelExpMult(lv));
            assertTrue(kills >= 25 && kills <= 35, "L" + lv + " kills=" + kills);
        }
    }

    @Test
    void highLevelsGiveOnePointFiveToTwoPercentPerKill() {
        for (int lv = 90; lv <= 100; lv++) {
            double share = W * HuHoa.levelExpMult(lv);
            assertTrue(share >= 0.015 && share <= 0.02, "L" + lv + " share=" + share);
        }
    }

    @Test
    void killExpMatchesRequirementShareAtMidLevel() {
        int lv = 10;
        long req = CultivationService.expToNext(lv);
        long got = HuHoa.exp(W, lv, lv);
        assertEquals(W * HuHoa.levelExpMult(lv) * req, got, 1.0);
        assertTrue(got >= 1);
    }

    @Test
    void weakMobsStillGiveAtLeastOneExp() {
        assertTrue(HuHoa.exp(1.0, 1, 1) >= 1);
        assertEquals(0, HuHoa.exp(1.0, 1, 20)); // quai thap hon >=10 cap: x0.1
    }

    @Test
    void levelRequirementGrowsSoLateLevelsStillTakeLongerThanEarly() {
        assertTrue(CultivationService.expToNext(50) > CultivationService.expToNext(10));
        assertTrue(CultivationData.MAX_LEVEL >= 100);
    }
}
