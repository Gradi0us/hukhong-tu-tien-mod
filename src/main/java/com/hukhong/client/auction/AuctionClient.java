package com.hukhong.client.auction;

import com.hukhong.auction.AuctionNet;
import net.minecraft.client.Minecraft;

/** Trang thai phia client cua Dau Gia Hanh / hom thu / Dau Gia Hoi (khong doc CULTIVATION). */
public final class AuctionClient {
    private AuctionClient() {}

    public static volatile AuctionNet.Lots lots;
    public static volatile AuctionNet.MailView mail;
    public static volatile AuctionNet.LiveState live = AuctionNet.LiveState.OFF;

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AuctionScreen)) mc.setScreen(new AuctionScreen());
    }

    public static void lots(AuctionNet.Lots l) {
        lots = l;
        if (Minecraft.getInstance().screen instanceof AuctionScreen s) s.refresh();
    }

    public static void mail(AuctionNet.MailView m) {
        mail = m;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MailScreen s) s.refresh();
        else mc.setScreen(new MailScreen());
    }

    public static void live(AuctionNet.LiveState s) {
        live = s;
        if (Minecraft.getInstance().screen instanceof LiveScreen ls) ls.refresh();
    }

    public static void openLive() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof LiveScreen)) mc.setScreen(new LiveScreen());
    }

    /** "3h 05m" / "12m 30s" / "45s". */
    public static String time(long ms) {
        long s = Math.max(0, ms / 1000);
        if (s >= 3600) return (s / 3600) + "h " + String.format("%02d", (s / 60) % 60) + "m";
        if (s >= 60) return (s / 60) + "m " + String.format("%02d", s % 60) + "s";
        return s + "s";
    }
}
