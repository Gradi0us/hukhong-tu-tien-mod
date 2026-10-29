package com.hukhong.story.world;

/** Sáu phe của Mạch Truyện Thế Giới (PLAN-v2 §2.1). */
public enum Faction {
    DA_KHUYET, CHG, TONG_MON, THANH, YEU_TOC, TIEN_NHAN;

    public String nameKey() { return "hukhong.faction." + name().toLowerCase(java.util.Locale.ROOT); }

    public static Faction parse(String s) {
        if (s == null) return THANH;
        String n = s.trim().toUpperCase(java.util.Locale.ROOT);
        for (Faction f : values()) if (f.name().equals(n)) return f;
        throw new IllegalArgumentException("phe khong hop le: " + s);
    }
}
