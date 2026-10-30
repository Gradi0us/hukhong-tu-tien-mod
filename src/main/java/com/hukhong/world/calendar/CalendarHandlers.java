package com.hukhong.world.calendar;

import net.minecraft.server.level.ServerLevel;

/** Cong dieu phoi cac handler gameplay cua su kien lich (Dot 5). CalendarScheduler chi goi 2 ham nay. */
public final class CalendarHandlers {
    private CalendarHandlers() {}

    /** @return true neu type co handler rieng (da xu ly), false de scheduler chay nhanh mac dinh. */
    public static boolean begin(ServerLevel ow, EventDef d, CalendarData.Act a) {
        switch (d.type()) {
            case "nguyet_thuc" -> NguyetThucHandler.begin(ow, a);
            case "thu_trieu" -> ThuTrieuHandler.begin(ow, a);
            case "loi_vu" -> LoiVuHandler.begin(ow, a);
            case "pvp_bracket" -> PvpBracketHandler.begin(ow, a);
            case "yeu_vuong" -> YeuVuongHandler.begin(ow, a);
            case "linh_khi_trieu" -> { } // khong can khoi dong: vung tinh tu CalendarData.active() moi tick
            default -> { return false; }
        }
        return true;
    }

    public static void end(ServerLevel ow, CalendarData.Act a) {
        EventDef d = EventDefs.get(a.defId());
        if (d == null) return;
        switch (d.type()) {
            case "nguyet_thuc" -> NguyetThucHandler.end(ow, a);
            case "thu_trieu" -> ThuTrieuHandler.end(ow, a);
            case "loi_vu" -> LoiVuHandler.end(ow, a);
            case "pvp_bracket" -> PvpBracketHandler.end(ow, a);
            case "yeu_vuong" -> YeuVuongHandler.end(ow, a);
            default -> { }
        }
    }
}
