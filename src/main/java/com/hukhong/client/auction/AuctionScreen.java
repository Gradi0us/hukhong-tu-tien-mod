package com.hukhong.client.auction;

import com.hukhong.auction.AuctionMath;
import com.hukhong.auction.AuctionNet;
import com.hukhong.auction.Tx;
import com.hukhong.currency.TuTinhText;
import com.hukhong.currency.Wallet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Man hinh Dau Gia Hanh: loc / sap xep / tim kiem, tra gia / mua ngay, dang vat pham dang cam. Server tinh lai moi thu. */
public class AuctionScreen extends Screen {
    private static final int W = 360, H = 232, ROWS = AuctionNet.PAGE, ROW_H = 20;
    private static final int PANEL = 0xF0201A28, EDGE = 0xFFB8A46A, TEXT = 0xFFE8E0D0, DIM = 0xFF9A9088, GOOD = 0xFF8FE08F, GOLD = 0xFFFFD34F, SEL = 0x40FFD34F;
    private static final String[] FILTERS = {"all", "honcot", "pill", "book", "mat", "gear", "mine"};
    private static final String[] FILTER_VI = {"Tất cả", "Hồn Cốt", "Đan", "Sách", "Liệu", "Gear", "Của tôi"};
    private static final String[] SORT_VI = {"Sắp hết hạn", "Giá thấp", "Giá cao", "Mới đăng"};

    private int filter, sort, page, hoursIdx;
    private long selected = -1;
    private EditBox search, bidBox, startBox, buyBox;
    private int left, top;

