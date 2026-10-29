package com.hukhong.passive;

import com.hukhong.core.Aptitude;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HuyetKhiTest {
    @Test
    void scalesWithAptitude() {
        assertEquals(0.20, PassiveMath.hkHealPct(Aptitude.SSS), 1e-9);
        assertEquals(300, PassiveMath.hkCooldown(Aptitude.SSS));
        assertEquals(0.08, PassiveMath.hkHealPct(Aptitude.D), 1e-9);
        assertEquals(600, PassiveMath.hkCooldown(Aptitude.D));
        assertTrue(PassiveMath.hkHealPct(Aptitude.B) < PassiveMath.hkHealPct(Aptitude.S));
        assertTrue(PassiveMath.hkCooldown(Aptitude.B) > PassiveMath.hkCooldown(Aptitude.S));
    }
}
