package com.hukhong.auction;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuctionMathTest {
    @Test void fees() {
        assertEquals(5, AuctionMath.saleFee(100));
        assertEquals(0, AuctionMath.saleFee(19));              // floor
        assertEquals(95, AuctionMath.net(100, 5));
        assertEquals(1, AuctionMath.listingFee(10));           // toi thieu 1 Ha
        assertEquals(30, AuctionMath.listingFee(3000));
        assertEquals(50, AuctionMath.saleFee(1000, 5));
    }

    @Test void feeConservation() {
        for (long p : new long[]{1, 7, 99, 100, 12_345, 1_000_000})
            assertEquals(p, AuctionMath.net(p, 5) + AuctionMath.saleFee(p, 5), "net + burned == price for " + p);
    }

    @Test void minNextBid() {
        assertEquals(500, AuctionMath.minNextBid(500, 0, false));
        assertEquals(525, AuctionMath.minNextBid(500, 500, true));
        assertEquals(11, AuctionMath.minNextBid(5, 10, true));  // +max(1, 5%) => 11
    }

    @Test void raise() {
        assertEquals(105, AuctionMath.raise(100, 5));
        assertEquals(110, AuctionMath.raise(100, 10));
        assertEquals(11, AuctionMath.raise(10, 5));              // luon >= +1
        assertEquals(3, AuctionMath.raise(2, 5));
    }

    @Test void prices() {
        assertTrue(AuctionMath.pricesValid(100, 0, 100));
        assertFalse(AuctionMath.pricesValid(99, 0, 100));
        assertFalse(AuctionMath.pricesValid(100, 50, 10));
        assertTrue(AuctionMath.pricesValid(100, 100, 10));
        assertEquals(12 * 3_600_000L, AuctionMath.durationMs(12));
    }

    @Test void parse() {
        assertEquals(250, AuctionMath.parseHa("250"));
        assertEquals(1200, AuctionMath.parseHa("12t"));
        assertEquals(30_000, AuctionMath.parseHa("3th"));
        assertEquals(-1, AuctionMath.parseHa("abc"));
        assertEquals(-1, AuctionMath.parseHa(""));
        assertEquals(-1, AuctionMath.parseHa("-5"));
    }

    @Test void liveSlots() {
        Set<DayOfWeek> days = Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY);
        LocalDateTime tue = LocalDateTime.of(2026, 11, 3, 10, 0); // thu 3
        assertEquals(LocalDateTime.of(2026, 11, 4, 20, 0), AuctionMath.nextSlot(tue, days, 20, 0));
        LocalDateTime wedEvening = LocalDateTime.of(2026, 11, 4, 20, 5);
        assertEquals(LocalDateTime.of(2026, 11, 4, 20, 0), AuctionMath.lastSlot(wedEvening, days, 20, 0));
        assertEquals(LocalDateTime.of(2026, 11, 7, 20, 0), AuctionMath.nextSlot(wedEvening, days, 20, 0));
        assertEquals(LocalDateTime.of(2026, 11, 4, 20, 0), AuctionMath.nextSlot(LocalDateTime.of(2026, 11, 4, 20, 0), days, 20, 0));
        assertNull(AuctionMath.nextSlot(tue, Set.of(), 20, 0));
        // thu 6 sau thu 4 20:00 -> lastSlot la thu 4 cua cung tuan
        assertEquals(LocalDateTime.of(2026, 11, 4, 20, 0), AuctionMath.lastSlot(LocalDateTime.of(2026, 11, 6, 9, 0), days, 20, 0));
    }

    @Test void rival() {
        assertTrue(AuctionMath.rivalWilling(100, 500, 300));
        assertFalse(AuctionMath.rivalWilling(400, 500, 300));  // vuot dinh gia
        assertFalse(AuctionMath.rivalWilling(100, 50, 300));   // het ngan sach
    }

    @Test void liveBidPrice() {
        assertEquals(1000, LiveAuction.bidPrice(1000, false, 5));   // lan dau 5% = gia khoi diem
        assertEquals(1100, LiveAuction.bidPrice(1000, false, 10));
        assertEquals(1050, LiveAuction.bidPrice(1000, true, 5));
        assertEquals(1100, LiveAuction.bidPrice(1000, true, 10));
    }
}
