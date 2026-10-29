package com.hukhong.client;

import com.hukhong.client.screen.profile.SkillEntries;
import com.hukhong.core.Aptitude;
import com.hukhong.core.Path;
import com.hukhong.phaptu.PhapElement;
import com.hukhong.phaptu.ThiemBoMath;
import com.hukhong.phaptu.beam.PhapBeamMath;
import com.hukhong.skill.BanMenhSkill;
import com.hukhong.skill.Skill;
import com.hukhong.skillbook.SkillBookData;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Tab Kỹ Năng của màn K: số hiển thị phải khớp hằng số của code gây sát thương. */
class SkillEntriesTest {
    private static final double EPS = 1e-9;

    private static SkillEntries.Ctx ctx(int level, Set<Path> paths, Aptitude apt, SkillBookData book, PhapElement el, double ap, double sp) {
        Map<Path, Aptitude> a = new EnumMap<>(Path.class);
        for (Path p : paths) a.put(p, apt);
        return new SkillEntries.Ctx(level, paths, a, book, el, ap, sp, 200, 1000, 1, new int[Path.values().length], 0);
    }

    private static String text(SkillEntries.Entry e) {
        return e.lines().stream().map(l -> l.text().getString()).collect(Collectors.joining("\n"));
    }

    private static SkillEntries.Entry key(SkillEntries.Page pg, String k) {
        return pg.keys().stream().filter(e -> e.key().equals(k)).findFirst().orElseThrow(() -> new AssertionError("missing key " + k));
    }

    @Test
    void baseHitMatchesEffectConstants() {
        assertEquals(Skill.KIEM_1_COEF * 100, SkillEntries.baseHit(Skill.KIEM_1, 100, 0, 1), EPS);   // 50
        assertEquals(Skill.KIEM_2_COEF * 100 * 1.4, SkillEntries.baseHit(Skill.KIEM_2, 100, 0, 6), EPS); // Cuong Hoa lv6 = x1.4
        assertEquals(Skill.THE_1_COEF * 50, SkillEntries.baseHit(Skill.THE_1, 50, 0, 1), EPS);       // 200
        assertEquals(Skill.MA_1_COEF * 80, SkillEntries.baseHit(Skill.MA_1, 80, 0, 1), EPS);         // 100
        assertEquals(0.95 * 1.12 * 100, SkillEntries.baseHit(Skill.DAO_1, 0, 100, 1), EPS);          // 106.4 (Dao Tu x1.12)
        assertEquals(0, SkillEntries.baseHit(Skill.THE_2, 100, 100, 1), EPS);
    }

    @Test
    void phapQuangLevel7AptitudeA() {
        var x = ctx(7, Set.of(Path.PHAP), Aptitude.A, SkillBookData.EMPTY, PhapElement.QUANG, 0, 20);
        var pages = SkillEntries.build(x);
        assertEquals(1, pages.size());
        var pg = pages.get(0);
        assertEquals(4, pg.keys().size());
        // Q: 0.52 x 20 = 10.4
        assertTrue(text(key(pg, "Q")).contains("10.4"), text(key(pg, "Q")));
        // R: 2.51 x 20 = 50.2
        assertTrue(text(key(pg, "R")).contains("50.2"), text(key(pg, "R")));
        // E = Thiem Bo moi: giu E chay / bam nhanh lui + Ba The, khong sat thuong, so lay tu ThiemBoMath
        String e = text(key(pg, "E"));
        assertTrue(e.contains("120%"), e);
        assertTrue(e.contains(String.valueOf((int) (ThiemBoMath.IMMUNE_TICKS / 20))), e);
        assertFalse(e.contains("Sát thương/mục tiêu"), e);
        // T = Tinh Ha Quang Tru: 20 nhip, moi nhip sp x coef x 0.105 x aptMult(A)
        double pulse = PhapBeamMath.pulseDamage(PhapElement.QUANG, Aptitude.A, 20);
        assertEquals(20 * 7.3 * PhapBeamMath.PULSE_RATIO * 1.0, pulse, 1e-9); // A: t = 3/6 -> aptMult 1.0
        String t = text(key(pg, "T"));
        assertTrue(t.contains(SkillEntries.num(pulse)), t);
        assertTrue(t.contains(SkillEntries.num(pulse * 20)), t);
        // Linh Truong (don thuong) o cuoi danh sach co mat
        assertEquals(1, pg.basic().size());
    }

    @Test
    void beamPulseScalesWithSkillLevel() {
        double base = PhapBeamMath.pulseDamage(PhapElement.LOI, Aptitude.SSS, 100); // sp x coefT x PULSE_RATIO x aptMult(SSS)=1.3
        assertEquals(100 * 8.1 * PhapBeamMath.PULSE_RATIO * 1.3, base, 1e-9);
        assertEquals(base * 1.72, SkillEntries.beamPulse(PhapElement.LOI, Aptitude.SSS, 100, 10), 1e-9);
    }

    @Test
    void delayedPulsesOutsideEnhanceWindowAreNotMultiplied() {
        // Loi Tran: 3 dot (4, 10, 16 tick) -> dot cuoi ngoai cua so 12 tick, khong nhan Cuong Hoa lv6 (x1.4)
        double got = SkillEntries.phapPulses(1.26, 100, 3, 4, 6, 6);
        assertEquals(1.26 / 3 * 100 * (1.4 + 1.4 + 1.0), got, 1e-9);
        assertEquals(1.26 * 100, SkillEntries.phapPulses(1.26, 100, 3, 4, 6, 1), 1e-9);
    }

    @Test
    void bookSkillAssignedToKeyUsesCoefAndLevel() {
        SkillBookData bd = SkillBookData.EMPTY.withLevel("pha_khong_tram", 3).withSlot(Path.KIEM, 0, "pha_khong_tram");
        var x = ctx(50, Set.of(Path.KIEM), Aptitude.B, bd, null, 100, 0);
        var pg = SkillEntries.build(x).get(0);
        // 0.90 x 1.16 x 100 = 104.4 (>=100 lam tron)
        assertTrue(text(key(pg, "Q")).contains("104"), text(key(pg, "Q")));
        // Ban Menh R o cap >= 40: Nhat Kiem coef(50) x Cong
        double c = BanMenhSkill.NHAT_KIEM.coef(50);
        assertTrue(text(key(pg, "R")).contains(SkillEntries.num(c * 100)), text(key(pg, "R")));
        // bi tich da hoc nhung chua gan phim thi vao danh sach rieng
        SkillBookData bd2 = bd.withLevel("phi_kiem_truy_hon", 1);
        var loose = SkillEntries.unassigned(ctx(50, Set.of(Path.KIEM), Aptitude.B, bd2, null, 100, 0));
        assertEquals(1, loose.size());
        assertTrue(text(loose.get(0)).contains("320"), text(loose.get(0))); // 0.80 x 100 x 4 thanh
    }

    @Test
    void everyOwnedPathListsItsKeys() {
        var x = ctx(60, Set.of(Path.KIEM, Path.DAO, Path.THE, Path.MA, Path.PHAP), Aptitude.S, SkillBookData.EMPTY, PhapElement.KIM, 80, 60);
        List<SkillEntries.Page> pages = SkillEntries.build(x);
        assertEquals(5, pages.size());
        for (var pg : pages) {
            assertFalse(pg.keys().isEmpty(), pg.path().name());
            for (var e : pg.keys()) assertFalse(text(e).contains("%s"), pg.path() + " " + e.key());
        }
    }
}
