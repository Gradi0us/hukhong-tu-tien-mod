package com.hukhong.entity;

import com.hukhong.effect.ModEffects;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.hukhong.events.BossBuff;
import software.bernie.geckolib.animatable.GeoEntity;

import java.util.EnumSet;

/** Ky nang dac biet quai Hu Hoa: cooldown -> bao truoc (particle) -> phat ky nang. */
public final class HuSkill {
    private HuSkill() {}

    public static void hu(LivingEntity t, int ticks) {
        t.addEffect(new MobEffectInstance(ModEffects.HU_HOA, ticks, 0));
    }

    static void burst(Mob m, ParticleOptions p, int n, double r) {
        if (m.level() instanceof ServerLevel sl)
            sl.sendParticles(p, m.getX(), m.getY() + m.getBbHeight() * 0.6, m.getZ(), n, r, 0.4, r, 0.02);
    }

    /** Khung chung: cooldown -> windup -> fire -> active (tuy ky nang). */
    abstract static class Base extends Goal {
        final Mob mob;
        final int cooldown, windup;
        final double min, max;
        int cd, timer, active;
        int phase; // 0 nghi, 1 windup, 2 active

        Base(Mob mob, int cooldown, int windup, double min, double max) {
            this.mob = mob; this.cooldown = cooldown; this.windup = windup; this.min = min; this.max = max;
            this.cd = BossBuff.cdFor(mob, cooldown) / 2;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        abstract void fire(LivingEntity t);
        void activeTick(LivingEntity t) {}
        int activeTicks() { return 0; }
        ParticleOptions warn() { return com.hukhong.vfx.ModParticles.HU_KHONG.get(); }

        @Override public boolean canUse() {
            if (cd > 0) { cd--; return false; }
            LivingEntity t = mob.getTarget();
            if (t == null || !t.isAlive()) return false;
            double d = mob.distanceToSqr(t);
            return d >= min * min && d <= max * max && mob.getSensing().hasLineOfSight(t);
        }

        @Override public boolean canContinueToUse() {
            LivingEntity t = mob.getTarget();
            return phase != 0 && t != null && t.isAlive();
        }

        @Override public void start() { phase = 1; timer = windup; mob.getNavigation().stop(); }

        @Override public void stop() { phase = 0; cd = BossBuff.cdFor(mob, cooldown); }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void tick() {
            LivingEntity t = mob.getTarget();
            if (t == null) return;
            mob.getLookControl().setLookAt(t, 30, 30);
            if (phase == 1) {
                mob.getNavigation().stop();
                if (timer % 2 == 0) burst(mob, warn(), 4, 0.5);
                if (--timer <= 0) {
                    if (mob instanceof GeoEntity g) g.triggerAnim("attack", "skill");
                    if (mob.level() instanceof ServerLevel sl) com.hukhong.vfx.ModSounds.play(sl, mob, com.hukhong.vfx.ModSounds.SKILL_HU_KHONG, 0.8f, 0.9f + mob.getRandom().nextFloat() * 0.3f);
                    fire(t);
                    active = activeTicks();
                    phase = active > 0 ? 2 : 0;
                    if (phase == 0) cd = BossBuff.cdFor(mob, cooldown);
                }
            } else if (phase == 2) {
                activeTick(t);
                if (--active <= 0 && phase == 2) { phase = 0; cd = BossBuff.cdFor(mob, cooldown); }
            }
        }
    }

    static int lv(Mob m) { return m instanceof HuMob h ? h.huLevel() : 1; }

    /** Sat thuong dien rong quanh mob: nguoi choi trong ban kinh r (ngang) va +-3 (doc). */
    static int aoe(Mob m, double r, float dmg, java.util.function.Consumer<LivingEntity> then) {
        int n = 0;
        for (Player p : m.level().getEntitiesOfClass(Player.class, m.getBoundingBox().inflate(r, 3, r))) {
            if (p.isSpectator() || p.distanceToSqr(m.getX(), p.getY(), m.getZ()) > r * r) continue;
            if (p.hurt(m.damageSources().mobAttack(m), dmg)) n++;
            if (then != null) then.accept(p);
        }
        return n;
    }

    static void ring(Mob m, double r, ParticleOptions pt) {
        if (!(m.level() instanceof ServerLevel sl)) return;
        for (int i = 0; i < 36; i++) {
            double a = i * Math.PI * 2 / 36;
            sl.sendParticles(pt, m.getX() + Math.cos(a) * r, m.getY() + 0.2, m.getZ() + Math.sin(a) * r, 1, 0.1, 0.05, 0.1, 0.0);
        }
    }

