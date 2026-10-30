package com.hukhong.client.auction;

import com.hukhong.auction.AuctionNet;
import com.hukhong.auction.Tx;
import com.hukhong.currency.TuTinhText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Hom thu Phi Kiem Truyen Thu: thu dang cho (tui day) + lich su gan day. */
public class MailScreen extends Screen {
    private static final int W = 300, H = 210;
    private static final int PANEL = 0xF0201A28, EDGE = 0xFFB8A46A, TEXT = 0xFFE8E0D0, DIM = 0xFF9A9088, GOLD = 0xFFFFD34F, GOOD = 0xFF8FE08F;
    private int left, top;

    public MailScreen() { super(Tx.t("hukhong.mail.title", "Phi Kiếm Truyền Thư", "Flying-Sword Mail")); }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Tx.t("hukhong.mail.claim", "Nhận tất cả", "Claim all"), b -> PacketDistributor.sendToServer(new AuctionNet.MailClaim()))
                .bounds(left + W - 88, top + 5, 80, 16).build());
    }

    void refresh() { }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        g.fill(left, top, left + W, top + H, PANEL);
        g.renderOutline(left, top, W, H, EDGE);
        g.drawString(font, title, left + 8, top + 9, GOLD, false);
        var m = AuctionClient.mail;
        net.minecraft.world.item.ItemStack hover = net.minecraft.world.item.ItemStack.EMPTY;
        if (m != null) {
            if (m.lines().isEmpty()) g.drawString(font, Tx.t("hukhong.mail.empty", "Hòm thư trống.", "Mailbox is empty."), left + 10, top + 30, DIM, false);
            for (int i = 0; i < m.lines().size() && i < 9; i++) {
                AuctionNet.MailLine l = m.lines().get(i);
                int y = top + 28 + i * 20;
                if (!l.item().isEmpty()) {
                    g.renderItem(l.item(), left + 10, y);
                    g.renderItemDecorations(font, l.item(), left + 10, y);
                    if (mx >= left + 10 && mx < left + 26 && my >= y && my < y + 16) hover = l.item();
                }
                Component subj = com.hukhong.auction.MailService.subject(l.subjectKey(), l.arg());
                g.drawString(font, font.plainSubstrByWidth(subj.getString(), 190), left + 32, y, l.pending() ? GOOD : TEXT, false);
                String info = (l.ha() > 0 ? TuTinhText.of(l.ha()).getString() + "  " : "") + AuctionClient.time(l.ageMs()) + " ago";
                g.drawString(font, font.plainSubstrByWidth(info, 250), left + 32, y + 9, DIM, false);
            }
        }
        super.render(g, mx, my, pt);
        if (!hover.isEmpty()) g.renderTooltip(font, hover, mx, my);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
