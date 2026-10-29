package com.hukhong.phaptu.linhtruong;

import com.hukhong.banmenh.BanMenhMath;
import com.hukhong.banmenh.BanMenhService;
import com.hukhong.core.Aptitude;
import com.hukhong.core.Path;
import com.hukhong.stats.StatService;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LinhTruongMathTest {
    private static StatService.Stats stats(Path p, int lv, Aptitude a) {
        return StatService.compute(lv, EnumSet.of(p), new EnumMap<>(Map.of(p, a)));
    }

    /** Kiem: (5 + Cong + Cong x Q) x 1.6/s x 0.55; Phap: sp x COEF x Q x (0.85 / 0.7s). Ca hai nhan bao kich trung binh. */
    private static double ratio(int lv, Aptitude a) {
        int st = BanMenhMath.stageForLevel(lv);
        double q = BanMenhService.QUALITY[st];
        StatService.Stats k = stats(Path.KIEM, lv, a), f = stats(Path.PHAP, lv, a);
        double kd = (5 + k.attack() + k.attack() * q) * 1.6 * 0.55 * (1 + k.crit() * (k.critDamage() - 1));
        double fd = LinhTruongMath.boltDamage(f.spell(), st) * (0.85 / (LinhTruongMath.COOLDOWN_TICKS / 20.0)) * (1 + f.crit() * (f.critDamage() - 1));
        return fd / kd;
    }

    @Test
    void qualityMatchesBanMenh() {
        assertEquals(BanMenhService.QUALITY.length, LinhTruongMath.QUALITY.length);
        for (int i = 0; i < LinhTruongMath.QUALITY.length; i++) assertEquals(BanMenhService.QUALITY[i], LinhTruongMath.QUALITY[i], 0f);
    }

    @Test
    void dpsWithinBandOfKiemAtEveryLevel() {
        for (Aptitude a : new Aptitude[]{Aptitude.B, Aptitude.S}) {
            for (int lv = 1; lv <= 100; lv++) {
                double r = ratio(lv, a);
                // dai muc tieu 85-95%; cho 3 diem dung sai o cac cap thap (< 5) vi Cong phang 5 cua kiem vanilla chi phoi
                double lo = lv < 5 ? 0.82 : 0.85, hi = lv < 5 ? 1.0 : 0.95;
                assertTrue(r >= lo && r <= hi, "level " + lv + " apt " + a + " ratio " + r);
            }
        }
    }

    @Test
    void damageScalesWithStage() {
        float d0 = LinhTruongMath.boltDamage(100, 0), d6 = LinhTruongMath.boltDamage(100, 6);
        assertEquals(100 * 1.15 * 1.0, d0, 1e-3);
        assertEquals(100 * 0.765 * 3.0, d6, 1e-3);
        assertTrue(d6 > d0 * 1.9);
        for (int s = 1; s <= 6; s++) assertTrue(LinhTruongMath.boltDamage(100, s) > LinhTruongMath.boltDamage(100, s - 1));
    }

    @Test
    void tipIsInFrontAndRightOfEye() {
        var eye = new net.minecraft.world.phys.Vec3(0, 0, 0);
        var t = LinhTruongMath.tip(eye, 0f, 0f); // nhin +Z, ben phai la -X
        assertEquals(0.85, t.z, 1e-6);
        assertEquals(-0.38, t.x, 1e-6);
        assertEquals(-0.12, t.y, 1e-6);
    }
}
