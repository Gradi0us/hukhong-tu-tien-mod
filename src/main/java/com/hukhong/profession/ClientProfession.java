package com.hukhong.profession;

/** Bản sao nghề phụ phía client (cập nhật bởi ProfessionNet.Sync). */
public final class ClientProfession {
    private ClientProfession() {}

    private static volatile ProfessionData data = ProfessionData.DEFAULT;

    public static void set(ProfessionData d) { data = d; }

    public static ProfessionData get() { return data; }
}
