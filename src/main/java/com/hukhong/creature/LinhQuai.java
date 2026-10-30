package com.hukhong.creature;

import com.hukhong.entity.HuHoa;
import com.hukhong.entity.HuMob;
import com.hukhong.entity.PoisonSpit;
import com.hukhong.events.CorruptBuff;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
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
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * Sinh vat thu dich: Doc Van Xa (doc), Sa Trung (chui cat, tro len co bao truoc), Bang Hon (lam cham/dong bang vung),
 * Moc Yeu (dung yen nhu cay, troi re), Doc Thiem (phun doc tam xa), Quan Hon (doi hinh 3-5, thuong tran).
 */
public class LinhQuai extends Monster implements GeoEntity, HuMob {
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(LinhQuai.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BURROWED = SynchedEntityData.defineId(LinhQuai.class, EntityDataSerializers.BOOLEAN);
    public static final int EMERGE_WARN_TICKS = 20, BURROW_IDLE_TICKS = 100;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final CreatureKind kind;
    private int auraCd, emerge, idle;

    public LinhQuai(EntityType<? extends LinhQuai> t, Level l) {
        super(t, l);
        kind = CreatureKind.of(t);
    }

    public CreatureKind kind() { return kind; }

    public boolean burrowed() { return entityData.get(BURROWED); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(LEVEL, 1).define(BURROWED, false);
    }

    @Override protected void registerGoals() {
        CreatureKind k = CreatureKind.of(getType());
        goalSelector.addGoal(0, new FloatGoal(this));
        if (k == CreatureKind.DOC_THIEM) goalSelector.addGoal(1, new SpitGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, k == CreatureKind.DOC_VAN_XA ? 1.3 : 1.1, false));
        if (k != CreatureKind.MOC_YEU) goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8)); // Moc Yeu dung yen nhu cay
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l, DifficultyInstance d, MobSpawnType t, SpawnGroupData g) {
        SpawnGroupData r = super.finalizeSpawn(l, d, t, g);
        entityData.set(LEVEL, CreatureCommon.scaleOnSpawn(this, kind, l));
        return r;
    }

    // ---- ky nang ----
    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (kind == CreatureKind.BANG_HON && tickCount % 4 == 0)
                level().addParticle(ParticleTypes.SNOWFLAKE, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
            return;
        }
        if (kind == CreatureKind.BANG_HON) freezeAura();
        if (kind == CreatureKind.SA_TRUNG) burrowTick();
    }

    /** Bang Hon: moi 1.5 giay, nguoi choi trong 5 block bi cham + dong bang dan. */
    private void freezeAura() {
        if (--auraCd > 0) return;
        auraCd = 30;
        boolean hit = false;
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(5), e -> !e.isSpectator() && !e.isCreative())) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze() + 40, p.getTicksFrozen() + 60));
            hit = true;
        }
        if (hit && level() instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1, getZ(), 24, 2.5, 0.8, 2.5, 0.02);
    }

    /** Sa Trung: chui cat khi khong co muc tieu; co nguoi lai gan -> bao truoc 1 giay (bui + cat) roi tro len. */
    private void burrowTick() {
        boolean near = level().getNearestPlayer(this, 7) instanceof Player np && !np.isSpectator() && !np.isCreative();
        if (burrowed()) {
            getNavigation().stop();
            setTarget(null);
            if (near) {
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(blockPosition().below())),
                            getX(), getY() + 0.1, getZ(), 6, 0.5, 0.1, 0.5, 0.05);
                }
                if (++emerge >= EMERGE_WARN_TICKS) setBurrowed(false);
            } else emerge = 0;
        } else {
            if (getTarget() != null || near || !onGround()) { idle = 0; return; }
            if (++idle >= BURROW_IDLE_TICKS && level().getBlockState(blockPosition().below()).is(BlockTags.SAND)) setBurrowed(true);
        }
    }

    private void setBurrowed(boolean v) {
        entityData.set(BURROWED, v);
        setInvisible(v);
        setSilent(v);
        emerge = 0;
        idle = 0;
        if (level() instanceof ServerLevel sl)
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(blockPosition().below())),
                    getX(), getY() + 0.2, getZ(), 20, 0.5, 0.2, 0.5, 0.08);
    }

    @Override public boolean isInvulnerableTo(DamageSource s) {
        return (burrowed() && !s.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) || super.isInvulnerableTo(s);
    }

    @Override public boolean isPushable() { return !burrowed() && super.isPushable(); }

    @Override public boolean doHurtTarget(Entity e) {
        boolean r = super.doHurtTarget(e);
        if (r && !level().isClientSide) {
            triggerAnim("attack", "attack");
            if (e instanceof LivingEntity le) switch (kind) {
                case DOC_VAN_XA, DOC_THIEM -> le.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
                case BANG_HON -> le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                case MOC_YEU -> { // troi re: dong cung gan nhu 2 giay
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 6));
                    if (level() instanceof ServerLevel sl)
                        sl.sendParticles(ParticleTypes.COMPOSTER, le.getX(), le.getY() + 0.2, le.getZ(), 12, 0.4, 0.1, 0.4, 0.02);
                }
                case QUAN_HON -> le.knockback(0.6, getX() - le.getX(), getZ() - le.getZ()); // thuong tran day lui
                default -> { }
            }
        }
        return r;
    }

    /** Doc Thiem: phun nuoc bot doc khi muc tieu cach 3-14 block. */
    private static final class SpitGoal extends Goal {
        private final LinhQuai m;
        private int cd = 40;

        SpitGoal(LinhQuai m) { this.m = m; setFlags(EnumSet.of(Flag.LOOK)); }

        @Override public boolean canUse() {
            LivingEntity t = m.getTarget();
            if (t == null || !t.isAlive()) return false;
            double d = m.distanceToSqr(t);
            return --cd <= 0 && d >= 9 && d <= 196 && m.getSensing().hasLineOfSight(t);
        }

        @Override public void start() {
            cd = 60;
            LivingEntity t = m.getTarget();
            m.triggerAnim("attack", "skill");
            PoisonSpit.launch(m, t, (float) HuHoa.skillDamage(m.huLevel(), 0.6), 2.0);
        }
    }

    @Override protected void dropCustomDeathLoot(ServerLevel sl, DamageSource src, boolean recent) {
        super.dropCustomDeathLoot(sl, src, recent);
        CreatureCommon.rollDrops(this, kind);
    }

    @Override public Component getDisplayName() { return HuHoa.name(huLevel(), getTypeName()); }
    @Override public boolean shouldShowName() { return !burrowed(); }
    @Override public boolean isSunBurnTick() { return false; }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(HuHoa.TAG_LEVEL, huLevel());
        tag.putBoolean("Burrowed", burrowed());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(HuHoa.TAG_LEVEL)) entityData.set(LEVEL, tag.getInt(HuHoa.TAG_LEVEL));
        if (tag.getBoolean("Burrowed")) { entityData.set(BURROWED, true); setInvisible(true); }
    }

    @Override public int huLevel() { return entityData.get(LEVEL); }
    @Override public void setHuLevel(int level) { entityData.set(LEVEL, Math.max(1, level)); }
    @Override public double huWeight() { return CreatureCommon.weight(kind); }
    @Override public float honTinhChance() { return CreatureCommon.honTinh(kind); }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) { CreatureCommon.controllers(this, kind.id, c); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
