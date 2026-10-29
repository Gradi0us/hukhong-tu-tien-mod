package com.hukhong.client.anim;

/**
 * Tư thế thi triển kỹ năng. Thuần dữ liệu (không chạm lớp client) để server/payload dùng chung;
 * keyframe dựng ở {@link CastAnimLibrary}. {@code ticks} = độ dài tư thế (0,4-1,4 giây).
 */
public enum CastAnim {
    HAND_SEAL(26),      // chắp ấn trước ngực rồi đẩy ra
    SWORD_THRUST(16),   // đâm thẳng
    SWORD_OVERHEAD(22), // bổ từ trên xuống
    SWORD_SPIN(26),     // xoay người quét ngang
    PUNCH_HEAVY(18),    // đấm nặng
    STOMP(24),          // nhấc chân dậm đất
    UPPERCUT(18),       // móc ngược lên
    CLAW_SWIPE(18),     // vồ hai tay
    ROAR(28),           // dang tay gầm
    CHANNEL(24),        // giữ thế dẫn khí
    ULT_RAISE(24),      // giơ hai tay quá đầu
    DASH_LUNGE(12),     // ngả người lao tới
    GUARD(16),          // khoanh tay phòng thủ
    OPEN_PALMS(24),     // mở lòng bàn tay (hồi phục)
    FIST_JAB(10),       // Thể Tu tay không: đấm thẳng tay phải
    FIST_CROSS(10),     // đấm thẳng tay trái xoay hông
    FIST_HOOK(12);      // móc ngang

    public final int ticks;

    CastAnim(int ticks) { this.ticks = ticks; }

    public static CastAnim of(int ordinal) {
        CastAnim[] v = values();
        return ordinal < 0 || ordinal >= v.length ? null : v[ordinal];
    }
}
