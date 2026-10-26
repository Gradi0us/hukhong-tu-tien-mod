package com.hukhong.skill;

/** Trạng thái runtime phía server (không lưu): thế tu luyện, hồi chiêu, thiền, combat. */
public final class SkillState {
    public boolean stance = false;
    /** Trang kỹ năng đang dùng (chỉ số trong SkillPages.owned). */
    public int page = 0;
    public long lastSwitch = Long.MIN_VALUE / 2;
    public final long[] cooldownEnd = new long[4];
    public long lastCombat = Long.MIN_VALUE / 2;
    public int still = 0;
    public double lx, ly, lz;

    public boolean meditating() { return still >= 60; }
}
