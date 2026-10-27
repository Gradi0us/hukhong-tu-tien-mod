package com.hukhong.city;

import com.hukhong.structures.StructurePlacer;

/**
 * Da bo khu cho rieng (the loai "trade" khong con duoc dat canh thanh): cho nam ngay trong thanh, xem {@link CityMarket}.
 * Giu lai de lenh thu /hukhong structure place trade ... va CityProtection van chay: dat thuong nhan quanh tam cau truc.
 */
public final class TradeDistrict {
    private TradeDistrict() {}

    public static final String TAG = CityMarket.TAG;

    /** Dung cho lenh thu: sinh bo thuong nhan Thanh cap Dia quanh tam cau truc vua dat. */
    public static void populate(StructurePlacer.Placed p) {
        CityMarket.populate(p, CityDef.ALL.get(6));
    }
}
