package com.hukhong.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Doc Mau: boss the gioi dam lay / rung ram. Bo ky nang: Dam Bao Tu (vung khi doc), Day Leo Quat (keo nguoi choi vao),
 * Do Trung (goi bay Hu Trung); pha 2: Phun Trao Doc (bung doc dien rong + mua nuoc bot).
 */
public class DocMau extends WorldBoss {
    private static final int CLOUD = 0, LASH = 1, BROOD = 2, ERUPT = 3;

    public DocMau(EntityType<? extends Monster> t, Level l) { super(t, l, BossEvent.BossBarColor.GREEN); }

    @Override protected String id() { return "doc_mau"; }
    @Override protected String track() { return "bicanh_boss"; }
    @Override protected int skillCount(int phase) { return phase >= 2 ? 4 : 3; }
    @Override protected int windupOf(int s) { return switch (s) { case CLOUD -> 26; case LASH -> 24; case BROOD -> 30; default -> 40; }; }
    @Override protected String animOf(int s) { return switch (s) { case CLOUD -> "skill_a"; case LASH -> "skill_c"; default -> "skill_b"; }; }
    @Override protected int cooldown(int phase) { return phase >= 2 ? 72 : 100; }
    @Override protected double radiusOf(int s) { return switch (s) { case LASH -> 18; case ERUPT -> 11; default -> 4; }; }

    @Override protected void fire(ServerLevel sl, int skill, LivingEntity t) {
        switch (skill) {
            case CLOUD -> {
                Vec3 at = t != null ? t.position() : pendingAt;
                AreaEffectCloud c = new AreaEffectCloud(sl, at.x, at.y, at.z);
                c.setOwner(this);
                c.setRadius(4.0f);
                c.setDuration(160);
                c.setRadiusPerTick(-0.015f);
                c.setParticle(ParticleTypes.ITEM_SLIME);
                c.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                c.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                sl.addFreshEntity(c);
                playSound(SoundEvents.SLIME_SQUISH, 2f, 0.5f);
            }
            case LASH -> {
                hitPlayers(sl, position(), 18, 1.0, p -> {
                    Vec3 in = position().subtract(p.position()).multiply(1, 0, 1).normalize();
                    p.setDeltaMovement(p.getDeltaMovement().add(in.x * 1.3, 0.3, in.z * 1.3));
                    p.hurtMarked = true;
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    Vec3 a = position().add(0, getBbHeight() * 0.6, 0), b = p.position().add(0, 1, 0);
                    for (int i = 0; i <= 14; i++) {
                        Vec3 q = a.lerp(b, i / 14.0);
                        sl.sendParticles(ParticleTypes.COMPOSTER, q.x, q.y, q.z, 2, 0.05, 0.05, 0.05, 0.0);
                    }
                });
            }
            case BROOD -> {
                summonSwarm(sl, t, 4);
                playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 2f, 0.6f);
            }
            default -> {
                hitPlayers(sl, position(), 11, 1.4, p -> p.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 2)));
                ringFx(sl, position(), 11, ParticleTypes.ITEM_SLIME);
                for (int i = 0; i < 4; i++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    PoisonSpit.launchDir(this, position().add(0, getBbHeight() * 0.8, 0), new Vec3(Math.cos(a), 0.5, Math.sin(a)), skillDmg(0.8), 2.5, 1.0f);
                }
                playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.9f);
            }
        }
    }
}
