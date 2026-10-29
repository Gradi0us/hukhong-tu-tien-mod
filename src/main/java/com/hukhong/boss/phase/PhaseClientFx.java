package com.hukhong.boss.phase;

import com.hukhong.boss.phase.BossPhaseConfig.Pal;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/** Hào quang theo pha (hạt phía client). Gọi từ aiStep() của boss khi {@code level().isClientSide}. */
public final class PhaseClientFx {
    private PhaseClientFx() {}

    public static void aura(Mob boss, PhasedBoss pb) {
        int ph = pb.currentPhase();
        Level lv = boss.level();
        if (ph < 2 || !lv.isClientSide || boss.tickCount % (ph >= 3 ? 1 : 2) != 0) return;
        Pal pal = pb.phaseConfig().aura();
        Vector3f c = new Vector3f(((pal.rgb >> 16) & 255) / 255f, ((pal.rgb >> 8) & 255) / 255f, (pal.rgb & 255) / 255f);
        DustParticleOptions dust = new DustParticleOptions(c, 1.1f + 0.3f * ph);
        double sc = Math.max(1.0, boss.getBbWidth());
        lv.addParticle(dust, boss.getRandomX(0.9), boss.getRandomY(), boss.getRandomZ(0.9), 0, 0.03, 0);
        if (ph >= 3) {
            lv.addParticle(PhaseMoves.particle(pal), boss.getRandomX(1.0), boss.getY() + 0.1, boss.getRandomZ(1.0), 0, 0.08 + 0.02 * sc, 0);
        }
    }
}
