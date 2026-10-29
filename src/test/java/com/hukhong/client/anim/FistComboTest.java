package com.hukhong.client.anim;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FistComboTest {
    @Test
    void cyclesInsideWindowAndResetsAfter() {
        UUID id = UUID.randomUUID();
        assertEquals(CastAnim.FIST_JAB, FistComboEvents.next(id, 1000));
        assertEquals(CastAnim.FIST_CROSS, FistComboEvents.next(id, 1008));
        assertEquals(CastAnim.FIST_HOOK, FistComboEvents.next(id, 1016));
        assertEquals(CastAnim.FIST_JAB, FistComboEvents.next(id, 1024));
        assertEquals(CastAnim.FIST_JAB, FistComboEvents.next(id, 1060)); // quá 20 tick -> quay về đòn đầu
    }
}
