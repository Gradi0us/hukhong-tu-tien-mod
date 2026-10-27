package com.hukhong.skillbook;

/** Phẩm bí tịch / công pháp: Phàm, Huyền, Địa, Thiên. Phẩm càng cao càng đắt để Cường Hóa. */
public enum SkillGrade {
    PHAM("Phàm", 1, 1.0, 0xFFCCCCCC),
    HUYEN("Huyền", 20, 1.5, 0xFF55FF55),
    DIA("Địa", 45, 2.25, 0xFF55AAFF),
    THIEN("Thiên", 70, 3.5, 0xFFFFAA33);

    /** Cấp tu luyện tối thiểu để học. */
    public final int minLevel;
    /** Hệ số chi phí Cường Hóa. */
    public final double costMult;
    public final int argb;
    public final String viName;

    SkillGrade(String viName, int minLevel, double costMult, int argb) {
        this.viName = viName;
        this.minLevel = minLevel;
        this.costMult = costMult;
        this.argb = argb;
    }

    public String langKey() { return "hukhong.skillbook.grade." + name().toLowerCase(java.util.Locale.ROOT); }

    public static SkillGrade of(int ordinal) {
        SkillGrade[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, ordinal))];
    }
}
