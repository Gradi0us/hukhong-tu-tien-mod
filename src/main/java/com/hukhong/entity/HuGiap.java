package com.hukhong.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Hu Giap: quai co khien khong lo. Khien phia truoc chan ~70% sat thuong; danh tu lung / ben hong de pha. Xung phong, dap dat. */
public class HuGiap extends HuBeast {
    public HuGiap(EntityType<? extends Monster> t, Level l) { super(t, l); }

    public static AttributeSupplier.Builder attrs() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30).add(Attributes.ATTACK_DAMAGE, 5).add(Attributes.ARMOR, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7).add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override protected String id() { return "hu_giap"; }
    @Override protected double hpMul() { return 2.2; }
    @Override protected double dmgMul() { return 1.3; }

    @Override protected void addSkills() {
        goalSelector.addGoal(1, new HuSkill.Dash(this, 170, 22, 6, 14, 1.0, 1.6));     // Xung Phong
        goalSelector.addGoal(1, new HuSkill.Slam(this, 150, 20, 5.0, 1.3));            // Dap Dat
        goalSelector.addGoal(1, new Bash(this));
        goalSelector.addGoal(0, new HuSkill.Rage(this));
    }

    /** Dap khien: danh bat lui + lam cham o cu ly gan. */
    static final class Bash extends HuSkill.Base {
        Bash(HuGiap m) { super(m, 90, 10, 0, 3.2); }
        @Override void fire(net.minecraft.world.entity.LivingEntity t) {
            Vec3 d = t.position().subtract(mob.position()).multiply(1, 0, 1).normalize();
            if (t.hurt(mob.damageSources().mobAttack(mob), (float) HuHoa.skillDamage(HuSkill.lv(mob), 1.0))) {
                t.setDeltaMovement(t.getDeltaMovement().add(d.x * 1.5, 0.35, d.z * 1.5));
                t.hurtMarked = true;
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                HuSkill.hu(t, 120);
            }
            HuSkill.burst(mob, ParticleTypes.CRIT, 16, 0.8);
        }
    }

    /** Khien chan don truc dien (cung quanh huong mat), tru sat thuong xuyen giap / no. */
    @Override public boolean hurt(DamageSource src, float amount) {
        if (!level().isClientSide && src.getEntity() != null && !src.is(DamageTypeTags.BYPASSES_ARMOR)
                && !src.is(DamageTypeTags.IS_EXPLOSION) && blocks(src.getEntity().position())) {
            amount *= 0.3f;
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.3, getZ(), 6, 0.4, 0.4, 0.4, 0.2);
                playSound(SoundEvents.SHIELD_BLOCK, 0.8f, 0.8f);
            }
        }
        return super.hurt(src, amount);
    }

    /** Thuan hinh hoc: {@code from} co nam trong cung chan cua khien (cos goc > 0.35) khong. */
    public boolean blocks(Vec3 from) {
        Vec3 to = from.subtract(position()).multiply(1, 0, 1);
        Vec3 look = getViewVector(1.0f).multiply(1, 0, 1);
        if (to.lengthSqr() < 1e-4 || look.lengthSqr() < 1e-4) return false;
        return look.normalize().dot(to.normalize()) > 0.35;
    }

    @Override public double huWeight() { return 2.4; }
    @Override public float honTinhChance() { return 0.3f; }
}
