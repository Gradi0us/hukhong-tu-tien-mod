package com.hukhong.client.anim;

import com.hukhong.skill.BanMenhSkill;
import com.hukhong.skill.Skill;
import com.hukhong.skillbook.BookSkill;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CastAnimMapTest {
    @Test
    void everySkillHasAnimation() {
        for (Skill s : Skill.values()) assertNotNull(CastAnimMap.of(s), s.name());
        for (BookSkill s : BookSkill.values()) assertNotNull(CastAnimMap.of(s), s.name());
        for (BanMenhSkill s : BanMenhSkill.values()) assertNotNull(CastAnimMap.of(s), s.name());
        for (int i = 0; i < 4; i++) assertNotNull(CastAnimMap.phap(i));
        assertNotNull(CastAnimMap.RESONANCE);
    }

    @Test
    void everyAnimationBuildsAndIsShort() {
        for (CastAnim a : CastAnim.values()) {
            KeyframeAnimation k = assertDoesNotThrow(() -> CastAnimLibrary.get(a), a.name());
            assertNotNull(k);
            assertTrue(a.ticks >= 8 && a.ticks <= 28, a.name() + " length " + a.ticks); // 0,4-1,4 s
            assertEquals(a.ticks, k.endTick, a.name());
        }
    }

    @Test
    void ordinalRoundTrip() {
        for (CastAnim a : CastAnim.values()) assertSame(a, CastAnim.of(a.ordinal()));
        assertNull(CastAnim.of(-1));
        assertNull(CastAnim.of(CastAnim.values().length));
    }
}
