package com.hukhong.skill;

import com.hukhong.core.Path;

/**
 * Kỹ năng Bản Mệnh (S2 §4) trên phím R: mỗi đạo một tuyệt kỹ, mở ở cấp UNLOCK_LEVEL (cảnh giới 4),
 * mạnh dần theo cấp. Thuần dữ liệu/toán để test.
 */
public enum BanMenhSkill {
    NHAT_KIEM(Path.KIEM, "bm_kiem", 900, 60, 3.0, 0.05, 9.0),
    PHAN_XET(Path.DAO, "bm_dao", 1200, 70, 5.0, 0.10, 13.0),
    DIA_LIET(Path.THE, "bm_the", 1100, 55, 2.5, 0.075, 7.0),
    HUYET_HAI(Path.MA, "bm_ma", 1400, 65, 0.6, 0.015, 1.5);

    public static final int UNLOCK_LEVEL = 40;
    /** Cấp tối đa tính scale. */
    public static final int MAX_LEVEL = 100;

    public final Path path;
    public final String icon;
    public final int cooldownTicks;
    public final int cost;
    private final double base, perLevel, cap;

    BanMenhSkill(Path path, String icon, int cd, int cost, double base, double perLevel, double cap) {
        this.path = path;
        this.icon = icon;
        this.cooldownTicks = cd;
        this.cost = cost;
        this.base = base;
        this.perLevel = perLevel;
        this.cap = cap;
    }

    public String langKey() { return "hukhong.skill." + icon; }

    /** Hệ số sát thương (nhân Công/Thần Thức; Huyết Hải: mỗi giây). */
    public double coef(int level) {
        int l = Math.max(UNLOCK_LEVEL, Math.min(MAX_LEVEL, level));
        return Math.min(cap, base + perLevel * (l - UNLOCK_LEVEL));
    }

    /** Tầm/bán kính (block): 8 -> 20 theo cấp cho Nhất Kiếm; 8 cho Địa Liệt; 7 cho Huyết Hải; 14 cho Phán Quyết. */
    public double range(int level) {
        int l = Math.max(UNLOCK_LEVEL, Math.min(MAX_LEVEL, level));
        double t = (double) (l - UNLOCK_LEVEL) / (MAX_LEVEL - UNLOCK_LEVEL);
        return switch (this) {
            case NHAT_KIEM -> 8 + 12 * t;
            case PHAN_XET -> 14 + 6 * t;
            case DIA_LIET -> 8 + 2 * t;
            case HUYET_HAI -> 7 + 3 * t;
        };
    }

    public static BanMenhSkill of(Path path, int level) {
        if (path == null || level < UNLOCK_LEVEL) return null;
        for (BanMenhSkill s : values()) if (s.path == path) return s;
        return null;
    }
}
