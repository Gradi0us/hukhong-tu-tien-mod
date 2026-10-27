package com.hukhong.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/** Hu Anh: bong ma Hu Khong biet bay. Quanh tren dau muc tieu, luot xuong (lao) va ban tia chu. */
public class HuAnh extends HuBeast {
    public HuAnh(EntityType<? extends Monster> t, Level l) {
        super(t, l);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder attrs() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16).add(Attributes.ATTACK_DAMAGE, 3.5).add(Attributes.ARMOR, 0)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FLYING_SPEED, 0.5).add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override protected PathNavigation createNavigation(Level l) {
        FlyingPathNavigation n = new FlyingPathNavigation(this, l);
        n.setCanOpenDoors(false);
        n.setCanFloat(true);
        n.setCanPassDoors(true);
        return n;
    }

    @Override protected String id() { return "hu_anh"; }
    @Override protected double hpMul() { return 1.0; }
    @Override protected double dmgMul() { return 1.0; }

    @Override protected void addSkills() {
        goalSelector.addGoal(1, new HuSkill.Dash(this, 90, 12, 4, 16, 1.25, 1.3));   // Luot
        goalSelector.addGoal(1, new HuSkill.Curse(this, 130, 18, 5, 18, 1.0));       // Tia Chu
        goalSelector.addGoal(0, new HuSkill.Rage(this));
        goalSelector.addGoal(3, new Hover(this));
    }

    /** Giu do cao ~3 block tren dau muc tieu khi o gan. */
    static final class Hover extends Goal {
        final HuAnh m;
        Hover(HuAnh m) { this.m = m; setFlags(EnumSet.noneOf(Flag.class)); }
        @Override public boolean canUse() { LivingEntity t = m.getTarget(); return t != null && t.isAlive() && m.distanceToSqr(t) < 14 * 14; }
        @Override public void tick() {
            LivingEntity t = m.getTarget();
            if (t == null) return;
            double dy = m.getY() - t.getY();
            if (dy < 1.5) m.setDeltaMovement(m.getDeltaMovement().add(0, 0.04, 0));
            else if (dy > 4.5) m.setDeltaMovement(m.getDeltaMovement().add(0, -0.03, 0));
        }
    }

    @Override public boolean causeFallDamage(float f, float m, net.minecraft.world.damagesource.DamageSource s) { return false; }
    @Override protected void checkFallDamage(double y, boolean onGround, BlockState st, BlockPos pos) {}

    @Override public double huWeight() { return 1.3; }
    @Override public float honTinhChance() { return 0.25f; }
}
