package com.hukhong.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HuThi extends Zombie implements GeoEntity, HuMob {
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(HuThi.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HuThi(EntityType<? extends Zombie> t, Level l) { super(t, l); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(LEVEL, 1);
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l, DifficultyInstance d, MobSpawnType t,
                                                  SpawnGroupData g) {
        SpawnGroupData r = super.finalizeSpawn(l, d, t, g);
        boolean cor = com.hukhong.events.CorruptBuff.at(l, blockPosition());
        int lv = com.hukhong.events.CorruptBuff.level(cor, HuHoa.levelFor(l, blockPosition(), getRandom()));
        double cm = com.hukhong.events.CorruptBuff.mul(cor);
        entityData.set(LEVEL, lv);
        HuHoa.applyStats(this, lv, 1.45 * cm, cm);
        return r;
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(HuHoa.TAG_LEVEL, huLevel());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(HuHoa.TAG_LEVEL)) entityData.set(LEVEL, tag.getInt(HuHoa.TAG_LEVEL));
    }

    @Override public Component getDisplayName() { return HuHoa.name(huLevel(), getTypeName()); }
    @Override public boolean shouldShowName() { return true; }
    @Override public boolean isSunBurnTick() { return false; }

    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new HuSkill.Dash(this));
        goalSelector.addGoal(1, new HuSkill.Leap(this, 160, 14, 5, 12, 3.5, 1.3));
        goalSelector.addGoal(1, new HuSkill.Slam(this, 180, 18, 4.5, 1.1));
        goalSelector.addGoal(0, new HuSkill.Rage(this));
    }

    @Override public void aiStep() { super.aiStep(); HuHoa.particles(this); }

    @Override public int huLevel() { return entityData.get(LEVEL); }
    @Override public double huWeight() { return 1.0; }
    @Override public float honTinhChance() { return 0.20f; }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "move", 3, s -> s.setAndContinue(s.isMoving()
                ? RawAnimation.begin().thenLoop("animation.hu_thi.walk")
                : RawAnimation.begin().thenLoop("animation.hu_thi.idle"))));
        c.add(new AnimationController<>(this, "attack", 0, s -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().then("animation.hu_thi.attack", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("skill", HuAnim.skillOrAttack("hu_thi")));
    }

    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity e) {
        boolean r = super.doHurtTarget(e);
        if (r && !level().isClientSide) triggerAnim("attack", "attack");
        return r;
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
