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

    /**
     * Boss tự thi triển chiêu phase bằng cơ chế riêng (thay vì {@link PhaseMoves} dùng chung). Trả true nếu đã nhận chiêu:
     * director không tạo Run, chỉ đặt hồi chiêu; boss phải báo {@link #idleForMove()} = false trong lúc đang thi triển.
     */
    default boolean startOwnMove(ServerLevel sl, Move m, net.minecraft.world.entity.LivingEntity target) { return false; }

    /** Camera cutscene giới thiệu riêng (null = mặc định của director). */
    default List<CamPath.Key> introCamera(int ticks) { return null; }

    /** Phụ đề cutscene giới thiệu riêng (null = tên boss + hukhong.boss.intro.&lt;id&gt;). */
    default List<BossPhaseConfig.SubLine> introLines(int ticks) { return null; }

    default BossPhaseConfig.Config phaseConfig() { return BossPhases.get(phaseId()); }

    /** Chiêu phase khả dụng ở pha {@code phase} (cộng dồn): pha 1 = rỗng, pha 2 = +2, pha 3 = +ultimate. */
    default List<Move> movesForPhase(int phase) { return phaseConfig().movesUpTo(phase); }

    /** Hệ số rút ngắn hồi chiêu của kỹ năng cũ theo pha hiện tại (1, 0.85, 0.7). */
    default double cdMul() { return phaseConfig().phase(currentPhase()).cdMul(); }

    // ------------------------------------------------------------------ Boss Combat Brain (boss/ai, PLAN-v1 §1)
    /** Hồ sơ chiến đấu; khác null => {@link com.hukhong.boss.ai.BossBrain} điều khiển di chuyển + đòn thường giữa các chiêu lớn. */
    default com.hukhong.boss.ai.BossProfile brainProfile() { return null; }

    /** Boss đang thi triển chiêu riêng (brain nhường quyền). Mặc định = !idleForMove(). */
    default boolean ownMoveActive() { return !idleForMove(); }

    /** Brain xin chen một chiêu lớn riêng của boss giữa các chuỗi đòn; trả true nếu đã bắt đầu chiêu. */
    default boolean brainBigMove(ServerLevel sl, net.minecraft.world.entity.LivingEntity target) { return false; }

    /**
     * Brain phát animation (strafe_l/r, backstep, sidestep_l/r, backflip, dash, leap, combo_&lt;id&gt;_&lt;n&gt;) trên controller
     * {@link com.hukhong.boss.ai.BrainAnims#CONTROLLER}; boss đăng ký controller bằng {@link com.hukhong.boss.ai.BrainAnims#controller}
     * (tên thiếu tự rơi về attack/walk). Boss không có controller đó thì lệnh bị bỏ qua.
     */
    default void playBrainAnim(String name) {
        if (this instanceof software.bernie.geckolib.animatable.GeoEntity g) g.triggerAnim(com.hukhong.boss.ai.BrainAnims.CONTROLLER, name);
    }

    /** Brain vừa bắt đầu vung nhát {@code hit} (0-based) của chuỗi {@code combo} — boss thêm hiệu ứng/đòn phụ riêng. */
    default void onBrainStrike(ServerLevel sl, String combo, int hit, net.minecraft.world.entity.LivingEntity target) {}

    /** Brain vừa bắt đầu né. */
    default void onBrainEvade(ServerLevel sl, com.hukhong.boss.ai.BossProfile.Evade kind, net.minecraft.world.entity.LivingEntity target) {}

    /**
     * Phản ứng (trừng phạt hồi máu / phản đòn) trỏ tới id không phải combo trong profile: boss tự xử lý (vd. bắt đầu chiêu riêng).
     * Trả true nếu đã xử lý.
     */
    default boolean brainReact(ServerLevel sl, net.minecraft.world.entity.LivingEntity target, String id) { return false; }
}
