package com.hukhong.client.screen.profile;

import com.hukhong.client.story.ClientHuAnh;
import com.hukhong.story.world.Faction;
import com.hukhong.world.calendar.CalendarClient;
import com.hukhong.world.calendar.CalendarNet;
import com.hukhong.world.calendar.CalendarScheduler;
import com.hukhong.world.calendar.LichMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Tab "Lich" cua ProfileScreen: ngay/mua + 2 lan (The gioi / Thien Co Du Ngon). Chi dung ClientCache + CalendarClient. */
final class LichTab {
    private static final int PURPLE = 0xFFB070FF, WORLD = 0xFF7FD4FF, GREY = 0xFF888888, ROW_H = 28, BTN_W = 34;
    private static final int[] FACTION_COLORS = {0xFFAA55FF, 0xFFFF5555, 0xFF55AAFF, 0xFFAAAAAA, 0xFFFFAA33, 0xFF55FFAA};

    private final List<Object[]> buttons = new ArrayList<>(); // {x, y, w, h, key}

    private static Component t(String key, String vi, Object... a) { return Component.translatableWithFallback(key, vi, a); }

    /** @return chieu cao noi dung. */
    int render(GuiGraphics g, Font font, int cx, int cy, int cw, int scrollPx, int mx, int my, List<Component> tooltip) {
        buttons.clear();
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        int top = cy - scrollPx, y = top;
        String[] seasons = {"Xuân", "Hạ", "Thu", "Đông"};
        Component head = t("hukhong.cal.head", "Ngày Lịch %s/7 · Mùa %s · %s:%s",
                LichMath.dayOfSeason(now) + 1, t("hukhong.cal.season." + LichMath.season(now), seasons[LichMath.season(now)]),
                String.format(Locale.ROOT, "%02d", LichMath.hour(now)), String.format(Locale.ROOT, "%02d", LichMath.minute(now)));
        g.drawString(font, head, cx, y, 0xFFFFD34F);
        y += 14;
        List<CalendarNet.Entry> all = new ArrayList<>(CalendarClient.entries());
        all.sort(Comparator.comparing((CalendarNet.Entry e) -> !e.active()).thenComparingLong(CalendarNet.Entry::start));
        y = lane(g, font, cx, y, cw, now, mx, my, tooltip, all, false, t("hukhong.cal.lane.world", "Thế giới"), WORLD);
        y += 4;
        y = lane(g, font, cx, y, cw, now, mx, my, tooltip, all, true, t("hukhong.cal.lane.story", "Thiên Cơ Dự Ngôn"), PURPLE);
        return y - top;
    }

    private int lane(GuiGraphics g, Font font, int cx, int y, int cw, long now, int mx, int my, List<Component> tooltip,
                     List<CalendarNet.Entry> all, boolean story, Component title, int color) {
        g.drawString(font, title, cx, y, color);
        g.fill(cx, y + 10, cx + cw, y + 11, color & 0x88FFFFFF);
        y += 14;
        int n = 0;
        for (CalendarNet.Entry e : all) {
            if (e.story() != story) continue;
            n++;
            y = row(g, font, cx, y, cw, now, mx, my, tooltip, e, color);
        }
        if (n == 0) {
            g.drawString(font, t("hukhong.cal.empty", "Chưa có sự kiện."), cx + 2, y, GREY);
            y += 12;
        }
        return y;
    }

    private int row(GuiGraphics g, Font font, int cx, int y, int cw, long now, int mx, int my, List<Component> tooltip,
                    CalendarNet.Entry e, int laneColor) {
        boolean hov = mx >= cx && mx < cx + cw && my >= y && my < y + ROW_H;
        if (hov) g.fill(cx, y, cx + cw, y + ROW_H - 2, 0x22FFFFFF);
        Faction[] fs = Faction.values();
        Faction f = fs[Math.max(0, Math.min(fs.length - 1, e.faction()))];
        int fc = FACTION_COLORS[f.ordinal() % FACTION_COLORS.length];
        g.fill(cx + 1, y + 2, cx + 9, y + 10, 0xFF000000);
        g.fill(cx + 2, y + 3, cx + 8, y + 9, fc);
        int bx = cx + cw - BTN_W - 2;
        String name = t(e.nameKey(), e.defId()).getString();
        name = font.plainSubstrByWidth(name, bx - (cx + 14) - 4);
        g.drawString(font, name, cx + 14, y + 2, e.story() ? laneColor : 0xFFFFFFFF);
        long toStart = e.start() - now;
        Component status;
        int sc;
        if (e.active() || toStart <= 0) { status = t("hukhong.cal.active", "Đang diễn ra"); sc = 0xFF55FF55; }
        else { status = t("hukhong.cal.in", "Sau %s", clock(toStart)); sc = toStart <= CalendarScheduler.WARN_1 ? 0xFFFFAA33 : 0xFFBBBBBB; }
        StringBuilder info = new StringBuilder();
        if (e.city() >= 0) {
            info.append("  ").append(t("hukhong.city.n." + com.hukhong.city.CityDef.ALL.get(Math.min(e.city(), com.hukhong.city.CityDef.ALL.size() - 1)).id(), "?").getString());
            info.append("  ").append(t("hukhong.cal.huanh", "Hư Ảnh %s%%", ClientHuAnh.get(e.city())).getString());
        }
        g.drawString(font, status, cx + 14, y + 14, sc);
        String rest = font.plainSubstrByWidth(info.toString(), Math.max(0, bx - (cx + 14 + font.width(status)) - 4));
        g.drawString(font, rest, cx + 14 + font.width(status), y + 14, GREY);
        // nut "Den"
        boolean can = (e.active() || toStart <= CalendarScheduler.WARN_1) && !e.used();
        boolean bh = can && mx >= bx && mx < bx + BTN_W && my >= y + 4 && my < y + 20;
        g.fill(bx, y + 4, bx + BTN_W, y + 20, can ? (bh ? 0xFF4A4A88 : 0xFF33335E) : 0xFF222230);
        Component bl = e.used() ? t("hukhong.cal.used", "Xong") : t("hukhong.cal.go", "Đến");
        g.drawString(font, bl, bx + (BTN_W - font.width(bl)) / 2, y + 8, can ? 0xFFFFFFFF : 0xFF666666);
        if (can) buttons.add(new Object[]{bx, y + 4, BTN_W, 16, e.key()});
        if (hov) {
            tooltip.add(t(e.nameKey(), e.defId()).copy().withColor(laneColor & 0xFFFFFF));
            tooltip.add(t(f.nameKey(), f.name()).copy().withColor(fc & 0xFFFFFF));
            tooltip.add(t("hukhong.cal.level", "Cấp gợi ý %s–%s", e.levelMin(), e.levelMax()));
            tooltip.add(t("hukhong.cal.reward." + e.defId(), "").copy().withColor(0xAAAAAA));
        }
        return y + ROW_H;
    }

    private static String clock(long ticks) {
        long s = ticks / 20;
        if (s >= 3600) return (s / 3600) + "h" + String.format(Locale.ROOT, "%02d", (s / 60) % 60);
        return String.format(Locale.ROOT, "%02d:%02d", s / 60, s % 60);
    }

    boolean click(double mx, double my) {
        for (Object[] b : buttons) {
            int bx = (Integer) b[0], by = (Integer) b[1], bw = (Integer) b[2], bh = (Integer) b[3];
            if (mx >= bx && mx < bx + bw && my >= by && my < by + bh) {
                PacketDistributor.sendToServer(new CalendarNet.Go((String) b[4]));
                return true;
            }
        }
        return false;
    }
}
