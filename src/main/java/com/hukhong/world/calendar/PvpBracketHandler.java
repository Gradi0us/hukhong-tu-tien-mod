package com.hukhong.world.calendar;

import com.hukhong.pvp.Bracket;
import net.minecraft.server.level.ServerLevel;

/** Giai Sinh Tu hang tuan (Dau Truong, Thien Khai Hoang Do): mo dang ky khi su kien bat dau, huy + hoan phi khi het gio. */
public final class PvpBracketHandler {
    private PvpBracketHandler() {}

    public static void begin(ServerLevel ow, CalendarData.Act a) { Bracket.open(ow, a.end()); }

    public static void end(ServerLevel ow, CalendarData.Act a) { Bracket.abortIfActive(ow); }
}
