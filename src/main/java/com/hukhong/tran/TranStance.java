package com.hukhong.tran;

import com.hukhong.skill.Skill;

/** Thế Trận Tu. Mỗi thế có 3 kỹ năng Q/E/R; T luôn là Thế (Skill.TRAN_ULT). Thuần, dùng được cả client (HUD). */
public enum TranStance {
    BASE, KHONG_CHE, SAT_THUONG, PHONG_NGU;

    private static final Skill[][] KIT = {
            {Skill.TRAN_1, Skill.TRAN_2, Skill.TRAN_3},
            {Skill.TRAN_CAGE, Skill.TRAN_SLOW, Skill.TRAN_SUPPRESS},
            {Skill.TRAN_PILLAR, Skill.TRAN_BLADES, Skill.TRAN_DOOM},
            {Skill.TRAN_WARD, Skill.TRAN_HEAL, Skill.TRAN_SHIELD},
    };

    public String langKey() { return "hukhong.tran.stance." + name().toLowerCase(java.util.Locale.ROOT); }

    public static TranStance of(int ordinal) { return values()[Math.floorMod(ordinal, values().length)]; }

    /** Thế sẽ vào khi đã chọn thế rồi bấm Q (slot 0) / E (1) / R (2). */
    public static TranStance fromSlot(int slot) {
        return switch (slot) {
            case 0 -> KHONG_CHE;
            case 1 -> SAT_THUONG;
            case 2 -> PHONG_NGU;
            default -> BASE;
        };
    }

    /** Kỹ năng của phím slot trong thế s (T = TRAN_ULT ở mọi thế). */
    public static Skill skillOf(TranStance s, int slot) {
        if (s == null || slot < 0 || slot > 3) return null;
        if (slot == 3) return Skill.TRAN_ULT;
        return KIT[s.ordinal()][slot];
    }
}
