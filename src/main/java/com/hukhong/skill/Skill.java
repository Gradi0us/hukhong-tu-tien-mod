package com.hukhong.skill;

import com.hukhong.core.CultivationData;
import com.hukhong.core.Path;

/** Kỹ năng P0. Slot: 0=Q, 1=E, 2=R (chưa dùng), 3=T (tối thượng). */
public enum Skill {
    KIEM_1(Path.KIEM, 0, "kiem_1", 10, 12),
    KIEM_2(Path.KIEM, 1, "kiem_2", 25, 160),
    KIEM_ULT(Path.KIEM, 3, "kiem_ult", 80, 1200),
    MA_1(Path.MA, 0, "ma_1", 0, 16),
    MA_2(Path.MA, 1, "ma_2", 20, 400),
    MA_ULT(Path.MA, 3, "ma_ult", 60, 2400),
    DAO_1(Path.DAO, 0, "dao_1", 8, 16),
    DAO_2(Path.DAO, 1, "dao_2", 25, 400),
    DAO_ULT(Path.DAO, 3, "dao_ult", 100, 1800),
    THE_1(Path.THE, 0, "the_1", 30, 300),
    THE_2(Path.THE, 1, "the_2", 25, 360),
    THE_ULT(Path.THE, 3, "the_ult", 100, 2000);

    /** Hệ số sát thương kỹ năng 1 (nhân Phép lực / Công lực) - dùng chung với bài test cân bằng. */
    public static final double DAO_1_COEF = 0.90, THE_1_COEF = 1.00;
    /** Hệ số x Công (attack_power): Trảm Khí 120%, Liên Hoàn 3 x 70%, Kiếm Vực 55%/nhịp; Huyết Trảo 125%. */
    public static final double KIEM_1_COEF = 1.20, KIEM_2_COEF = 0.70, KIEM_ULT_COEF = 0.55, MA_1_COEF = 1.25;

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

    /** Đạo có bộ kỹ năng dùng cho phím (trang đang dùng theo phím V): Kiếm, Đạo, Thể, Ma. */
    public static Path skillPath(CultivationData d, int page) { return SkillPages.active(d, page); }
}
