package com.hukhong.boss.phase;

import com.hukhong.boss.phase.BossPhaseConfig.Config;
import com.hukhong.boss.phase.BossPhaseConfig.Move;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossPhasesTest {
    private static final List<String> IDS = List.of("hu_vuong", "sa_hat_vuong", "bang_nhan", "doc_mau", "chg_1", "chg_2", "chg_3", "chg_4", "da_khuyet", "su_do");

    @Test void all_nine_bosses_configured() {
        for (String id : IDS) assertTrue(BossPhases.has(id), id);
    }

    @Test void three_phases_with_thresholds() {
        for (String id : IDS) {
            Config c = BossPhases.get(id);
            assertEquals(3, c.count(), id);
            assertEquals(1, c.phaseFor(1f), id);
            assertEquals(1, c.phaseFor(0.67f), id);
            assertEquals(2, c.phaseFor(0.66f), id);
            assertEquals(2, c.phaseFor(0.34f), id);
            assertEquals(3, c.phaseFor(0.33f), id);
            assertEquals(3, c.phaseFor(0f), id);
        }
    }

    @Test void moves_two_then_one_ultimate() {
        for (String id : IDS) {
            Config c = BossPhases.get(id);
            assertEquals(0, c.movesUpTo(1).size(), id);
            List<Move> p2 = c.movesUpTo(2);
            assertEquals(4, p2.size(), id);
            for (Move m : p2) assertTrue(!m.ultimate(), id + ":" + m.id());
            List<Move> p3 = c.movesUpTo(3);
            assertEquals(8, p3.size(), id);
            Move ult = p3.get(4);
            assertTrue(ult.ultimate(), id);
            assertTrue(ult.windup() >= 80 && ult.radius() >= 15, id + " ultimate phai co vong canh bao lon + gong lau");
        }
    }

    @Test void scale_transition_and_cutscene_ranges() {
        for (String id : IDS) {
            Config c = BossPhases.get(id);
            assertEquals(1.0f, c.phase(1).scale(), 1e-6);
            assertEquals(1.25f, c.phase(2).scale(), 1e-6);
            assertEquals(1.5f, c.phase(3).scale(), 1e-6);
            for (int p = 2; p <= 3; p++) {
                var ph = c.phase(p);
                assertTrue(ph.transition() >= 60 && ph.transition() <= 120, id + " chuyen pha 3-6s");
                assertTrue(ph.reveal() > 0 && ph.reveal() < ph.transition());
                assertTrue(!ph.cam().isEmpty() && !ph.subs().isEmpty(), id);
                int last = -1;
                for (CamPath.Key k : ph.cam()) { assertTrue(k.t() > last, id + " keyframe phai tang dan"); last = k.t(); }
                assertEquals(ph.transition(), ph.cam().get(ph.cam().size() - 1).t(), id);
                for (var s : ph.subs()) {
                    assertTrue(s.from() < s.to() && s.to() <= ph.transition(), id + " phu de trong khung cutscene");
                    // Da Khuyet + CHG I..IV (da lam lai) dung khoa thoai rieng hukhong.dialog.<boss>.p<pha>.*
                    String own = java.util.Map.of("da_khuyet", "dakhuyet", "doc_mau", "docmau", "hu_vuong", "huvuong",
                            "bang_nhan", "bangnhan", "sa_hat_vuong", "sahat", "su_do", "sudo").get(id);
                    String pre = own != null ? "hukhong.dialog." + own + ".p" + p + "."
                            : id.matches("chg_[1234]") ? "hukhong.dialog.chg" + id.charAt(4) + ".p" + p + "." : "hukhong.boss.sub." + id + "." + p + ".";
                    assertTrue(s.key().startsWith(pre), s.key());
                }
            }
        }
    }

    @Test void hu_vuong_has_own_moves_not_generic() {
        List<Move> all = BossPhases.get("hu_vuong").movesUpTo(3);
        for (Move m : all) assertTrue(m.id().startsWith("hvg_"), m.id());
        assertEquals(8, all.stream().map(Move::id).distinct().count());
    }

    @Test void da_khuyet_has_own_moves_not_generic() {
        for (Move m : BossPhases.get("da_khuyet").movesUpTo(3)) assertTrue(m.id().startsWith("dk_"), m.id());
    }

    @Test void chg_1_has_own_moves_not_generic() {
        List<Move> all = BossPhases.get("chg_1").movesUpTo(3);
        for (Move m : all) assertTrue(m.id().startsWith("chg1_"), m.id());
        assertEquals(8, all.stream().map(Move::id).distinct().count());
    }

    @Test void chg2_has_own_maliketh_moves_not_generic() {
        List<Move> all = BossPhases.get("chg_2").movesUpTo(3);
        for (Move m : all) assertTrue(m.id().startsWith("chg2_"), m.id());
        assertEquals(8, all.stream().map(Move::id).distinct().count());
    }

    @Test void chg3_has_own_moves_not_generic() {
        for (Move m : BossPhases.get("chg_3").movesUpTo(3)) assertTrue(m.id().startsWith("c3_"), m.id());
    }

    @Test void chg4_has_own_moves_not_generic() {
        List<Move> all = BossPhases.get("chg_4").movesUpTo(3);
        for (Move m : all) assertTrue(m.id().startsWith("c4_"), m.id());
        assertEquals("c4_nihil", all.get(4).id());
    }

    @Test void doc_mau_has_own_moves_not_generic() {
        List<Move> all = BossPhases.get("doc_mau").movesUpTo(3);
        for (Move m : all) assertTrue(m.id().startsWith("dm_"), m.id());
        assertEquals(8, all.stream().map(Move::id).distinct().count());
        assertEquals("dm_van_doc_khai", all.get(4).id());
    }

    @Test void su_do_has_own_margit_moves_not_generic() {
        List<Move> all = BossPhases.get("su_do").movesUpTo(3);
        for (Move m : all) assertTrue(m.id().startsWith("sd_"), m.id());
        assertEquals(8, all.stream().map(Move::id).distinct().count());
    }

    @Test void bang_nhan_has_own_moves_not_generic() {
        List<Move> all = BossPhases.get("bang_nhan").movesUpTo(3);
        for (Move m : all) assertTrue(m.id().startsWith("bn_"), m.id());
        assertEquals(8, all.stream().map(Move::id).distinct().count());
        assertEquals("bn_bang_nguyet", all.get(4).id());
    }

    @Test void sa_hat_has_own_moves_not_generic() {
        List<Move> all = BossPhases.get("sa_hat_vuong").movesUpTo(3);
        for (Move m : all) assertTrue(m.id().startsWith("sh_"), m.id());
        assertEquals(8, all.stream().map(Move::id).distinct().count());
        assertEquals("sh_tinh_vu", all.get(4).id());
    }

    @Test void arena_effects_accumulate() {
        Config c = BossPhases.get("hu_vuong");
        assertTrue(c.arenaFor(1).isEmpty());
        assertTrue(c.arenaFor(2).contains(BossPhaseConfig.ArenaEffect.PURPLE_SKY));
        assertTrue(c.arenaFor(3).contains(BossPhaseConfig.ArenaEffect.PURPLE_SKY) && c.arenaFor(3).contains(BossPhaseConfig.ArenaEffect.CRACKED_FLOOR));
    }
    @Test void hv_forms_a_have_own_moves_not_generic() {
        java.util.Map<String, String> pre = java.util.Map.of("hvf_huyet_ma", "hvhm_", "hvf_cot_long", "hvcl_", "hvf_moc_yeu", "hvmy_", "hvf_bang_phach", "hvbp_");
        java.util.Map<String, String> ult = java.util.Map.of("hvf_huyet_ma", "hvhm_thanh_huyet", "hvf_cot_long", "hvcl_thoi_khong",
                "hvf_moc_yeu", "hvmy_dai_thu", "hvf_bang_phach", "hvbp_bang_bao");
        for (var e : pre.entrySet()) {
            Config c = BossPhases.get(e.getKey());
            List<Move> all = c.movesUpTo(3);
            assertEquals(8, all.size(), e.getKey());
            for (Move m : all) assertTrue(m.id().startsWith(e.getValue()), m.id());
            assertEquals(8, all.stream().map(Move::id).distinct().count(), e.getKey());
            assertEquals(ult.get(e.getKey()), all.get(4).id());
            assertTrue(all.get(4).ultimate() && all.get(4).windup() >= 80 && all.get(4).radius() >= 15, e.getKey());
            for (int p = 2; p <= 3; p++)
                for (var sub : c.phase(p).subs()) assertTrue(sub.key().startsWith("hukhong.dialog." + e.getKey() + ".p" + p + "."), sub.key());
        }
    }
}
