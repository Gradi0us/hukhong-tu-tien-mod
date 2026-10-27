package com.hukhong.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/** Tâm Ma: bóng nhân bản của người thử luyện (S3). Client vẽ bằng model người + skin chủ, nhuộm tối + mắt đỏ. */
public class TamMa extends Monster {
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(TamMa.class, EntityDataSerializers.OPTIONAL_UUID);

    public TamMa(EntityType<? extends TamMa> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder attrs() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(OWNER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    /** Ban sao bong ngan ngui: &gt;0 = tick con lai truoc khi tan bien (-1 = khong phai ban sao). Chi dung phia server. */
    public int ghostLife = -1;

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && ghostLife > 0 && --ghostLife == 0) {
            ((net.minecraft.server.level.ServerLevel) level()).sendParticles(com.hukhong.vfx.ModParticles.MA_KHI.get(),
                    getX(), getY() + 1, getZ(), 14, 0.3, 0.7, 0.3, 0.03);
            discard();
        }
    }

    public void setOwnerId(UUID id) { entityData.set(OWNER, Optional.ofNullable(id)); }

    public Optional<UUID> getOwnerId() { return entityData.get(OWNER); }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        getOwnerId().ifPresent(u -> t.putUUID("Owner", u));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.hasUUID("Owner")) setOwnerId(t.getUUID("Owner"));
    }

    @Override
    public boolean isSunBurnTick() { return false; }
}
