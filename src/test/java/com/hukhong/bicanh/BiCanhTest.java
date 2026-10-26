package com.hukhong.bicanh;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BiCanhTest {
    @Test
    void rewardsAreDeterministicAndBounded() {
        for (BiCanhTier t : BiCanhTier.values()) {
            List<BiCanhRewards.Entry> a = BiCanhRewards.roll(t, new Random(42));
            assertEquals(a, BiCanhRewards.roll(t, new Random(42)));
            for (var e : a) assertTrue(e.count() >= 1 && e.count() <= 20, e.toString());
            assertTrue(a.stream().anyMatch(e -> e.kind() == BiCanhRewards.Kind.HON_TINH));
            assertTrue(a.stream().anyMatch(e -> e.kind() == BiCanhRewards.Kind.HOI_XUAN_DAN));
        }
    }

    @Test
    void boneOnlyFromHighTiersAndRateRises() {
        int[] bones = new int[5];
        Random r = new Random(7);
        for (BiCanhTier t : BiCanhTier.values())
            for (int i = 0; i < 4000; i++)
                for (var e : BiCanhRewards.roll(t, r))
                    if (e.kind() == BiCanhRewards.Kind.HON_COT) {
                        bones[t.ordinal()]++;
                        assertTrue(e.sub() >= 0 && e.sub() <= 2);
                    }
        assertEquals(0, bones[0]);
        assertEquals(0, bones[1]);
        assertTrue(bones[2] > 0 && bones[2] < bones[3] && bones[3] < bones[4]);
        assertEquals(0.30, bones[4] / 4000.0, 0.04);
    }

    @Test
    void pillPoolIndexInRange() {
        Random r = new Random(1);
        for (int i = 0; i < 2000; i++)
            for (var e : BiCanhRewards.roll(BiCanhTier.HONG, r))
                if (e.kind() == BiCanhRewards.Kind.PHA_CANH_DAN)
                    assertTrue(e.sub() >= 0 && e.sub() < BiCanhRewards.PILL_KINDS);
    }

    @Test
    void tierGateAndRoll() {
        assertTrue(BiCanhTier.BACH.allows(25));
        assertFalse(BiCanhTier.BACH.allows(26));
        assertFalse(BiCanhTier.HONG.allows(74));
        assertEquals(BiCanhTier.BACH, BiCanhTier.roll(0.5, 5));
        assertEquals(BiCanhTier.HONG, BiCanhTier.roll(0.99, 90));
        assertEquals(BiCanhTier.BACH, BiCanhTier.roll(0.1, 0));
        for (double x = 0; x < 1; x += 0.05) assertTrue(BiCanhTier.roll(x, 20).allows(20));
    }

    @Test
    void layoutChain() {
        for (BiCanhTier t : BiCanhTier.values()) {
            var rooms = BiCanhLayout.rooms(t);
            assertEquals(t.waves + 2, rooms.size());
            assertEquals(BiCanhLayout.Kind.ENTRY, rooms.get(0).kind());
            assertEquals(BiCanhLayout.Kind.BOSS, rooms.get(rooms.size() - 1).kind());
            assertTrue(BiCanhLayout.endX(rooms) < BiCanhLayout.SLOT_SPACING - 8);
            for (var room : rooms) {
                assertEquals(room.index(), BiCanhLayout.roomAt(rooms, room.x0() + 5));
                assertEquals(-1, BiCanhLayout.roomAt(rooms, room.x0() + BiCanhLayout.ROOM_LEN + 2));
                var p1 = BiCanhLayout.pillars(99, room);
                assertEquals(p1.size(), BiCanhLayout.pillars(99, room).size());
                for (int[] p : p1) {
                    assertTrue(p[0] > room.x0() && p[0] < room.x0() + BiCanhLayout.ROOM_LEN - 1);
                    assertTrue(Math.abs(p[1]) > 1 && Math.abs(p[1]) < BiCanhLayout.HALF_Z);
                }
            }
        }
        assertEquals(3, BiCanhTier.BACH.mobsForWave(0));
        assertTrue(BiCanhTier.partyScale(4) > BiCanhTier.partyScale(1));
    }
}