    /** Lao thang toi: Hu Thi (Hu Hoa Trao), Hu Giap (Xung Phong), Hu Anh (Luot). Bay: huong lao co thanh phan doc. */
    static final class Dash extends Base {
        final double speed, mult;
        Dash(Mob m) { this(m, 100, 15, 3, 9, 1.1, 1.5); }
        Dash(Mob m, int cd, int windup, double min, double max, double speed, double mult) {
            super(m, cd, windup, min, max); this.speed = speed; this.mult = mult;
        }
        @Override void fire(LivingEntity t) {
            Vec3 d = t.position().subtract(mob.position()).multiply(1, 0, 1).normalize().scale(speed);
            double dy = mob.isNoGravity() ? Math.max(-0.6, Math.min(0.6, (t.getY(0.5) - mob.getY()) * 0.15)) : 0.25;
            mob.setDeltaMovement(d.x, dy, d.z);
            mob.hasImpulse = true;
        }
        @Override int activeTicks() { return 10; }
        @Override void activeTick(LivingEntity t) {
            if (mob.distanceToSqr(t) <= 2.5 * 2.5) {
                if (t.hurt(mob.damageSources().mobAttack(mob), (float) HuHoa.skillDamage(lv(mob), mult))) hu(t, 160);
                burst(mob, com.hukhong.vfx.ModParticles.HU_KHONG.get(), 12, 0.5);
                active = 0;
            }
        }
    }

    /** Hu Chu: Hu Vong - phun to lam cham + keo lai gan. */
    static final class Web extends Base {
        Web(Mob m) { super(m, 140, 16, 4, 14); }
        @Override void fire(LivingEntity t) {
            if (mob.level() instanceof ServerLevel sl) {
                Vec3 a = mob.position().add(0, mob.getBbHeight() * 0.5, 0), b = t.position().add(0, t.getBbHeight() * 0.5, 0);
                for (int i = 0; i <= 12; i++) {
                    Vec3 p = a.lerp(b, i / 12.0);
                    sl.sendParticles(ParticleTypes.ITEM_COBWEB, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.01);
                }
            }
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            Vec3 pull = mob.position().subtract(t.position()).multiply(1, 0, 1).normalize().scale(1.0);
            t.setDeltaMovement(t.getDeltaMovement().add(pull.x, 0.2, pull.z));
            t.hurtMarked = true;
        }
        @Override ParticleOptions warn() { return ParticleTypes.ITEM_COBWEB; }
    }

    /** Nhay vot len dau muc tieu, ha xuong gay no dien rong. */
    static final class Leap extends Base {
        final double radius, mult;
        Leap(Mob m, int cd, int windup, double min, double max, double radius, double mult) {
            super(m, cd, windup, min, max); this.radius = radius; this.mult = mult;
        }
        @Override void fire(LivingEntity t) {
            Vec3 d = t.position().subtract(mob.position()).multiply(1, 0, 1);
            double dist = d.length();
            Vec3 v = dist < 1e-3 ? Vec3.ZERO : d.normalize().scale(Math.max(0.45, Math.min(1.1, dist * 0.1)));
            mob.setDeltaMovement(v.x, 0.62, v.z);
            mob.hasImpulse = true;
        }
        @Override int activeTicks() { return 40; }
        @Override void activeTick(LivingEntity t) {
            if (activeTicks() - active >= 5 && mob.onGround()) {
                aoe(mob, radius, (float) HuHoa.skillDamage(lv(mob), mult), l -> { if (l instanceof Player) hu(l, 120); });
                ring(mob, radius, ParticleTypes.POOF);
                burst(mob, com.hukhong.vfx.ModParticles.HU_KHONG.get(), 14, 0.6);
                active = 0;
            }
        }
    }

    /** Dap dat: dien rong quanh than, hat tung + lam cham. */
    static final class Slam extends Base {
        final double radius, mult;
        Slam(Mob m, int cd, int windup, double radius, double mult) { super(m, cd, windup, 0, radius + 1.5); this.radius = radius; this.mult = mult; }
        @Override void fire(LivingEntity t) {
            aoe(mob, radius, (float) HuHoa.skillDamage(lv(mob), mult), l -> {
                l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                l.setDeltaMovement(l.getDeltaMovement().add(0, 0.5, 0));
                l.hurtMarked = true;
            });
            ring(mob, radius, ParticleTypes.EXPLOSION);
            ring(mob, radius * 0.6, com.hukhong.vfx.ModParticles.HU_KHONG.get());
            mob.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 1.2f);
        }
        @Override ParticleOptions warn() { return ParticleTypes.CRIT; }
    }

    /** Doc Thuy: nuoc bot doc (thay cung ten): trung thi no doc ban kinh nho. */
    static final class Spit extends Base {
        final double mult, aoe;
        Spit(Mob m, int cd, int windup, double min, double max, double mult, double aoe) {
            super(m, cd, windup, min, max); this.mult = mult; this.aoe = aoe;
        }
        @Override void fire(LivingEntity t) {
            PoisonSpit.launch(mob, t, (float) HuHoa.skillDamage(lv(mob), mult), aoe);
        }
        @Override ParticleOptions warn() { return ParticleTypes.ITEM_SLIME; }
    }

