package com.hukhong.skill;

/** Trạng thái runtime phía server (không lưu): thế tu luyện, hồi chiêu, thiền, combat. */
public final class SkillState {
    public boolean stance = false;
    /** Trang kỹ năng đang dùng (chỉ số trong SkillPages.owned). */
    public int page = 0;
    public long lastSwitch = Long.MIN_VALUE / 2;
    public final long[] cooldownEnd = new long[4];
    /** Tổng CD của lần cast R gần nhất (cho HUD). */
    public int rTotal = 900;
    public long lastCombat = Long.MIN_VALUE / 2;
    public int still = 0;
    public double lx, ly, lz;
    /** Kiếm Vực (kênh): đang bật, ô phím đang giữ, số tick đã chạy. Chỉ ở server (đồng bộ HUD qua SkillSyncPayload.domain). */
    public boolean domainOn = false;
    public int domainSlot = 3, domainTick = 0;
    /** Phần lẻ hồi tài nguyên Kiếm chưa cộng; thời điểm lần hồi do đòn đánh thường gần nhất. */
    public double regenFrac = 0;
    public long lastHitMana = Long.MIN_VALUE / 2;

    public boolean meditating() { return still >= 60; }
}
