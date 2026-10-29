package com.hukhong.linhbao;

import com.hukhong.banmenh.BanMenhService;

/**
 * Toán thuần của Linh Bảo (bản mệnh phi-kiếm của Đạo / Thể / Ma). Giai đoạn lấy từ ArtifactData (cùng nguồn Hồn Tinh,
 * cùng bảng chi phí + trần cảnh giới với Bản Mệnh Kiếm); hệ số phẩm = BanMenhService.QUALITY[giai đoạn].
 * tools/balance_sim.py đọc các hằng số này bằng regex: đổi tên/định dạng phải sửa cả bên đó.
 */
public final class LinhBaoMath {
    private LinhBaoMath() {}

    /**
     * HỆ SỐ THEO GIAI ĐOẠN (0..6 = Phàm..Thần), nhân thêm QUALITY[giai đoạn]. Không dùng 1 hệ số phẳng vì khoảng cách DPS so với Kiếm
     * không tỉ lệ với QUALITY (Kiếm có bước nhảy cấp 100: Thần x1.5 + QUALITY 3.0); bảng này hiệu chỉnh bằng tools/balance_sim.py --linhbao
     * để mỗi giai đoạn đạt ~80-95% DPS bền vững của Kiếm. Đổi số phải chạy lại sim.
     *
     * Đạo Linh: mỗi DAO_BOLT_INTERVAL_TICKS (trong combat) bắn 1 tia vào mục tiêu = Thần Thức x COEF[giai đoạn] x QUALITY. */
    public static final double[] DAO_BOLT_COEF = {0.25, 0.25, 0.35, 0.70, 1.05, 1.25, 2.70};
    public static final int DAO_BOLT_INTERVAL_TICKS = 50;
    /** Từ giai đoạn Địa (3): cứ 2 tia thì 1 tia chữa đồng đội thấp máu nhất trong HEAL_RANGE. */
    public static final int DAO_HEAL_MIN_STAGE = 3, DAO_HEAL_EVERY = 2;
    public static final double DAO_HEAL_SP_COEF = 0.10, DAO_HEAL_RANGE = 16, DAO_BOLT_RANGE = 24;

    /** Linh Thể: đòn tay cộng Công x COEF[giai đoạn] x QUALITY. */
    public static final double[] THE_HIT_COEF = {0.50, 0.50, 0.75, 0.85, 0.90, 0.95, 2.10};

    /** Ma Công Bá Thể Quyết: đòn thường cộng Công x COEF[giai đoạn] x QUALITY; hút máu thêm = LS_BASE + LS_PER_STAGE x giai đoạn (phần sát thương cộng thêm). */
    public static final double[] MA_HIT_COEF = {0.10, 0.10, 0.15, 0.28, 0.32, 0.35, 1.65};
    public static final double MA_LS_BASE = 0.04, MA_LS_PER_STAGE = 0.02;
    /** Trần hồi máu mỗi đòn: 4% máu tối đa (cùng DamageRules). */
    public static final double LS_HEAL_CAP_PCT = 0.04;

    public static int clamp(int stage) { return Math.max(0, Math.min(6, stage)); }

    public static double quality(int stage) { return BanMenhService.QUALITY[clamp(stage)]; }

    public static double daoBolt(double spellPower, int stage) { return spellPower * DAO_BOLT_COEF[clamp(stage)] * quality(stage); }

    public static double daoHeal(double spellPower, int stage) {
        return stage < DAO_HEAL_MIN_STAGE ? 0 : spellPower * DAO_HEAL_SP_COEF * (1 + 0.25 * (stage - DAO_HEAL_MIN_STAGE));
    }

    public static double theHit(double attackPower, int stage) { return attackPower * THE_HIT_COEF[clamp(stage)] * quality(stage); }

    public static double maHit(double attackPower, int stage) { return attackPower * MA_HIT_COEF[clamp(stage)] * quality(stage); }

    public static double maLifesteal(int stage) { return MA_LS_BASE + MA_LS_PER_STAGE * clamp(stage); }

    /** Số hạt rune quay quanh Đạo Linh theo giai đoạn. */
    public static int orbRunes(int stage) { return 1 + clamp(stage) / 2; }

    /**
     * Vị trí Đạo Linh so với chân người chơi (cùng công thức cho server-FX và client-render): vai phải, lơ lửng nhấp nhô.
     * yawDeg = yBodyRot; trả {dx, dy, dz}.
     */
    public static double[] orbOffset(double yawDeg, double ageTicks) {
        double yaw = Math.toRadians(yawDeg);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);      // hướng trước
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);     // bên phải
        double bob = Math.sin(ageTicks * 0.12) * 0.07;
        return new double[]{rx * 0.55 - fx * 0.12, 1.95 + bob, rz * 0.55 - fz * 0.12};
    }

    /** Màu theo giai đoạn (RGB): Đạo vàng-trắng, Thể cam-đồng sáng dần, Ma đỏ thẫm -> tím. */
    public static int daoColor(int stage) { return stage >= 4 ? 0xFFF4C8 : 0xFFD866; }

    public static int theColor(int stage) { return lerp(0xB86A28, 0xFFC060, clamp(stage) / 6.0); }

    public static int maColor(int stage) { return lerp(0x8A0F2A, 0x8B3FD0, clamp(stage) / 6.0); }

    private static int lerp(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
