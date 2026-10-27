package com.hukhong.story;

import com.hukhong.core.CultivationService;
import com.hukhong.entity.HuSkill;
import com.hukhong.vfx.ModParticles;
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
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

/**
 * Sứ Đồ Dạ Khuyết (Bước 19), mini-boss cấp <= 65. Ba biến thể (EntityType): 0 Dạ Kiếm (lao + cận chiến),
 * 1 Hư Chú Sư (phép Hư Hóa tầm xa), 2 Huyết Tế Tư (hồi máu đồng bọn). Rơi Văn Kiện theo bậc.
 */
public class SuDo extends Monster implements GeoEntity {
    public static final String[] IDS = {"su_do_kiem", "su_do_chu", "su_do_te"};
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(SuDo.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.hukhong.su_do"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private final int variant;
    private int skillCd = 60;

    public SuDo(EntityType<? extends Monster> t, Level l) {
        super(t, l);
        variant = t == StoryRegistry.SU_DO_CHU.get() ? 1 : t == StoryRegistry.SU_DO_TE.get() ? 2 : 0;
        xpReward = 0;
    }

    public static AttributeSupplier.Builder attrs() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60).add(Attributes.ATTACK_DAMAGE, 6).add(Attributes.ARMOR, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    public int variant() { return variant; }
    public int suLevel() { return entityData.get(LEVEL); }

    /** Đặt cấp (<=65): chỉ số tăng theo cấp, đầy máu. */
    public void setLevel(int lv) {
        entityData.set(LEVEL, lv);
        applyLevel();
        setHealth(getMaxHealth());
    }

    private void applyLevel() {
        int lv = suLevel();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(40 + lv * 6.0);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue((4 + lv * 0.25) * (variant == 0 ? 1.3 : 0.9));
        getAttribute(Attributes.ARMOR).setBaseValue(2 + lv * 0.2);
        setCustomName(com.hukhong.entity.BossLevel.tag(Component.translatable(getType().getDescriptionId()), lv));
        setCustomNameVisible(true);
        bar.setName(getCustomName());
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(LEVEL, 10);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }
    @Override public void remove(RemovalReason r) { super.remove(r); bar.removeAllPlayers(); }

    @Override public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide) { com.hukhong.audio.BossMusic.watch(this, "su_do"); com.hukhong.arena.BossArena.attach(this, 16, com.hukhong.arena.BossArena.Theme.SU_DO, false, null); }
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SuDoLevel", suLevel());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SuDoLevel")) { entityData.set(LEVEL, Math.max(1, Math.min(StoryService.MAX_LEVEL, tag.getInt("SuDoLevel")))); applyLevel(); }
        if (getCustomName() != null) bar.setName(getCustomName());
    }

    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide && tickCount % 4 == 0)
            level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getRandomX(0.5), getY() + 0.1, getRandomZ(0.5), 0, 0.02, 0);
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel sl = (ServerLevel) level();
        bar.setProgress(getHealth() / getMaxHealth());
        if (skillCd > 0) { skillCd--; return; }
        LivingEntity t = getTarget();
        if (variant == 2) { // Huyết Tế Tư: hồi máu bản thân + đồng bọn
            boolean hurt = false;
            for (SuDo s : sl.getEntitiesOfClass(SuDo.class, getBoundingBox().inflate(12), SuDo::isAlive)) {
                if (s.getHealth() < s.getMaxHealth()) hurt = true;
                s.heal(s.getMaxHealth() * 0.08f);
                sl.sendParticles(ParticleTypes.HEART, s.getX(), s.getY() + 1.5, s.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
            }
            if (hurt || t != null) { triggerAnim("skill", "cast"); playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1f, 0.8f); }
            skillCd = 100;
            return;
        }
        if (t == null || !t.isAlive()) return;
        double d2 = distanceToSqr(t);
        if (variant == 1 && d2 <= 16 * 16) { // Hư Chú Sư: phép tầm xa
            t.hurt(damageSources().indirectMagic(this, this), (float) (3 + suLevel() * 0.25));
            HuSkill.hu(t, 100);
            Vec3 a = position().add(0, 1.5, 0), b = t.position().add(0, 1.0, 0);
            for (int i = 0; i <= 10; i++) {
                Vec3 p = a.lerp(b, i / 10.0);
                sl.sendParticles(ModParticles.HU_KHONG.get(), p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
            }
            triggerAnim("skill", "cast");
            playSound(SoundEvents.EVOKER_CAST_SPELL, 1f, 0.8f);
            skillCd = 60;
        } else if (variant == 0 && d2 > 16 && d2 <= 100) { // Dạ Kiếm: lao tới
            Vec3 dir = t.position().subtract(position()).multiply(1, 0, 1).normalize();
            setDeltaMovement(dir.x * 0.9, 0.25, dir.z * 0.9);
            hurtMarked = true;
            triggerAnim("skill", "attack");
            skillCd = 80;
        }
    }

    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity e) {
        boolean r = super.doHurtTarget(e);
        if (r && !level().isClientSide) triggerAnim("skill", "attack");
        return r;
    }

    @Override public void die(DamageSource src) {
        super.die(src);
        if (level() instanceof ServerLevel && src.getEntity() instanceof ServerPlayer p)
            CultivationService.addExp(p, 8L * suLevel());
    }

    @Override protected void dropCustomDeathLoot(ServerLevel sl, DamageSource src, boolean recentlyHit) {
        super.dropCustomDeathLoot(sl, src, recentlyHit);
        if (!recentlyHit) return;
        if (random.nextDouble() < com.hukhong.skillbook.SkillBookRewards.suDoChance(suLevel()))
            sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.5, getZ(), com.hukhong.skillbook.SkillBookRewards.stack(
                    new java.util.Random(random.nextLong()), com.hukhong.skillbook.SkillBookRewards.suDoGrade(suLevel(), random.nextDouble()),
                    com.hukhong.skillbook.SkillBookRewards.killerPaths(src))));
        StoryDocs.Doc d = StoryDocs.roll(random, suLevel());
        if (d != null) {
            sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.5, getZ(), new ItemStack(StoryRegistry.DOC_ITEMS.get(d.id()).get())));
        }
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        String id = IDS[variant];
        c.add(new AnimationController<>(this, "move", 4, s -> s.setAndContinue(s.isMoving()
                ? RawAnimation.begin().thenLoop("animation." + id + ".walk")
                : RawAnimation.begin().thenLoop("animation." + id + ".idle"))));
        c.add(new AnimationController<>(this, "skill", 0, s -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().then("animation." + id + ".attack", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("cast", RawAnimation.begin().then("animation." + id + ".cast", Animation.LoopType.PLAY_ONCE)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
