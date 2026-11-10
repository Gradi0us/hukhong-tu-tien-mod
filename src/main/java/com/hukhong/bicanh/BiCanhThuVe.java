package com.hukhong.bicanh;

import com.hukhong.boss.phase.BossPhaseConfig;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.boss.phase.PhasedBoss;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bí Cảnh Thủ Vệ — boss riêng của phòng boss Bí Cảnh (kiểu Tree Sentinel + Crucible Knight: kích + khiên tháp, đuôi giáp,
 * mũ sừng, cánh lửa ở pha 2). 3 pha qua {@link BossPhaseDirector}; bộ chiêu riêng {@link ThuVeMoves} (tv_*).
 * Khi rảnh: giơ khiên — chặn một phần đòn/đạn từ phía trước. Chỉ số theo {@link BiCanhTier} ({@link #setTier}).
 */
public class BiCanhThuVe extends Monster implements GeoEntity, PhasedBoss {
    public static final String ID = "bicanh_thu_ve";
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(BiCanhThuVe.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GUARD = SynchedEntityData.defineId(BiCanhThuVe.class, EntityDataSerializers.BOOLEAN);
    /** Hệ số chặn của khiên (phần sát thương còn lại): đạn / cận chiến. */
    public static final float BLOCK_PROJ = 0.25f, BLOCK_MELEE = 0.5f;

    private final BossPhaseDirector director = new BossPhaseDirector(this);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.hukhong." + ID),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
    private int skillCd = 50;
    int tier;

    public BiCanhThuVe(EntityType<? extends Monster> t, Level l) {
        super(t, l);
        xpReward = 80;
    }

    public static AttributeSupplier.Builder attrs() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 160).add(Attributes.ATTACK_DAMAGE, 9).add(Attributes.ARMOR, 10)
                .add(Attributes.ARMOR_TOUGHNESS, 4).add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.FOLLOW_RANGE, 40).add(Attributes.STEP_HEIGHT, 1.5);
    }

    /** Chỉ số gốc theo bậc Bí Cảnh (trước HuHoa.applyStats); bậc cao hồi tối thượng nhanh hơn, thêm cột sét. */
    public void setTier(BiCanhTier t) {
        tier = t.ordinal();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseHp(t));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseAtk(t));
        getAttribute(Attributes.ARMOR).setBaseValue(8 + 2 * tier);
        setHealth(getMaxHealth());
    }

    public static double baseHp(BiCanhTier t) { return 140 + 80 * t.ordinal(); }
    public static double baseAtk(BiCanhTier t) { return 5 + 2.5 * t.ordinal(); }
    public int tier() { return tier; }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PHASE, 1);
        b.define(GUARD, false);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 20f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }
    @Override public void remove(RemovalReason r) { super.remove(r); bar.removeAllPlayers(); }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TvPhase", currentPhase());
        tag.putInt("TvTier", tier);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("TvPhase")) entityData.set(PHASE, Math.max(1, Math.min(3, tag.getInt("TvPhase"))));
        tier = Math.max(0, Math.min(BiCanhTier.values().length - 1, tag.getInt("TvTier")));
    }

    public boolean guarding() { return entityData.get(GUARD); }

    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide && tickCount % 3 == 0) {
            level().addParticle(ParticleTypes.SMALL_FLAME, getRandomX(0.6), getY() + getBbHeight() * 0.85, getRandomZ(0.6), 0, 0.02, 0);
            if (currentPhase() >= 2)
                level().addParticle(ParticleTypes.FLAME, getX() - Math.sin(Math.toRadians(yBodyRot)) * -0.6, getY() + getBbHeight() * 0.7,
                        getZ() + Math.cos(Math.toRadians(yBodyRot)) * -0.6, 0, 0.05, 0);
        }
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        bar.setProgress(getHealth() / getMaxHealth());
        if (ultCd > 0) ultCd--;
        LivingEntity t = getTarget();
        entityData.set(GUARD, mv == null && t != null && !director.frozen() && skillCd > 10);
        if (mv != null || director.frozen()) return;
        if (skillCd > 0) { skillCd--; return; }
        if (t != null && t.isAlive() && distanceToSqr(t) < 24 * 24) {
            String id = ThuVeMoves.pick(random, currentPhase(), ultCd <= 0, distanceTo(t));
            startMove((ServerLevel) level(), id, t);
        }
    }

    // ================================================================ bộ chiêu riêng
    String mv;
    int mt, ultCd = 400;
    LivingEntity mTarget;
    Vec3 aim = new Vec3(0, 0, 1), spot, from, to;
    final Set<UUID> hitOnce = new HashSet<>();

    public String currentMove() { return mv; }
    public int moveTick() { return mt; }

    public boolean startMove(ServerLevel sl, String id, LivingEntity target) {
        if (mv != null || !isAlive() || !ThuVeMoves.handles(id)) return false;
        mv = id; mt = 0; mTarget = target; spot = null; from = null; to = null;
        hitOnce.clear();
        entityData.set(GUARD, false);
        getNavigation().stop();
        ThuVeMoves.begin(sl, this);
        return true;
    }

    void endMove() {
        if (mv == null) return;
        mv = null; mTarget = null;
        setNoGravity(false);
    }

    @Override public String phaseId() { return ID; }
    @Override public BossPhaseDirector director() { return director; }
    @Override public int currentPhase() { return entityData.get(PHASE); }
    @Override public void setCurrentPhase(int p) { entityData.set(PHASE, Math.max(1, Math.min(3, p))); }
    @Override public void applyPhase(ServerLevel sl, int ph) {
        bar.setColor(ph == 1 ? BossEvent.BossBarColor.YELLOW : ph == 2 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.PURPLE);
        if (director.transitioning()) ThuVeMoves.transformFx(sl, this, ph);
    }
    @Override public boolean startOwnMove(ServerLevel sl, BossPhaseConfig.Move m, LivingEntity target) {
        return ThuVeMoves.handles(m.id()) && startMove(sl, m.id(), target);
    }
    @Override public List<CamPath.Key> introCamera(int T) { return ThuVeMoves.introCam(T); }
    @Override public List<BossPhaseConfig.SubLine> introLines(int T) { return ThuVeMoves.introSubs(T); }
    @Override public boolean idleForMove() { return mv == null; }
    @Override public void cancelSkill() { endMove(); skillCd = 40; }
    @Override public void playMoveAnim(boolean ultimate) { triggerAnim("skill", ultimate ? "ult" : "attack"); }
    @Override public float moveDamage(double mult) { return (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * mult); }

    /** Đòn có hướng đến từ nửa trước (cone ~120 độ). */
    boolean frontal(DamageSource src) {
        Vec3 sp = src.getSourcePosition();
        if (sp == null) return false;
        Vec3 v = sp.subtract(position()).multiply(1, 0, 1);
        if (v.lengthSqr() < 1e-4) return false;
        double yaw = Math.toRadians(yBodyRot);
        Vec3 face = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        return v.normalize().dot(face) > 0.5;
    }

    /** Phần sát thương còn lại khi khiên chặn (1 = không chặn). Thuần logic, gametest gọi trực tiếp. */
    public float blockFactor(DamageSource src) {
        if (!guarding() || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || src.is(DamageTypeTags.BYPASSES_SHIELD) || !frontal(src)) return 1f;
        return src.is(DamageTypeTags.IS_PROJECTILE) ? BLOCK_PROJ : BLOCK_MELEE;
    }

    @Override public boolean hurt(DamageSource src, float amt) {
        if (director.invulnerable() && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        float k = blockFactor(src);
        if (k < 1f && level() instanceof ServerLevel sl) {
            amt *= k;
            playSound(SoundEvents.SHIELD_BLOCK, 1.4f, 0.7f);
            Vec3 f = getViewVector(1f).multiply(1, 0, 1).normalize().scale(1.1);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX() + f.x, getY() + getBbHeight() * 0.5, getZ() + f.z, 10, 0.3, 0.5, 0.3, 0.15);
        }
        return super.hurt(src, amt);
    }

    @Override public boolean isImmobile() { return super.isImmobile() || mv != null || director.frozen(); }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl) || !isAlive()) return;
        director.tick(sl);
        if (mv != null) {
            if (director.transitioning()) endMove();
            else if (!ThuVeMoves.step(sl, this, mt++)) { endMove(); skillCd = (int) Math.round(com.hukhong.events.BossBuff.cd(36) * cdMul()); }
        }
    }

    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity e) {
        boolean r = super.doHurtTarget(e);
        if (r && !level().isClientSide) triggerAnim("skill", "attack");
        return r;
    }

    @Override public void die(DamageSource src) {
        super.die(src);
        endMove();
        if (level() instanceof ServerLevel sl) ThuVeMoves.deathScene(sl, this);
    }

    @Override protected void tickDeath() {
        if (++deathTime >= 50 && !level().isClientSide && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        String p = "animation." + ID + ".";
        c.add(new AnimationController<>(this, "move", 5, s -> {
            if (isDeadOrDying()) return s.setAndContinue(RawAnimation.begin().then(p + "death", Animation.LoopType.HOLD_ON_LAST_FRAME));
            if (s.isMoving()) return s.setAndContinue(RawAnimation.begin().thenLoop(p + "walk"));
            return s.setAndContinue(RawAnimation.begin().thenLoop(p + (guarding() ? "guard" : "idle")));
        }));
        AnimationController<BiCanhThuVe> sk = new AnimationController<>(this, "skill", 2, s -> PlayState.STOP);
        for (String a : List.of("attack", "combo", "shield_bash", "tail", "charge", "stomp", "wings", "leap", "ult", "phase"))
            sk.triggerableAnim(a, RawAnimation.begin().then(p + a, Animation.LoopType.PLAY_ONCE));
        c.add(sk);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
