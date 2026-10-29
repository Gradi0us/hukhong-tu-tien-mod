package com.hukhong.entity;

import com.hukhong.events.CorruptBuff;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Khung chung cho quai Hu Hoa moi (v3): cap theo khoang cach + tuoi the gioi, +30% khi sinh trong vung nhiem. */
public abstract class HuBeast extends Monster implements GeoEntity, HuMob {
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(HuBeast.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected HuBeast(EntityType<? extends Monster> t, Level l) { super(t, l); }

    /** Id model/animation (vd "hu_giap"). */
    protected abstract String id();
    protected abstract double hpMul();
    protected abstract double dmgMul();
    /** Them ky nang (chay trong registerGoals, KHONG duoc dung field cua lop con). */
    protected abstract void addSkills();

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(LEVEL, 1);
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l, DifficultyInstance d, MobSpawnType t, SpawnGroupData g) {
        SpawnGroupData r = super.finalizeSpawn(l, d, t, g);
        boolean cor = CorruptBuff.at(l, blockPosition());
        int lv = CorruptBuff.level(cor, HuHoa.levelFor(l, blockPosition(), getRandom()));
        entityData.set(LEVEL, lv);
        double cm = CorruptBuff.mul(cor);
        HuHoa.applyStats(this, lv, hpMul() * cm, dmgMul() * cm);
        return r;
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        addSkills();
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
    @Override public void aiStep() { super.aiStep(); HuHoa.particles(this); }
    @Override public int huLevel() { return entityData.get(LEVEL); }
    @Override public void setHuLevel(int level) { entityData.set(LEVEL, Math.max(1, level)); }

    @Override public boolean doHurtTarget(Entity e) {
        boolean r = super.doHurtTarget(e);
        if (r && !level().isClientSide) triggerAnim("attack", "attack");
        return r;
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        String id = id();
        c.add(new AnimationController<>(this, "move", 3, s -> s.setAndContinue(s.isMoving()
                ? RawAnimation.begin().thenLoop("animation." + id + ".walk")
                : RawAnimation.begin().thenLoop("animation." + id + ".idle"))));
        c.add(new AnimationController<>(this, "attack", 0, s -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().then("animation." + id + ".attack", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("skill", HuAnim.skillOrAttack(id)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
