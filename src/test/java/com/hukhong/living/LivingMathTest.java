package com.hukhong.living;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class LivingMathTest {
    @Test void scheduleCitizen() {
        assertEquals(NpcSchedule.WORK, NpcSchedule.activity(0, 3000));
        assertEquals(NpcSchedule.EAT, NpcSchedule.activity(0, 6100));
        assertEquals(NpcSchedule.EAT, NpcSchedule.activity(0, 12000));
        assertEquals(NpcSchedule.SLEEP, NpcSchedule.activity(0, 18000));
        assertEquals(NpcSchedule.WORK, NpcSchedule.activity(0, 24000 + 3000)); // wrap
        assertEquals(NpcSchedule.SLEEP, NpcSchedule.activity(0, -2000));       // -2000 == 22000
        assertEquals(NpcSchedule.WORK, NpcSchedule.activity(0, -1000));        // 23000: da day
    }

    @Test void scheduleRoles() {
        assertEquals(NpcSchedule.WORK, NpcSchedule.activity(1, 18000));   // linh canh dem
        assertEquals(NpcSchedule.SLEEP, NpcSchedule.activity(5, 13000));  // tre em ngu som
        assertEquals(NpcSchedule.WORK, NpcSchedule.activity(6, 18000));   // Dau Gia Su luon o vi tri
        assertEquals(NpcSchedule.WORK, NpcSchedule.activity(7, 18000));   // Thay Boi
        assertTrue(NpcSchedule.isNight(15000));
        assertFalse(NpcSchedule.isNight(5000));
    }

    @Test void affectionDiscount() {
        assertEquals(0, AffectionMath.discountPct(0));
        assertEquals(3, AffectionMath.discountPct(20));
        assertEquals(10, AffectionMath.discountPct(60));
        assertEquals(15, AffectionMath.discountPct(100));
        assertEquals(-8, AffectionMath.discountPct(-30));
        assertEquals(900, AffectionMath.adjust(1000, 60));
        assertEquals(1080, AffectionMath.adjust(1000, -30));
        assertEquals(1, AffectionMath.adjust(1, 100));
        assertEquals(100, AffectionMath.clamp(500));
        assertEquals(-100, AffectionMath.clamp(-500));
    }

    @Test void affectionTiers() {
        assertEquals(0, AffectionMath.tier(-80));
        assertEquals(2, AffectionMath.tier(0));
        assertEquals(3, AffectionMath.tier(20));
        assertEquals(5, AffectionMath.tier(90));
        assertTrue(AffectionMath.offersQuest(10));
        assertFalse(AffectionMath.offersQuest(9));
        assertTrue(AffectionMath.offersSpecial(50));
        assertTrue(AffectionMath.giftDelta(false, true) > AffectionMath.giftDelta(true, false));
    }

    @Test void questMath() {
        assertTrue(QuestMath.reward(50, 1.0, true) == 2 * QuestMath.reward(50, 1.0, false));
        assertTrue(QuestMath.reward(80, 1.0, false) > QuestMath.reward(10, 1.0, false));
        Random r = new Random(7);
        for (int i = 0; i < 200; i++) {
            int[] o = QuestMath.offset(r, 60, 110);
            double d = Math.hypot(o[0], o[1]);
            assertTrue(d >= 59 && d <= 111, "distance " + d);
            int c = QuestMath.count(r, 5, 9, 70);
            assertTrue(c >= 7 && c <= 11);
        }
        assertEquals(5, QuestMath.distXZ(0, 0, 3, 4), 1e-9);
    }
}
