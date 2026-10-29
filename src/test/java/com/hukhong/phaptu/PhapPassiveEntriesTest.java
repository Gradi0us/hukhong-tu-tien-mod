package com.hukhong.phaptu;

import com.hukhong.client.screen.profile.PassiveEntries;
import com.hukhong.core.Aptitude;
import com.hukhong.core.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhapPassiveEntriesTest {
    @Test
    void phapShowsGeneralAndElementPassive() {
        for (PhapElement el : PhapElement.values()) {
            var l = PassiveEntries.of(Path.PHAP, Aptitude.S, 40, el);
            assertEquals(2, l.size(), el.id());
            assertFalse(l.get(1)[1].getString().contains("%s"), el.id());
            assertFalse(l.get(1)[1].getString().isBlank());
        }
        var kim = PassiveEntries.of(Path.PHAP, Aptitude.SSS, 40, PhapElement.KIM).get(1)[1].getString();
        assertTrue(kim.contains("35%"), kim); // Kim Cuong SSS = +35%
    }

    @Test
    void everyPathHasPassives() {
        for (Path p : Path.values()) assertFalse(PassiveEntries.of(p, Aptitude.B, 30, PhapElement.QUANG).isEmpty(), p.name());
    }
}
