package com.hukhong.entity;

import com.hukhong.HuKhong;
import com.hukhong.events.BossScaling;
import com.hukhong.events.CorruptionManager;
import com.hukhong.flight.FlightBans;
import com.hukhong.item.ModItems;
import com.hukhong.vfx.ModParticles;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
 * Hu Vuong - boss the gioi (Buoc 15). 3 pha theo mau; cam bay quanh no (tag hukhong_world_boss).
 * Pha 1: Hu Chan (nen dat). Pha 2: + Trieu Hoi. Pha 3: + Hu Trieu (xung dien rong), nhanh hon.
 */
public class HuVuong extends Monster implements GeoEntity, HuMob, com.hukhong.boss.phase.PhasedBoss {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(HuVuong.class, EntityDataSerializers.INT);
    private static final int WINDUP = 30;
    private static final int SLAM = 1, SUMMON = 2, PULSE = 3, BARRAGE = 4;
    private static final ResourceLocation HP_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "boss_players_hp");
    private static final ResourceLocation DMG_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "boss_players_dmg");
    private static final ResourceLocation SPD_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "boss_enrage_speed");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar = new ServerBossEvent(BossLevel.tag(Component.translatable("entity.hukhong.hu_vuong"), 30),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int skillCd = 80, windup, pending, appliedPlayers = 1;
    private final com.hukhong.boss.phase.BossPhaseDirector director = new com.hukhong.boss.phase.BossPhaseDirector(this);

    public HuVuong(EntityType<? extends Monster> t, Level l) {
        super(t, l);
        addTag(FlightBans.WORLD_BOSS_TAG);
        xpReward = 0;
        setPersistenceRequired();
    }

    /** Chỉ số gốc (Bước 27: cân bằng; BalanceSimTest khóa TTK theo các hằng này). */
    public static final double HP = 9700, DMG = 14, ARMOR = 5;

    public static AttributeSupplier.Builder attrs() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HP).add(Attributes.ATTACK_DAMAGE, DMG).add(Attributes.ARMOR, ARMOR)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9).add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PHASE, 1);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int phase() { return entityData.get(PHASE); }

    // ------------------------------------------------------------ PhasedBoss (khung pha: cutscene + biến đổi + chiêu mới)
    @Override public String phaseId() { return "hu_vuong"; }
    @Override public com.hukhong.boss.phase.BossPhaseDirector director() { return director; }
    @Override public int currentPhase() { return phase(); }
    @Override public void setCurrentPhase(int p) { entityData.set(PHASE, Math.max(1, Math.min(3, p))); }
    @Override public void applyPhase(ServerLevel sl, int ph) { applyPhaseEffects(ph); }
    @Override public boolean idleForMove() { return pending == 0; }
    @Override public void cancelSkill() { pending = 0; windup = 0; skillCd = 40; }
    @Override public void playMoveAnim(boolean ultimate) { triggerAnim("skill", ultimate ? "roar" : "slam"); }
    @Override public float moveDamage(double mult) { return (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * mult); }

    @Override public boolean isImmobile() { return super.isImmobile() || (director != null && director.frozen()); }

    @Override public boolean hurt(DamageSource src, float amt) {
        if (director != null && director.invulnerable() && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(src, amt);
    }

    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && isAlive()) director.tick(sl);
    }

    @Override public boolean isPushable() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }

    @Override public void remove(RemovalReason r) { super.remove(r); bar.removeAllPlayers(); }

    @Override public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide) { com.hukhong.audio.BossMusic.watch(this, "hu_vuong"); com.hukhong.arena.BossArena.attach(this, 20, com.hukhong.arena.BossArena.Theme.HU_VUONG, true, null); }
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HuPhase", phase());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HuPhase")) entityData.set(PHASE, Math.max(1, Math.min(3, tag.getInt("HuPhase"))));
        applyPhaseEffects(phase());
    }

    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            HuHoa.particles(this);
            com.hukhong.boss.phase.PhaseClientFx.aura(this, this);
            if (phase() >= 2 && tickCount % 2 == 0)
                level().addParticle(phase() == 3 ? ParticleTypes.SOUL_FIRE_FLAME : ModParticles.HU_KHONG.get(),
                        getRandomX(1.0), getRandomY(), getRandomZ(1.0), 0, 0.08, 0);
        }
    }

    // ------------------------------------------------------------ server logic
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel sl = (ServerLevel) level();
        bar.setProgress(getHealth() / getMaxHealth());

        if (tickCount % 100 == 1) rescale(sl);

        if (phase() >= 2 && tickCount % 10 == 0)
            sl.sendParticles(phase() == 3 ? ParticleTypes.SOUL_FIRE_FLAME : ModParticles.HU_KHONG.get(),
                    getX(), getY() + getBbHeight() * 0.5, getZ(), 6, 1.2, 1.2, 1.2, 0.02);

        LivingEntity t = getTarget();
        if (pending != 0) {
            getNavigation().stop();
            if (t != null) getLookControl().setLookAt(t, 30, 30);
            if (windup % 2 == 0)
                sl.sendParticles(ModParticles.HU_KHONG.get(), getX(), getY() + 0.2, getZ(), 10, radius(pending) * 0.35, 0.1, radius(pending) * 0.35, 0.0);
            if (--windup <= 0) {
                fire(sl, pending);
                pending = 0;
                skillCd = com.hukhong.events.BossBuff.cd(phase() == 1 ? 100 : phase() == 2 ? 80 : 60);
            }
            return;
        }
        if (skillCd > 0) { skillCd--; return; }
        if (t == null || !t.isAlive() || distanceToSqr(t) > 20 * 20) return;
        int pick = switch (phase()) {
            case 1 -> SLAM;
            case 2 -> switch (random.nextInt(4)) { case 0 -> SUMMON; case 1 -> BARRAGE; default -> SLAM; };
            default -> switch (random.nextInt(4)) { case 0 -> SUMMON; case 1 -> PULSE; case 2 -> BARRAGE; default -> SLAM; };
        };
        if (pick == SUMMON && minionCount(sl) >= 6) pick = SLAM;
        pending = pick;
        windup = WINDUP;
        triggerAnim("skill", pick == SUMMON || pick == BARRAGE ? "summon" : "slam");
        playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 1.5f, 0.6f);
    }

    private int minionCount(ServerLevel sl) {
        return sl.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(24), e -> e != this && e instanceof HuMob).size();
    }

    private static double radius(int skill) { return skill == SLAM ? 7 : skill == PULSE ? 14 : 4; }

    private void fire(ServerLevel sl, int skill) {
        double base = getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (skill == SUMMON) {
            for (int i = 0; i < 3; i++) {
                EntityType<? extends Mob> type = random.nextBoolean() ? ModEntities.HU_THI.get() : ModEntities.HU_COT.get();
                Mob m = type.create(sl);
                if (m == null) continue;
                double a = random.nextDouble() * Math.PI * 2;
                m.moveTo(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3, random.nextFloat() * 360f, 0f);
                m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                m.setTarget(getTarget());
                sl.addFreshEntity(m);
            }
            sl.sendParticles(ModParticles.HU_KHONG.get(), getX(), getY() + 1, getZ(), 40, 2, 1, 2, 0.05);
            playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5f, 0.7f);
            return;
        }
        if (skill == BARRAGE) {  // Mua Nuoc Bot Doc (pha 2+): xoe 5 tia ve phia muc tieu + 4 vien ngau nhien
            LivingEntity tg = getTarget();
            Vec3 d = tg != null ? tg.position().subtract(position()).multiply(1, 0, 1) : getViewVector(1f).multiply(1, 0, 1);
            double a0 = Math.atan2(d.z, d.x);
            Vec3 from = position().add(0, getBbHeight() * 0.7, 0);
            for (int i = 0; i < 9; i++) {
                double a = i < 5 ? a0 + Math.toRadians((i - 2) * 14) : random.nextDouble() * Math.PI * 2;
                PoisonSpit.launchDir(this, from, new Vec3(Math.cos(a), 0.15, Math.sin(a)), (float) (base * 0.7), 2.8, 1.1f);
            }
            playSound(SoundEvents.SLIME_SQUISH, 2f, 0.5f);
            return;
        }
        double r = radius(skill);
        double dmg = base * (skill == SLAM ? 1.2 : 0.8);
        for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(r, 3, r))) {
            if (p.isSpectator() || p.distanceToSqr(this) > r * r) continue;
            p.hurt(damageSources().mobAttack(this), (float) dmg);
            HuSkill.hu(p, 160);
            Vec3 away = p.position().subtract(position()).multiply(1, 0, 1).normalize();
            if (skill == SLAM) p.setDeltaMovement(p.getDeltaMovement().add(0, 0.6, 0));
            else {
                p.setDeltaMovement(p.getDeltaMovement().add(away.x * 1.2, 0.3, away.z * 1.2));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            p.hurtMarked = true;
        }
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48;
            sl.sendParticles(skill == SLAM ? ParticleTypes.EXPLOSION : ParticleTypes.SOUL_FIRE_FLAME,
                    getX() + Math.cos(a) * r, getY() + 0.2, getZ() + Math.sin(a) * r, 1, 0.1, 0.1, 0.1, 0.0);
        }
        sl.sendParticles(ModParticles.HU_KHONG.get(), getX(), getY() + 0.5, getZ(), 40, r * 0.4, 0.3, r * 0.4, 0.05);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.0f, 0.7f);
    }

    private void applyPhaseEffects(int ph) {
        bar.setColor(ph == 1 ? BossEvent.BossBarColor.PURPLE : ph == 2 ? BossEvent.BossBarColor.PINK : BossEvent.BossBarColor.RED);
        AttributeInstance spd = getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) {
            spd.removeModifier(SPD_MOD);
            if (ph == 3) spd.addPermanentModifier(new AttributeModifier(SPD_MOD, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    /** Co gian HP/sat thuong theo so nguoi choi trong 64 block; giu nguyen ti le mau. */
    private void rescale(ServerLevel sl) {
        int n = BossScaling.clamp(sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(64), p -> !p.isSpectator()).size());
        if (n == appliedPlayers) return;
        appliedPlayers = n;
        float frac = getHealth() / getMaxHealth();
        setModifier(Attributes.MAX_HEALTH, HP_MOD, BossScaling.hpMul(n) - 1);
        setModifier(Attributes.ATTACK_DAMAGE, DMG_MOD, BossScaling.dmgMul(n) - 1);
        setHealth(Math.max(1f, getMaxHealth() * frac));
    }

    private void setModifier(Holder<Attribute> a, ResourceLocation id, double v) {
        AttributeInstance inst = getAttribute(a);
        if (inst == null) return;
        inst.removeModifier(id);
        inst.addPermanentModifier(new AttributeModifier(id, v, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    /** Goi ngay sau khi spawn: tinh san theo nguoi choi xung quanh, day mau. */
    public void initScaling(ServerLevel sl) {
        appliedPlayers = 0;
        rescale(sl);
        setHealth(getMaxHealth());
    }

    @Override public boolean doHurtTarget(Entity e) {
        boolean r = super.doHurtTarget(e);
        if (r && !level().isClientSide) {
            triggerAnim("skill", "attack");
            if (e instanceof LivingEntity le) HuSkill.hu(le, 100);
        }
        return r;
    }

    @Override public void die(DamageSource src) {
        super.die(src);
        if (level() instanceof ServerLevel sl) {
            CorruptionManager.onBossKilled(sl, blockPosition());
            for (ServerPlayer p : sl.players())
                if (p.distanceToSqr(this) < 128 * 128) p.displayClientMessage(Component.translatable("hukhong.boss.slain"), false);
        }
    }

    @Override protected void dropCustomDeathLoot(ServerLevel sl, DamageSource src, boolean recentlyHit) {
        super.dropCustomDeathLoot(sl, src, recentlyHit);
        com.hukhong.currency.Wallet.grantHonTinhBoss(sl, position(), src, 8 + random.nextInt(5));
        if (random.nextDouble() < com.hukhong.skillbook.SkillBookRewards.HU_VUONG_CHANCE)
            drop(sl, com.hukhong.skillbook.SkillBookRewards.stack(new java.util.Random(random.nextLong()),
                    com.hukhong.skillbook.SkillBookRewards.huVuongGrade(random.nextDouble()), com.hukhong.skillbook.SkillBookRewards.killerPaths(src)));
        var herb = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID,
                random.nextFloat() < 0.6f ? "huyet_tham" : "linh_thao"));
        if (herb != net.minecraft.world.item.Items.AIR) drop(sl, new ItemStack(herb, 1 + random.nextInt(2)));
    }

    private void drop(ServerLevel sl, ItemStack st) { sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.5, getZ(), st)); }

    // ------------------------------------------------------------ HuMob
    @Override public int huLevel() { return 30; }
    @Override public Component getDisplayName() { return BossLevel.tag(super.getDisplayName(), huLevel()); }
    @Override public double huWeight() { return 40.0; }
    @Override public float honTinhChance() { return 0f; }

    // ------------------------------------------------------------ GeckoLib
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "move", 4, s -> s.setAndContinue(s.isMoving()
                ? RawAnimation.begin().thenLoop("animation.hu_vuong.walk")
                : RawAnimation.begin().thenLoop("animation.hu_vuong.idle"))));
        c.add(new AnimationController<>(this, "skill", 0, s -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().then("animation.hu_vuong.attack", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("slam", RawAnimation.begin().then("animation.hu_vuong.slam", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("summon", RawAnimation.begin().then("animation.hu_vuong.summon", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("roar", RawAnimation.begin().then("animation.hu_vuong.roar", Animation.LoopType.PLAY_ONCE)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
