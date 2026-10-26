package com.hukhong.endgame;

import com.hukhong.HuKhong;
import com.hukhong.endgame.EndgameKit.Skill;
import com.hukhong.entity.HuSkill;
import com.hukhong.entity.ModEntities;
import com.hukhong.events.BossScaling;
import com.hukhong.flight.FlightBans;
import com.hukhong.item.ModItems;
import com.hukhong.vfx.ModParticles;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Nguyên Thủy Tịch Diệt (Bước 25): boss cuối, 4 pha, 6 chiêu độc nhất (kéo, đập, tia Vòng Hở, xóa nét, triệu hồi, thiên thạch).
 * Pha 3: Dạ Khuyết xuất hiện. Có tag world boss (cấm bay). Scale theo số người chơi.
 */
public class NguyenThuyTichDiet extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(NguyenThuyTichDiet.class, EntityDataSerializers.INT);
    private static final ResourceLocation HP_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "ntd_hp");
    private static final ResourceLocation DMG_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "ntd_dmg");
    private static final ResourceLocation SPD_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "ntd_speed");
    private static final DustParticleOptions TELEGRAPH = new DustParticleOptions(new Vector3f(0.7f, 0.2f, 1f), 1.5f);
    private static final BossEvent.BossBarColor[] COLORS = {BossEvent.BossBarColor.PURPLE, BossEvent.BossBarColor.PINK,
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarColor.RED};

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.hukhong.nguyen_thuy_tich_diet"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_20);
    private int skillCd = 100, windup, pending = -1, last = -1, invulTicks, intro, appliedPlayers = 1;
    private boolean daKhuyetSpawned;
    private Vec3 pendingAt = Vec3.ZERO, pendingDir = new Vec3(0, 0, 1);

    public NguyenThuyTichDiet(EntityType<? extends Monster> t, Level l) {
        super(t, l);
        addTag(FlightBans.WORLD_BOSS_TAG);
        xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attrs() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24000).add(Attributes.ATTACK_DAMAGE, 60).add(Attributes.ARMOR, 30)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 64);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PHASE, 1);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int phase() { return entityData.get(PHASE); }

    @Override public boolean isPushable() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }
    @Override public boolean isImmobile() { return super.isImmobile() || windup > 0 || intro > 0; }

    @Override public boolean hurt(DamageSource src, float amt) {
        if ((intro > 0 || invulTicks > 0) && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(src, amt);
    }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }
    @Override public void remove(RemovalReason r) { super.remove(r); bar.removeAllPlayers(); }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("NtdPhase", phase());
        tag.putBoolean("NtdDak", daKhuyetSpawned);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NtdPhase")) entityData.set(PHASE, Math.max(1, Math.min(EndgameKit.PHASES, tag.getInt("NtdPhase"))));
        daKhuyetSpawned = tag.getBoolean("NtdDak");
        applyPhaseEffects(phase());
        bar.setName(barName());
    }

    private Component barName() {
        return Component.translatable("hukhong.endgame.boss.bar", Component.translatable("entity.hukhong.nguyen_thuy_tich_diet"), phase());
    }

    /** Gọi ngay sau khi spawn: scale theo người chơi, đầy máu, mở màn (miễn thương, title). */
    public void begin(ServerLevel sl) {
        appliedPlayers = 0;
        rescale(sl);
        setHealth(getMaxHealth());
        intro = EndgameKit.INTRO_TICKS;
        bar.setName(barName());
        applyPhaseEffects(1);
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) > 160 * 160) continue;
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("entity.hukhong.nguyen_thuy_tich_diet")));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("hukhong.endgame.boss.line.1")));
        }
    }

    // ------------------------------------------------------------ server AI
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel sl = (ServerLevel) level();
        bar.setProgress(getHealth() / getMaxHealth());
        if (intro > 0) {
            intro--;
            getNavigation().stop();
            sl.sendParticles(ModParticles.HU_KHONG.get(), getX(), getY() + 3, getZ(), 12, 2.5, 3, 2.5, 0.04);
            return;
        }
        if (tickCount % 100 == 1) rescale(sl);

        int ph = EndgameKit.phase(getHealth() / getMaxHealth());
        if (ph > phase()) enterPhase(sl, ph);
        if (invulTicks > 0) {
            invulTicks--;
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 3, getZ(), 6, 1.5, 3, 1.5, 0.05);
            return;
        }
        if (tickCount % 10 == 0)
            sl.sendParticles(phase() >= 3 ? ParticleTypes.SOUL_FIRE_FLAME : ModParticles.HU_KHONG.get(),
                    getX(), getY() + 3, getZ(), 8, 1.6, 2.5, 1.6, 0.02);

        LivingEntity t = getTarget();
        if (pending >= 0) {
            getNavigation().stop();
            if (t != null && EndgameKit.SKILLS[pending].shape() != EndgameKit.Shape.BEAM) getLookControl().setLookAt(t, 30, 30);
            Skill k = EndgameKit.SKILLS[pending];
            if (windup % 2 == 0) telegraph(sl, k);
            if (--windup <= 0) {
                fire(sl, k);
                pending = -1;
                skillCd = EndgameKit.cooldown(phase());
            }
            return;
        }
        if (skillCd > 0) { skillCd--; return; }
        if (t == null || !t.isAlive() || distanceToSqr(t) > 40 * 40) return;
        int n = EndgameKit.skillsAvailable(phase());
        int pick = random.nextInt(n);
        if (n > 1 && pick == last) pick = (pick + 1) % n;
        Skill k = EndgameKit.SKILLS[pick];
        if (k.shape() == EndgameKit.Shape.SUMMON && minionCount(sl) >= 8) pick = 1;
        k = EndgameKit.SKILLS[pick];
        last = pick;
        pending = pick;
        windup = k.windup();
        pendingAt = t.position();
        Vec3 d = t.position().subtract(position()).multiply(1, 0, 1);
        pendingDir = d.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : d.normalize();
        triggerAnim("skill", k.anim());
        playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2f, 0.5f);
    }

    private int minionCount(ServerLevel sl) {
        return sl.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(40), e -> e != this && e instanceof com.hukhong.entity.HuMob).size();
    }

    private void ring(ServerLevel sl, Vec3 c, double r) {
        int pts = (int) Math.min(64, 12 + r * 3);
        for (int i = 0; i < pts; i++) {
            double a = i * Math.PI * 2 / pts;
            sl.sendParticles(TELEGRAPH, c.x + Math.cos(a) * r, c.y + 0.15, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    private Vec3 beamDir(int i, int n) {
        double a = i * Math.PI * 2 / n;
        double cs = Math.cos(a), sn = Math.sin(a);
        return new Vec3(pendingDir.x * cs - pendingDir.z * sn, 0, pendingDir.x * sn + pendingDir.z * cs);
    }

    private void telegraph(ServerLevel sl, Skill k) {
        switch (k.shape()) {
            case BEAM -> {
                int n = EndgameKit.beamCount(phase());
                for (int b = 0; b < n; b++) {
                    Vec3 d = beamDir(b, n);
                    for (double s = 3; s < k.radius(); s += 2)
                        sl.sendParticles(TELEGRAPH, getX() + d.x * s, getY() + 0.2, getZ() + d.z * s, 1, 0.2, 0, 0.2, 0);
                }
            }
            case METEOR -> ring(sl, pendingAt, k.radius());
            case SUMMON -> sl.sendParticles(ModParticles.HU_KHONG.get(), getX(), getY() + 0.2, getZ(), 12, 3, 0.1, 3, 0.0);
            default -> ring(sl, position(), k.radius());
        }
    }

    private void hit(Player p, double dmg, Vec3 from, boolean knock) {
        p.hurt(damageSources().mobAttack(this), (float) dmg);
        HuSkill.hu(p, 140);
        if (knock) {
            Vec3 away = p.position().subtract(from).multiply(1, 0, 1).normalize();
            p.setDeltaMovement(p.getDeltaMovement().add(away.x * 1.1, 0.6, away.z * 1.1));
            p.hurtMarked = true;
        }
    }

    private void fire(ServerLevel sl, Skill k) {
        double dmg = getAttributeValue(Attributes.ATTACK_DAMAGE) * k.dmgMul();
        double r = k.radius();
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.5f);
        switch (k.shape()) {
            case PULL -> {  // "Trả lại": hút mọi người về tâm, sát thương khi sát bên
                for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(r, 6, r))) {
                    if (p.isSpectator() || p.distanceToSqr(this) > r * r) continue;
                    Vec3 to = position().subtract(p.position()).multiply(1, 0, 1).normalize();
                    p.setDeltaMovement(p.getDeltaMovement().add(to.x * 1.8, 0.3, to.z * 1.8));
                    p.hurtMarked = true;
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    if (p.distanceToSqr(this) < 7 * 7) hit(p, dmg, position(), false);
                }
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 2, getZ(), 120, r * 0.4, 2, r * 0.4, 0.4);
            }
            case SLAM -> {
                for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(r, 3, r)))
                    if (!p.isSpectator() && p.distanceToSqr(this) <= r * r) hit(p, dmg, position(), true);
                for (int i = 0; i < 60; i++) {
                    double a = i * Math.PI * 2 / 60;
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + Math.cos(a) * r, getY() + 0.2, getZ() + Math.sin(a) * r, 1, 0.1, 0.1, 0.1, 0);
                }
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.5, getZ(), 1, 0, 0, 0, 0);
            }
            case BEAM -> {  // "Vòng Hở": các tia đều nhau quét ra từ thân
                int n = EndgameKit.beamCount(phase());
                for (int b = 0; b < n; b++) {
                    Vec3 d = beamDir(b, n);
                    for (double s = 2; s < r; s += 1.0)
                        sl.sendParticles(ParticleTypes.END_ROD, getX() + d.x * s, getY() + 1.2, getZ() + d.z * s, 2, 0.15, 0.15, 0.15, 0.01);
                    for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(position(), position()).inflate(r, 4, r))) {
                        if (p.isSpectator()) continue;
                        Vec3 rel = p.position().subtract(position()).multiply(1, 0, 1);
                        double along = rel.dot(d);
                        double side = Math.abs(rel.x * d.z - rel.z * d.x);
                        if (along > 0 && along < r && side < 2.2) hit(p, dmg, position(), true);
                    }
                }
            }
            case ERASE -> {  // "Xóa nét": làm mù, yếu, mờ thị giác
                for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(r, 4, r))) {
                    if (p.isSpectator() || p.distanceToSqr(this) > r * r) continue;
                    hit(p, dmg, position(), false);
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
                }
                ring(sl, position(), r);
                sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 2, getZ(), 80, r * 0.3, 2, r * 0.3, 0.1);
            }
            case SUMMON -> {  // Hư Sinh
                for (int i = 0; i < EndgameKit.summonCount(phase()); i++) {
                    EntityType<? extends Mob> type = switch (random.nextInt(3)) {
                        case 0 -> ModEntities.HU_THI.get();
                        case 1 -> ModEntities.HU_COT.get();
                        default -> ModEntities.HU_CHU.get();
                    };
                    Mob m = type.create(sl);
                    if (m == null) continue;
                    double a = random.nextDouble() * Math.PI * 2;
                    m.moveTo(getX() + Math.cos(a) * 6, getY(), getZ() + Math.sin(a) * 6, random.nextFloat() * 360f, 0f);
                    m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    m.setTarget(getTarget());
                    sl.addFreshEntity(m);
                }
                sl.sendParticles(ModParticles.HU_KHONG.get(), getX(), getY() + 1, getZ(), 60, 4, 1, 4, 0.05);
            }
            case METEOR -> {  // "Tịch Diệt": mưa thiên thạch quanh mục tiêu
                LivingEntity t = getTarget();
                Vec3 c = t != null ? t.position() : pendingAt;
                for (int i = 0; i < EndgameKit.meteors(phase()); i++) {
                    double a = random.nextDouble() * Math.PI * 2, d = random.nextDouble() * r;
                    Vec3 at = c.add(Math.cos(a) * d, 0, Math.sin(a) * d);
                    sl.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, 2, 0.4, 0.2, 0.4, 0);
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 4, at.z, 20, 0.2, 4, 0.2, 0);
                    for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(at, at).inflate(2.6, 3, 2.6)))
                        if (!p.isSpectator() && p.position().distanceToSqr(at) <= 2.6 * 2.6) hit(p, dmg, at, true);
                }
            }
        }
    }

    private void enterPhase(ServerLevel sl, int ph) {
        entityData.set(PHASE, ph);
        applyPhaseEffects(ph);
        bar.setName(barName());
        invulTicks = EndgameKit.PHASE_INVUL_TICKS;
        pending = -1;
        windup = 0;
        skillCd = EndgameKit.PHASE_INVUL_TICKS + 20;
        triggerAnim("skill", "phase");
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 2, getZ(), 1, 0, 0, 0, 0);
        sl.sendParticles(ModParticles.HU_KHONG.get(), getX(), getY() + 2, getZ(), 120, 3, 2.5, 3, 0.12);
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 4f, 0.5f);
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) > 100 * 100) continue;
            p.displayClientMessage(Component.translatable("hukhong.endgame.boss.phase", ph), true);
            if (ph == 4) p.displayClientMessage(Component.translatable("hukhong.endgame.boss.line.4"), false);
        }
        if (ph >= EndgameKit.DA_KHUYET_PHASE && !daKhuyetSpawned) spawnDaKhuyet(sl);
    }

    private void spawnDaKhuyet(ServerLevel sl) {
        daKhuyetSpawned = true;
        DaKhuyet d = EndgameRegistry.DA_KHUYET.get().create(sl);
        if (d == null) return;
        d.moveTo(getX() + 9, getY(), getZ() + 3, 0f, 0f);
        d.finalizeSpawn(sl, sl.getCurrentDifficultyAt(d.blockPosition()), MobSpawnType.EVENT, null);
        d.begin(sl, this);
        sl.addFreshEntity(d);
    }

    private void applyPhaseEffects(int ph) {
        setModifier(Attributes.MOVEMENT_SPEED, SPD_MOD, EndgameKit.speedModifier(ph));
        setModifier(Attributes.ATTACK_DAMAGE, DMG_MOD, EndgameKit.damageModifier(Math.max(1, appliedPlayers), ph));
        bar.setColor(COLORS[EndgameKit.phaseIdx(ph)]);
    }

    private void rescale(ServerLevel sl) {
        int n = BossScaling.clamp(sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(80), p -> !p.isSpectator()).size());
        if (n == appliedPlayers) return;
        appliedPlayers = n;
        float frac = getHealth() / getMaxHealth();
        setModifier(Attributes.MAX_HEALTH, HP_MOD, BossScaling.hpMul(n) - 1);
        setModifier(Attributes.ATTACK_DAMAGE, DMG_MOD, EndgameKit.damageModifier(n, phase()));
        setHealth(Math.max(1f, getMaxHealth() * frac));
    }

    private void setModifier(Holder<Attribute> a, ResourceLocation id, double v) {
        AttributeInstance inst = getAttribute(a);
        if (inst == null) return;
        inst.removeModifier(id);
        inst.addPermanentModifier(new AttributeModifier(id, v, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    @Override public boolean doHurtTarget(Entity e) {
        boolean r = super.doHurtTarget(e);
        if (r && !level().isClientSide) {
            triggerAnim("skill", "attack");
            if (e instanceof LivingEntity le) HuSkill.hu(le, 120);
        }
        return r;
    }

    // ------------------------------------------------------------ chết
    @Override public void die(DamageSource src) {
        super.die(src);
        if (level() instanceof ServerLevel sl) EndgameService.onBossDefeated(sl, this);
    }

    @Override protected void dropCustomDeathLoot(ServerLevel sl, DamageSource src, boolean recentlyHit) {
        super.dropCustomDeathLoot(sl, src, recentlyHit);
        sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.5, getZ(), new ItemStack(ModItems.HON_TINH.get(), 40 + random.nextInt(16))));
    }

    // ------------------------------------------------------------ client + GeckoLib
    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide && tickCount % 2 == 0)
            level().addParticle(phase() >= 3 ? ParticleTypes.SOUL_FIRE_FLAME : ModParticles.HU_KHONG.get(),
                    getRandomX(1.4), getRandomY(), getRandomZ(1.4), 0, 0.08, 0);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        String n = "animation.nguyen_thuy_tich_diet.";
        c.add(new AnimationController<>(this, "move", 4, s -> s.setAndContinue(s.isMoving()
                ? RawAnimation.begin().thenLoop(n + "walk") : RawAnimation.begin().thenLoop(n + "idle"))));
        AnimationController<NguyenThuyTichDiet> sk = new AnimationController<>(this, "skill", 0, s -> PlayState.STOP);
        for (String a : new String[]{"attack", "skill_a", "skill_b", "skill_c", "phase"})
            sk.triggerableAnim(a, RawAnimation.begin().then(n + a, Animation.LoopType.PLAY_ONCE));
        c.add(sk);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
