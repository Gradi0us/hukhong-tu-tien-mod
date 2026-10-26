package com.hukhong.bicanh;

import java.util.Locale;

/** 5 cấp nguy hiểm của Bí Cảnh (S4 §5.1). Thuần Java, kiểm thử được. */
public enum BiCanhTier {
    //   minLv maxLv phút party waves mobLv weight
    BACH(1, 25, 20, 4, 2, 8, 40),
    HOANG(15, 45, 25, 5, 3, 14, 28),
    TU(35, 65, 30, 6, 3, 20, 18),
    HAC(55, 85, 40, 8, 4, 26, 10),
    HONG(75, 100, 45, 10, 5, 30, 4);

    public final int minLevel, maxLevel, minutes, maxParty, waves, mobLevel, weight;

    BiCanhTier(int minLevel, int maxLevel, int minutes, int maxParty, int waves, int mobLevel, int weight) {
        this.minLevel = minLevel; this.maxLevel = maxLevel; this.minutes = minutes;
        this.maxParty = maxParty; this.waves = waves; this.mobLevel = mobLevel; this.weight = weight;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String nameKey() { return "hukhong.bicanh.tier." + ordinal(); }
    public boolean allows(int playerLevel) { return playerLevel >= minLevel && playerLevel <= maxLevel; }
    public int ticks() { return minutes * 60 * 20; }

    /** Số quái của sóng w (0-based), trước hệ số party. */
    public int mobsForWave(int w) { return 3 + ordinal() + 2 * w; }

    /** Hệ số theo số người: 1 + 0,35 (n-1). */
    public static double partyScale(int n) { return 1.0 + 0.35 * Math.max(0, n - 1); }

    public static BiCanhTier byIndex(int i) { return values()[Math.max(0, Math.min(values().length - 1, i))]; }

    /** Chọn cấp theo trọng số trong các cấp mà playerLevel được phép vào; không có -> BACH. r thuộc [0,1). */
    public static BiCanhTier roll(double r, int playerLevel) {
        int total = 0;
        for (BiCanhTier t : values()) if (t.allows(playerLevel)) total += t.weight;
        if (total == 0) return BACH;
        double x = r * total;
        for (BiCanhTier t : values()) {
            if (!t.allows(playerLevel)) continue;
            x -= t.weight;
            if (x < 0) return t;
        }
        return BACH;
    }
}
