package com.hukhong.skillbook;

import com.hukhong.core.Path;
import com.hukhong.skill.Skill;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SkillBookMathTest {
    @Test
    void damageAndCooldownScaling() {
        assertEquals(1.0, SkillBookMath.dmgMult(1), 1e-9);
        assertEquals(1.08, SkillBookMath.dmgMult(2), 1e-9);
        assertEquals(1.72, SkillBookMath.dmgMult(10), 1e-9);
        assertEquals(1.72, SkillBookMath.dmgMult(99), 1e-9, "clamp ở cấp 10");
        assertEquals(1.0, SkillBookMath.dmgMult(-5), 1e-9);
        assertEquals(1.0, SkillBookMath.cdMult(1), 1e-9);
        assertEquals(0.97, SkillBookMath.cdMult(2), 1e-9);
        assertEquals(0.73, SkillBookMath.cdMult(10), 1e-9);
        for (int l = 1; l <= 12; l++) assertTrue(SkillBookMath.cdMult(l) >= 1 - SkillBookMath.CD_CAP - 1e-9, "trần -30%");
        assertEquals(292, SkillBookMath.cooldown(400, 10));
        assertEquals(400, SkillBookMath.cooldown(400, 1));
        assertEquals(1, SkillBookMath.cooldown(1, 10), "không về 0");
        for (int l = 1; l < 10; l++) {
            assertTrue(SkillBookMath.dmgMult(l + 1) > SkillBookMath.dmgMult(l));
            assertTrue(SkillBookMath.cdMult(l + 1) < SkillBookMath.cdMult(l));
        }
    }

    @Test
    void levelCapFollowsPlayerLevel() {
        assertEquals(1, SkillBookMath.levelCap(1));
        assertEquals(1, SkillBookMath.levelCap(9));
        assertEquals(2, SkillBookMath.levelCap(10));
        assertEquals(6, SkillBookMath.levelCap(50));
        assertEquals(10, SkillBookMath.levelCap(90));
        assertEquals(10, SkillBookMath.levelCap(100));
        for (int lv = 1; lv <= 100; lv++) {
            assertTrue(SkillBookMath.typicalLevel(lv) <= SkillBookMath.levelCap(lv));
            assertTrue(SkillBookMath.typicalLevel(lv) >= 1);
            if (lv > 1) assertTrue(SkillBookMath.typicalLevel(lv) >= SkillBookMath.typicalLevel(lv - 1));
        }
    }

    @Test
    void upgradeCostsMonotonicAndGradeScaled() {
        for (SkillBookMath.Mode m : SkillBookMath.Mode.values())
            for (SkillGrade g : SkillGrade.values())
                for (int l = 1; l < 9; l++) {
                    var a = SkillBookMath.cost(l, g, m);
                    var b = SkillBookMath.cost(l + 1, g, m);
                    assertTrue(b.honTinh() >= a.honTinh(), m + " " + g + " " + l);
                    assertTrue(b.contribution() >= a.contribution());
                    assertTrue(b.scrolls() >= a.scrolls());
                    assertTrue(a.honTinh() > 0);
                }
        for (SkillBookMath.Mode m : SkillBookMath.Mode.values())
            for (int l = 1; l < 10; l++)
                for (int g = 0; g < 3; g++)
                    assertTrue(SkillBookMath.cost(l, SkillGrade.of(g + 1), m).honTinh() >= SkillBookMath.cost(l, SkillGrade.of(g), m).honTinh());
        assertEquals(SkillBookMath.Cost.NONE, SkillBookMath.cost(10, SkillGrade.PHAM, SkillBookMath.Mode.CONTRIBUTION));
        var c = SkillBookMath.cost(1, SkillGrade.PHAM, SkillBookMath.Mode.CONTRIBUTION);
        assertEquals(4, c.honTinh());
        assertEquals(30, c.contribution());
        assertEquals(0, c.scrolls());
        var s = SkillBookMath.cost(1, SkillGrade.PHAM, SkillBookMath.Mode.SCROLL);
        assertEquals(1, s.scrolls());
        assertEquals(0, s.contribution());
        assertEquals(c.honTinh() * 3, SkillBookMath.cost(1, SkillGrade.PHAM, SkillBookMath.Mode.HON_TINH).honTinh());
        // tổng Hồn Tinh để Cường Hóa tối đa một kỹ năng Phàm nằm trong tầm 1-2 boss + vài Bí Cảnh
        assertTrue(SkillBookMath.totalHonTinh(SkillGrade.PHAM) < 150);
        assertTrue(SkillBookMath.totalHonTinh(SkillGrade.THIEN) > SkillBookMath.totalHonTinh(SkillGrade.PHAM) * 3);
    }

    @Test
    void catalogIsConsistent() {
        Set<String> ids = new HashSet<>();
        for (BookSkill b : BookSkill.values()) {
            assertTrue(ids.add(b.id), "id trùng " + b.id);
            assertNull(Skill.byId(b.id), "id trùng kỹ năng nền " + b.id);
            assertEquals(b, BookSkill.byId(b.id));
            assertTrue(b.cost > 0 && b.cooldownTicks > 0);
        }
        for (Path p : Path.values()) {
            if (p == Path.PHAP) continue; // Pháp Tu dùng kỹ năng theo nguyên tố, chưa có bí tịch riêng
            assertEquals(4, BookSkill.ofPath(p).size(), "mỗi đạo 4 kỹ năng " + p);
            for (SkillGrade g : SkillGrade.values())
                assertEquals(1, BookSkill.ofPath(p).stream().filter(b -> b.grade == g).count(), p + " " + g);
        }
        assertTrue(BookSkill.universal().size() >= 3);
        assertNotNull(BookSkill.byId("lang_ba_vi_bo"));
        assertNull(BookSkill.byId("khong_co"));
        assertNull(BookSkill.byId(null));
        // phẩm cao đắt hơn / hồi chiêu dài hơn trong cùng đạo
        for (Path p : Path.values()) {
            BookSkill prev = null;
            for (BookSkill b : BookSkill.ofPath(p)) {
                if (prev != null) assertTrue(b.cooldownTicks >= prev.cooldownTicks, p + " " + b);
                prev = b;
            }
        }
        assertEquals(19, BookSkill.values().length);
    }

    @Test
    void dataAssignmentAndResolve() {
        SkillBookData d = SkillBookData.EMPTY;
        // mặc định: kỹ năng nền
        assertEquals(Skill.KIEM_1, SkillBook.resolve(d, Path.KIEM, 0).base());
        assertEquals(Skill.KIEM_ULT, SkillBook.resolve(d, Path.KIEM, 3).base());
        assertNull(SkillBook.resolve(d, Path.KIEM, 2), "R trống = Bản Mệnh");
        assertNull(SkillBook.resolve(d, null, 0));
        // chưa học thì không gán được
        assertFalse(SkillBook.assignable(d, Path.KIEM, "pha_khong_tram"));
        assertFalse(SkillBook.assignable(d, Path.KIEM, "ma_1"), "kỹ năng nền đạo khác");
        assertTrue(SkillBook.assignable(d, Path.KIEM, "kiem_ult"));
        d = d.withLevel("pha_khong_tram", 1).withLevel("thanh_tam_chu", 1).withLevel("lang_ba_vi_bo", 1);
        assertTrue(d.learned("pha_khong_tram"));
        assertFalse(d.learned("kiem_1"), "kỹ năng nền không tính là học");
        assertTrue(SkillBook.assignable(d, Path.KIEM, "pha_khong_tram"));
        assertFalse(SkillBook.assignable(d, Path.MA, "pha_khong_tram"), "sai đạo");
        assertFalse(SkillBook.assignable(d, Path.KIEM, "thanh_tam_chu"), "kỹ năng Đạo trên trang Kiếm");
        assertTrue(SkillBook.assignable(d, Path.MA, "lang_ba_vi_bo"), "kỹ năng chung dùng mọi đạo");
        d = d.withSlot(Path.KIEM, 1, "pha_khong_tram");
        assertEquals("pha_khong_tram", SkillBook.resolve(d, Path.KIEM, 1).id());
        assertEquals(BookSkill.PHA_KHONG_TRAM, SkillBook.resolve(d, Path.KIEM, 1).book());
        // gán sang phím khác thì phím cũ trống, không nhân đôi
        d = d.withSlot(Path.KIEM, 0, "pha_khong_tram");
        assertNull(d.slot(Path.KIEM, 1));
        assertEquals(Skill.KIEM_2, SkillBook.resolve(d, Path.KIEM, 1).base(), "phím cũ trở về mặc định");
        // chuyển kỹ năng nền sang phím khác: phím mặc định cũ không chạy lại
        d = d.withSlot(Path.KIEM, 0, "kiem_ult");
        assertEquals(Skill.KIEM_ULT, SkillBook.resolve(d, Path.KIEM, 0).base());
        assertNull(SkillBook.resolve(d, Path.KIEM, 3), "kiem_ult đã chuyển đi");
        // bỏ gán
        d = d.withSlot(Path.KIEM, 0, null);
        assertEquals(Skill.KIEM_1, SkillBook.resolve(d, Path.KIEM, 0).base());
        assertEquals(Skill.KIEM_ULT, SkillBook.resolve(d, Path.KIEM, 3).base());
        // trang độc lập
        d = d.withSlot(Path.MA, 0, "lang_ba_vi_bo");
        assertEquals(Skill.KIEM_1, SkillBook.resolve(d, Path.KIEM, 0).base());
        assertEquals("lang_ba_vi_bo", SkillBook.resolve(d, Path.MA, 0).id());
        // R: gán thủ công chạy khi chưa mở Bản Mệnh, nhường Bản Mệnh khi đã mở (cấp 40)
        d = d.withSlot(Path.MA, 2, "lang_ba_vi_bo");
        assertEquals("lang_ba_vi_bo", SkillBook.resolve(d, Path.MA, 2, 39).id());
        assertNull(SkillBook.resolve(d, Path.MA, 2, 40));
        // cấp
        assertEquals(1, d.level("kiem_1"));
        d = d.withLevel("kiem_1", 5);
        assertEquals(5, d.level("kiem_1"));
        assertEquals(10, d.withLevel("kiem_1", 50).level("kiem_1"));
    }

    @Test
    void rewardGradesFollowWeights() {
        double[] w = {0, 0, 50, 50};
        assertEquals(SkillGrade.DIA, SkillBookRewards.gradeAt(w, 0.1));
        assertEquals(SkillGrade.THIEN, SkillBookRewards.gradeAt(w, 0.9));
        assertEquals(SkillGrade.THIEN, SkillBookRewards.gradeAt(w, 1.0));
        assertEquals(SkillGrade.PHAM, SkillBookRewards.gradeAt(new double[]{1, 0, 0, 0}, 0.99));
        // Bí Cảnh: cấp cao hơn cho phẩm tốt hơn và nhiều hơn
        for (int t = 0; t < 4; t++) {
            assertTrue(SkillBookRewards.biCanhChance(t + 1) > SkillBookRewards.biCanhChance(t));
            assertTrue(SkillBookRewards.biCanhGrade(t + 1, 0.99).ordinal() >= SkillBookRewards.biCanhGrade(t, 0.99).ordinal());
        }
        assertEquals(SkillGrade.PHAM, SkillBookRewards.biCanhGrade(0, 0.5));
        assertEquals(SkillGrade.THIEN, SkillBookRewards.biCanhGrade(4, 0.99));
        // không bao giờ rơi phẩm Thiên ở Hư Vương / Bí Cảnh Bạch
        for (double x = 0; x < 1; x += 0.01) {
            assertNotEquals(SkillGrade.THIEN, SkillBookRewards.huVuongGrade(x));
            assertNotEquals(SkillGrade.THIEN, SkillBookRewards.biCanhGrade(0, x));
        }
        assertEquals(1.0, SkillBookRewards.questChance(0, true));
        assertTrue(SkillBookRewards.questChance(2, false) < 0.2);
        // pick ưu tiên đạo của người nhận
        Random r = new Random(5);
        int mine = 0;
        for (int i = 0; i < 400; i++) {
            BookSkill s = SkillBookRewards.pick(r, SkillGrade.DIA, Set.of(Path.THE));
            if (s.path == Path.THE || s.path == null) mine++;
        }
        assertTrue(mine > 300, "ưu tiên ~75% + ngẫu nhiên: " + mine);
        for (int i = 0; i < 50; i++) assertEquals(SkillGrade.THIEN, SkillBookRewards.pick(r, SkillGrade.THIEN, Set.of()).grade);
    }

    @Test
    void sampledScrollSourcesAreBounded() {
        // Bí Cảnh Hồng: cứ ~3 lượt có 1 bí tịch, phẩm Địa/Thiên chiếm đa số
        Random r = new Random(11);
        int n = 0, high = 0;
        for (int i = 0; i < 3000; i++) {
            if (r.nextDouble() < SkillBookRewards.biCanhChance(4)) {
                n++;
                if (SkillBookRewards.biCanhGrade(4, r.nextDouble()).ordinal() >= 2) high++;
            }
        }
        assertTrue(n > 800 && n < 1200, "n=" + n);
        assertTrue(high > n * 0.75);
    }
}
