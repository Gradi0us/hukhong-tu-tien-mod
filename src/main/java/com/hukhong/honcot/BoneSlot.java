package com.hukhong.honcot;

/** 6 slot Hồn Cốt (S3 §5): Đầu · Thân · Tay Trái · Tay Phải · Chân Trái · Chân Phải. */
public enum BoneSlot {
    HEAD, TORSO, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG;

    public static final int COUNT = 6;

    public String langKey() { return "hukhong.honcot.slot." + name().toLowerCase(java.util.Locale.ROOT); }

    public static BoneSlot of(int i) { return i >= 0 && i < COUNT ? values()[i] : null; }
}
