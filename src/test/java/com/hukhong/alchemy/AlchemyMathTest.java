package com.hukhong.alchemy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlchemyMathTest {
    @Test
    void qualityThresholds() {
        assertNull(AlchemyMath.qualityFor(0.1));
        assertEquals(AlchemyMath.Quality.HA, AlchemyMath.qualityFor(0.25));
        assertEquals(AlchemyMath.Quality.TRUNG, AlchemyMath.qualityFor(0.5));
        assertEquals(AlchemyMath.Quality.THUONG, AlchemyMath.qualityFor(0.8));
        assertEquals(AlchemyMath.Quality.CUC, AlchemyMath.qualityFor(1.0));
    }

    @Test
    void bandNarrowsWithTierAndWidensWithDiHoa() {
        assertTrue(AlchemyMath.bandHalf(6, false) < AlchemyMath.bandHalf(3, false));
        assertEquals(AlchemyMath.bandHalf(4, false) + 4, AlchemyMath.bandHalf(4, true));
        assertTrue(AlchemyMath.inBand(55, 5));
        assertFalse(AlchemyMath.inBand(70, 5));
    }

    @Test
    void heatDecaysAndClamps() {
        assertEquals(39, AlchemyMath.passive(40, 8, false));
        assertEquals(40, AlchemyMath.passive(40, 9, false));
        assertEquals(40, AlchemyMath.passive(40, 8, true)); // Di Hoa nguoi cham hon
        assertEquals(0, AlchemyMath.passive(0, 8, false));
        assertEquals(100, AlchemyMath.clampHeat(130));
    }

    @Test
    void toxinMathMatchesSpec() {
        // Pha Canh Dan cuu pham Trung = 18 (S7 §1.3)
        assertEquals(18.0, AlchemyMath.toxinAdd(2.0, 9, AlchemyMath.Quality.TRUNG), 1e-9);
        assertEquals(9.0, AlchemyMath.toxinAdd(2.0, 9, AlchemyMath.Quality.CUC), 1e-9);
        assertEquals(1.0, AlchemyMath.effectMult(0));
        assertEquals(0.75, AlchemyMath.effectMult(45));
        assertEquals(0.5, AlchemyMath.effectMult(70));
        assertEquals(0.25, AlchemyMath.effectMult(90));
        assertEquals(0.0, AlchemyMath.effectMult(100));
        assertEquals(100.0, AlchemyMath.addToxin(95, 50));
        assertEquals(0.0, AlchemyMath.decay(0.5, 24000));
        assertEquals(9.0, AlchemyMath.decay(10, 24000), 1e-9);
    }
}
