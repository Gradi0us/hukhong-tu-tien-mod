package com.hukhong.client.hud;

import com.hukhong.network.MaSyncPayload;

/** Bản sao client của trạng thái Ma Tu (Ác Tính...). Countdown đếm lùi cục bộ giữa 2 lần sync. */
public final class ClientMaState {
    private ClientMaState() {}

    private static volatile float acTinh;
    private static volatile int countdown = -1;
    private static volatile boolean lost;
    private static volatile int huyetTe;
    private static volatile boolean absorb = true;

    public static void update(MaSyncPayload p) {
        acTinh = p.acTinh(); countdown = p.countdown(); lost = p.lost(); huyetTe = p.huyetTe(); absorb = p.absorb();
    }

    public static void tick() { if (countdown > 0) countdown--; }

    public static float acTinh() { return acTinh; }
    public static int countdown() { return countdown; }
    public static boolean lost() { return lost; }
    public static int huyetTe() { return huyetTe; }
    public static boolean absorb() { return absorb; }

    /** Cùng ngưỡng với server (MaRules.tierOf), tầng 3 khi đang đếm ngược. */
    public static int tier() { return countdown >= 0 ? 3 : com.hukhong.pathrules.MaRules.tierOf(acTinh); }
}
