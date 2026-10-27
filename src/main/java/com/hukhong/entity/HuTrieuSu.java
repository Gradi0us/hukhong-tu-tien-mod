package com.hukhong.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Hu Trieu Su: phap su giu khoang cach: nhe doc, tia chu, trieu hoi bay Hu Trung, bung doc khi bi ap sat. */
public class HuTrieuSu extends HuBeast {
    public HuTrieuSu(EntityType<? extends Monster> t, Level l) { super(t, l); }

    public static AttributeSupplier.Builder attrs() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 22).add(Attributes.ATTACK_DAMAGE, 3).add(Attributes.ARMOR, 1)
                .add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.FOLLOW_RANGE, 36);
    }

    @Override protected String id() { return "hu_trieu_su"; }
    @Override protected double hpMul() { return 1.3; }
    @Override protected double dmgMul() { return 0.9; }

    @Override protected void addSkills() {
        goalSelector.addGoal(1, new HuSkill.SummonSwarm(this, 260, 26, 3, 8));
        goalSelector.addGoal(1, new HuSkill.Spit(this, 90, 14, 6, 20, 1.0, 2.5));
        goalSelector.addGoal(1, new HuSkill.Curse(this, 150, 18, 5, 16, 0.9));
        goalSelector.addGoal(1, new HuSkill.VenomBurst(this, 170, 14, 4.0));
        goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, 5f, 1.0, 1.3));
        goalSelector.addGoal(0, new HuSkill.Rage(this));
    }

    @Override public double huWeight() { return 1.6; }
    @Override public float honTinhChance() { return 0.3f; }
}
