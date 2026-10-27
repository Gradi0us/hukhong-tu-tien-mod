package com.hukhong.currency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WalletMathTest {
    @Test
    void convertItemsToWalletValue() {
        // 3 Ha + 2 Trung + 1 Thuong = 3 + 200 + 10000
        assertEquals(10203L, WalletMath.convert(3, 2, 1));
        assertEquals(0L, WalletMath.convert(0, 0, 0));
    }

    @Test
    void creditAccumulatesAndSaturates() {
        assertEquals(150L, WalletMath.credit(100, 50));
        assertEquals(100L, WalletMath.credit(100, -5), "amount am bi bo qua");
        assertEquals(Long.MAX_VALUE, WalletMath.credit(Long.MAX_VALUE - 1, 10));
    }

    @Test
    void debitGivesExactChange() {
        assertTrue(WalletMath.canPay(10203, 150));
        long after = WalletMath.debit(10203, 150);
        assertEquals(10053L, after);
        // so du con lai quy doi dung chong tien: 10053 = 53 Ha + 1 Thuong
        assertArrayEquals(new long[]{53, 0, 1}, TuTinh.canonical(after));
    }

    @Test
    void debitRejectsInsufficientBalance() {
        assertFalse(WalletMath.canPay(99, 100));
        assertThrows(IllegalStateException.class, () -> WalletMath.debit(99, 100));
    }

    @Test
    void payThenGiveRoundTripsValue() {
        long bal = WalletMath.convert(0, 0, 2); // 20000
        bal = WalletMath.debit(bal, 12345);
        bal = WalletMath.credit(bal, 345);
        assertEquals(8000L, bal);
    }
}
