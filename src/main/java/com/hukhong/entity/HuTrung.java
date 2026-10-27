package com.hukhong.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Hu Trung: bay sau nho, nhanh, mong, nhay chom + cho doc. Sinh theo dan 4-7 con. */
public class HuTrung extends HuBeast {
    public HuTrung(EntityType<? extends Monster> t, Level l) { super(t, l); }

    public static AttributeSupplier.Builder attrs() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8).add(Attributes.ATTACK_DAMAGE, 2.5).add(Attributes.ARMOR, 0)
                .add(Attributes.MOVEMENT_SPEED, 0.34).add(Attributes.FOLLOW_RANGE, 28);
    }

    @Override protected String id() { return "hu_trung"; }
    @Override protected double hpMul() { return 0.5; }
    @Override protected double dmgMul() { return 0.6; }

    @Override protected void addSkills() {
        goalSelector.addGoal(1, new HuSkill.Leap(this, 60, 6, 2.5, 7, 1.8, 0.7));
    }

    @Override public boolean doHurtTarget(Entity e) {
        boolean r = super.doHurtTarget(e);
        if (r && e instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
        return r;
    }

    @Override public double huWeight() { return 0.35; }
    @Override public float honTinhChance() { return 0.08f; }
}
