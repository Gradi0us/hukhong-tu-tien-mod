package com.hukhong.profession;

import com.mojang.serialization.Codec;

/** Nghề phụ. Mỗi người chơi chọn tối đa 1 nghề, một lần (xem ProfessionService.choose). */
public enum Profession {
    NONE("none", "Chưa có nghề"),
    LUYEN_DAN("luyen_dan", "Luyện Đan Sư"),
    PHU_VAN("phu_van", "Phù Văn Sư"),
    DOAN_TAO("doan_tao", "Đoán Tạo Sư");

    public final String id, vi;

    Profession(String id, String vi) { this.id = id; this.vi = vi; }

    public static final Codec<Profession> CODEC = Codec.STRING.xmap(Profession::byId, p -> p.id);

    public static Profession byId(String id) {
        for (Profession p : values()) if (p.id.equals(id)) return p;
        return NONE;
    }

    public static Profession byOrdinal(int i) { return i >= 0 && i < values().length ? values()[i] : NONE; }

    public String nameKey() { return "hukhong.profession.name." + id; }
    public String descKey() { return "hukhong.profession.desc." + id; }
    public String needKey() { return "hukhong.profession.need." + id; }
}
