package com.hukhong.passive.client;

import com.hukhong.passive.PassiveSyncPayload;

/** Bản sao client của số tầng nội tại. */
public final class ClientPassiveState {
    private ClientPassiveState() {}

    private static volatile int kiem, dao, the, ma;

    public static void update(PassiveSyncPayload p) { kiem = p.kiem(); dao = p.dao(); the = p.the(); ma = p.ma(); }

    public static int kiem() { return kiem; }
    public static int dao() { return dao; }
    public static int the() { return the; }
    public static int ma() { return ma; }
}
