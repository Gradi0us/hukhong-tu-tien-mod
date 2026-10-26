package com.hukhong.city;

import java.util.List;

/** 10 thành cố định (S4 §1.2). Neo (ax, az) tương đối spawn. Index 0 = Lưu Ly Cảng (thành gốc, CitySavedData). */
public record CityDef(int index, String id, Tier tier, int ax, int az) {
    public static final List<CityDef> ALL = List.of(
            new CityDef(0, "luu_ly_cang", Tier.NHAN, 1000, -600),
            new CityDef(1, "hac_thach_quan", Tier.NHAN, 600, 3600),
            new CityDef(2, "co_tien_thanh", Tier.NHAN, -3400, 1000),
            new CityDef(3, "huyet_nguyet_phuong", Tier.NHAN, -2000, -2600),
            new CityDef(4, "dan_ha_thanh", Tier.NHAN, 3800, 2000),
            new CityDef(5, "co_xao_thanh", Tier.NHAN, -1800, 3200),
            new CityDef(6, "lac_nhat_thanh", Tier.DIA, -5600, -600),
            new CityDef(7, "thanh_van_thanh", Tier.DIA, 3000, 5400),
            new CityDef(8, "bac_han_thanh", Tier.DIA, 1000, -6600),
            new CityDef(9, "thien_khai_hoang_do", Tier.THIEN, 6600, -5000));

    public String nameKey() { return "hukhong.city.n." + id; }

    public static CityDef byId(String id) {
        for (CityDef d : ALL) if (d.id.equals(id)) return d;
        return null;
    }
}
