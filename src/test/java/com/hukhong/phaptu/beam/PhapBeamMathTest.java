package com.hukhong.phaptu.beam;

import com.hukhong.core.Aptitude;
import com.hukhong.core.Path;
import com.hukhong.phaptu.PhapElement;
import com.hukhong.phaptu.PhapMath;
import com.hukhong.skill.Skill;
import com.hukhong.skillbook.SkillBook;
import com.hukhong.skillbook.SkillBookData;
import com.hukhong.stats.StatService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PhapBeamMathTest {
    static final PhapElement[] BEAM = {PhapElement.QUANG, PhapElement.LOI, PhapElement.HOA, PhapElement.THUY};

    @Test
    void onlyFourElementsLockT() {
        for (PhapElement e : PhapElement.values())
            assertEquals(e == PhapElement.QUANG || e == PhapElement.LOI || e == PhapElement.HOA || e == PhapElement.THUY, PhapBeamMath.locksT(e), e.name());
        assertFalse(PhapBeamMath.locksT(null));
    }

    @Test
    void aptitudeMultIsLinear070To130() {
        assertEquals(0.70, PhapBeamMath.aptMult(Aptitude.D), 1e-9);
        assertEquals(1.30, PhapBeamMath.aptMult(Aptitude.SSS), 1e-9);
        double step = (1.30 - 0.70) / 6;
        Aptitude[] v = Aptitude.values();
        for (int i = 1; i < v.length; i++) assertEquals(step, PhapBeamMath.aptMult(v[i]) - PhapBeamMath.aptMult(v[i - 1]), 1e-9);
        assertEquals(PhapMath.lerp(0.7, 1.3, Aptitude.B), PhapBeamMath.aptMult(Aptitude.B), 1e-12);
    }

    @Test
    void costAndCooldownMeetSpec() {
        // nhịp: 4 tick, 4s => 20 nhịp; gần hết thanh Pháp Lực
        assertEquals(20, PhapBeamMath.pulses(PhapBeamMath.FIRE_MAX_TICKS));
        int max = 1000;
        assertEquals(400, PhapBeamMath.upfrontCost(max, 1.0));
        assertEquals(7.0, PhapBeamMath.drainPerTick(max, 1.0), 1e-9);
        double total = PhapBeamMath.totalCost(max, 1.0, PhapBeamMath.FIRE_MAX_TICKS) / max;
        assertTrue(total >= 0.9 && total <= 1.0, "toan bo kenh ton " + total);
        assertTrue(PhapBeamMath.COOLDOWN_TICKS >= Skill.PHAP_ULT.cooldownTicks);
        assertTrue(PhapBeamMath.COOLDOWN_TICKS / 20 >= 60 && PhapBeamMath.COOLDOWN_TICKS / 20 <= 75);
        assertEquals(40.0, PhapBeamMath.RANGE);
        assertEquals(1.6, PhapBeamMath.RADIUS);
    }

    @Test
    void fullChannelAtSssIsAbout27To3TimesOldUlt() {
        for (PhapElement e : BEAM) {
            double ratio = PhapBeamMath.fullChannelDamage(e, Aptitude.SSS, 100) / PhapBeamMath.oldUltDamage(e, 100);
            assertTrue(ratio >= 2.5 && ratio <= 3.0, e + " ratio " + ratio);
        }
    }

    @Test
    void pulseScalesWithSpellPowerAndAptitude() {
        double a = PhapBeamMath.pulseDamage(PhapElement.HOA, Aptitude.B, 50);
        assertEquals(a * 2, PhapBeamMath.pulseDamage(PhapElement.HOA, Aptitude.B, 100), 1e-9);
        assertTrue(PhapBeamMath.pulseDamage(PhapElement.HOA, Aptitude.SSS, 50) > a);
        assertEquals(PhapBeamMath.pulseDamage(PhapElement.LOI, Aptitude.D, 80) / PhapBeamMath.pulseDamage(PhapElement.LOI, Aptitude.SSS, 80), 0.70 / 1.30, 1e-9);
    }

    @Test
    void lockedTIgnoresAssignmentButOtherElementsKeepIt() {
        SkillBookData d = SkillBookData.EMPTY.withSlot(Path.PHAP, 3, "phap_1");
        assertEquals(Skill.PHAP_1, SkillBook.resolve(d, Path.PHAP, 3, 30, false).base(), "Tho/Kim: gan van co hieu luc");
        assertEquals(Skill.PHAP_ULT, SkillBook.resolve(d, Path.PHAP, 3, 30, true).base(), "Quang/Loi/Hoa/Thuy: bo qua gan");
        // phap_ult gan sang phim khac khong duoc dung de co T cu
        SkillBookData d2 = SkillBookData.EMPTY.withSlot(Path.PHAP, 0, "phap_ult");
        assertEquals(Skill.PHAP_1, SkillBook.resolve(d2, Path.PHAP, 0, 30, true).base());
        // phim T bi gan phap_1 khong lam mat Q
        assertEquals(Skill.PHAP_1, SkillBook.resolve(d, Path.PHAP, 0, 30, true).base());
        // trang khac khong bi anh huong
        assertEquals(Skill.KIEM_ULT, SkillBook.resolve(SkillBookData.EMPTY, Path.KIEM, 3, 30, true).base());
    }

    /** Ghi bảng số liệu (D/B/S/SSS x L30/L60) ra build/phap-beam-table.txt cho báo cáo. */
    @Test
    void writeNumbersTable() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("elem apt lvl spell | oldT | pulse | full4s | x old\n");
        for (PhapElement e : BEAM) {
            for (int lv : new int[]{30, 60}) {
                for (Aptitude a : new Aptitude[]{Aptitude.D, Aptitude.B, Aptitude.S, Aptitude.SSS}) {
                    double sp = StatService.compute(lv, EnumSet.of(Path.PHAP), Map.of(Path.PHAP, a)).spell();
                    double old = PhapBeamMath.oldUltDamage(e, sp), pulse = PhapBeamMath.pulseDamage(e, a, sp), full = PhapBeamMath.fullChannelDamage(e, a, sp);
                    sb.append(String.format(Locale.ROOT, "%s %s L%d sp=%.1f | old=%.1f | pulse=%.1f | full=%.1f | x%.2f%n", e, a, lv, sp, old, pulse, full, full / old));
                }
            }
        }
        Files.createDirectories(Paths.get("build"));
        Files.writeString(Paths.get("build/phap-beam-table.txt"), sb.toString());
        assertTrue(sb.length() > 100);
    }
}
