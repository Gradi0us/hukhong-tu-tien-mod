package com.hukhong.endgame;

import com.hukhong.boss.ai.BossProfile;
import com.hukhong.boss.ai.BossProfile.Branch;
import com.hukhong.boss.ai.BossProfile.Combo;
import com.hukhong.boss.ai.BossProfile.Cond;
import com.hukhong.boss.ai.BossProfile.Hit;
import com.hukhong.boss.ai.BossProfile.PhaseVals;
import com.hukhong.boss.ai.GapClose;
import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.Set;

/**
 * Hồ sơ BossBrain của Dạ Khuyết (PLAN-v1 §2, kiểu Malenia): nhanh nhất game, đi vòng ngân kiếm, né nhiều, đòn trúng hút máu
 * ({@link DaKhuyet#brainStrike}). Chiêu lớn cũ (DaKhuyetMoves) được brain chen vào sau mỗi 2 chuỗi đòn thường.
 * <pre>
 * Tam Nguyệt Trảm   ngang - chéo - xoay người chém TRỄ (nặng); sau nhát 2: lùi > 4 ô -> Đâm Xuyên, né ngang > 45° -> Quét 270°
 * Đâm - Rút - Đâm   đâm nhanh, rút lùi 1 bước, đâm lại ngay
 * Kiếm Vũ Liên Hoàn 6 nhát cực nhanh (mỗi nhát hút máu)
 * Nhảy Chém        bật lên chém bổ xuống (nặng) -> đáp đất quét
 * Phản Nguyệt       phản đòn: bị đánh 3 lần / 2 giây -> lướt ngang + ánh đỏ -> gạt kiếm -> chém (không đỡ được)
 * Trừng phạt hồi máu: ăn/uống thuốc -> Đâm Xuyên (lướt tới nếu xa)
 * Áp sát            P1 lướt + đâm · P2 lướt / bay lượn rồi lao xuống · P3 lướt máu ra sau lưng
 * </pre>
 */
public final class DaKhuyetBrain {
    private DaKhuyetBrain() {}

    /** Nghỉ giữa đòn theo pha: 14-22 / 10-18 / 6-14; pha 3 dưới 30% máu: 4-8 ({@link #ENRAGED}). */
    static final PhaseVals P1 = new PhaseVals(3, 5, 0.22, 24f, 0.30, 30, 14, 22);
    static final PhaseVals P2 = new PhaseVals(4, 8, 0.24, 28f, 0.45, 24, 10, 18);
    static final PhaseVals P3 = new PhaseVals(2, 6, 0.26, 32f, 0.55, 18, 6, 14);
    public static final PhaseVals ENRAGED = new PhaseVals(2, 6, 0.28, 32f, 0.55, 16, 4, 8);

