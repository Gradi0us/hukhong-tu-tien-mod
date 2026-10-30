package com.hukhong.creature;

import com.hukhong.entity.HuMob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Linh Ngu: ca song/bien, roi vay lam nguyen lieu. Khong the bat bang xo. */
public class LinhNgu extends AbstractFish implements GeoEntity, HuMob {
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(LinhNgu.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LinhNgu(EntityType<? extends LinhNgu> t, Level l) { super(t, l); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(LEVEL, 1);
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l, DifficultyInstance d, MobSpawnType t, SpawnGroupData g) {
        SpawnGroupData r = super.finalizeSpawn(l, d, t, g);
        entityData.set(LEVEL, CreatureCommon.scaleOnSpawn(this, CreatureKind.LINH_NGU, l));
        return r;
    }

    @Override public InteractionResult mobInteract(Player p, InteractionHand h) { return InteractionResult.PASS; } // khong xo duoc
    @Override public ItemStack getBucketItemStack() { return new ItemStack(Items.WATER_BUCKET); }
    @Override protected SoundEvent getFlopSound() { return SoundEvents.COD_FLOP; }

    @Override protected void dropCustomDeathLoot(ServerLevel sl, DamageSource src, boolean recent) {
        super.dropCustomDeathLoot(sl, src, recent);
        CreatureCommon.rollDrops(this, CreatureKind.LINH_NGU);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(com.hukhong.entity.HuHoa.TAG_LEVEL, huLevel());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(com.hukhong.entity.HuHoa.TAG_LEVEL)) entityData.set(LEVEL, tag.getInt(com.hukhong.entity.HuHoa.TAG_LEVEL));
    }

    @Override public int huLevel() { return entityData.get(LEVEL); }
    @Override public void setHuLevel(int level) { entityData.set(LEVEL, Math.max(1, level)); }
    @Override public double huWeight() { return CreatureCommon.weight(CreatureKind.LINH_NGU); }
    @Override public float honTinhChance() { return CreatureCommon.honTinh(CreatureKind.LINH_NGU); }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) { CreatureCommon.controllers(this, CreatureKind.LINH_NGU.id, c); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
