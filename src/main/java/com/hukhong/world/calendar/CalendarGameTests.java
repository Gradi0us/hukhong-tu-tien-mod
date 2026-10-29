package com.hukhong.world.calendar;

import com.hukhong.HuKhong;
import com.hukhong.travel.TravelRules;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Luat nut "Den": 1 lan / lan xuat hien / nguoi, bi chan khi dang chien dau, ngoai cua so thoi gian. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class CalendarGameTests {
    private CalendarGameTests() {}

    private static CalendarScheduler.Occ occ(long now, long start) {
        EventDef d = EventDefs.get("linh_khi_trieu");
        return d == null ? null : new CalendarScheduler.Occ(d, start);
    }

    @GameTest(template = "empty")
    public static void calendarGoOncePerOccurrence(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-cal1"));
        long now = p.serverLevel().getGameTime();
        CalendarScheduler.Occ o = occ(now, now - 100);
        if (o == null) { h.fail("sample event linh_khi_trieu must be loaded"); return; }
        CalendarData data = new CalendarData();
        String err = CalendarService.validate(p, o, data);
        if (err != null) { h.fail("fresh player in-window must be allowed, got " + err); return; }
        data.markUsed(o.key(), p.getUUID());
        if (!"hukhong.cal.err_used".equals(CalendarService.validate(p, o, data))) { h.fail("second use of same occurrence must be rejected"); return; }
        CalendarScheduler.Occ next = occ(now, now - 100 + LichMath.DAY);
        if (!"hukhong.cal.err_early".equals(CalendarService.validate(p, next, data))) { h.fail("occurrence >2min away must be rejected as early"); return; }
        CalendarScheduler.Occ soon = occ(now, now + 2000);
        if (CalendarService.validate(p, soon, data) != null) { h.fail("occurrence starting within 2 min must be allowed"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void calendarGoBlockedInCombat(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-cal2"));
        long now = p.serverLevel().getGameTime();
        CalendarScheduler.Occ o = occ(now, now - 100);
        if (o == null) { h.fail("sample event linh_khi_trieu must be loaded"); return; }
        CalendarData data = new CalendarData();
        TravelRules.markCombat(p);
        if (!"hukhong.cal.err_combat".equals(CalendarService.validate(p, o, data))) { h.fail("combat must block the calendar trip"); return; }
        h.succeed();
    }
}
