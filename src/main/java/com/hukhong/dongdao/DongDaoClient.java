package com.hukhong.dongdao;

import java.util.List;

/** Ban sao phia client cua danh sach Dong Dao (chi hien thi). */
public final class DongDaoClient {
    private DongDaoClient() {}

    private static volatile List<DongDaoNet.Entry> entries = List.of();

    public static List<DongDaoNet.Entry> entries() { return entries; }

    static void set(List<DongDaoNet.Entry> l) { entries = List.copyOf(l); }
}
