package com.hukhong.client.screen.profile;

import com.hukhong.dongdao.DongDaoClient;
import com.hukhong.dongdao.DongDaoNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Tab "Dong Dao" cua ProfileScreen: danh sach nguoi choi khac + nut "Den". Chi dung DongDaoClient (khong doc CULTIVATION). */
final class DongDaoTab {
    private static final int ROW_H = 20, BTN_W = 34, GREY = 0xFF888888;

    private final List<Object[]> buttons = new ArrayList<>(); // {x, y, w, h, uuid}

    private static Component t(String key, String vi, Object... a) { return Component.translatableWithFallback(key, vi, a); }

    static void request() { PacketDistributor.sendToServer(new DongDaoNet.Request()); }

    private static Component dimName(String dim) {
        String path = dim.contains(":") ? dim.substring(dim.indexOf(':') + 1) : dim;
        return t("hukhong.dongdao.dim." + path, path);
    }

    /** @return chieu cao noi dung. */
    int render(GuiGraphics g, Font font, int cx, int cy, int cw, int scrollPx, int mx, int my) {
        buttons.clear();
        int top = cy - scrollPx, y = top;
        g.drawString(font, t("hukhong.dongdao.head", "Đồng Đạo — dịch chuyển miễn phí (hồi 30 giây)"), cx, y, 0xFFFFD34F);
        y += 14;
        List<DongDaoNet.Entry> all = DongDaoClient.entries();
        if (all.isEmpty()) {
            g.drawString(font, t("hukhong.dongdao.empty", "Chưa có đồng đạo nào trực tuyến."), cx + 2, y, GREY);
            y += 12;
        }
        for (DongDaoNet.Entry e : all) {
            boolean hov = mx >= cx && mx < cx + cw && my >= y && my < y + ROW_H;
            if (hov) g.fill(cx, y, cx + cw, y + ROW_H - 2, 0x22FFFFFF);
            int bx = cx + cw - BTN_W - 2;
            g.drawString(font, font.plainSubstrByWidth(e.name(), bx - cx - 8), cx + 4, y + 2, 0xFFFFFFFF);
            g.drawString(font, font.plainSubstrByWidth(dimName(e.dim()).getString(), bx - cx - 8), cx + 4, y + 11, GREY);
            boolean bh = mx >= bx && mx < bx + BTN_W && my >= y + 1 && my < y + 17;
            g.fill(bx, y + 1, bx + BTN_W, y + 17, bh ? 0xFF4A4A88 : 0xFF33335E);
            Component bl = t("hukhong.dongdao.go", "Đến");
            g.drawString(font, bl, bx + (BTN_W - font.width(bl)) / 2, y + 5, 0xFFFFFFFF);
            buttons.add(new Object[]{bx, y + 1, BTN_W, 16, e.id()});
            y += ROW_H;
        }
        return y - top;
    }

    boolean click(double mx, double my) {
        for (Object[] b : buttons) {
            int bx = (Integer) b[0], by = (Integer) b[1], bw = (Integer) b[2], bh = (Integer) b[3];
            if (mx >= bx && mx < bx + bw && my >= by && my < by + bh) {
                PacketDistributor.sendToServer(new DongDaoNet.Go((java.util.UUID) b[4]));
                return true;
            }
        }
        return false;
    }
}
