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

    /** Su kien Yeu Vuong dang dien ra -> sau vai tick co boss song tai diem su kien; het su kien -> boss bien mat. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void yeuVuongEventHasLivingBoss(GameTestHelper h) {
        net.minecraft.server.level.ServerLevel ow = h.getLevel();
        net.minecraft.core.BlockPos at = h.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        long now = ow.getGameTime();
        CalendarData.Act a = new CalendarData.Act("yeu_vuong@" + now, "yeu_vuong", now, now + 12000, -1, at.getX(), at.getZ());
        CalendarData.get(ow).addActive(a);
        try {
            YeuVuongHandler.ensure(ow, a, false);
            h.runAfterDelay(5, () -> {
                try {
                    var bosses = YeuVuongHandler.find(ow, a);
                    if (bosses.isEmpty()) { h.fail("yeu_vuong event must have a living boss near the event point"); return; }
                    if (bosses.get(0).distanceToSqr(at.getX(), at.getY(), at.getZ()) > 40 * 40) { h.fail("boss too far from event point"); return; }
                    YeuVuongHandler.ensure(ow, a, false);
                    if (YeuVuongHandler.find(ow, a).size() != 1) { h.fail("ensure must not spawn a duplicate boss"); return; }
                    YeuVuongHandler.end(ow, a);
                    if (!YeuVuongHandler.find(ow, a).isEmpty()) { h.fail("boss must be removed when the event ends"); return; }
                    h.succeed();
                } finally {
                    CalendarData.get(ow).removeActive(a.key());
                }
            });
        } catch (RuntimeException ex) {
            CalendarData.get(ow).removeActive(a.key());
            throw ex;
        }
    }
}
