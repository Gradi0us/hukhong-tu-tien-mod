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
import net.minecraft.world.entity.monster.Skeleton;
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

public class HuCot extends Skeleton implements GeoEntity, HuMob {
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(HuCot.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HuCot(EntityType<? extends Skeleton> t, Level l) { super(t, l); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(LEVEL, 1);
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l, DifficultyInstance d, MobSpawnType t,
                                                  SpawnGroupData g) {
        SpawnGroupData r = super.finalizeSpawn(l, d, t, g);
        int lv = HuHoa.levelFor(l, blockPosition(), getRandom());
        entityData.set(LEVEL, lv);
        HuHoa.applyStats(this, lv, 1.5, 1.0);
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
        goalSelector.addGoal(1, new HuSkill.Shot(this));
    }

    @Override public void aiStep() { super.aiStep(); HuHoa.particles(this); }

    @Override public int huLevel() { return entityData.get(LEVEL); }
    @Override public double huWeight() { return 1.1; }
    @Override public float honTinhChance() { return 0.22f; }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "move", 3, s -> s.setAndContinue(s.isMoving()
                ? RawAnimation.begin().thenLoop("animation.hu_cot.walk")
                : RawAnimation.begin().thenLoop("animation.hu_cot.idle"))));
        c.add(new AnimationController<>(this, "attack", 0, s -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().then("animation.hu_cot.attack", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("skill", HuAnim.skillOrAttack("hu_cot")));
    }

    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity e) {
        boolean r = super.doHurtTarget(e);
        if (r && !level().isClientSide) triggerAnim("attack", "attack");
        return r;
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
