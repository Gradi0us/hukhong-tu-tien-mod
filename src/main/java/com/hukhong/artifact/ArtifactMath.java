package com.hukhong.artifact;

import com.hukhong.banmenh.BanMenhMath;
import com.hukhong.core.Path;

/** Toán thuần pháp khí các đạo (Đạo/Thể/Ma). Chi phí + trần cảnh giới dùng lại bảng của Bản Mệnh Kiếm (BanMenhMath). */
public final class ArtifactMath {
    private ArtifactMath() {}

    public static final int MAX_STAGE = BanMenhMath.MAX_STAGE;

    /** Đạo có pháp khí (Kiếm dùng Bản Mệnh Kiếm). */
    public static boolean hasArtifact(Path p) { return p == Path.DAO || p == Path.THE || p == Path.MA; }

    public static int tinhCost(int targetStage) { return BanMenhMath.tinhCost(targetStage); }

    public static int check(int cur, int level, int tinh) { return BanMenhMath.check(cur, level, tinh); }

    private static int c(int stage) { return Math.max(0, Math.min(MAX_STAGE, stage)); }

    /** Số đôi cánh Pháp Bảo theo giai đoạn 0..6. */
    private static final int[] WING_PAIRS = {0, 1, 1, 2, 2, 3, 4};

    public static int wingPairs(int stage) { return WING_PAIRS[c(stage)]; }

    // Chỉ số cộng thêm mỗi giai đoạn (cố ý nhỏ: tối đa ~ +12% ở Thần phẩm).
    /** Đạo: hồi tài nguyên +4%/giai đoạn (nhân gốc). */
    public static double daoRegen(int stage) { return 0.04 * c(stage); }
    /** Đạo: tài nguyên tối đa +3%/giai đoạn. */
    public static double daoMaxResource(int stage) { return 0.03 * c(stage); }
    /** Đạo: Thần Thức +2%/giai đoạn. */
    public static double daoSpell(int stage) { return 0.02 * c(stage); }
    /** Thể: +2 máu mỗi giai đoạn (tối đa +12 = 6 tim). */
    public static double theHealth(int stage) { return 2.0 * c(stage); }
    /** Thể: Phòng +3%/giai đoạn. */
    public static double theDefense(int stage) { return 0.03 * c(stage); }
    /** Ma: Công +2%/giai đoạn. */
    public static double maAttack(int stage) { return 0.02 * c(stage); }
    /** Ma: hút huyết +0.4%/giai đoạn (phân số). */
    public static double maLifesteal(int stage) { return 0.004 * c(stage); }
}