    // Hit(windup, active, recovery, reach, arc°, dmg, lunge, recoil, delayed, heavy, unblockable)
    public static final BossProfile PROFILE = BossProfile.builder()
            .phase(1, P1).phase(2, P2).phase(3, P3)
            .weight(0.8).accel(3, 3).moveSpeed(0.34)
            .gapClose(GapClose.then(GapClose.DASH, "dam_dash"), 9)
            .delayExtra(10)
            .combo(new Combo("tam_nguyet", List.of(
                    BossProfile.hit(8, 3, 5, 3.0, 120, 0.8, 0.8),
                    BossProfile.hit(6, 3, 6, 3.0, 120, 0.8, 0.8),
                    new Hit(8, 4, 12, 3.4, 200, 1.3, 1.0, 0.4, true, true, false)),
                    0, 4, 3, 1, List.of(
                            new Branch(1, Cond.RETREAT, 4, "dam_xuyen"),
                            new Branch(1, Cond.STRAFE, 45, "quet")), false))
            .combo(new Combo("dam_rut_dam", List.of(
                    new Hit(7, 3, 6, 3.2, 50, 0.9, 2.0, 1.0, false, false, false),
                    new Hit(3, 3, 10, 3.2, 50, 1.1, 2.4, 0.5, false, false, false)),
                    2, 6, 2, 1, List.of(), false))
            .combo(new Combo("kiem_vu", List.of(
                    BossProfile.hit(5, 2, 1, 2.8, 110, 0.35, 0.4),
                    BossProfile.hit(2, 2, 1, 2.8, 110, 0.35, 0.4),
                    BossProfile.hit(2, 2, 1, 2.8, 110, 0.35, 0.4),
                    BossProfile.hit(2, 2, 1, 2.8, 110, 0.35, 0.4),
                    BossProfile.hit(2, 2, 1, 2.8, 110, 0.35, 0.4),
                    new Hit(3, 3, 16, 3.0, 140, 0.5, 0.6, 0.3, false, false, false)),
                    0, 3.5, 2, 1, List.of(), false))
            .combo(new Combo("nhay_chem", List.of(
                    new Hit(12, 4, 6, 3.0, 160, 1.4, 3.0, 0, false, true, false),
                    new Hit(5, 4, 14, 3.4, 270, 1.0, 0.3, 0, false, false, false)),
                    3, 7, 1.5, 1, List.of(), false))
            // chỉ dùng làm nhánh / phản ứng / nối áp sát
            .combo(new Combo("dam_xuyen", List.of(
                    new Hit(6, 4, 10, 3.0, 40, 1.2, 4.5, 0, false, true, false)),
                    0, 7, 0, 1, List.of(), true))
            .combo(new Combo("quet", List.of(
                    new Hit(6, 4, 14, 3.6, 270, 1.1, 0.3, 0, false, true, false)),
                    0, 4, 0, 1, List.of(), true))
            .combo(new Combo("phan_nguyet", List.of(
                    new Hit(3, 3, 3, 3.0, 180, 0.6, 0.5, 0, false, false, false),
                    new Hit(4, 4, 12, 3.6, 220, 1.4, 1.8, 0, false, true, true)),
                    0, 5, 0, 1, List.of(), true))
            .combo(new Combo("dam_dash", List.of(
                    new Hit(3, 3, 10, 3.2, 60, 1.1, 1.2, 0, false, false, false)),
                    0, 5, 0, 1, List.of(), true))
            .punishHeal("dam_xuyen")
            .counter(3, 40, "phan_nguyet")
            .bigMoveEvery(2)
            .build();

    static final GapClose DASH_THRUST = PROFILE.gapClose();
    static final GapClose HOVER_DIVE = GapClose.then(GapClose.HOVER, "nhay_chem");
    static final GapClose BLOOD_BLINK = GapClose.then(GapClose.BLINK, "tam_nguyet");

    /** P1 lướt + đâm · P2 60% lướt / 40% bay lượn · P3 50% lướt máu / 25% lướt / 25% bay lượn. */
    static GapClose gapClose(int phase, RandomSource r) {
        float x = r.nextFloat();
        if (phase <= 1) return DASH_THRUST;
        if (phase == 2) return x < 0.6f ? DASH_THRUST : HOVER_DIVE;
        return x < 0.5f ? BLOOD_BLINK : x < 0.75f ? DASH_THRUST : HOVER_DIVE;
    }

    /** Animation brain có thật trong da_khuyet.animation.json (tools/gen_da_khuyet_brain_anims.py). */
    public static final Set<String> ANIMS = Set.of(
            "strafe_l", "strafe_r", "backstep", "sidestep_l", "sidestep_r", "backflip", "dash", "hover", "blink",
            "combo_tam_nguyet_1", "combo_tam_nguyet_2", "combo_tam_nguyet_3",
            "combo_dam_rut_dam_1", "combo_dam_rut_dam_2",
            "combo_kiem_vu_1", "combo_kiem_vu_2", "combo_kiem_vu_3", "combo_kiem_vu_4", "combo_kiem_vu_5", "combo_kiem_vu_6",
            "combo_nhay_chem_1", "combo_nhay_chem_2",
            "combo_dam_xuyen_1", "combo_quet_1", "combo_phan_nguyet_1", "combo_phan_nguyet_2", "combo_dam_dash_1");
}
