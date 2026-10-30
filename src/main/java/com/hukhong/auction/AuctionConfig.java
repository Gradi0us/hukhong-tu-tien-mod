package com.hukhong.auction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

/** Cau hinh Dau Gia (config/hukhong-auction.json, tao voi gia tri mac dinh neu chua co). Doc 1 lan, an toan khi thieu thu muc config (test). */
public final class AuctionConfig {
    public int[] durationsHours = {12, 24, 48};
    public int saleFeePct = AuctionMath.SALE_FEE_PCT;
    public int listingFeePct = AuctionMath.LISTING_FEE_PCT;
    public int maxLotsPerPlayer = AuctionMath.MAX_LOTS;
    /** Dau Gia Hoi: gio may chu. */
    public String[] liveDays = {"WEDNESDAY", "SATURDAY"};
    public int liveHour = 20, liveMinute = 0, liveLeadMinutes = 10, liveGraceMinutes = 20;
    public int liveRadius = 48, bidSeconds = 20;
    /** Ngan sach NPC mua lai / ngay thuc (Ha) va so lo NPC toi da tren san. */
    public long npcDailyBudget = 30_000L;
    public int npcMaxLots = 10;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile AuctionConfig cfg;

    public static AuctionConfig get() {
        AuctionConfig c = cfg;
        if (c == null) cfg = c = load();
        return c;
    }

    /** Dung trong test de ghi de. */
    public static void set(AuctionConfig c) { cfg = c; }

    private static AuctionConfig load() {
        try {
            Path f = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("hukhong-auction.json");
            if (Files.exists(f)) {
                AuctionConfig c = GSON.fromJson(Files.readString(f), AuctionConfig.class);
                if (c != null && c.durationsHours != null && c.durationsHours.length > 0 && c.liveDays != null) return c;
            } else {
                Files.writeString(f, GSON.toJson(new AuctionConfig()));
            }
        } catch (Throwable t) {
            LogUtils.getLogger().warn("Auction config unavailable, using defaults: {}", t.toString());
        }
        return new AuctionConfig();
    }

    public Set<DayOfWeek> days() {
        Set<DayOfWeek> s = EnumSet.noneOf(DayOfWeek.class);
        for (String d : liveDays) {
            try { s.add(DayOfWeek.valueOf(d.trim().toUpperCase(java.util.Locale.ROOT))); } catch (IllegalArgumentException ignored) { }
        }
        return s;
    }

    public int hours(int idx) { return durationsHours[Math.max(0, Math.min(durationsHours.length - 1, idx))]; }
}
