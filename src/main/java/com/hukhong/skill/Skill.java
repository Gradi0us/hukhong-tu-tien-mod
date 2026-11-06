package com.hukhong.skill;

import com.hukhong.core.CultivationData;
import com.hukhong.core.Path;

/** Kỹ năng P0. Slot: 0=Q, 1=E, 2=R (chưa dùng), 3=T (tối thượng). */
public enum Skill {
    KIEM_1(Path.KIEM, 0, "kiem_1", 12, 120), // 2026-11-06: Thiên Kiếm Trấn (đinh kiếm giam 5 địch), hồi 6s
    KIEM_2(Path.KIEM, 1, "kiem_2", 25, 300), // 2026-11-06: Thủ Hộ Chi Kiếm (6 kiếm đỡ đòn + phản kích), hồi 15s
    KIEM_ULT(Path.KIEM, 3, "kiem_ult", 64, 1200), // 2026-11-05: mana -20%
    MA_1(Path.MA, 0, "ma_1", 0, 16),
    MA_2(Path.MA, 1, "ma_2", 20, 400),
    MA_ULT(Path.MA, 3, "ma_ult", 60, 2400),
    DAO_1(Path.DAO, 0, "dao_1", 5, 12),
    DAO_2(Path.DAO, 1, "dao_2", 25, 400),
    DAO_ULT(Path.DAO, 3, "dao_ult", 100, 1800),
    THE_1(Path.THE, 0, "the_1", 30, 300),
    THE_2(Path.THE, 1, "the_2", 25, 360),
    THE_ULT(Path.THE, 3, "the_ult", 100, 2000),
    /** Pháp Tu: 4 phím chung cho 6 nguyên tố; hiệu ứng/hệ số theo nguyên tố ở com.hukhong.phaptu (PhapSkills, PhapMath). */
    PHAP_1(Path.PHAP, 0, "phap_1", 3, 14),
    PHAP_2(Path.PHAP, 1, "phap_2", 25, 160),
    PHAP_3(Path.PHAP, 2, "phap_3", 35, 400),
    PHAP_ULT(Path.PHAP, 3, "phap_ult", 90, 1500),
    /** Trận Tu: BASE (Khốn/Sát/Hộ Thân) + 3 thế (T + Q/E/R). TRAN_ULT là phím Thế (T). Logic ở com.hukhong.tran. */
    TRAN_1(Path.TRAN, 0, "tran_khon", 4, 14),
    TRAN_2(Path.TRAN, 1, "tran_sat", 25, 400),
    TRAN_3(Path.TRAN, 2, "tran_ho", 35, 400),
    TRAN_ULT(Path.TRAN, 3, "tran_stance", 0, 4),
    TRAN_CAGE(Path.TRAN, 0, "tran_cage", 30, 600),
    TRAN_SLOW(Path.TRAN, 1, "tran_slow", 25, 400),
    TRAN_SUPPRESS(Path.TRAN, 2, "tran_suppress", 35, 500),
    TRAN_PILLAR(Path.TRAN, 0, "tran_pillar", 45, 300), // 2026-11-05: 25 -> 45 mana
    TRAN_BLADES(Path.TRAN, 1, "tran_blades", 35, 400),
    TRAN_DOOM(Path.TRAN, 2, "tran_doom", 60, 1200),
    TRAN_WARD(Path.TRAN, 0, "tran_ward", 25, 400),
    TRAN_HEAL(Path.TRAN, 1, "tran_heal", 30, 400),
    TRAN_SHIELD(Path.TRAN, 2, "tran_shield", 40, 600),
    DAN_1(Path.DAN, 0, "dan_1", 4, 14),
    DAN_2(Path.DAN, 1, "dan_2", 30, 500),
    DAN_3(Path.DAN, 2, "dan_3", 35, 360),
    DAN_ULT(Path.DAN, 3, "dan_ult", 90, 1800),
    /** Ma Tu R: Ma Vực (xem MaDomain). */
    MA_3(Path.MA, 2, "ma_3", 40, 1000);

    /** Hệ số sát thương kỹ năng 1 (nhân Phép lực / Công lực) - dùng chung với bài test cân bằng. */
    public static final double DAO_1_COEF = 1.10, THE_1_COEF = 4.00;
    /** Hệ số x Công (attack_power): Trảm Khí 120%, Liên Hoàn 3 x 70%, Kiếm Vực 55%/nhịp; Huyết Trảo 125% tổng (3 nhát, nhát 3 bạo kích thêm). */
    public static final double KIEM_1_COEF = 4.5, KIEM_2_COEF = 0.50, KIEM_ULT_COEF = 0.55, MA_1_COEF = 1.25;

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

    /** Tra kỹ năng nền theo id icon (kiem_1, ma_ult...) hoặc null. */
    public static Skill byId(String id) {
        if (id == null) return null;
        for (Skill s : values()) if (s.icon.equals(id)) return s;
        return null;
    }

    /** Đạo có bộ kỹ năng dùng cho phím (trang đang dùng theo phím V): Kiếm, Đạo, Thể, Ma. */
    public static Path skillPath(CultivationData d, int page) { return SkillPages.active(d, page); }
}