    /** Lui nhanh ra xa khi bi ap sat. */
    static final class Backstep extends Base {
        Backstep(Mob m) { super(m, 80, 6, 0, 3.5); }
        @Override void fire(LivingEntity t) {
            Vec3 d = mob.position().subtract(t.position()).multiply(1, 0, 1).normalize().scale(1.0);
            mob.setDeltaMovement(d.x, 0.35, d.z);
            mob.hasImpulse = true;
        }
    }

    /** Bung doc quanh than: giam suc (yeu, cham, mu ngan), sat thuong nho. */
    static final class VenomBurst extends Base {
        final double radius;
        VenomBurst(Mob m, int cd, int windup, double radius) { super(m, cd, windup, 0, radius); this.radius = radius; }
        @Override void fire(LivingEntity t) {
            aoe(mob, radius, (float) HuHoa.skillDamage(lv(mob), 0.5), l -> {
                l.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                l.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                l.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            });
            ring(mob, radius, ParticleTypes.ITEM_SLIME);
            burst(mob, ParticleTypes.SPORE_BLOSSOM_AIR, 30, radius * 0.4);
        }
        @Override ParticleOptions warn() { return ParticleTypes.SPORE_BLOSSOM_AIR; }
    }

    /** Chu Thuat: tia Hu Hoa tuc thi, yeu + Hu Hoa. */
    static final class Curse extends Base {
        final double mult;
        Curse(Mob m, int cd, int windup, double min, double max, double mult) { super(m, cd, windup, min, max); this.mult = mult; }
        @Override void fire(LivingEntity t) {
            t.hurt(mob.damageSources().indirectMagic(mob, mob), (float) HuHoa.skillDamage(lv(mob), mult));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            hu(t, 140);
            if (mob.level() instanceof ServerLevel sl) {
                Vec3 a = mob.position().add(0, mob.getBbHeight() * 0.7, 0), b = t.position().add(0, t.getBbHeight() * 0.5, 0);
                for (int i = 0; i <= 12; i++) {
                    Vec3 p = a.lerp(b, i / 12.0);
                    sl.sendParticles(com.hukhong.vfx.ModParticles.HU_KHONG.get(), p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
                }
            }
        }
    }

    /** Trieu Hoi Hu Trung (toi da {@code cap} con quanh day). */
    static final class SummonSwarm extends Base {
        final int count, cap;
        SummonSwarm(Mob m, int cd, int windup, int count, int cap) { super(m, cd, windup, 0, 24); this.count = count; this.cap = cap; }
        @Override public boolean canUse() {
            return super.canUse() && mob.level().getEntitiesOfClass(HuTrung.class, mob.getBoundingBox().inflate(16)).size() < cap;
        }
        @Override void fire(LivingEntity t) {
            if (!(mob.level() instanceof ServerLevel sl)) return;
            for (int i = 0; i < count; i++) {
                HuTrung c = V3Entities.HU_TRUNG.get().create(sl);
                if (c == null) continue;
                double a = mob.getRandom().nextDouble() * Math.PI * 2;
                c.moveTo(mob.getX() + Math.cos(a) * 2, mob.getY(), mob.getZ() + Math.sin(a) * 2, mob.getRandom().nextFloat() * 360f, 0f);
                c.finalizeSpawn(sl, sl.getCurrentDifficultyAt(c.blockPosition()), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);
                c.setTarget(t);
                sl.addFreshEntity(c);
            }
            burst(mob, com.hukhong.vfx.ModParticles.HU_KHONG.get(), 30, 1.2);
        }
    }

    /** Pha 2 cua boss thuong (quai Hu co tag boss, vd. boss Bi Canh): duoi 50% mau thi cuong hoa + soc dia. */
    static final class Rage extends Base {
        boolean done;
        Rage(Mob m) { super(m, 400, 20, 0, 40); }
        @Override public boolean canUse() {
            if (done || !BossBuff.isBoss(mob) || mob.getHealth() > mob.getMaxHealth() * 0.5f) return false;
            LivingEntity t = mob.getTarget();
            return t != null && t.isAlive();
        }
        @Override void fire(LivingEntity t) {
            done = true;
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 1));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 600, 0));
            aoe(mob, 7, (float) HuHoa.skillDamage(lv(mob), 2.0), l -> {
                Vec3 away = l.position().subtract(mob.position()).multiply(1, 0, 1).normalize();
                l.setDeltaMovement(l.getDeltaMovement().add(away.x * 1.2, 0.4, away.z * 1.2));
                l.hurtMarked = true;
                hu(l, 160);
            });
            ring(mob, 7, ParticleTypes.SOUL_FIRE_FLAME);
            burst(mob, com.hukhong.vfx.ModParticles.HU_KHONG.get(), 60, 1.5);
            mob.playSound(net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL, 1.5f, 0.8f);
        }
    }
}
