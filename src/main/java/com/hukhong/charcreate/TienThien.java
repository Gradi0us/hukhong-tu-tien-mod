package com.hukhong.charcreate;

import com.hukhong.core.Aptitude;

import java.util.Collection;
import java.util.Random;

/** Tien Thien: cap khoi dau 1..10 theo tu chat dao tot nhat da chon. SSS=10, SS=9, S=8, A=6-7, B=4-5, C=2-3, D=1. */
public final class TienThien {
    private TienThien() {}

    public static int min(Aptitude a) {
        return switch (a) { case SSS -> 10; case SS -> 9; case S -> 8; case A -> 6; case B -> 4; case C -> 2; case D -> 1; };
    }

    public static int max(Aptitude a) {
        return switch (a) { case SSS -> 10; case SS -> 9; case S -> 8; case A -> 7; case B -> 5; case C -> 3; case D -> 1; };
    }

    public static Aptitude best(Collection<Aptitude> chosen) {
        Aptitude b = Aptitude.D;
        for (Aptitude a : chosen) if (a.ordinal() > b.ordinal()) b = a;
        return b;
    }

    public static int roll(Collection<Aptitude> chosen, Random r) {
        Aptitude b = best(chosen);
        return min(b) + r.nextInt(max(b) - min(b) + 1);
    }

    public static final int CAP = 20;

    /** Mốc 10/20 đầu tiên trong (from, target]: Tiên Thiên phải dừng ở đó chờ chọn lõi; không có thì về target. */
    public static int firstStop(int from, int target) {
        for (int m = 10; m <= CAP; m += 10) if (m > from && m <= target) return m;
        return target;
    }

    /** Điểm Tiên Thiên của MỘT đạo theo tư chất: SSS 10, SS 7, S 5, A 3, B 2, C 1, D 0. */
    public static int gradePoints(Aptitude a) {
        return switch (a) { case SSS -> 10; case SS -> 7; case S -> 5; case A -> 3; case B -> 2; case C -> 1; case D -> 0; };
    }

    /**
     * Thưởng Tiên Thiên đa đạo: 0 nếu 1 đạo; ngược lại tổng điểm của 2 tư chất tốt nhất, tối đa 20
     * (2 đạo SSS+SSS = 20; tam năng chỉ tính 2 đạo tốt nhất).
     */
    public static int multiPathLevel(Collection<Aptitude> chosen) {
        if (chosen.size() < 2) return 0;
        int a = 0, b = 0;
        for (Aptitude x : chosen) {
            int v = gradePoints(x);
            if (v > a) { b = a; a = v; } else if (v > b) b = v;
        }
        return Math.min(CAP, a + b);
    }

    /**
     * Cấp khởi đầu = max(Tiên Thiên cũ 1..10 theo tư chất tốt nhất, thưởng đa đạo), tối đa 20.
     * Tất định theo seed -> client (hiển thị) và server (áp dụng) tính giống nhau.
     */
    public static int startLevel(Collection<Aptitude> chosen, long seed) {
        if (chosen.isEmpty()) return 1;
        int legacy = roll(chosen, new Random(seed ^ 0x5454L));
        return Math.max(1, Math.min(CAP, Math.max(legacy, multiPathLevel(chosen))));
    }
}
