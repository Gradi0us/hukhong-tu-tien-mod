package com.hukhong.dongdao;

import java.util.List;

/** Ban sao phia client cua danh sach Dong Dao (chi hien thi). */
public final class DongDaoClient {
    private DongDaoClient() {}

    private static volatile List<DongDaoNet.Entry> entries = List.of();

    public static List<DongDaoNet.Entry> entries() { return entries; }

    private static volatile DongDaoNet.Death death = new DongDaoNet.Death(false, "", 0, 0, 0, -1);
    private static volatile long deathRecvMs;

    public static DongDaoNet.Death death() { return death; }
    /** Tuoi (tick) hien tai, -1 neu khong ro. */
    public static long deathAgeTicks() { return death.ageTicks() < 0 ? -1 : death.ageTicks() + (System.currentTimeMillis() - deathRecvMs) / 50; }
    static void setDeath(DongDaoNet.Death d) { death = d; deathRecvMs = System.currentTimeMillis(); }

    static void set(List<DongDaoNet.Entry> l) { entries = List.copyOf(l); }
}
