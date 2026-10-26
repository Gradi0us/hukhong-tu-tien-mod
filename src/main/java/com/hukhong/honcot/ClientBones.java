package com.hukhong.honcot;

/** Bản sao client của 6 ô Hồn Cốt. */
public final class ClientBones {
    private ClientBones() {}

    private static volatile BoneSlots slots = BoneSlots.EMPTY;

    public static void set(BoneSlots s) { slots = s; }

    public static BoneSlots get() { return slots; }
}
