package com.hukhong.phaptu.linhtruong;

import com.hukhong.banmenh.BanMenhMath;
import net.minecraft.world.phys.Vec3;

/**
 * Toán thuần của Linh Trượng (Bản Mệnh Trượng của Pháp Tu). Cùng STAGE_LEVELS và QUALITY với Bản Mệnh Kiếm.
 *
 * Cân bằng (tools/balance_sim.py): đòn thường Kiếm = (5 + Công + Công x Q) x 1.6/s x 0.55 thời gian vung; Pháp Tu có Phép lực
 * khác Công, nên hệ số COEF theo giai đoạn được chọn để DPS tia ~90% (85-95%) Kiếm tại mọi cấp (xem LinhTruongMathTest).
 */
public final class LinhTruongMath {
    private LinhTruongMath() {}

    /** Giống BanMenhService.QUALITY (không đổi Kiếm: sao chép giá trị, test khớp hai bảng). */
    public static final float[] QUALITY = {1.0f, 1.15f, 1.35f, 1.6f, 1.9f, 2.3f, 3.0f};
    /** Hệ số Phép lực của tia thường theo giai đoạn (đã nhân QUALITY ở dưới). Giảm dần vì Phép lực tăng nhanh hơn Công. */
    public static final double[] COEF = {1.15, 1.055, 0.985, 0.925, 0.872, 0.822, 0.765};
    /** Tầm tia (block) và hồi chiêu (tick) = 0,7 giây. */
    public static final double RANGE = 12.0;
    public static final int COOLDOWN_TICKS = 14;
    /** Bề rộng trúng của tia (bán kính, block). */
    public static final double BEAM_WIDTH = 0.45;

    public static final int MAX_STAGE = BanMenhMath.MAX_STAGE;

    public static int clampStage(int s) { return Math.max(0, Math.min(MAX_STAGE, s)); }

    public static double coef(int stage) { return COEF[clampStage(stage)]; }

    public static float quality(int stage) { return QUALITY[clampStage(stage)]; }

    /** Sát thương một tia thường = Phép lực x COEF[giai đoạn] x QUALITY[giai đoạn]. */
    public static float boltDamage(double spellPower, int stage) {
        return (float) (spellPower * coef(stage) * quality(stage));
    }

    /** Số hạt/vòng quay quanh đầu trượng theo giai đoạn. */
    public static int motes(int stage) { return 2 + clampStage(stage) / 2; }

    /**
     * Vị trí đầu trượng (viên ngọc) tính từ mắt + hướng nhìn: lệch phải 0,38, xuống 0,12, ra trước 0,85.
     * Dùng chung server (kỹ năng) và client (hạt chờ) để hai bên trùng nhau; model cầm tay xấp xỉ vị trí này.
     */
    public static Vec3 tip(Vec3 eye, float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
        Vec3 look = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 up = right.cross(look).normalize();
        return eye.add(right.scale(0.38)).add(up.scale(-0.12)).add(look.scale(0.85));
    }
}
