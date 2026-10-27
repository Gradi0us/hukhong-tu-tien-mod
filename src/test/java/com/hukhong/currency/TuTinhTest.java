package com.hukhong.currency;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TuTinhTest {
    @Test
    void valueAndCanonical() {
        assertEquals(10_000 + 2_500 + 7, TuTinh.value(7, 25, 1));
        assertArrayEquals(new long[]{7, 25, 1}, TuTinh.canonical(12_507));
        assertArrayEquals(new long[]{0, 0, 3}, TuTinh.canonical(30_000));
        assertArrayEquals(new long[]{0, 0, 0}, TuTinh.canonical(-5));
    }

    @Test
    void exactPaymentUsesSmallCoinsFirst() {
        TuTinh.Plan p = TuTinh.pay(30, 5, 1, 30);
        assertEquals(30, p.ha());
        assertEquals(0, p.trung());
        assertEquals(0, p.thuong());
        assertEquals(0, p.change());
    }

    @Test
    void changeIsMadeWhenOverpayingWithBiggerCoin() {
        TuTinh.Plan p = TuTinh.pay(0, 3, 0, 150); // 2 Trung cover 150, change 50
        assertEquals(0, p.ha());
        assertEquals(2, p.trung());
        assertEquals(50, p.change());
        TuTinh.Plan q = TuTinh.pay(10, 0, 1, 20); // 10 Ha + 1 Thuong, change 9990
        assertEquals(10, q.ha());
        assertEquals(1, q.thuong());
        assertEquals(9_990, q.change());
    }

    @Test
    void insufficientFundsAreRejected() {
        assertNull(TuTinh.pay(99, 0, 0, 100));
        assertNull(TuTinh.pay(0, 0, 0, 1));
        assertNull(TuTinh.pay(5, 5, 5, -1));
        assertNotNull(TuTinh.pay(0, 0, 0, 0));
    }

    @Test
    void paymentConservesValue() {
        Random r = new Random(42);
        for (int i = 0; i < 5_000; i++) {
            long ha = r.nextInt(250), trung = r.nextInt(250), thuong = r.nextInt(5);
            long total = TuTinh.value(ha, trung, thuong);
            long price = r.nextInt((int) total + 20);
            TuTinh.Plan p = TuTinh.pay(ha, trung, thuong, price);
            if (price > total) { assertNull(p); continue; }
            assertNotNull(p);
            assertTrue(p.ha() <= ha && p.trung() <= trung && p.thuong() <= thuong, "never spends coins it does not have");
            long after = TuTinh.value(ha - p.ha(), trung - p.trung(), thuong - p.thuong()) + p.change();
            assertEquals(total - price, after, "value conserved: " + ha + "/" + trung + "/" + thuong + " price " + price);
            assertTrue(p.change() < TuTinh.THUONG);
        }
    }

    @Test
    void dropsScaleWithLevelAndStayModest() {
        Random r = new Random(1);
        long lo = TuTinh.mobDrop(1, r), hi = 0;
        for (int i = 0; i < 200; i++) hi = Math.max(hi, TuTinh.mobDrop(100, r));
        assertTrue(lo >= 1 && lo <= 3);
        assertTrue(hi >= 9 && hi < TuTinh.TRUNG, "normal mobs never drop a Trung-sized pile");
        assertTrue(TuTinh.bossDrop(30, 1, r) >= 5 * TuTinh.TRUNG);
        assertTrue(TuTinh.bossDrop(30, 2, r) > TuTinh.bossDrop(30, 1, new Random(3)));
        assertTrue(TuTinh.bicanhChest(4, r) >= 100 * TuTinh.TRUNG);
        assertEquals(30, TuTinh.altarFee(10));
    }
}
