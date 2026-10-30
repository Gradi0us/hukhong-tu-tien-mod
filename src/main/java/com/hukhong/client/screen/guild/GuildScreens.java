package com.hukhong.client.screen.guild;

import com.hukhong.HuKhong;
import com.hukhong.currency.SellPrices;
import com.hukhong.currency.ShopMenu;
import com.hukhong.currency.TuTinhText;
import com.hukhong.currency.Wallet;
import com.hukhong.guild.GuildData;
import com.hukhong.guild.GuildMath;
import com.hukhong.guild.GuildMenu;
import com.hukhong.guild.GuildRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.List;

/** Man hinh Hiep Hoi Mao Hiem Gia (the bac, nhiem vu ngay) + cua hang Tu Tinh cua thuong nhan. Bang chu + nut, khong o do. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class GuildScreens {
    private GuildScreens() {}

    @SubscribeEvent
    public static void onRegister(RegisterMenuScreensEvent e) {
        e.register(GuildRegistry.GUILD_MENU.get(), GuildScreen::new);
        e.register(GuildRegistry.SHOP_MENU.get(), ShopScreen::new);
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

    private static ResourceLocation badge(int rank) {
        return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "textures/gui/guild/badge_" + GuildMath.clampRank(rank) + ".png");
    }

    // ================= HIEP HOI =================
    static class GuildScreen extends AbstractContainerScreen<GuildMenu> {
        private static final int ROW0 = 74, ROWH = 22;

        GuildScreen(GuildMenu m, Inventory inv, Component title) {
            super(m, inv, title);
            imageWidth = 256;
            imageHeight = 216;
            inventoryLabelY = 10000;
            titleLabelY = 10000;
        }

        private GuildData data() { return Minecraft.getInstance().player.getData(GuildRegistry.DATA.get()); }

        private List<GuildMath.Quest> quests(GuildData d) {
            return GuildMath.daily(d.rank(), d.day(), d.genLevel(), Minecraft.getInstance().player.getUUID().getLeastSignificantBits());
        }

        @Override
        protected void init() {
            super.init();
            rebuild();
        }

        private void rebuild() {
            clearWidgets();
            GuildData d = data();
            List<GuildMath.Quest> qs = quests(d);
            for (int i = 0; i < qs.size(); i++) {
                int prog = d.progOf(i), y = topPos + ROW0 + i * ROWH;
                if (prog == GuildData.DONE) continue;
                final int slot = i;
                GuildMath.Quest q = qs.get(i);
                String key = prog == GuildData.NONE ? "hukhong.guild.btn.accept"
                        : (q.kind() == GuildMath.K_HERB && prog < q.amount()) ? "hukhong.guild.btn.submit"
                        : prog >= q.amount() ? "hukhong.guild.btn.turnin" : "hukhong.guild.btn.working";
                Button b = Button.builder(Component.translatable(key), x -> press(menu.containerId, slot)).bounds(leftPos + 168, y, 58, 18).build();
                b.active = !key.endsWith("working");
                addRenderableWidget(b);
                if (prog >= 0)
                    addRenderableWidget(Button.builder(Component.literal("x"), x -> press(menu.containerId, GuildMenu.B_ABANDON + slot)).bounds(leftPos + 229, y, 18, 18).build());
            }
            int ty = topPos + ROW0 + qs.size() * ROWH + 14;
            GuildMath.Trial t = GuildMath.trial(d.rank() + 1);
            if (d.rank() < GuildMath.RANKS - 1) {
                boolean can = GuildMath.canTrial(d.rank(), d.points(), com.hukhong.client.ClientCache.cultivation().level());
                String key = d.trial() == GuildData.NONE ? "hukhong.guild.btn.trial_start" : d.trial() >= t.amount() ? "hukhong.guild.btn.trial_finish" : "hukhong.guild.btn.working";
                Button b = Button.builder(Component.translatable(key), x -> press(menu.containerId, GuildMenu.B_TRIAL)).bounds(leftPos + 168, ty, 79, 18).build();
                b.active = d.trial() == GuildData.NONE ? can : d.trial() >= t.amount();
                addRenderableWidget(b);
            }
        }

        @Override
        public void containerTick() {
            super.containerTick();
            if (minecraft != null && minecraft.player != null && (tickCount++ % 10 == 0)) rebuild();
        }

        private int tickCount = 0;

        @Override
        protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
            panel(g, leftPos, topPos, imageWidth, imageHeight);
            GuildData d = data();
            int lv = com.hukhong.client.ClientCache.cultivation().level();
            g.drawString(font, Component.translatable("hukhong.guild.title"), leftPos + 12, topPos + 8, GOLD, false);
            g.blit(badge(d.rank()), leftPos + 12, topPos + 22, 0, 0, 40, 40, 40, 40);
            g.drawString(font, Component.translatable("hukhong.guild.card", Component.translatable("hukhong.guild.rank." + d.rank())), leftPos + 60, topPos + 24, TEXT, false);
            if (d.rank() < GuildMath.RANKS - 1) {
                int need = GuildMath.pointsFor(d.rank() + 1);
                g.drawString(font, Component.translatable("hukhong.guild.points", d.points(), need), leftPos + 60, topPos + 36, DIM, false);
                int bw = 150, fill = (int) (bw * Math.min(1.0, d.points() / (double) need));
                g.fill(leftPos + 60, topPos + 48, leftPos + 60 + bw, topPos + 54, 0xFF3A3040);
                g.fill(leftPos + 60, topPos + 48, leftPos + 60 + fill, topPos + 54, GOLD);
                boolean lvOk = lv >= GuildMath.levelFor(d.rank() + 1);
                g.drawString(font, Component.translatable("hukhong.guild.need_level", GuildMath.levelFor(d.rank() + 1)), leftPos + 60, topPos + 58, lvOk ? GOOD : BAD, false);
            } else {
                g.drawString(font, Component.translatable("hukhong.guild.points_max", d.points()), leftPos + 60, topPos + 36, GOOD, false);
            }
            g.drawString(font, Component.translatable("hukhong.guild.perk", GuildMath.discountPct(d.rank())), leftPos + 150, topPos + 8, GOOD, false);
            g.drawString(font, Component.translatable("hukhong.guild.wallet", TuTinhText.of(Wallet.count(Minecraft.getInstance().player))), leftPos + 12, topPos + 66 - 1, DIM, false);

            List<GuildMath.Quest> qs = quests(d);
            for (int i = 0; i < qs.size(); i++) {
                GuildMath.Quest q = qs.get(i);
                int prog = d.progOf(i), y = topPos + ROW0 + i * ROWH;
                boolean done = prog == GuildData.DONE;
                Component line = describe(q);
                String s = font.plainSubstrByWidth(line.getString(), 150);
                g.drawString(font, s, leftPos + 12, y + 1, done ? DIM : TEXT, false);
                String sub = done ? Component.translatable("hukhong.guild.state.done").getString()
                        : prog >= 0 ? Component.translatable("hukhong.guild.progress_line", Math.min(prog, q.amount()), q.amount()).getString() + "  "
                        + TuTinhText.of(q.ha()).getString() + " +" + q.points()
                        : TuTinhText.of(q.ha()).getString() + " +" + q.points() + (q.bonus() ? " +" : "");
                g.drawString(font, font.plainSubstrByWidth(sub, 150), leftPos + 12, y + 11, done ? GOOD : DIM, false);
            }
            int ty = topPos + ROW0 + qs.size() * ROWH + 14;
            if (d.rank() < GuildMath.RANKS - 1) {
                GuildMath.Trial t = GuildMath.trial(d.rank() + 1);
                g.drawString(font, Component.translatable("hukhong.guild.trial_title", Component.translatable("hukhong.guild.rank." + (d.rank() + 1))), leftPos + 12, ty - 1, GOLD, false);
                Component td = Component.translatable(t.kind() == GuildMath.K_HUNT ? "hukhong.guild.trial.hunt" : "hukhong.guild.trial.bicanh",
                        t.amount(), Component.translatable("hukhong.bicanh.tier." + t.tier()));
                g.drawString(font, font.plainSubstrByWidth(td.getString(), 150), leftPos + 12, ty + 9, d.trial() >= 0 ? TEXT : DIM, false);
                if (d.trial() >= 0) g.drawString(font, Component.translatable("hukhong.guild.progress_line", Math.min(d.trial(), t.amount()), t.amount()), leftPos + 12, ty + 19, GOOD, false);
            } else {
                g.drawString(font, Component.translatable("hukhong.guild.max_rank"), leftPos + 12, ty, GOLD, false);
            }
        }

        private static Component describe(GuildMath.Quest q) {
            return switch (q.kind()) {
                case GuildMath.K_KILL -> Component.translatable("hukhong.guild.q.0", q.amount(), q.lvLo(), q.lvHi());
                case GuildMath.K_HERB -> Component.translatable("hukhong.guild.q.1", q.amount());
                case GuildMath.K_COURIER -> Component.translatable("hukhong.guild.q.2", q.tier());
                case GuildMath.K_CLEANSE -> Component.translatable("hukhong.guild.q.3", q.amount());
                case GuildMath.K_HUNT -> Component.translatable("hukhong.guild.q.4", q.amount());
                default -> Component.translatable("hukhong.guild.q.5", Component.translatable("hukhong.bicanh.tier." + q.tier()));
            };
        }

        @Override
        protected void renderLabels(GuiGraphics g, int mx, int my) { }
    }

    // ================= CUA HANG =================
    static class ShopScreen extends AbstractContainerScreen<ShopMenu> {
        private static final int SELL_PAGE = 7, ROW_H = 22, ROW0 = 44;
        private boolean sellTab;
        private int page;
        private boolean confirm;
        private long confirmUntil;
        private List<SellPrices.Row> sellRows = List.of();
        private long sig;

        ShopScreen(ShopMenu m, Inventory inv, Component title) {
            super(m, inv, title);
            imageWidth = 300;
            inventoryLabelY = 10000;
            titleLabelY = 10000;
        }

        private List<ShopMenu.Entry> rows() { return menu.s.buys(); }

        private static long signature(List<SellPrices.Row> l) {
            long h = l.size();
            for (SellPrices.Row r : l) h = h * 31 + r.count() * 7919L + r.unit();
            return h;
        }

        private int pages() { return Math.max(1, (sellRows.size() + SELL_PAGE - 1) / SELL_PAGE); }

        @Override
        protected void init() {
            sellRows = SellPrices.rows(Minecraft.getInstance().player);
            sig = signature(sellRows);
            page = Math.max(0, Math.min(page, pages() - 1));
            imageHeight = Math.max(ROW0 + 12 + menu.s.buys().size() * ROW_H, ROW0 + SELL_PAGE * ROW_H + 34);
            super.init();
            addRenderableWidget(Button.builder(Component.translatable("hukhong.shop.tab.buy"), b -> { sellTab = false; confirm = false; rebuildInit(); }).bounds(leftPos + 12, topPos + 22, 60, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("hukhong.shop.tab.sell"), b -> { sellTab = true; rebuildInit(); }).bounds(leftPos + 76, topPos + 22, 60, 16).build());
            if (!sellTab) {
                List<ShopMenu.Entry> rows = rows();
                for (int i = 0; i < rows.size(); i++) {
                    final int idx = i;
                    int y = topPos + ROW0 + i * ROW_H;
                    Button b = Button.builder(Component.translatable("hukhong.shop.btn.buy"), x -> press(menu.containerId, idx)).bounds(leftPos + imageWidth - 70, y, 58, 18).build();
                    b.active = menu.s.rank() >= rows.get(i).minRank();
                    addRenderableWidget(b);
                }
                return;
            }
            int from = page * SELL_PAGE, to = Math.min(sellRows.size(), from + SELL_PAGE);
            for (int i = from; i < to; i++) {
                final int idx = i;
                int y = topPos + ROW0 + (i - from) * ROW_H;
                addRenderableWidget(Button.builder(Component.translatable("hukhong.shop.btn.sell1"), x -> press(menu.containerId, ShopMenu.B_SELL1 + idx)).bounds(leftPos + imageWidth - 100, y, 40, 18).build());
                addRenderableWidget(Button.builder(Component.translatable("hukhong.shop.btn.sellall"), x -> press(menu.containerId, ShopMenu.B_SELLALL + idx)).bounds(leftPos + imageWidth - 56, y, 44, 18).build());
            }
            int by = topPos + ROW0 + SELL_PAGE * ROW_H + 6;
            Button prev = Button.builder(Component.literal("<"), b -> { page--; rebuildInit(); }).bounds(leftPos + 12, by, 20, 18).build();
            prev.active = page > 0;
            addRenderableWidget(prev);
            Button next = Button.builder(Component.literal(">"), b -> { page++; rebuildInit(); }).bounds(leftPos + 70, by, 20, 18).build();
            next.active = page < pages() - 1;
            addRenderableWidget(next);
            long total = 0;
            for (SellPrices.Row r : sellRows) total += r.total();
            Component label = confirm ? Component.translatable("hukhong.shop.btn.sell_everything_confirm", TuTinhText.of(total))
                    : Component.translatable("hukhong.shop.btn.sell_everything");
            Button all = Button.builder(label, b -> {
                if (!confirm) { confirm = true; confirmUntil = System.currentTimeMillis() + 4000; }
                else { confirm = false; press(menu.containerId, ShopMenu.B_SELLEVERYTHING); }
                rebuildInit();
            }).bounds(leftPos + 100, by, imageWidth - 112, 18).build();
            all.active = !sellRows.isEmpty();
            addRenderableWidget(all);
        }

        private void rebuildInit() { clearWidgets(); init(); }

        @Override
        public void containerTick() {
            super.containerTick();
            if (!sellTab || minecraft == null || minecraft.player == null) return;
            if (confirm && System.currentTimeMillis() > confirmUntil) { confirm = false; rebuildInit(); return; }
            if (minecraft.player.tickCount % 5 == 0 && signature(SellPrices.rows(minecraft.player)) != sig) rebuildInit();
        }

        @Override
        protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
            panel(g, leftPos, topPos, imageWidth, imageHeight);
            g.drawString(font, title, leftPos + 12, topPos + 8, GOLD, false);
            var mc = Minecraft.getInstance();
            Component w = Component.translatable("hukhong.shop.wallet", TuTinhText.of(Wallet.count(mc.player)));
            g.drawString(font, w, leftPos + imageWidth - 12 - font.width(w), topPos + 8, TEXT, false);
            if (sellTab) { renderSell(g); return; }
            if (menu.s.discountPct() > 0)
                g.drawString(font, Component.translatable("hukhong.shop.discount", menu.s.discountPct()), leftPos + 146, topPos + 26, GOOD, false);
            List<ShopMenu.Entry> rows = rows();
            if (rows.isEmpty()) g.drawString(font, Component.translatable("hukhong.shop.empty"), leftPos + 12, topPos + 48, DIM, false);
            for (int i = 0; i < rows.size(); i++) {
                ShopMenu.Entry e = rows.get(i);
                int y = topPos + ROW0 + i * ROW_H;
                boolean locked = menu.s.rank() < e.minRank();
                g.renderItem(e.stack(), leftPos + 12, y + 1);
                g.renderItemDecorations(font, e.stack(), leftPos + 12, y + 1);
                g.drawString(font, font.plainSubstrByWidth(e.stack().getHoverName().getString(), 190), leftPos + 34, y, locked ? DIM : TEXT, false);
                Component price = locked ? Component.translatable("hukhong.shop.locked", Component.translatable(menu.s.marketLocks() ? com.hukhong.market.MarketRep.tierKey(e.minRank()) : "hukhong.guild.rank." + e.minRank()))
                        : TuTinhText.of(e.price());
                g.drawString(font, font.plainSubstrByWidth(price.getString(), 190), leftPos + 34, y + 10, locked ? BAD : GOLD, false);
            }
        }

        private void renderSell(GuiGraphics g) {
            if (sellRows.isEmpty()) g.drawString(font, Component.translatable("hukhong.shop.sell_empty"), leftPos + 12, topPos + 48, DIM, false);
            g.drawString(font, Component.translatable("hukhong.shop.rep_note", SellPrices.REP_PCT), leftPos + 146, topPos + 26, GOOD, false);
            int from = page * SELL_PAGE, to = Math.min(sellRows.size(), from + SELL_PAGE);
            int textW = imageWidth - 34 - 108;
            for (int i = from; i < to; i++) {
                SellPrices.Row r = sellRows.get(i);
                int y = topPos + ROW0 + (i - from) * ROW_H;
                g.renderItem(r.proto(), leftPos + 12, y + 1);
                g.drawString(font, font.plainSubstrByWidth(r.proto().getHoverName().getString() + " x" + r.count(), textW), leftPos + 34, y, TEXT, false);
                String sub = Component.translatable("hukhong.shop.sell_line", TuTinhText.of(r.unit()), TuTinhText.of(r.total())).getString();
                g.drawString(font, font.plainSubstrByWidth(sub, textW), leftPos + 34, y + 10, GOLD, false);
            }
            Component pg = Component.literal((page + 1) + "/" + pages());
            g.drawString(font, pg, leftPos + 51 - font.width(pg) / 2, topPos + ROW0 + SELL_PAGE * ROW_H + 11, DIM, false);
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            super.render(g, mx, my, pt); // nen mo (renderBackground) ve truoc, roi renderBg + widget; tooltip ve sau cung
            if (!sellTab) return;
            int from = page * SELL_PAGE, to = Math.min(sellRows.size(), from + SELL_PAGE);
            for (int i = from; i < to; i++) {
                int x = leftPos + 12, y = topPos + ROW0 + (i - from) * ROW_H + 1;
                if (mx >= x && mx < x + 16 && my >= y && my < y + 16) g.renderTooltip(font, sellRows.get(i).proto(), mx, my);
            }
        }

        @Override
        protected void renderLabels(GuiGraphics g, int mx, int my) { }
    }
}
