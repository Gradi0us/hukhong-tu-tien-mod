package com.hukhong.boss.phase;

import com.hukhong.boss.phase.BossPhaseConfig.Config;
import com.hukhong.boss.phase.BossPhaseConfig.Move;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossPhasesTest {
    private static final List<String> IDS = List.of("hu_vuong", "sa_hat_vuong", "bang_nhan", "doc_mau", "chg_1", "chg_2", "chg_3", "chg_4", "da_khuyet");

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
            assertEquals(2, p2.size(), id);
            for (Move m : p2) assertTrue(!m.ultimate(), id + ":" + m.id());
            List<Move> p3 = c.movesUpTo(3);
            assertEquals(3, p3.size(), id);
            Move ult = p3.get(2);
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
                    assertTrue(s.key().startsWith("hukhong.boss.sub." + id + "." + p + "."), s.key());
                }
            }
        }
    }

    @Test void arena_effects_accumulate() {
        Config c = BossPhases.get("hu_vuong");
        assertTrue(c.arenaFor(1).isEmpty());
        assertTrue(c.arenaFor(2).contains(BossPhaseConfig.ArenaEffect.PURPLE_SKY));
        assertTrue(c.arenaFor(3).contains(BossPhaseConfig.ArenaEffect.PURPLE_SKY) && c.arenaFor(3).contains(BossPhaseConfig.ArenaEffect.CRACKED_FLOOR));
    }
}
