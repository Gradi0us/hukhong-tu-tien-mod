package com.hukhong.creature;

import com.hukhong.creature.pet.FetchItemsGoal;
import com.hukhong.creature.pet.PetMath;
import com.hukhong.creature.pet.PetService;
import com.hukhong.entity.HuHoa;
import com.hukhong.entity.HuMob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Sinh vat hien/trung lap/tinh anh/thu chien dau: Linh Loc, Hac Tien, Ngoc Tho, Hoa Huynh, Linh Ho, Thiet Boi Hung (+ con).
 * Hanh vi theo {@link CreatureKind}. Ngoc Tho / Linh Ho (linh sung) va Thiet Boi Hung con (thu chien dau) thuan phuc duoc.
 */
public class LinhThu extends TamableAnimal implements GeoEntity, HuMob {
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(LinhThu.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final CreatureKind kind;
    private int skillCd = 60, charmCd = 40;

    public LinhThu(EntityType<? extends LinhThu> t, Level l) {
        super(t, l);
        kind = CreatureKind.of(t);
        if (kind.fly) moveControl = new FlyingMoveControl(this, 20, true);
    }

    public CreatureKind kind() { return kind; }

    @Override protected PathNavigation createNavigation(Level l) {
        CreatureKind k = CreatureKind.of(getType());
        if (k != null && k.fly) {
            FlyingPathNavigation n = new FlyingPathNavigation(this, l);
            n.setCanOpenDoors(false);
            n.setCanFloat(true);
            n.setCanPassDoors(true);
            return n;
        }
        return super.createNavigation(l);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(LEVEL, 1);
    }

    @Override protected void registerGoals() {
        CreatureKind k = CreatureKind.of(getType()); // khong dung field lop con o day
        goalSelector.addGoal(0, new FloatGoal(this));
        if (k.tameSlot != null) goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        switch (k) {
            case LINH_LOC -> {
                goalSelector.addGoal(1, new PanicGoal(this, 1.5));
                goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 10f, 1.0, 1.4));
            }
            case HAC_TIEN -> goalSelector.addGoal(1, new PanicGoal(this, 1.4));
            case NGOC_THO -> {
                goalSelector.addGoal(1, new PanicGoal(this, 1.6) { @Override public boolean canUse() { return !isTame() && super.canUse(); } });
                goalSelector.addGoal(2, new FetchItemsGoal(this));
                goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.2, 5f, 2f));
            }
            case LINH_HO -> {
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
                goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.2, 6f, 2f));
                targetSelector.addGoal(1, new HurtByTargetGoal(this));
            }
            case THIET_BOI_HUNG -> {
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
                targetSelector.addGoal(1, new HurtByTargetGoal(this));
            }
            case THIET_BOI_HUNG_CON -> {
                goalSelector.addGoal(1, new PanicGoal(this, 1.5) { @Override public boolean canUse() { return !isTame() && super.canUse(); } });
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
                goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.2, 7f, 2f));
                targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
                targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
                targetSelector.addGoal(3, new HurtByTargetGoal(this));
            }
            default -> { }
        }
        if (k.tameSlot != null && k != CreatureKind.THIET_BOI_HUNG_CON) { // linh sung cung ho tro chu
            targetSelector.addGoal(2, new OwnerHurtByTargetGoal(this));
        }
        if (k.fly) goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        else goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l, DifficultyInstance d, MobSpawnType t, SpawnGroupData g) {
        SpawnGroupData r = super.finalizeSpawn(l, d, t, g);
        entityData.set(LEVEL, CreatureCommon.scaleOnSpawn(this, kind, l));
        return r;
    }

    @Override public AgeableMob getBreedOffspring(ServerLevel l, AgeableMob o) { return null; }

    @Override public boolean isFood(ItemStack s) { return false; }

    @Override public boolean causeFallDamage(float f, float m, DamageSource s) { return !kind.fly && super.causeFallDamage(f, m, s); }

    // ---- tuong tac: thuan phuc / ngoi ----
    @Override public InteractionResult mobInteract(Player p, InteractionHand h) {
        ItemStack held = p.getItemInHand(h);
        if (!isTame() && kind.tameSlot != null && PetService.isTameItem(this, held)) {
            if (level().isClientSide) return InteractionResult.SUCCESS;
            if (!(p instanceof ServerPlayer sp)) return InteractionResult.PASS;
            PetService.Result r = PetService.tryTame(sp, this, held);
            if (r == PetService.Result.OK) {
                if (!p.isCreative()) held.shrink(1);
                level().broadcastEntityEvent(this, (byte) 7);
            }
            sp.displayClientMessage(Component.translatable("hukhong.pet.tame." + r.name().toLowerCase(), getDisplayName()), true);
            return InteractionResult.CONSUME;
        }
        if (isTame() && isOwnedBy(p) && h == InteractionHand.MAIN_HAND && held.isEmpty() && p.isSecondaryUseActive()) {
            if (!level().isClientSide) {
                setOrderedToSit(!isOrderedToSit());
                p.displayClientMessage(Component.translatable(isOrderedToSit() ? "hukhong.pet.sit" : "hukhong.pet.follow", getDisplayName()), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(p, h);
    }

    // ---- ky nang ----
    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (kind == CreatureKind.HOA_HUYNH && tickCount % 6 == 0)
                level().addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, getRandomX(0.3), getRandomY(), getRandomZ(0.3), 0, 0, 0);
            return;
        }
        if (kind.fly && !onGround()) setDeltaMovement(getDeltaMovement().multiply(1, 0.6, 1));
        if (tickCount % 40 == 0) PetService.syncLevel(this);
        if (kind == CreatureKind.THIET_BOI_HUNG && !isTame()) roar();
        if (kind == CreatureKind.LINH_HO && isTame() && !isOrderedToSit() && --charmCd <= 0 && getOwner() instanceof ServerPlayer o) {
            charmCd = PetMath.CHARM_COOLDOWN;
            if (PetService.charm(this, o)) triggerAnim("attack", "skill");
        }
    }

    /** Thiet Boi Hung gam: lam choang (cham + yeu) nguoi choi trong 6 block roi hat ra. */
    private void roar() {
        if (--skillCd > 0 || getTarget() == null || distanceToSqr(getTarget()) > 36) return;
        skillCd = 200;
        triggerAnim("attack", "skill");
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(6), e -> !e.isSpectator() && !e.isCreative())) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 5));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
            p.knockback(0.8, getX() - p.getX(), getZ() - p.getZ());
        }
        if (level() instanceof ServerLevel sl)
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 1, getZ(), 20, 1.5, 0.5, 1.5, 0.02);
    }

    @Override public boolean doHurtTarget(Entity e) {
        boolean r = super.doHurtTarget(e);
        if (r && !level().isClientSide) triggerAnim("attack", "attack");
        return r;
    }

    @Override protected void dropCustomDeathLoot(ServerLevel sl, DamageSource src, boolean recent) {
        super.dropCustomDeathLoot(sl, src, recent);
        if (!isTame()) CreatureCommon.rollDrops(this, kind);
    }

    // ---- hien thi / luu ----
    @Override public Component getDisplayName() {
        return isTame() || kind == CreatureKind.THIET_BOI_HUNG ? HuHoa.name(huLevel(), getTypeName()) : super.getDisplayName();
    }

    @Override public boolean shouldShowName() { return isTame() || kind == CreatureKind.THIET_BOI_HUNG || super.shouldShowName(); }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(HuHoa.TAG_LEVEL, huLevel());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(HuHoa.TAG_LEVEL)) entityData.set(LEVEL, tag.getInt(HuHoa.TAG_LEVEL));
    }

    @Override public int huLevel() { return entityData.get(LEVEL); }
    @Override public void setHuLevel(int level) { entityData.set(LEVEL, Math.max(1, level)); }
    @Override public double huWeight() { return isTame() ? 0 : CreatureCommon.weight(kind); }
    @Override public float honTinhChance() { return isTame() ? 0f : CreatureCommon.honTinh(kind); }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) { CreatureCommon.controllers(this, kind.id, c); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
