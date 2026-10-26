package com.hukhong.flight;

import com.hukhong.core.Path;

/** Kiểu bay theo đạo (S2 §2.1-2.4). Ngưỡng cấp từ canon: Kiếm 20, Đạo/Thể/Ma 60. */
public enum FlightMode {
    KIEM(Path.KIEM, 20, 4.0, 4.0, 6.0, 0.075f, 4),      // ngự kiếm: 4 LL/s, bay nhanh 6/s, tốc x1.5
    DAO(Path.DAO, 60, 2.5, 1.0, 2.5, 0.065f, 3),        // cánh thiên sứ: 2.5 TL/s, đứng yên 1/s
    THE(Path.THE, 60, 1.0, 0.5, 2.0, 0.06f, 20),        // đạp không: chỉ Bá Khí (không dùng mayfly)
    MA(Path.MA, 60, 3.0, 1.5, 3.0, 0.065f, 3);          // ma vân [GIẢ ĐỊNH 3/s]

    public final Path path;
    public final int minLevel;
    public final double cost, hoverCost, fastCost;
    public final float speed;
    public final int startMin;

    FlightMode(Path p, int lv, double cost, double hover, double fast, float speed, int startMin) {
        this.path = p; this.minLevel = lv; this.cost = cost; this.hoverCost = hover;
        this.fastCost = fast; this.speed = speed; this.startMin = startMin;
    }

    /** True flight (mayfly) hay chỉ nhảy bậc không. */
    public boolean trueFlight() { return this != THE; }

    /** Số đôi cánh Đạo: 60->1, 80->2, 95->3. */
    public static int wingPairs(int level) { return level >= 95 ? 3 : level >= 80 ? 2 : 1; }

    public static FlightMode byOrdinal(int i) { return i < 0 || i >= values().length ? null : values()[i]; }
}
