package com.hukhong.world;

import com.hukhong.charcreate.Origin;
import com.hukhong.charcreate.TienThien;
import com.hukhong.core.Aptitude;
import com.hukhong.entity.HuHoa;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class W2MathTest {
    @Test void tienThienRanges() {
        for (Aptitude a : Aptitude.values()) {
            for (int s = 0; s < 200; s++) {
                int v = TienThien.roll(List.of(a), new Random(s));
                assertTrue(v >= TienThien.min(a) && v <= TienThien.max(a), a + " " + v);
            }
        }
        assertEquals(10, TienThien.roll(List.of(Aptitude.SSS), new Random(1)));
        assertEquals(9, TienThien.roll(List.of(Aptitude.SS), new Random(1)));
        assertEquals(8, TienThien.roll(List.of(Aptitude.S), new Random(1)));
        assertEquals(1, TienThien.roll(List.of(Aptitude.D), new Random(1)));
        assertEquals(Aptitude.S, TienThien.best(List.of(Aptitude.D, Aptitude.S, Aptitude.B)));
        assertEquals(6, TienThien.min(Aptitude.A)); assertEquals(7, TienThien.max(Aptitude.A));
    }

    @Test void ageBonus() {
        assertEquals(0, HuHoa.ageBonus(0));
        assertEquals(0, HuHoa.ageBonus(2));
        assertEquals(1, HuHoa.ageBonus(3));
        assertEquals(10, HuHoa.ageBonus(30));
        assertEquals(HuHoa.AGE_BONUS_CAP, HuHoa.ageBonus(100_000));
        assertEquals(0, HuHoa.ageBonus(-5));
        assertEquals(1, HuHoa.levelFor(0, 0, 0));
        assertEquals(1 + 5, HuHoa.levelFor(0, 5, 0));
        assertEquals(HuHoa.MAX_MOB_LEVEL, HuHoa.levelFor(20000, 30, 1));
    }

    @Test void lowLevelMobStillGivesExp() {
        // truoc fix: round(40/540)=0
        assertTrue(HuHoa.exp(1.0, 1, 1) >= 1);
        assertTrue(HuHoa.exp(1.0, 2, 5) >= 1);
        assertEquals(0, HuHoa.exp(1.0, 1, 20)); // quai thap hon >=10 cap: van bi phat
    }

    @Test void spawnBlockRule() {
        assertTrue(SpawnRules.blocked(true, true, false, false));   // zombie vanilla ngoai thanh
        assertFalse(SpawnRules.blocked(true, true, true, false));   // Hu Hoa ngoai thanh
        assertTrue(SpawnRules.blocked(true, true, true, true));     // Hu Hoa trong thanh
        assertFalse(SpawnRules.blocked(true, false, false, true));  // dong vat
        assertFalse(SpawnRules.blocked(false, true, false, true));  // spawner/lenh
        assertTrue(SafeZones.inBox(10, 10, 0, 0, 20));
        assertFalse(SafeZones.inBox(100, 0, 0, 0, 20));
    }

    @Test void originKinds() {
        assertEquals(OriginSpawn.Kind.CITY, OriginSpawn.kindOf(Origin.CITY));
        assertEquals(OriginSpawn.Kind.SECT, OriginSpawn.kindOf(Origin.SECT));
        assertEquals(OriginSpawn.Kind.VILLAGE, OriginSpawn.kindOf(Origin.BASTARD));
    }
}
