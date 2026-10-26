package com.hukhong.pathrules;

import com.hukhong.core.Aptitude;
import com.hukhong.core.Path;
import com.hukhong.stats.StatService;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MaRulesTest {
    @Test
    void tiers() {
        assertEquals(0, MaRules.tierOf(79.9));
        assertEquals(1, MaRules.tierOf(80));
        assertEquals(2, MaRules.tierOf(90));
        assertEquals(3, MaRules.tierOf(95));
    }

    @Test
    void atkBonusCapped() {
        MaData m = new MaData();
        m.huyetTe = 100; m.acTinh = 85; m.lostTicks = 10; m.thanTicks = 10; // 0.5+0.2+0.25+0.4 > 0.9
        assertEquals(MaRules.ATK_CAP_SOLO, MaRules.atkBonus(m, false), 1e-9);
        assertEquals(MaRules.ATK_CAP_MULTI, MaRules.atkBonus(m, true), 1e-9);
        m.absorb = false; m.lostTicks = 0; m.thanTicks = 0; m.acTinh = 0;
        assertEquals(0, MaRules.atkBonus(m, false), 1e-9);
        assertTrue(MaRules.lifestealBonus(m) <= MaRules.HH_BONUS_CAP);
    }

    @Test
    void kiemHasHighestAttackAtEveryLevel() {
        for (int lv : new int[]{1, 10, 50, 100}) {
            double kiem = StatService.compute(lv, Set.of(Path.KIEM), Map.of(Path.KIEM, Aptitude.B)).attack();
            for (Path o : new Path[]{Path.DAO, Path.THE, Path.MA})
                assertTrue(kiem > StatService.compute(lv, Set.of(o), Map.of(o, Aptitude.B)).attack(), o + " @" + lv);
        }
    }
}