    public AuctionScreen() { super(Tx.t("hukhong.auction.title", "Đấu Giá Hành", "Auction House")); }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        for (int i = 0; i < FILTERS.length; i++) {
            final int f = i;
            addRenderableWidget(Button.builder(Tx.t("hukhong.auction.f." + FILTERS[i], FILTER_VI[i], FILTER_VI[i]), b -> { filter = f; page = 0; query(); })
                    .bounds(left + 8 + i * 49, top + 22, 48, 14).build());
        }
        search = new EditBox(font, left + 8, top + 40, 110, 14, Tx.t("hukhong.auction.search", "Tìm", "Search"));
        search.setHint(Tx.t("hukhong.auction.search", "Tìm", "Search"));
        search.setMaxLength(32);
        search.setResponder(s -> { page = 0; });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Tx.t("hukhong.auction.go", "Tìm", "Go"), b -> query()).bounds(left + 120, top + 40, 28, 14).build());
        addRenderableWidget(Button.builder(Component.literal(sortLabel()), b -> {
            sort = (sort + 1) % SORT_VI.length;
            b.setMessage(Component.literal(sortLabel()));
            query();
        }).bounds(left + 152, top + 40, 80, 14).build());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { page = Math.max(0, page - 1); query(); }).bounds(left + 300, top + 40, 20, 14).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; query(); }).bounds(left + 324, top + 40, 20, 14).build());
        bidBox = new EditBox(font, left + 236, top + H - 52, 56, 14, Component.empty());
        bidBox.setHint(Tx.t("hukhong.auction.bidhint", "Giá", "Bid"));
        addRenderableWidget(bidBox);
        addRenderableWidget(Button.builder(Tx.t("hukhong.auction.bid", "Trả giá", "Bid"), b -> act(AuctionNet.OP_BID)).bounds(left + 294, top + H - 53, 46, 16).build());
        addRenderableWidget(Button.builder(Tx.t("hukhong.auction.buy", "Mua ngay", "Buy now"), b -> act(AuctionNet.OP_BUY)).bounds(left + 294 + 0, top + H - 36, 46, 16).build());
        // dang ban
        startBox = new EditBox(font, left + 8, top + H - 36, 56, 14, Component.empty());
        startBox.setHint(Tx.t("hukhong.auction.starthint", "Khởi điểm", "Start"));
        buyBox = new EditBox(font, left + 68, top + H - 36, 56, 14, Component.empty());
        buyBox.setHint(Tx.t("hukhong.auction.buyhint", "Mua ngay", "Buy now"));
        addRenderableWidget(startBox);
        addRenderableWidget(buyBox);
        addRenderableWidget(Button.builder(Component.literal(hoursLabel()), b -> {
            List<String> h = hours();
            hoursIdx = (hoursIdx + 1) % Math.max(1, h.size());
            b.setMessage(Component.literal(hoursLabel()));
        }).bounds(left + 128, top + H - 37, 36, 16).build());
        addRenderableWidget(Button.builder(Tx.t("hukhong.auction.list", "Đăng tay cầm", "List held"), b -> list()).bounds(left + 168, top + H - 37, 62, 16).build());
        addRenderableWidget(Button.builder(Tx.t("hukhong.auction.cancel", "Rút lô", "Withdraw"), b -> act(AuctionNet.OP_CANCEL)).bounds(left + 236, top + H - 36, 54, 16).build());
        addRenderableWidget(Button.builder(Tx.t("hukhong.auction.mail", "Thư", "Mail"), b -> Minecraft.getInstance().player.connection.sendCommand("thu")).bounds(left + W - 40, top + 4, 32, 14).build());
        query();
    }

    private List<String> hours() { var l = AuctionClient.lots; return l == null ? List.of("12", "24", "48") : l.hours(); }

    private String hoursLabel() { List<String> h = hours(); return h.get(Math.min(hoursIdx, h.size() - 1)) + "h"; }

    private String sortLabel() { return SORT_VI[sort]; }

    void refresh() { }

    private void query() {
        PacketDistributor.sendToServer(new AuctionNet.Query(filter, sort, search == null ? "" : search.getValue(), page));
    }

    private void act(int op) {
        if (selected < 0) return;
        long amount = op == AuctionNet.OP_BID ? AuctionMath.parseHa(bidBox.getValue()) : 0;
        if (op == AuctionNet.OP_BID && amount <= 0) {
            AuctionNet.LotView v = selectedView();
            if (v == null) return;
            amount = v.bidder().isEmpty() ? v.start() : v.bid() + Math.max(1, v.bid() * AuctionMath.MIN_STEP_PCT / 100);
        }
        PacketDistributor.sendToServer(new AuctionNet.Act(op, selected, amount, 0, 0));
    }

    private void list() {
        long start = AuctionMath.parseHa(startBox.getValue()), buy = buyBox.getValue().isBlank() ? 0 : AuctionMath.parseHa(buyBox.getValue());
        if (start <= 0 || buy < 0) {
            Minecraft.getInstance().player.displayClientMessage(Tx.t("hukhong.auction.err.input", "Nhập giá khởi điểm (vd 500, 12t = 12 Trung, 3th = 3 Thượng).", "Enter a start price (e.g. 500, 12t = 12 Trung, 3th = 3 Thuong)."), true);
            return;
        }
        PacketDistributor.sendToServer(new AuctionNet.Act(AuctionNet.OP_LIST, 0, start, buy, hoursIdx));
    }

    private AuctionNet.LotView selectedView() {
        var l = AuctionClient.lots;
        if (l == null) return null;
        for (AuctionNet.LotView v : l.views()) if (v.id() == selected) return v;
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        var l = AuctionClient.lots;
        if (l != null) {
            for (int i = 0; i < l.views().size(); i++) {
                int y = top + 58 + i * ROW_H;
                if (mx >= left + 8 && mx < left + W - 8 && my >= y && my < y + ROW_H) { selected = l.views().get(i).id(); break; }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    /** Bỏ lớp làm mờ mặc định của Screen.render (nó vẽ đè lên nội dung tự vẽ khiến chữ bị nhòe). */
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) { }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        g.fill(left, top, left + W, top + H, PANEL);
        g.renderOutline(left, top, W, H, EDGE);
        g.drawString(font, title, left + 8, top + 8, GOLD, false);
        Component w = Tx.t("hukhong.shop.wallet", "Ví: %s", "Wallet: %s", TuTinhText.of(Wallet.count(Minecraft.getInstance().player)));
        g.drawString(font, w, left + W - 48 - font.width(w), top + 8, TEXT, false);
        var l = AuctionClient.lots;
        net.minecraft.world.item.ItemStack hover = net.minecraft.world.item.ItemStack.EMPTY;
        if (l != null) {
            if (l.views().isEmpty()) g.drawString(font, Tx.t("hukhong.auction.empty", "Không có lô nào.", "No lots."), left + 12, top + 66, DIM, false);
            for (int i = 0; i < l.views().size(); i++) {
                AuctionNet.LotView v = l.views().get(i);
                int y = top + 58 + i * ROW_H;
                if (v.id() == selected) g.fill(left + 8, y, left + W - 8, y + ROW_H - 1, SEL);
                g.renderItem(v.stack(), left + 10, y + 1);
                g.renderItemDecorations(font, v.stack(), left + 10, y + 1);
                if (mx >= left + 10 && mx < left + 26 && my >= y + 1 && my < y + 17) hover = v.stack();
                g.drawString(font, font.plainSubstrByWidth(v.stack().getHoverName().getString(), 140), left + 30, y + 1, v.high() ? GOOD : TEXT, false);
                String sub = (v.npc() ? "NPC" : v.seller()) + (v.bidder().isEmpty() ? "" : " > " + v.bidder());
                g.drawString(font, font.plainSubstrByWidth(sub, 140), left + 30, y + 10, DIM, false);
                g.drawString(font, font.plainSubstrByWidth(TuTinhText.of(v.price()).getString(), 120), left + 176, y + 1, GOLD, false);
                if (v.buyNow() > 0) g.drawString(font, font.plainSubstrByWidth(TuTinhText.of(v.buyNow()).getString(), 110), left + 176, y + 10, GOOD, false);
                g.drawString(font, AuctionClient.time(v.remainMs()), left + W - 12 - font.width(AuctionClient.time(v.remainMs())), y + 5, DIM, false);
            }
            g.drawString(font, (l.page() + 1) + "/" + l.pages() + "  " + l.myLots() + "/" + l.maxLots(), left + 262, top + 44, DIM, false);
        }
        AuctionNet.LotView sv = selectedView();
        if (sv != null) {
            g.drawString(font, Tx.t("hukhong.auction.floor", "Giá sàn lô: %s", "Lot floor: %s", TuTinhText.of(sv.floor())), left + 8, top + H - 54, DIM, false);
        }
        super.render(g, mx, my, pt);
        if (!hover.isEmpty()) g.renderTooltip(font, hover, mx, my);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
