package com.hukhong.artifact;

import com.hukhong.banmenh.BanMenhMath;
import com.hukhong.core.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactMathTest {
    @Test
    void sameCostTableAndRealmCapAsBanMenh() {
        for (int s = 1; s <= 6; s++) assertEquals(BanMenhMath.tinhCost(s), ArtifactMath.tinhCost(s));
        assertEquals(BanMenhMath.REALM_CAP, ArtifactMath.check(0, 19, 99));
        assertEquals(BanMenhMath.NEED_TINH, ArtifactMath.check(0, 20, 1));
        assertEquals(BanMenhMath.OK, ArtifactMath.check(0, 20, ArtifactMath.tinhCost(1)));
        assertEquals(BanMenhMath.MAXED, ArtifactMath.check(6, 100, 999));
    }

    @Test
    void pathsAndWings() {
        assertTrue(ArtifactMath.hasArtifact(Path.DAO));
        assertTrue(ArtifactMath.hasArtifact(Path.THE));
        assertTrue(ArtifactMath.hasArtifact(Path.MA));
        assertFalse(ArtifactMath.hasArtifact(Path.KIEM));
        assertEquals(0, ArtifactMath.wingPairs(0));
        int prev = 0;
        for (int s = 1; s <= 6; s++) { assertTrue(ArtifactMath.wingPairs(s) >= prev); prev = ArtifactMath.wingPairs(s); }
        assertEquals(4, ArtifactMath.wingPairs(6));
        assertEquals(4, ArtifactMath.wingPairs(99));
    }

    @Test
    void bonusesAreModestAndMonotonic() {
        assertEquals(0, ArtifactMath.daoRegen(0));
        assertEquals(0.24, ArtifactMath.daoRegen(6), 1e-9);
        assertEquals(12.0, ArtifactMath.theHealth(6), 1e-9);
        assertTrue(ArtifactMath.theDefense(6) <= 0.2);
        assertTrue(ArtifactMath.maAttack(6) <= 0.2);
        assertTrue(ArtifactMath.maLifesteal(6) <= 0.03);
        assertEquals(ArtifactMath.daoSpell(6), ArtifactMath.daoSpell(60), 1e-9);
    }

    @Test
    void dataClampsAndSetsPerPath() {
        ArtifactData d = new ArtifactData(9, -2, 3);
        assertEquals(6, d.dao());
        assertEquals(0, d.the());
        assertEquals(3, d.ma());
        assertEquals(4, d.with(Path.THE, 4).the());
        assertEquals(6, d.with(Path.THE, 4).dao());
        assertEquals(0, d.get(Path.KIEM));
        assertSame(d, d.with(Path.KIEM, 5));
    }
}
