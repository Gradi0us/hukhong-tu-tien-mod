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
}
