package com.hukhong.tran;

import com.hukhong.skill.Skill;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TranMathTest {
    @Test
    void rTongBangTamNhipCongHeSo() {
        assertEquals(8, TranMath.R_HITS);
        assertEquals(0.55 * 8, TranMath.rTotalCoef(), 1e-9);
    }

    @Test
    void luongDapLongTheoSoQuai() {
        assertEquals(6, TranMath.cageHitLimit(0));
        assertEquals(10, TranMath.cageHitLimit(2));
        assertEquals(16, TranMath.cageHitLimit(99)); // trần 5 quái
    }

    @Test
    void phimTheoTheoTungTheVaTKhongDoi() {
        assertSame(Skill.TRAN_1, TranStance.skillOf(TranStance.BASE, 0));
        assertSame(Skill.TRAN_CAGE, TranStance.skillOf(TranStance.KHONG_CHE, 0));
        assertSame(Skill.TRAN_DOOM, TranStance.skillOf(TranStance.SAT_THUONG, 2));
        assertSame(Skill.TRAN_SHIELD, TranStance.skillOf(TranStance.PHONG_NGU, 2));
        for (TranStance s : TranStance.values()) assertSame(Skill.TRAN_ULT, TranStance.skillOf(s, 3));
    }

    @Test
    void qerChonTheDungThuTu() {
        assertSame(TranStance.KHONG_CHE, TranStance.fromSlot(0));
        assertSame(TranStance.SAT_THUONG, TranStance.fromSlot(1));
        assertSame(TranStance.PHONG_NGU, TranStance.fromSlot(2));
    }
}
