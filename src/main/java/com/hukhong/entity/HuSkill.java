package com.hukhong.entity;

import com.hukhong.HuKhong;
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
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import software.bernie.geckolib.animatable.GeoEntity;

import java.util.EnumSet;

/** Ky nang dac biet quai Hu Hoa: cooldown -> bao truoc (particle) -> phat ky nang. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class HuSkill {
    private HuSkill() {}

    static final String TAG_ARROW = "HuTienDmg";

    public static void hu(LivingEntity t, int ticks) {
        t.addEffect(new MobEffectInstance(ModEffects.HU_HOA, ticks, 0));
    }

    private static void burst(Mob m, ParticleOptions p, int n, double r) {
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
            this.cd = cooldown / 2;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        abstract void fire(LivingEntity t);
        void activeTick(LivingEntity t) {}
        int activeTicks() { return 0; }
        ParticleOptions warn() { return ParticleTypes.WITCH; }

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

        @Override public void stop() { phase = 0; cd = cooldown; }

        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override public void tick() {
            LivingEntity t = mob.getTarget();
            if (t == null) return;
            mob.getLookControl().setLookAt(t, 30, 30);
            if (phase == 1) {
                mob.getNavigation().stop();
                if (timer % 2 == 0) burst(mob, warn(), 4, 0.5);
                if (--timer <= 0) {
                    if (mob instanceof GeoEntity g) g.triggerAnim("attack", "attack");
                    fire(t);
                    active = activeTicks();
                    phase = active > 0 ? 2 : 0;
                    if (phase == 0) cd = cooldown;
                }
            } else if (phase == 2) {
                activeTick(t);
                if (--active <= 0 && phase == 2) { phase = 0; cd = cooldown; }
            }
        }
    }

    /** Hu Thi: Hu Hoa Trao - lao toi + danh gay Hu Hoa. */
    static final class Dash extends Base {
        Dash(Mob m) { super(m, 100, 15, 3, 9); }
        @Override void fire(LivingEntity t) {
            Vec3 d = t.position().subtract(mob.position()).multiply(1, 0, 1).normalize().scale(1.1);
            mob.setDeltaMovement(d.x, 0.25, d.z);
            mob.hasImpulse = true;
        }
        @Override int activeTicks() { return 10; }
        @Override void activeTick(LivingEntity t) {
            if (mob.distanceToSqr(t) <= 2.5 * 2.5) {
                int lv = ((HuMob) mob).huLevel();
                if (t.hurt(mob.damageSources().mobAttack(mob), (float) HuHoa.skillDamage(lv, 1.5))) hu(t, 160);
                burst(mob, ParticleTypes.SOUL_FIRE_FLAME, 12, 0.5);
                active = 0;
            }
        }
        @Override ParticleOptions warn() { return ParticleTypes.SOUL_FIRE_FLAME; }
    }

    /** Hu Cot: Hu Tien - mui ten tim xuyen, no nho gay Hu Hoa. */
    static final class Shot extends Base {
        Shot(Mob m) { super(m, 120, 18, 5, 20); }
        @Override void fire(LivingEntity t) {
            int lv = ((HuMob) mob).huLevel();
            double dmg = HuHoa.skillDamage(lv, 1.2);
            Arrow a = new Arrow(mob.level(), mob, new ItemStack(Items.ARROW), null);
            a.pickup = AbstractArrow.Pickup.DISALLOWED;
            a.setBaseDamage(dmg / 2.4);
            a.setNoGravity(true);
            a.getPersistentData().putDouble(TAG_ARROW, dmg);
            double dx = t.getX() - mob.getX(), dy = t.getEyeY() - a.getY(), dz = t.getZ() - mob.getZ();
            a.shoot(dx, dy, dz, 2.4f, 0.5f);
            mob.level().addFreshEntity(a);
        }
        @Override ParticleOptions warn() { return ParticleTypes.PORTAL; }
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

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent e) {
        if (!(e.getProjectile() instanceof Arrow a) || a.level().isClientSide) return;
        if (!a.getPersistentData().contains(TAG_ARROW)) return;
        double dmg = a.getPersistentData().getDouble(TAG_ARROW);
        // xuyen thu cong: toi da 2 muc tieu song, bo qua va cham de mui ten bay tiep
        if (e.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity victim && victim != a.getOwner()) {
            int n = a.getPersistentData().getInt("HuPierce");
            if (n < 2) {
                a.getPersistentData().putInt("HuPierce", n + 1);
                victim.hurt(a.damageSources().arrow(a, a.getOwner()), (float) dmg);
                if (victim instanceof Player pl) hu(pl, 120);
                e.setCanceled(true);
            }
        }
        if (a.level() instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, a.getX(), a.getY(), a.getZ(), 15, 0.4, 0.4, 0.4, 0.02);
        for (Player p : a.level().getEntitiesOfClass(Player.class, a.getBoundingBox().inflate(2.0))) {
            p.hurt(a.damageSources().magic(), (float) (dmg * 0.3));
            hu(p, 120);
        }
    }
}
