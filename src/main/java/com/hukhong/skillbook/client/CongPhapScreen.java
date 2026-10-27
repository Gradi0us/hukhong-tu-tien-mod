package com.hukhong.skillbook.client;

import com.hukhong.client.ClientCache;
import com.hukhong.client.keybind.ClientSkillState;
import com.hukhong.client.keybind.SkillKeys;
import com.hukhong.core.CultivationData;
import com.hukhong.core.Path;
import com.hukhong.skill.BanMenhSkill;
import com.hukhong.skill.Skill;
import com.hukhong.skill.SkillPages;
import com.hukhong.skillbook.AssignSkillPayload;
import com.hukhong.skillbook.BookSkill;
import com.hukhong.skillbook.ClientSkillBook;
import com.hukhong.skillbook.SkillBook;
import com.hukhong.skillbook.SkillBookData;
import com.hukhong.skillbook.SkillBookMath;
import com.hukhong.skillbook.SkillBookRegistry;
import com.hukhong.skillbook.SkillGrade;
import com.hukhong.skillbook.UpgradeSkillPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Công Pháp: danh sách kỹ năng nền + kỹ năng học từ bí tịch theo đạo, gán phím Q/E/R/T cho từng trang, Cường Hóa (cấp 1-10).
 * Mở từ bảng tu sĩ (phím K). Mọi thao tác gửi payload, server xác thực lại.
 */
public class CongPhapScreen extends Screen {
    private static final int GOLD = 0xFFFFD34F, GREY = 0xFF888888, ROW = 16, TAB_H = 14;

    /** Một dòng trong danh sách: id, tên, phẩm, đã học. */
    private record Row(String id, Component name, SkillGrade grade, boolean owned, int level) {}

    private int tab = 0;
    private String selected = null;
    private double scroll = 0;
    private int x0, y0, pw, ph, lx, ly, lw, lh, rx, rw, sy;
    private final List<Component> tooltip = new ArrayList<>();
    private final List<int[]> btnRects = new ArrayList<>(); // {x, y, w, h, mode}

    public CongPhapScreen() { super(Component.translatableWithFallback("hukhong.skillbook.title", "Công Pháp")); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) { }

    private static net.minecraft.network.chat.MutableComponent t(String key, String vi, Object... a) { return Component.translatableWithFallback(key, vi, a); }

    private void layout() {
        pw = Math.min(width - 16, 440);
        ph = Math.min(height - 16, 270);
        x0 = (width - pw) / 2;
        y0 = (height - ph) / 2;
        lx = x0 + 8;
        ly = y0 + 22 + TAB_H + 6;
        lw = Math.min(150, pw / 3);
        sy = y0 + ph - 50;       // hàng ô phím
        lh = sy - 6 - ly;
        rx = lx + lw + 10;
        rw = x0 + pw - 8 - rx;
    }

    // ------------------------------------------------------------------ dữ liệu
    private List<Path> owned() { return SkillPages.owned(ClientCache.cultivation().paths()); }

    /** Tab 0..n-1 = đạo sở hữu; tab n = Chung. */
    private Path tabPath() {
        List<Path> o = owned();
        return tab < o.size() ? o.get(tab) : null;
    }

    private Path slotPath() {
        Path p = tabPath();
        return p != null ? p : Skill.skillPath(ClientCache.cultivation(), ClientSkillState.page());
    }

    private List<Row> rows() {
        SkillBookData d = ClientSkillBook.get();
        Path p = tabPath();
        List<Row> l = new ArrayList<>();
        if (p != null) {
            for (Skill s : Skill.values())
                if (s.path == p) l.add(new Row(s.icon, Component.translatable(s.langKey()), SkillBook.gradeOf(s.icon), true, d.level(s.icon)));
        }
        for (BookSkill b : p != null ? BookSkill.ofPath(p) : BookSkill.universal())
            l.add(new Row(b.id, t(b.langKey(), b.viName), b.grade, d.learned(b.id), d.level(b.id)));
        return l;
    }

    private int totalHeight() { return rows().size() * ROW; }

    private static Component nameOf(String id) { return SkillBook.nameOf(id); }

