package com.hukhong.currency;

/** Thuan Java cho vi Tu Tinh (so du tinh bang Ha): cong, tru, quy doi. Kiem thu duoc. */
public final class WalletMath {
    private WalletMath() {}

    /** Cong tien, bao hoa o Long.MAX_VALUE. */
    public static long credit(long balance, long amount) {
        long b = Math.max(0, balance), a = Math.max(0, amount);
        return b > Long.MAX_VALUE - a ? Long.MAX_VALUE : b + a;
    }

    public static boolean canPay(long balance, long price) { return price <= 0 || balance >= price; }

    /** Tru gia (da kiem tra canPay). Nem IllegalStateException neu khong du, de loi khong bi nuot im lang. */
    public static long debit(long balance, long price) {
        if (!canPay(balance, price)) throw new IllegalStateException("insufficient wallet: " + balance + " < " + price);
        return balance - Math.max(0, price);
    }

    /** Gia tri cua so dong Tu Tinh trong tui (Ha, Trung, Thuong) khi quy doi vao vi. */
    public static long convert(long ha, long trung, long thuong) { return TuTinh.value(ha, trung, thuong); }
}
