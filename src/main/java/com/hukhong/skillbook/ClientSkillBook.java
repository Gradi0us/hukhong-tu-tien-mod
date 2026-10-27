package com.hukhong.skillbook;

/** Bản sao client của {@link SkillBookData} (không chứa lớp client nên an toàn nạp ở server). */
public final class ClientSkillBook {
    private ClientSkillBook() {}

    private static volatile SkillBookData data = SkillBookData.EMPTY;

    public static void set(SkillBookData d) { data = d; }

    public static SkillBookData get() { return data; }
}
