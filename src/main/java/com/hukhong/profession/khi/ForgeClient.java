package com.hukhong.profession.khi;

/** Ban sao trang thai phien Doan Tao Dai phia client (cap nhat boi ForgeNet.State). */
public final class ForgeClient {
    private ForgeClient() {}

    private static volatile ForgeNet.State state = ForgeNet.State.IDLE;

    public static void set(ForgeNet.State s) { state = s; }

    public static ForgeNet.State get() { return state; }
}
