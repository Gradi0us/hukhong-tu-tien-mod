package com.hukhong.boss.phase;

import com.hukhong.boss.phase.BossPhaseConfig.Move;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * Boss dùng khung pha: {@link BossPhaseDirector} điều khiển Đánh -> Chuyển pha -> Đánh.
 * Boss tự giữ pha (dữ liệu đồng bộ cho client chọn texture/xương), tự báo đang rảnh để director chen chiêu phase.
 * Cách gắn (xem HuVuong): tạo director trong constructor; gọi {@code director().tick(sl)} ở tick() phía server;
 * {@code isImmobile()} gồm {@code director().frozen()}; {@code hurt()} chặn khi {@code director().invulnerable()}.
 */
public interface PhasedBoss {
    /** Id cấu hình ({@link BossPhases}) và tiền tố khoá lang phụ đề. */
    String phaseId();

    BossPhaseDirector director();

    /** Pha hiện tại 1..3 (đồng bộ client). */
    int currentPhase();

    /** Đặt pha (chỉ director gọi, đúng thời điểm "lộ diện"). */
    void setCurrentPhase(int phase);

    /** Áp phần phía server của pha: màu thanh máu, tốc độ, hệ số sát thương... Gọi khi lộ diện và khi nạp lại từ save. */
    void applyPhase(ServerLevel sl, int phase);

    /** Boss không đang gồng/niệm kỹ năng riêng => director được chen chiêu phase. */
    boolean idleForMove();

    /** Huỷ kỹ năng đang gồng (khi bắt đầu chuyển pha). */
    void cancelSkill();

    /** Chơi hoạt ảnh gồng chiêu phase (boss tự chọn tên animation đang có). */
    void playMoveAnim(boolean ultimate);

    /** Sát thương một đòn phase = mult x chỉ số hiện tại của boss. */
    float moveDamage(double mult);

    default BossPhaseConfig.Config phaseConfig() { return BossPhases.get(phaseId()); }

    /** Chiêu phase khả dụng ở pha {@code phase} (cộng dồn): pha 1 = rỗng, pha 2 = +2, pha 3 = +ultimate. */
    default List<Move> movesForPhase(int phase) { return phaseConfig().movesUpTo(phase); }

    /** Hệ số rút ngắn hồi chiêu của kỹ năng cũ theo pha hiện tại (1, 0.85, 0.7). */
    default double cdMul() { return phaseConfig().phase(currentPhase()).cdMul(); }
}
