package com.hukhong.skill;

import com.hukhong.core.CultivationData;
import com.hukhong.core.Path;

/** Kỹ năng P0. Slot: 0=Q, 1=E, 2=R (chưa dùng), 3=T (tối thượng). */
public enum Skill {
    DAO_1(Path.DAO, 0, "dao_1", 8, 16),
    DAO_2(Path.DAO, 1, "dao_2", 25, 400),
    DAO_ULT(Path.DAO, 3, "dao_ult", 100, 1800),
    THE_1(Path.THE, 0, "the_1", 30, 300),
    THE_2(Path.THE, 1, "the_2", 25, 360),
    THE_ULT(Path.THE, 3, "the_ult", 100, 2000);

    /** Hệ số sát thương kỹ năng 1 (nhân Phép lực / Công lực) - dùng chung với bài test cân bằng. */
    public static final double DAO_1_COEF = 0.90, THE_1_COEF = 1.00;

    public final Path path;
    public final int slot;
    public final String icon;
    public final int cost;
    public final int cooldownTicks;

    Skill(Path path, int slot, String icon, int cost, int cooldownTicks) {
        this.path = path;
        this.slot = slot;
        this.icon = icon;
        this.cost = cost;
        this.cooldownTicks = cooldownTicks;
    }

    public String langKey() { return "hukhong.skill." + icon; }

    public static Skill of(Path path, int slot) {
        if (path == null) return null;
        for (Skill s : values()) if (s.path == path && s.slot == slot) return s;
        return null;
    }

    /** Đạo có bộ kỹ năng dùng cho phím (P0: Đạo trước, rồi Thể). */
    public static Path skillPath(CultivationData d) {
        if (d.paths().contains(Path.DAO)) return Path.DAO;
        if (d.paths().contains(Path.THE)) return Path.THE;
        return null;
    }
}
