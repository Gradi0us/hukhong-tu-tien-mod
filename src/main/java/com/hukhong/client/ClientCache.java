package com.hukhong.client;

import com.hukhong.age.AgeData;
import com.hukhong.core.CultivationData;

/** Bản sao phía client của dữ liệu server (chỉ đọc cho HUD/UI). */
public final class ClientCache {
    private ClientCache() {}

    private static volatile CultivationData cultivation = CultivationData.DEFAULT;
    private static volatile AgeData age = AgeData.DEFAULT;

    public static void update(CultivationData c, AgeData a) { cultivation = c; age = a; }

    public static CultivationData cultivation() { return cultivation; }

    public static AgeData age() { return age; }
}
