package com.hukhong.client.screen.society;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityMenu;
import com.hukhong.city.CityRank;
import com.hukhong.city.Tier;
import com.hukhong.society.SectDef;
import com.hukhong.society.SectMath;
import com.hukhong.society.SectMenu;
import com.hukhong.society.SocietyRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Màn Tông Môn + màn Chức Vị Thành: bảng chữ đơn giản + nút (không có ô đồ). */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class SocietyScreens {
    private SocietyScreens() {}

    @SubscribeEvent
    public static void onRegister(RegisterMenuScreensEvent e) {
        e.register(SocietyRegistry.SECT_MENU.get(), SectScreen::new);
        e.register(SocietyRegistry.CITY_MENU.get(), CityScreen::new);
    }

    private static final int PANEL = 0xF0201A28, EDGE = 0xFFB8A46A, TEXT = 0xFFE8E0D0, DIM = 0xFF9A9088, GOOD = 0xFF8FE08F, BAD = 0xFFE08F8F, GOLD = 0xFFFFD34F;

    private static void press(int containerId, int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(containerId, id);
    }

    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.fill(x, y, x + w, y + 1, EDGE);
        g.fill(x, y + h - 1, x + w, y + h, EDGE);
        g.fill(x, y, x + 1, y + h, EDGE);
        g.fill(x + w - 1, y, x + w, y + h, EDGE);
    }

    // ================= TÔNG MÔN =================
    static class SectScreen extends AbstractContainerScreen<SectMenu> {
        SectScreen(SectMenu m, Inventory inv, Component title) {
            super(m, inv, title);
            imageWidth = 236;
            imageHeight = 176;
            inventoryLabelY = 10000;
            titleLabelY = 8;
        }

        @Override
        protected void init() {
            super.init();
            var s = menu.s;
            boolean member = !s.memberSect().isEmpty();
            boolean here = member && s.memberSect().equals(s.boardSect());
            int bx = leftPos + 12, y = topPos + 120;
            if (!member) {
                if (s.applyCode() == SectMath.OK) btn("hukhong.sect.btn.apply", SectMenu.B_APPLY, bx, y, 100);
                if (s.boardSect().equals(s.invite())) btn("hukhong.sect.btn.accept", SectMenu.B_ACCEPT, bx + 106, y, 106);
            } else {
                btn("hukhong.sect.btn.leave", SectMenu.B_LEAVE, bx + 130, topPos + 148, 82);
                if (here) {
                    if (s.quest().isEmpty()) {
                        btn("hukhong.sect.btn.quest_kill", SectMenu.B_KILL, bx, y, 100);
                        btn("hukhong.sect.btn.quest_herb", SectMenu.B_HERB, bx + 106, y, 106);
                    } else {
                        boolean done = s.need() > 0 && s.progress() >= s.need();
                        if (done) btn("hukhong.sect.btn.claim", SectMenu.B_CLAIM, bx, y, 100);
                        else if (SectMath.Q_HERB.equals(s.quest())) btn("hukhong.sect.btn.submit", SectMenu.B_SUBMIT, bx, y, 100);
                        btn("hukhong.sect.btn.abandon", SectMenu.B_ABANDON, bx + 106, y, 106);
                    }
                }
            }
        }

        private void btn(String key, int id, int x, int y, int w) {
            addRenderableWidget(Button.builder(Component.translatable(key), b -> press(menu.containerId, id)).bounds(x, y, w, 18).build());
        }

        @Override
        protected void renderBg(GuiGraphics g, float pt, int mx, int my) { panel(g, leftPos, topPos, imageWidth, imageHeight); }

        @Override
        protected void renderLabels(GuiGraphics g, int mx, int my) {
            var s = menu.s;
            SectDef board = SectDef.byId(s.boardSect());
            g.drawString(font, title, 12, 8, GOLD, false);
            int y = 22;
            if (board != null) g.drawString(font, Component.translatable("hukhong.sect.tier_line", Component.translatable(board.tier().key())), 12, y, DIM, false);
            y += 12;
            if (s.memberSect().isEmpty()) {
                g.drawString(font, Component.translatable("hukhong.sect.not_member"), 12, y, TEXT, false);
                y += 12;
                if (board != null) {
                    g.drawString(font, Component.translatable("hukhong.sect.req", board.minLevel(), board.minApt().name(), board.maxAge()), 12, y, DIM, false);
                    y += 12;
                }
                boolean ok = s.applyCode() == SectMath.OK;
                g.drawString(font, ok ? Component.translatable("hukhong.sect.eligible") : Component.translatable("hukhong.sect.apply_fail." + s.applyCode()),
                        12, y, ok ? GOOD : BAD, false);
                y += 12;
                if (!s.invite().isEmpty()) {
                    SectDef iv = SectDef.byId(s.invite());
                    if (iv != null) g.drawString(font, Component.translatable("hukhong.sect.pending_invite", Component.translatable(iv.nameKey())), 12, y, GOLD, false);
                }
                return;
            }
            SectDef mine = SectDef.byId(s.memberSect());
            SectMath.Rank rank = SectMath.Rank.values()[Math.min(s.rank(), 3)];
            g.drawString(font, Component.translatable("hukhong.sect.you_are", mine == null ? s.memberSect() : Component.translatable(mine.nameKey()).getString(),
                    Component.translatable(rank.key()).getString()), 12, y, TEXT, false);
            y += 12;
            g.drawString(font, Component.translatable("hukhong.sect.ch_line", s.totalCh(), s.capLeft()), 12, y, TEXT, false);
            y += 12;
            if (mine != null && rank != SectMath.Rank.TRUONG_LAO) {
                SectMath.Rank nx = SectMath.Rank.values()[rank.ordinal() + 1];
                g.drawString(font, Component.translatable("hukhong.sect.next_rank", Component.translatable(nx.key()),
                        SectMath.rankLevelNeeded(mine.tier(), nx), SectMath.rankChNeeded(mine.tier(), nx)), 12, y, DIM, false);
            }
            y += 14;
            if (!s.memberSect().equals(s.boardSect())) {
                g.drawString(font, Component.translatable("hukhong.sect.other_board"), 12, y, BAD, false);
                return;
            }
            if (s.quest().isEmpty()) {
                g.drawString(font, Component.translatable("hukhong.sect.no_quest"), 12, y, DIM, false);
            } else {
                boolean done = s.progress() >= s.need();
                g.drawString(font, Component.translatable("hukhong.sect.quest." + s.quest()), 12, y, GOLD, false);
                g.drawString(font, Component.translatable("hukhong.sect.quest_progress_line", Math.min(s.progress(), s.need()), s.need()), 12, y + 12,
                        done ? GOOD : TEXT, false);
            }
        }
    }

    // ================= THÀNH / CHỨC VỊ =================
    static class CityScreen extends AbstractContainerScreen<CityMenu> {
        CityScreen(CityMenu m, Inventory inv, Component title) {
            super(m, inv, title);
            imageWidth = 236;
            imageHeight = 190;
            inventoryLabelY = 10000;
        }

        @Override
        protected void init() {
            super.init();
            addRenderableWidget(Button.builder(Component.translatable("hukhong.city.btn.donate", CityRank.HERBS_PER_DONATION, CityRank.DONATION_POINTS),
                    b -> press(menu.containerId, CityMenu.B_DONATE)).bounds(leftPos + 12, topPos + 164, 212, 18).build());
        }

        @Override
        protected void renderBg(GuiGraphics g, float pt, int mx, int my) { panel(g, leftPos, topPos, imageWidth, imageHeight); }

        @Override
        protected void renderLabels(GuiGraphics g, int mx, int my) {
            var s = menu.s;
            CityDef d = CityDef.byId(s.cityId());
            Tier tier = d == null ? Tier.NHAN : d.tier();
            g.drawString(font, title, 12, 8, GOLD, false);
            g.drawString(font, Component.translatable("hukhong.city.tier_line", Component.translatable(tier.key())), 12, 20, DIM, false);
            CityRank.Rank cur = CityRank.Rank.values()[Math.min(s.rank(), 5)];
            g.drawString(font, Component.translatable("hukhong.city.your_rank", Component.translatable(cur.key()).getString(), s.points()), 12, 32, TEXT, false);
            String lord = s.lord() ? Component.translatable("hukhong.city.you").getString()
                    : (s.lordName().isEmpty() ? Component.translatable("hukhong.city.no_lord").getString() : s.lordName());
            g.drawString(font, Component.translatable("hukhong.city.lord_line", lord), 12, 44, s.lord() ? GOLD : TEXT, false);
            g.drawString(font, Component.translatable("hukhong.city.cap_line", s.capLeft(), s.herbs()), 12, 56, DIM, false);
            int y = 72;
            for (CityRank.Rank r : CityRank.Rank.values()) {
                boolean have = r.ordinal() <= cur.ordinal();
                int pts = CityRank.pointsFor(r, tier), lv = CityRank.levelFor(r, tier);
                Component line = Component.translatable("hukhong.city.ladder", Component.translatable(r.key()).getString(), pts, lv);
                g.drawString(font, (have ? "✔ " : "• ") + line.getString(), 12, y, have ? GOOD : (r.ordinal() == cur.ordinal() + 1 ? TEXT : DIM), false);
                y += 12;
            }
        }
    }
}
