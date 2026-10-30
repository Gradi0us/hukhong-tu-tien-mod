package com.hukhong.client.auction;

import com.hukhong.auction.AuctionNet;
import com.hukhong.auction.LiveAuction;
import com.hukhong.auction.Tx;
import com.hukhong.currency.TuTinhText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Dau Gia Hoi truc tiep: [Gio the +5%] [+10%] [Bo] (cung HUD ben duoi). Khong dung game. */
public class LiveScreen extends Screen {
    private static final int W = 300, H = 120;
    private static final int PANEL = 0xE0201A28, EDGE = 0xFFB8A46A, TEXT = 0xFFE8E0D0, DIM = 0xFF9A9088, GOLD = 0xFFFFD34F, GOOD = 0xFF8FE08F, BAD = 0xFFE08F8F;
    private int left, top;
    private Button b5, b10, pass;

    public LiveScreen() { super(Tx.t("hukhong.live.title", "Đấu Giá Hội", "Grand Auction")); }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = height - H - 28;
        b5 = addRenderableWidget(Button.builder(Component.literal("+5%"), b -> PacketDistributor.sendToServer(new AuctionNet.LiveBid(5))).bounds(left + 10, top + H - 26, 86, 18).build());
        b10 = addRenderableWidget(Button.builder(Component.literal("+10%"), b -> PacketDistributor.sendToServer(new AuctionNet.LiveBid(10))).bounds(left + 106, top + H - 26, 86, 18).build());
        pass = addRenderableWidget(Button.builder(Tx.t("hukhong.live.pass", "Bỏ lô", "Pass"), b -> PacketDistributor.sendToServer(new AuctionNet.LiveBid(0))).bounds(left + 202, top + H - 26, 88, 18).build());
        refresh();
    }

    void refresh() {
        if (b5 == null) return;
        var s = AuctionClient.live;
        boolean can = s.active() && s.phase() == LiveAuction.P_BID && !s.locked() && !s.passed() && !s.iHold();
        b5.active = b10.active = can;
        pass.active = s.active() && s.phase() == LiveAuction.P_BID && !s.passed();
        b5.setMessage(Component.literal("+5%  " + (can ? TuTinhText.of(s.raise5()).getString() : "")));
        b10.setMessage(Component.literal("+10%  " + (can ? TuTinhText.of(s.raise10()).getString() : "")));
    }

    /** Bỏ lớp làm mờ mặc định của Screen.render (nó vẽ đè lên nội dung tự vẽ khiến chữ bị nhòe). */
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) { renderTransparentBackground(g); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(left, top, left + W, top + H, PANEL);
        g.renderOutline(left, top, W, H, EDGE);
        refresh();
        LiveHud.draw(g, font, left + 10, top + 8, mx, my, true);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
