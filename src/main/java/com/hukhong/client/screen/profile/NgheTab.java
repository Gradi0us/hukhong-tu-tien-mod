package com.hukhong.client.screen.profile;

import com.hukhong.client.ClientCache;
import com.hukhong.profession.ClientProfession;
import com.hukhong.profession.Profession;
import com.hukhong.profession.ProfessionData;
import com.hukhong.profession.ProfessionMath;
import com.hukhong.profession.ProfessionNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Tab "Nghe" cua ProfileScreen: chon nghe (2 buoc bam: chon roi bam lan nua de xac nhan) hoac xem cap/xp hien tai. */
final class NgheTab {
    private static final int ROW_H = 30, GOLD = 0xFFFFD34F, GREY = 0xFF888888;
    private static final Profession[] CHOOSABLE = {Profession.LUYEN_DAN, Profession.PHU_VAN, Profession.DOAN_TAO};

    private final List<int[]> rows = new ArrayList<>(); // {x, y, w, h, profOrdinal}
    private Profession pending = Profession.NONE;

    private static Component t(String key, String vi, Object... a) { return Component.translatableWithFallback(key, vi, a); }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** @return chieu cao noi dung. */
    int render(GuiGraphics g, Font font, int cx, int cy, int cw, int scrollPx, int mx, int my) {
        rows.clear();
        ProfessionData d = ClientProfession.get();
        int y = cy - scrollPx, top = y;
        if (d.profession() == Profession.NONE) {
            g.drawString(font, t("hukhong.profession.pick_hint", "Chon 1 nghe phu. Chi chon duoc 1 lan, khong doi."), cx, y, GOLD);
            y += 14;
            for (Profession p : CHOOSABLE) {
                boolean sel = p == pending, hov = inRect(mx, my, cx, y, cw, ROW_H - 2);
                g.fill(cx, y, cx + cw, y + ROW_H - 2, sel ? 0xFF3A3A55 : hov ? 0xFF2A2A3A : 0xFF1C1C28);
                g.drawString(font, t(p.nameKey(), p.vi), cx + 4, y + 3, 0xFFFFFFFF, false);
                g.drawString(font, t(p.descKey(), ""), cx + 4, y + 15, GREY, false);
                if (sel) {
                    Component c = t("hukhong.profession.confirm", "Bam lan nua de xac nhan");
                    g.drawString(font, c, cx + cw - font.width(c) - 4, y + 3, GOLD, false);
                }
                rows.add(new int[]{cx, y, cw, ROW_H - 2, p.ordinal()});
                y += ROW_H;
            }
            return y - top;
        }
        int lv = d.level(), cap = ProfessionMath.cap(ClientCache.cultivation().realmIndex());
        g.drawString(font, t(d.profession().nameKey(), d.profession().vi), cx, y, GOLD, false);
        y += 12;
        g.drawString(font, t("hukhong.profession.level", "Cap %s / tran %s", lv, cap), cx, y, 0xFFFFFFFF, false);
        y += 12;
        long need = ProfessionMath.need(lv);
        float ratio = lv >= cap ? 1f : Math.min(1f, d.xp() / (float) need);
        g.fill(cx, y, cx + cw, y + 8, 0xFF222233);
        g.fill(cx, y, cx + (int) (cw * ratio), y + 8, 0xFF4FA8FF);
        g.drawString(font, t("hukhong.profession.xp", "EXP %s / %s", d.xp(), need), cx + 2, y, 0xFFFFFFFF, false);
        y += 14;
        g.drawString(font, t(d.profession().descKey(), ""), cx, y, GREY, false);
        y += 12;
        g.drawString(font, t("hukhong.profession.cap_note", "Tran cap theo canh gioi tu luyen."), cx, y, GREY, false);
        y += 12;
        return y - top;
    }

    /** @return true neu bam trung mot dong chon nghe. */
    boolean click(double mx, double my) {
        for (int[] r : rows) {
            if (!inRect(mx, my, r[0], r[1], r[2], r[3])) continue;
            Profession p = Profession.byOrdinal(r[4]);
            if (p == pending) {
                PacketDistributor.sendToServer(new ProfessionNet.Choose(p.ordinal()));
                pending = Profession.NONE;
            } else {
                pending = p;
            }
            return true;
        }
        return false;
    }
}
