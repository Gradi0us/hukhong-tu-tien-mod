package com.hukhong.client.auction;

import com.hukhong.HuKhong;
import com.hukhong.auction.LiveAuction;
import com.hukhong.auction.Tx;
import com.hukhong.currency.TuTinhText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** HUD Dau Gia Hoi: "Lo 3/8 - Gia hien tai: 2.4 (Tan tu Mac Phong) - 14s" khi dang o gan hoi truong. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class LiveHud {
    private LiveHud() {}

    private static final int TEXT = 0xFFE8E0D0, DIM = 0xFF9A9088, GOLD = 0xFFFFD34F, GOOD = 0xFF8FE08F, BAD = 0xFFE08F8F;

    @SubscribeEvent
    public static void onLayers(RegisterGuiLayersEvent e) {
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "live_auction_hud"), (LayeredDraw.Layer) (g, dt) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof LiveScreen || mc.options.hideGui || !AuctionClient.live.active()) return;
            g.fill(g.guiWidth() / 2 - 150, 6, g.guiWidth() / 2 + 150, 50, 0xB0201A28);
            draw(g, mc.font, g.guiWidth() / 2 - 144, 10, -1, -1, false);
        });
    }

    /** Ve khoi thong tin: lo, gia, nguoi dang giu, dong ho. Dung chung HUD va LiveScreen. */
    public static void draw(GuiGraphics g, Font font, int x, int y, int mx, int my, boolean icon) {
        var s = AuctionClient.live;
        if (!s.active()) return;
        if (s.phase() == LiveAuction.P_COUNT) {
            g.drawString(font, Tx.t("hukhong.live.hud.count", "Đấu Giá Hội khai mạc sau %s", "Grand Auction opens in %s", AuctionClient.time(s.secLeft() * 1000L)), x, y, GOLD, false);
            return;
        }
        Component name = s.mystery() && s.stack().isEmpty() ? Tx.t("hukhong.live.mystery", "Bảo Vật Bí Ẩn (chưa giám định)", "Mystery Treasure (unidentified)") : s.stack().getHoverName();
        int ix = x;
        if (!s.stack().isEmpty()) {
            g.renderItem(s.stack(), x, y);
            if (mx >= x && mx < x + 16 && my >= y && my < y + 16) g.renderTooltip(font, s.stack(), mx, my);
            ix += 20;
        } else if (s.mystery()) {
            g.drawString(font, "?", x + 5, y + 4, GOLD, false);
            ix += 20;
        }
        g.drawString(font, Tx.t("hukhong.live.hud.lot", "Lô %s/%s", "Lot %s/%s", s.idx() + 1, s.count()), ix, y, DIM, false);
        g.drawString(font, font.plainSubstrByWidth(name.getString(), 220), ix, y + 10, s.locked() ? BAD : TEXT, false);
        String holder = s.holder().isEmpty() ? "-" : s.holder();
        Component price = Tx.t("hukhong.live.hud.price", "Giá: %s (%s)", "Price: %s (%s)", TuTinhText.of(s.price()), holder);
        g.drawString(font, font.plainSubstrByWidth(price.getString(), 270), x, y + 24, s.iHold() ? GOOD : GOLD, false);
        String clock = switch (s.phase()) {
            case LiveAuction.P_BID -> s.secLeft() + "s";
            case LiveAuction.P_SOLD -> Tx.t("hukhong.live.hud.sold", "ĐÃ BÁN", "SOLD").getString();
            case LiveAuction.P_UNSOLD -> Tx.t("hukhong.live.hud.unsold", "LUI", "PASSED").getString();
            case LiveAuction.P_INTRO -> "...";
            default -> "";
        };
        g.drawString(font, clock, x + 280 - font.width(clock), y, s.secLeft() <= 5 && s.phase() == LiveAuction.P_BID ? BAD : GOLD, false);
        if (s.locked()) g.drawString(font, Tx.t("hukhong.live.hud.locked", "Lô khóa theo chương", "Locked by chapter"), x, y + 36, BAD, false);
    }
}
