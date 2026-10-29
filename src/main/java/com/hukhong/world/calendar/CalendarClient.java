package com.hukhong.world.calendar;

import java.util.List;

/** Ban sao phia client cua lich (chi hien thi; dong ho dem nguoc dung game time cua level client). */
public final class CalendarClient {
    private CalendarClient() {}

    private static volatile List<CalendarNet.Entry> entries = List.of();

    public static List<CalendarNet.Entry> entries() { return entries; }

    static void set(List<CalendarNet.Entry> l) { entries = List.copyOf(l); }
}