    private static int baseCooldown(String id) {
        Skill s = Skill.byId(id);
        if (s != null) return s.cooldownTicks;
        BookSkill b = BookSkill.byId(id);
        return b == null ? 0 : b.cooldownTicks;
    }

    private static int baseCost(String id) {
        Skill s = Skill.byId(id);
        if (s != null) return s.cost;
        BookSkill b = BookSkill.byId(id);
        return b == null ? 0 : b.cost;
    }

    private static boolean owned(SkillBookData d, String id) { return Skill.byId(id) != null || d.learned(id); }

    private static String secs(int ticks) { return String.format(Locale.ROOT, "%.1fs", ticks / 20.0); }

    // ------------------------------------------------------------------ kho đồ (client)
    private int clientCount(java.util.function.Predicate<ItemStack> f) {
        var pl = Minecraft.getInstance().player;
        if (pl == null) return 0;
        int n = 0;
        for (ItemStack s : pl.getInventory().items) if (!s.isEmpty() && f.test(s)) n += s.getCount();
        for (ItemStack s : pl.getInventory().offhand) if (!s.isEmpty() && f.test(s)) n += s.getCount();
        return n;
    }

    private int clientHonTinh() { return clientCount(s -> s.is(com.hukhong.item.ModItems.HON_TINH.get())); }

    private int clientScrolls(BookSkill b) { return b == null ? 0 : clientCount(s -> SkillBookRegistry.skillOf(s) == b); }

    // ------------------------------------------------------------------ vẽ
    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        layout();
        tooltip.clear();
        btnRects.clear();
        CultivationData cd = ClientCache.cultivation();
        SkillBookData d = ClientSkillBook.get();
        List<Path> o = owned();
        if (tab > o.size()) tab = 0;

        g.fill(x0 - 1, y0 - 1, x0 + pw + 1, y0 + ph + 1, 0xFF555555);
        g.fill(x0, y0, x0 + pw, y0 + ph, 0xEE111119);
        g.drawCenteredString(font, title, width / 2, y0 + 6, GOLD);

        // tab
        int n = o.size() + 1;
        int tw = Math.min(90, (pw - 16) / n - 4);
        for (int i = 0; i < n; i++) {
            int tx = x0 + 8 + i * (tw + 4), ty = y0 + 20;
            boolean sel = i == tab, hov = mx >= tx && mx < tx + tw && my >= ty && my < ty + TAB_H;
            int col = i < o.size() ? o.get(i).argb : 0xFFCCCCCC;
            g.fill(tx, ty, tx + tw, ty + TAB_H, sel ? 0xFF3A3A55 : hov ? 0xFF2A2A3A : 0xFF1C1C28);
            if (sel) g.fill(tx, ty + TAB_H - 1, tx + tw, ty + TAB_H, col);
            Component nm = i < o.size() ? Component.translatable(SkillPages.pathLangKey(o.get(i))) : t("hukhong.skillbook.universal.tab", "Chung");
            g.drawCenteredString(font, nm, tx + tw / 2, ty + 3, sel ? 0xFFFFFFFF : 0xFFAAAAAA);
        }

