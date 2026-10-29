package com.hukhong.story.flow;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Di bo toan bo bang buoc cho 3 xuat than: den duoc ENDING, chuong khong lui, buoc TALK/DUEL deu co nguon hoan thanh (thoai) va moi buoc co chu chi dan. */
class FlowStepsWalkTest {
    private static String res(String path) throws IOException {
        try (InputStream in = FlowStepsWalkTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "thieu resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyGroupReachesEndingWithMonotonicChapters() {
        for (String group : new String[]{"city", "sect", "village"}) {
            FlowSteps.Step s = FlowSteps.firstOf(0, group);
            assertNotNull(s, group);
            int ch = 0, count = 0;
            Set<String> seen = new HashSet<>();
            FlowSteps.Step last = null;
            while (s != null) {
                assertTrue(seen.add(s.id()), "lap buoc " + s.id());
                assertTrue(s.chapter() >= ch, "chuong lui o " + s.id());
                ch = s.chapter();
                last = s;
                s = FlowSteps.next(s, group);
                assertTrue(++count < 200);
            }
            assertEquals("c10_end", last.id(), group);
            assertEquals(FlowSteps.Kind.ENDING, last.kind());
        }
    }

    @Test
    void talkAndDuelStepsHaveDialogueSource() throws IOException {
        for (FlowSteps.Step s : FlowSteps.ALL) {
            if (s.kind() == FlowSteps.Kind.TALK && !s.arg().equals("su_phu")) {
                String json = res("/data/hukhong/hukhong/dialogue/" + s.arg() + ".json");
                assertTrue(json.contains("\"step\": \"" + s.id() + "\""), "thoai " + s.arg() + " khong co step_done cho " + s.id());
            } else if (s.kind() == FlowSteps.Kind.TALK) {
                assertTrue(res("/data/hukhong/hukhong/dialogue/su_phu.json").contains("\"" + s.id() + "\""));
            } else if (s.kind() == FlowSteps.Kind.DUEL) {
                String json = res("/data/hukhong/hukhong/dialogue/" + s.arg() + ".json");
                assertTrue(json.contains("\"" + s.id() + "\"") && json.contains("\"duel\""), "thoai duel thieu " + s.id());
            }
        }
    }

    @Test
    void everyStepHasObjectiveText() throws IOException {
        String vi = res("/assets/hukhong/lang/vi_vn.json");
        Path q3 = Path.of("docs/research/lang-q3.txt");
        String extra = Files.exists(q3) ? Files.readString(q3, StandardCharsets.UTF_8) : "";
        for (FlowSteps.Step s : FlowSteps.ALL) {
            assertTrue(vi.contains("\"" + s.objKey() + "\"") || extra.contains(s.objKey() + " |"), "thieu text muc tieu " + s.objKey());
            if (s.kind() == FlowSteps.Kind.KILL) assertTrue(s.n() > 0);
        }
    }

    @Test
    void noStepIsADeadEnd() {
        for (FlowSteps.Step s : FlowSteps.ALL) {
            if (s.kind() != FlowSteps.Kind.ENDING) assertNotNull(FlowSteps.next(s, s.group().equals("all") ? "city" : s.group()), s.id());
        }
    }
}
