package com.hukhong.auction;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/** Toan thuan cua Dau Gia Hanh / Dau Gia Hoi (kiem thu JUnit). Tien = Ha (Tu Tinh pham Ha). */
public final class AuctionMath {
    private AuctionMath() {}

    public static final int SALE_FEE_PCT = 5, LISTING_FEE_PCT = 1, MAX_LOTS = 20, MIN_STEP_PCT = 5;

    /** Phi ban (dot): floor(price * pct / 100). */
    public static long saleFee(long price, int pct) { return Math.max(0, price) * Math.max(0, pct) / 100; }

    public static long saleFee(long price) { return saleFee(price, SALE_FEE_PCT); }

    /** Phi dang (1% gia khoi diem, toi thieu 1 Ha). */
    public static long listingFee(long start, int pct) { return Math.max(1, Math.max(0, start) * Math.max(0, pct) / 100); }

    public static long listingFee(long start) { return listingFee(start, LISTING_FEE_PCT); }

    /** Nguoi ban nhan: price - phi ban. */
    public static long net(long price, int pct) { return Math.max(0, price) - saleFee(price, pct); }

    /** Gia toi thieu cho lan tra gia ke tiep: chua co ai tra = gia khoi diem; da co = hien tai + max(1, 5%). */
    public static long minNextBid(long start, long current, boolean hasBid) {
        if (!hasBid) return start;
        return current + Math.max(1, current * MIN_STEP_PCT / 100);
    }

    /** Dau gia truc tiep: nang gia them pct% (lam tron len), toi thieu +1 Ha. */
    public static long raise(long current, int pct) {
        long r = (current * (100L + pct) + 99) / 100;
        return Math.max(current + 1, r);
    }

    public static long durationMs(int hours) { return Math.max(1, hours) * 3_600_000L; }

    /** Hop le: start >= floor, buyNow = 0 (khong co) hoac >= start. */
    public static boolean pricesValid(long start, long buyNow, long floor) {
        return start >= Math.max(1, floor) && (buyNow == 0 || buyNow >= start);
    }

    /** Gia cuoi cung cua mot lan ban co mua-ngay: tien nguoi mua tra (khong doi) - dung de test bao toan. */
    public static long burnedOnSale(long price, int pct) { return saleFee(price, pct); }

    /** Doc so tien nhap tay: "250" = Ha, "12t" = Trung (x100), "3th" = Thuong (x10000). Tra -1 neu khong hop le. */
    public static long parseHa(String in) {
        if (in == null) return -1;
        String s = in.trim().toLowerCase(java.util.Locale.ROOT).replace(",", "").replace(".", "");
        if (s.isEmpty()) return -1;
        long mult = 1;
        if (s.endsWith("th")) { mult = 10_000L; s = s.substring(0, s.length() - 2); }
        else if (s.endsWith("t")) { mult = 100L; s = s.substring(0, s.length() - 1); }
        try {
            long v = Long.parseLong(s.trim());
            if (v < 0 || v > Long.MAX_VALUE / mult / 4) return -1;
            return v * mult;
        } catch (NumberFormatException e) { return -1; }
    }

    // ---------------------------------------------------------------- Dau Gia Hoi (thoi gian thuc)

    /** Moc bat dau gan nhat >= after theo (ngay trong tuan, gio, phut) cua may chu. */
    public static LocalDateTime nextSlot(LocalDateTime after, Set<DayOfWeek> days, int hour, int minute) {
        if (days.isEmpty()) return null;
        LocalDateTime base = after.toLocalDate().atTime(LocalTime.of(hour, minute));
        for (int i = 0; i < 9; i++) {
            LocalDateTime t = base.plusDays(i);
            if (days.contains(t.getDayOfWeek()) && !t.isBefore(after)) return t;
        }
        return null;
    }

    /** Moc da qua gan nhat <= now (de chay bu khi may chu bat tre trong grace). */
    public static LocalDateTime lastSlot(LocalDateTime now, Set<DayOfWeek> days, int hour, int minute) {
        if (days.isEmpty()) return null;
        LocalDateTime t = now.toLocalDate().atTime(LocalTime.of(hour, minute));
        for (int i = 0; i < 9; i++) {
            if (days.contains(t.getDayOfWeek()) && !t.isAfter(now)) return t;
            t = t.minusDays(1);
        }
        return null;
    }

    /** Khoa moc: "yyyy-MM-ddTHH:mm". */
    public static String slotKey(LocalDateTime t) { return t.toString(); }

    /** NPC doi thu: co tra gia khong? price = gia ke tiep; budget con lai; valuation = gia tri nguoi do dinh. */
    public static boolean rivalWilling(long nextPrice, long budget, long valuation) { return nextPrice <= budget && nextPrice <= valuation; }
}
