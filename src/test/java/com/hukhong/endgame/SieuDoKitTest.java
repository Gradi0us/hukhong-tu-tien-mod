package com.hukhong.endgame;

import com.hukhong.endgame.SieuDoKit.Phase;
import com.hukhong.endgame.SieuDoKit.Status;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Dieu kien ending_truth (dat / khong dat) + chuoi pha Tam Chien -> giu Manh -> man ket + song Hu Khong. */
class SieuDoKitTest {
    @Test
    void availabilityTrueAndFalseCases() {
        assertTrue(new Status(10, 29, 4, 4, 8).met());
        assertEquals(SieuDoKit.M_DOCS, new Status(9, 10, 4, 4, 8).missing());
        assertEquals(SieuDoKit.M_HU_ANH, new Status(10, 30, 4, 4, 8).missing());
        assertEquals(SieuDoKit.M_CHG, new Status(10, 10, 3, 4, 8).missing());
        assertEquals(SieuDoKit.M_ZONES, new Status(10, 10, 4, 3, 8).missing());
        assertEquals(15, new Status(0, 99, 0, 0, 10).missing());
        assertTrue(new Status(10, 0, 4, 0, 0).met(), "khong co vung nao -> khong doi vung");
        assertEquals(0, SieuDoKit.zonesNeeded(0));
        assertEquals(1, SieuDoKit.zonesNeeded(1));
        assertEquals(4, SieuDoKit.zonesNeeded(9));
        assertEquals(7, SieuDoKit.zonesNeeded(14));
    }

    @Test
    void phaseSequence() {
        Phase p = Phase.INTRO;
        assertEquals(Phase.INTRO, SieuDoKit.next(p, SieuDoKit.INTRO_TICKS - 1, 0, 0, true));
        p = SieuDoKit.next(p, SieuDoKit.INTRO_TICKS, 0, 0, true);
        assertEquals(Phase.TAM_CHIEN, p);
        assertEquals(Phase.TAM_CHIEN, SieuDoKit.next(p, 50, 2, 0, true));
        assertEquals(Phase.TAM_CHIEN, SieuDoKit.next(p, 0, 0, 0, true), "tan anh chua kip sinh");
        p = SieuDoKit.next(p, 400, 0, 0, true);
        assertEquals(Phase.GIU_MANH, p);
        assertEquals(Phase.GIU_MANH, SieuDoKit.next(p, 100, 0, SieuDoKit.HOLD_TICKS - 1, true));
        p = SieuDoKit.next(p, 100, 0, SieuDoKit.HOLD_TICKS, true);
        assertEquals(Phase.OUTRO, p);
        assertEquals(Phase.OUTRO, SieuDoKit.next(p, SieuDoKit.OUTRO_TICKS - 1, 0, 0, true));
        assertEquals(Phase.DONE, SieuDoKit.next(p, SieuDoKit.OUTRO_TICKS, 0, 0, true));
        assertEquals(Phase.DONE, SieuDoKit.next(Phase.OUTRO, SieuDoKit.OUTRO_TICKS, 0, 0, false), "man ket khong that bai");
        for (Phase q : new Phase[]{Phase.INTRO, Phase.TAM_CHIEN, Phase.GIU_MANH}) assertEquals(Phase.FAILED, SieuDoKit.next(q, 5, 1, 0, false));
        assertEquals(Phase.FAILED, SieuDoKit.next(Phase.FAILED, 0, 0, 9999, true));
    }

    @Test
    void holdAndWaves() {
        int h = 0;
        for (int i = 0; i < 100; i++) h = SieuDoKit.holdAfter(h, true);
        assertEquals(100, h);
        assertEquals(100, SieuDoKit.holdAfter(h, false));
        assertEquals(SieuDoKit.HOLD_TICKS, SieuDoKit.holdAfter(SieuDoKit.HOLD_TICKS, true));
        assertEquals(60 * 20, SieuDoKit.HOLD_TICKS);
        double r = SieuDoKit.waveRadius(1);
        assertTrue(SieuDoKit.waveHits(r, r, true));
        assertTrue(SieuDoKit.waveHits(r + 1.5, r, true));
        assertFalse(SieuDoKit.waveHits(r, r, false), "nhay de ne");
        assertFalse(SieuDoKit.waveHits(r + 3, r, true));
        assertEquals(SieuDoKit.waveRadius(0), SieuDoKit.waveRadius(3));
        assertFalse(SieuDoKit.waveWarning(0));
        assertTrue(SieuDoKit.waveWarning(SieuDoKit.WAVE_PERIOD - 10));
        assertTrue(SieuDoKit.waveBurst(SieuDoKit.WAVE_PERIOD - 1));
        assertEquals(2, SieuDoKit.waveIndex(250));
        assertEquals(50, SieuDoKit.waveTick(250));
        assertTrue(SieuDoKit.waveDamage(20f) >= 4f);
    }

    @Test
    void shadesAndCutsceneScripts() {
        for (int i = 0; i < SieuDoKit.SHADES; i++) {
            assertEquals("hukhong.sieudo.shade." + (i + 1), SieuDoKit.shadeLineKey(i));
            if (i > 0) assertTrue(SieuDoKit.shadeLineTick(i) > SieuDoKit.shadeLineTick(i - 1));
        }
        for (int lv = 1; lv <= 100; lv++) assertTrue(SieuDoKit.shadeLevel(lv) >= 20 && SieuDoKit.shadeLevel(lv) <= 65);
        assertTrue(SieuDoKit.shadeLevel(98) < 98);
        for (var s : SieuDoKit.INTRO_SUBS) assertTrue(s.from() < s.to() && s.to() <= SieuDoKit.INTRO_TICKS);
        for (boolean soul : new boolean[]{false, true}) for (boolean lost : new boolean[]{false, true}) {
            var subs = SieuDoKit.outroSubs(soul, lost);
            assertEquals(lost ? 5 : 4, subs.size());
            assertTrue(subs.stream().anyMatch(s -> s.key().equals(SieuDoKit.modaoKey(soul))));
            for (var s : subs) assertTrue(s.from() < s.to() && s.to() <= SieuDoKit.OUTRO_TICKS, s.key());
        }
        for (var keys : List.of(SieuDoKit.INTRO_KEYS, SieuDoKit.OUTRO_KEYS))
            for (int i = 1; i < keys.size(); i++) assertTrue(keys.get(i).t() > keys.get(i - 1).t());
    }

    @Test
    void worldTierVariant() {
        assertEquals(EndgameKit.hpMul(2), EndgameKit.hpMul(2, EndgameKit.VARIANT_NONE));
        assertTrue(EndgameKit.hpMul(2, EndgameKit.VARIANT_LINH_KHI) < 1.0);
        assertTrue(EndgameKit.dmgMul(2, EndgameKit.VARIANT_LINH_KHI) < 1.0);
        assertEquals(1.3, EndgameKit.expMul(2, EndgameKit.VARIANT_LINH_KHI, true), 1e-9);
        assertEquals(1.0, EndgameKit.expMul(2, EndgameKit.VARIANT_LINH_KHI, false), 1e-9);
        assertEquals(1.0, EndgameKit.expMul(2, EndgameKit.VARIANT_NONE, true), 1e-9);
        assertTrue(EndgameKit.validEnding(EndgameKit.ENDING_TRUTH));
        assertFalse(EndgameKit.validEnding(4));
    }
}
