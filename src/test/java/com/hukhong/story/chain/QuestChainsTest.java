package com.hukhong.story.chain;

import com.hukhong.story.flow.WaveCountdown;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestChainsTest {
    @Test
    void stageAndOfferRules() {
        List<String> none = List.of();
        assertEquals(0, QuestChains.stageOf(none, "merchant"));
        assertEquals(3, QuestChains.stageOf(List.of("qc.merchant.3", "qc.dying.1"), "merchant"));
        var m = QuestChains.OFFERS.get(0);
        assertTrue(QuestChains.offerApplies(m, none, 2, 10));
        assertFalse(QuestChains.offerApplies(m, none, 1, 10));
        assertFalse(QuestChains.offerApplies(m, List.of("qc.merchant.done"), 5, 50));
        assertFalse(QuestChains.offerApplies(m, List.of("qc.merchant.2"), 5, 50));
        var elder = QuestChains.OFFERS.get(3);
        assertFalse(QuestChains.offerApplies(elder, none, 5, 50));
        assertTrue(QuestChains.offerApplies(elder, List.of("qc.noi_gian.1"), 5, 40));
        assertFalse(QuestChains.offerApplies(elder, List.of("qc.noi_gian.1"), 4, 40));
    }

    @Test
    void compassPoints() {
        assertEquals(0, QuestChains.compass(0, -10));
        assertEquals(2, QuestChains.compass(10, 0));
        assertEquals(4, QuestChains.compass(0, 10));
        assertEquals(6, QuestChains.compass(-10, 0));
    }

    @Test
    void countdownTiming() {
        assertEquals(30, WaveCountdown.secondsLeft(0, 600));
        assertEquals(1, WaveCountdown.secondsLeft(581, 600));
        assertEquals(0, WaveCountdown.secondsLeft(600, 600));
        assertTrue(WaveCountdown.tickSound(5));
        assertTrue(WaveCountdown.tickSound(1));
        assertFalse(WaveCountdown.tickSound(6));
        assertFalse(WaveCountdown.tickSound(0));
    }

    private static String res(String p) throws IOException {
        try (InputStream in = QuestChainsTest.class.getResourceAsStream(p)) {
            assertNotNull(in, p);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyOfferNpcHasDialogueAndBranchesAreReferenced() throws IOException {
        for (var o : QuestChains.OFFERS) {
            String j = res("/data/hukhong/hukhong/dialogue/" + o.npc() + ".json");
            assertTrue(j.contains("\"npc\":\"" + o.npc() + "\""), o.npc());
        }
        String mo = res("/data/hukhong/hukhong/dialogue/mo_dao.json");
        for (String f : List.of("qc.merchant.c.truth", "qc.disciple.c.saved", "qc.disciple.c.abandon", "qc.noi_gian.c.expose",
                "qc.noi_gian.c.blackmail", "qc.dying.c.keep", "qc.dying.c.deliver"))
            assertTrue(mo.contains(f), f);
    }
}
