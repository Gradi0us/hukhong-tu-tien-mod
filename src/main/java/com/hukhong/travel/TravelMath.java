package com.hukhong.travel;

/** Thuan Java: gia dich chuyen (Ha) theo khoang cach, hang so quy tac. */
public final class TravelMath {
    private TravelMath() {}

    public static final int CHANNEL_TICKS = 60, COOLDOWN_TICKS = 1200, COMBAT_TICKS = 100, DISCOVER_RADIUS = 150, RESUMMON_COOLDOWN_TICKS = 1200;

    /** Gia (Ha): 3 + 1 Ha / 64 block ngang, toi da 120 Ha. */
    public static String nameKey(String id) {
        if (id.startsWith("c:")) return "hukhong.city.n." + id.substring(2);
        if (id.startsWith("s:")) return "hukhong.sect.n." + id.substring(2);
        if (id.startsWith("g:")) return "hukhong.guild.hall";
        if (id.startsWith("m:")) return "hukhong.market.n." + id.substring(2);
        return "hukhong.travel.objective";
    }

    public static long cost(double dist) {
        return Math.max(3, Math.min(120, 3 + (long) (Math.max(0, dist) / 64.0)));
    }
}
