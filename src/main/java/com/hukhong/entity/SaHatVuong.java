package com.hukhong.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Sa Hat Vuong: boss the gioi sa mac / dat hoang. Bo ky nang: Phun Doc (xoe 5 tia), Chan Dong (dap dat),
 * Kep Quet (quat truoc mat); pha 2: Lop Cat (hut vao tam + goi bay sau).
 */
public class SaHatVuong extends WorldBoss {
    private static final int SPIT = 0, QUAKE = 1, PINCER = 2, VORTEX = 3;

    public SaHatVuong(EntityType<? extends Monster> t, Level l) { super(t, l, BossEvent.BossBarColor.YELLOW); }

    @Override protected String id() { return "sa_hat_vuong"; }
    @Override protected String track() { return "chg"; }
    @Override protected int skillCount(int phase) { return phase >= 2 ? 4 : 3; }
    @Override protected int windupOf(int s) { return switch (s) { case SPIT -> 24; case QUAKE -> 30; case PINCER -> 20; default -> 36; }; }
    @Override protected String animOf(int s) { return switch (s) { case SPIT -> "skill_a"; case QUAKE, VORTEX -> "skill_b"; default -> "skill_c"; }; }
    @Override protected int cooldown(int phase) { return phase >= 2 ? 70 : 100; }
    @Override protected double radiusOf(int s) { return switch (s) { case QUAKE -> 8; case PINCER -> 6; case VORTEX -> 13; default -> 3; }; }

    @Override protected void fire(ServerLevel sl, int skill, LivingEntity t) {
        switch (skill) {
            case SPIT -> {
                spitFan(sl, t, 5, 12, 1.0, 2.8, false);
                playSound(SoundEvents.SLIME_SQUISH, 2f, 0.6f);
            }
            case QUAKE -> {
                hitPlayers(sl, position(), 8, 1.4, p -> {
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                    p.setDeltaMovement(p.getDeltaMovement().add(0, 0.6, 0));
                    p.hurtMarked = true;
                });
                ringFx(sl, position(), 8, ParticleTypes.CLOUD);
                sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 60, 4, 0.3, 4, 0.05);
                playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.6f);
            }
            case PINCER -> {
                Vec3 f = aim(t);
                hitPlayers(sl, position(), 6, 1.2, p -> {
                    Vec3 to = p.position().subtract(position()).multiply(1, 0, 1);
                    if (to.lengthSqr() > 1e-4 && to.normalize().dot(f) > 0.2) {
                        p.setDeltaMovement(p.getDeltaMovement().add(f.x * 1.3, 0.35, f.z * 1.3));
                        p.hurtMarked = true;
                    }
                });
                ringFx(sl, position().add(f.scale(3)), 3, ParticleTypes.CRIT);
            }
            default -> {
                hitPlayers(sl, position(), 13, 0.8, p -> {
                    Vec3 in = position().subtract(p.position()).multiply(1, 0, 1).normalize();
                    p.setDeltaMovement(p.getDeltaMovement().add(in.x * 1.2, 0.25, in.z * 1.2));
                    p.hurtMarked = true;
                    HuSkill.hu(p, 120);
                });
                ringFx(sl, position(), 13, ParticleTypes.CLOUD);
                summonSwarm(sl, t, 3);
            }
        }
    }
}