        // danh sách
        List<Row> rows = rows();
        if (selected == null || rows.stream().noneMatch(r -> r.id().equals(selected))) selected = rows.isEmpty() ? null : rows.get(0).id();
        g.fill(lx - 2, ly - 2, lx + lw + 2, ly + lh + 2, 0x66000000);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() * ROW - lh)));
        g.enableScissor(lx, ly, lx + lw, ly + lh);
        int y = ly - (int) scroll;
        Path sp = slotPath();
        for (Row r : rows) {
            boolean sel = r.id().equals(selected), hov = mx >= lx && mx < lx + lw && my >= y && my < y + ROW && my >= ly && my < ly + lh;
            if (sel) g.fill(lx, y, lx + lw, y + ROW - 1, 0xFF33334D);
            else if (hov) g.fill(lx, y, lx + lw, y + ROW - 1, 0xFF222233);
            int col = r.owned() ? r.grade().argb : 0xFF555555;
            g.fill(lx + 2, y + 3, lx + 8, y + 9, col);
            g.drawString(font, r.name(), lx + 12, y + 3, r.owned() ? 0xFFFFFFFF : 0xFF666666, false);
            String tag = r.owned() ? "Lv" + r.level() : "-";
            g.drawString(font, tag, lx + lw - font.width(tag) - 4, y + 3, r.owned() ? GOLD : 0xFF555555, false);
            if (sp != null && r.owned()) {
                for (int i = 0; i < 4; i++) {
                    SkillBook.Resolved rs = SkillBook.resolve(d, sp, i, cd.level());
                    if (rs != null && rs.id().equals(r.id())) {
                        String k = SkillKeys.SKILL[i].getTranslatedKeyMessage().getString();
                        g.drawString(font, k, lx + lw - font.width(tag) - font.width(k) - 10, y + 3, 0xFF7FD4FF, false);
                    }
                }
            }
            y += ROW;
        }
        g.disableScissor();
        if (rows.size() * ROW > lh) {
            int bh = Math.max(12, lh * lh / (rows.size() * ROW));
            int by = ly + (int) ((lh - bh) * (scroll / (rows.size() * ROW - lh)));
            g.fill(lx + lw + 3, ly, lx + lw + 5, ly + lh, 0xFF222233);
            g.fill(lx + lw + 3, by, lx + lw + 5, by + bh, 0xFFAAAAAA);
        }

        if (selected != null) renderDetail(g, mx, my, cd, d);

        renderSlots(g, mx, my, cd, d);

        g.drawCenteredString(font, t("hukhong.skillbook.hint", "Chọn kỹ năng rồi bấm ô phím để gán - chuột phải ô phím để bỏ gán"),
                width / 2, y0 + ph - 11, 0xFF777777);
        if (!tooltip.isEmpty()) g.renderComponentTooltip(font, tooltip, mx, my);
    }

    private void renderDetail(GuiGraphics g, int mx, int my, CultivationData cd, SkillBookData d) {
        String id = selected;
        boolean have = owned(d, id);
        BookSkill b = BookSkill.byId(id);
        Skill base = Skill.byId(id);
        SkillGrade grade = SkillBook.gradeOf(id);
        int lv = d.level(id);
        int y = ly;
        g.drawString(font, nameOf(id), rx, y, grade.argb, false);
        y += 11;
        Component pathN = b != null && b.path == null ? t("hukhong.skillbook.universal", "Chung (mọi đạo)")
                : Component.translatable(SkillPages.pathLangKey(b != null ? b.path : base.path));
        Component line = pathN.copy().append(" - ").append(t(grade.langKey(), grade.viName));
        g.drawString(font, line, rx, y, GREY, false);
        y += 11;
        if (b != null) {
            for (var l : font.split(t(b.descKey(), b.viDesc), rw)) {
                g.drawString(font, l, rx, y, 0xFFBBBBBB, false);
                y += 10;
            }
        } else {
            for (var l : font.split(t("hukhong.skillbook.base_note", "Kỹ năng nền của đạo, luôn có sẵn."), rw)) {
                g.drawString(font, l, rx, y, 0xFFBBBBBB, false);
                y += 10;
            }
        }
        y += 3;
        if (!have) {
            Component need = t("hukhong.skillbook.locked", "Chưa học. Tìm Bí Tịch / Công Pháp (Bí Cảnh, boss, nhiệm vụ tông môn, NPC). Cần cấp %s.", b == null ? 1 : b.minLevel());
            for (var l : font.split(need, rw)) {
                g.drawString(font, l, rx, y, 0xFFFF8855, false);
                y += 10;
            }
            return;
        }
        int cap = SkillBookMath.levelCap(cd.level());
        g.drawString(font, t("hukhong.skillbook.level", "Cấp %s/%s (giới hạn theo cấp tu: %s)", lv, SkillBookMath.MAX_LEVEL, cap), rx, y, GOLD, false);
        y += 11;
        int cost = baseCost(id), cdt = baseCooldown(id);
        if (cdt > 0) {
            g.drawString(font, t("hukhong.skillbook.stats", "Tiêu hao %s - Hồi chiêu %s - Hiệu lực x%s",
                    com.hukhong.gates.Resources.costFor(cd, cost), secs(SkillBookMath.cooldown(cdt, lv)),
                    String.format(Locale.ROOT, "%.2f", SkillBookMath.dmgMult(lv))), rx, y, 0xFFDDDDDD, false);
            y += 12;
        }
        if (lv >= SkillBookMath.MAX_LEVEL) {
            g.drawString(font, t("hukhong.skillbook.maxed", "Đã đạt cấp tối đa"), rx, y, 0xFF55FF55, false);
            return;
        }
        g.drawString(font, t("hukhong.skillbook.upgrade", "Cường Hóa lên cấp %s:", lv + 1), rx, y, 0xFFFFFFFF, false);
        y += 11;
        List<Component> ba = new ArrayList<>();
        ba.add(t("hukhong.skillbook.tip.level", "Cấp %s -> %s", lv, lv + 1).withStyle(net.minecraft.ChatFormatting.GOLD));
        ba.add(t("hukhong.skillbook.tip.dmg", "Sát thương/hiệu lực: x%s -> x%s",
                String.format(Locale.ROOT, "%.2f", SkillBookMath.dmgMult(lv)), String.format(Locale.ROOT, "%.2f", SkillBookMath.dmgMult(lv + 1))));
        if (cdt > 0) ba.add(t("hukhong.skillbook.tip.cd", "Hồi chiêu: %s -> %s", secs(SkillBookMath.cooldown(cdt, lv)), secs(SkillBookMath.cooldown(cdt, lv + 1))));
        if (lv + 1 > cap) ba.add(t("hukhong.skillbook.tip.cap", "Cần tăng cấp tu luyện để mở cấp này.").withStyle(net.minecraft.ChatFormatting.RED));

        int hon = clientHonTinh();
        int bw = rw, bh = 14;
        for (SkillBookMath.Mode m : SkillBookMath.Mode.values()) {
            if (m == SkillBookMath.Mode.SCROLL && b == null) continue;
            SkillBookMath.Cost c = SkillBookMath.cost(lv, grade, m);
            boolean ok = hon >= c.honTinh() && (m != SkillBookMath.Mode.SCROLL || clientScrolls(b) >= c.scrolls()) && lv < cap;
            String txt = switch (m) {
                case CONTRIBUTION -> t("hukhong.skillbook.pay.ch", "Hồn Tinh %s + Cống Hiến %s", c.honTinh(), c.contribution()).getString();
                case SCROLL -> t("hukhong.skillbook.pay.scroll", "Hồn Tinh %s + %s bản sao bí tịch", c.honTinh(), c.scrolls()).getString();
                case HON_TINH -> t("hukhong.skillbook.pay.hon_tinh", "Chỉ Hồn Tinh %s", c.honTinh()).getString();
            };
            boolean hov = mx >= rx && mx < rx + bw && my >= y && my < y + bh;
            g.fill(rx, y, rx + bw, y + bh, ok ? (hov ? 0xFF3F5F3F : 0xFF2A3F2A) : 0xFF2A2A2A);
            g.drawString(font, txt, rx + 4, y + 3, ok ? 0xFFFFFFFF : 0xFF777777, false);
            btnRects.add(new int[]{rx, y, bw, bh, m.ordinal()});
            if (hov) {
                tooltip.addAll(ba);
                tooltip.add(t("hukhong.skillbook.tip.have", "Đang có: Hồn Tinh %s", hon).withStyle(net.minecraft.ChatFormatting.GRAY));
                if (m == SkillBookMath.Mode.SCROLL) tooltip.add(t("hukhong.skillbook.tip.have_scroll", "Bản sao bí tịch: %s", clientScrolls(b)).withStyle(net.minecraft.ChatFormatting.GRAY));
                if (m == SkillBookMath.Mode.CONTRIBUTION) tooltip.add(t("hukhong.skillbook.tip.ch_note", "Dùng Cống Hiến ví tông môn (cần là đệ tử).").withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            y += bh + 3;
        }
    }

    private void renderSlots(GuiGraphics g, int mx, int my, CultivationData cd, SkillBookData d) {
        Path p = slotPath();
        if (p == null) return;
        g.drawString(font, t("hukhong.skillbook.slots", "Phím kỹ năng trang").append(": ").append(Component.translatable(SkillPages.pathLangKey(p))),
                lx, sy - 9, p.argb, false);
        int gap = 6, bw = (pw - 16 - 3 * gap) / 4, bh = 24;
        for (int i = 0; i < 4; i++) {
            int bx = lx + i * (bw + gap);
            boolean hov = mx >= bx && mx < bx + bw && my >= sy && my < sy + bh;
            g.fill(bx - 1, sy - 1, bx + bw + 1, sy + bh + 1, hov ? 0xFFAAAAAA : 0xFF444455);
            g.fill(bx, sy, bx + bw, sy + bh, 0xFF1A1A28);
            String k = SkillKeys.SKILL[i].getTranslatedKeyMessage().getString();
            g.drawString(font, k, bx + 3, sy + 3, GOLD, false);
            SkillBook.Resolved rs = SkillBook.resolve(d, p, i, cd.level());
            Component nm;
            int col = 0xFFFFFFFF;
            if (rs != null) nm = nameOf(rs.id());
            else if (i == 2 && BanMenhSkill.of(p, cd.level()) != null) {
                nm = t("hukhong.skillbook.slot.bm", "Bản Mệnh (ưu tiên)");
                col = 0xFFAAAAAA;
            } else {
                nm = Component.literal("-");
                col = 0xFF666666;
            }
            String s = font.plainSubstrByWidth(nm.getString(), bw - 6);
            g.drawString(font, s, bx + 3, sy + 13, col, false);
            if (hov) {
                tooltip.add(t("hukhong.skillbook.tip.slot", "Phím %s", k).withStyle(net.minecraft.ChatFormatting.GOLD));
                if (rs != null) {
                    int lv = d.level(rs.id());
                    tooltip.add(nm);
                    tooltip.add(t("hukhong.skillbook.stats", "Tiêu hao %s - Hồi chiêu %s - Hiệu lực x%s",
                            com.hukhong.gates.Resources.costFor(cd, rs.cost()), secs(SkillBookMath.cooldown(rs.baseCooldown(), lv)),
                            String.format(Locale.ROOT, "%.2f", SkillBookMath.dmgMult(lv))).withStyle(net.minecraft.ChatFormatting.GRAY));
                }
                if (i == 2 && BanMenhSkill.of(p, cd.level()) != null)
                    tooltip.add(t("hukhong.skillbook.tip.r_bm", "Phím R nhường cho Kỹ Năng Bản Mệnh khi đã mở khóa.").withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
    }

    // ------------------------------------------------------------------ chuột
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        layout();
        List<Path> o = owned();
        int n = o.size() + 1;
        int tw = Math.min(90, (pw - 16) / n - 4);
        for (int i = 0; i < n; i++) {
            int tx = x0 + 8 + i * (tw + 4), ty = y0 + 20;
            if (mx >= tx && mx < tx + tw && my >= ty && my < ty + TAB_H) {
                if (tab != i) { tab = i; scroll = 0; selected = null; }
                return true;
            }
        }
        if (mx >= lx && mx < lx + lw && my >= ly && my < ly + lh) {
            int idx = (int) ((my - ly + scroll) / ROW);
            List<Row> rows = rows();
            if (idx >= 0 && idx < rows.size()) { selected = rows.get(idx).id(); return true; }
        }
        for (int[] r : btnRects) {
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3] && selected != null) {
                PacketDistributor.sendToServer(new UpgradeSkillPayload(selected, r[4]));
                return true;
            }
        }
        Path p = slotPath();
        if (p != null && my >= sy && my < sy + 24) {
            int gap = 6, bw = (pw - 16 - 3 * gap) / 4;
            for (int i = 0; i < 4; i++) {
                int bx = lx + i * (bw + gap);
                if (mx >= bx && mx < bx + bw) {
                    if (button == 1) PacketDistributor.sendToServer(new AssignSkillPayload(p.ordinal(), i, ""));
                    else if (selected != null && SkillBook.assignable(ClientSkillBook.get(), p, selected))
                        PacketDistributor.sendToServer(new AssignSkillPayload(p.ordinal(), i, selected));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy2) {
        scroll = Math.max(0, Math.min(scroll - sy2 * ROW, Math.max(0, totalHeight() - lh)));
        return true;
    }
}
