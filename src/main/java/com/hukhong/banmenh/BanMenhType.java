package com.hukhong.banmenh;

import java.util.Locale;

/** 8 loại Bản Mệnh Kiếm (S2 §2.1). id = tên texture. weight = tỉ lệ roll gốc; rare = bị dịch theo tư chất. */
public enum BanMenhType {
    THANH_PHONG(20, false), LIET_DIEM(15, false), HAN_BANG(15, false), LOI_DINH(12, false),
    HUYET_ANH(10, true), TRONG_NHAC(10, true), NGOC_TAM(8, true), TINH_THAN(5, true);

    public final int weight;
    public final boolean rare;

    BanMenhType(int weight, boolean rare) { this.weight = weight; this.rare = rare; }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** custom_model_data (1..8) chọn model. */
    public int modelIndex() { return ordinal() + 1; }

    public static BanMenhType byId(String id) {
        for (BanMenhType t : values()) if (t.id().equals(id)) return t;
        return null;
    }
}
