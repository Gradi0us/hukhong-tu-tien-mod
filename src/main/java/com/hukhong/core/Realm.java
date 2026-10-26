package com.hukhong.core;

/** Bảng cảnh giới (canon §1). Chỉ số 0..12 khớp key lang hukhong.realm.N. */
public final class Realm {
    private Realm() {}

    public static int indexOf(int level) {
        if (level < 10) return 0;
        if (level < 90) return level / 10;          // 10-19 ->1 ... 80-89 ->8
        if (level <= 95) return 9;                  // Đấu Kình I..V
        if (level <= 98) return 10;                 // Luyện Thần Giả
        if (level == 99) return 11;                 // Bán Thần
        return 12;                                  // Thần
    }

    public static String langKey(int level) { return "hukhong.realm." + indexOf(level); }

    /** Đấu Kình giai đoạn 1..5 (cấp 90..95 -> I..V) hoặc 0 nếu không thuộc. */
    public static int subStage(int level) { return level >= 90 && level <= 95 ? level - 89 : 0; }
}
