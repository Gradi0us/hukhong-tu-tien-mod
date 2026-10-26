package com.hukhong.client.screen.profile;

import com.hukhong.cores.CoreSlots;

/** Bản sao client của Lõi đã chọn. */
public final class ClientCores {
    private ClientCores() {}

    private static volatile CoreSlots slots = CoreSlots.EMPTY;

    public static void set(CoreSlots s) { slots = s; }

    public static CoreSlots get() { return slots; }
}
