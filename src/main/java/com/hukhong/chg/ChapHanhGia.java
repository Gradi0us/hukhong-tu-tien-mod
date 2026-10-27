package com.hukhong.chg;

import com.hukhong.HuKhong;
import com.hukhong.chg.ChgKit.Def;
import com.hukhong.chg.ChgKit.Skill;
import com.hukhong.entity.HuSkill;
import com.hukhong.entity.ModEntities;
import com.hukhong.events.BossScaling;
import com.hukhong.story.StoryData;
import com.hukhong.story.StoryRegistry;
import com.hukhong.story.StoryService;
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
import net.minecraft.world.entity.LightningBolt;
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
 * Chấp Hành Giả (Bước 20): 4 biến thể (EntityType chg_1..4), bộ kỹ năng + pha + nội tại pha 3 trong {@link ChgKit}.
 * Giáng lâm: rơi từ trời 60 tick (miễn thương), chạm đất có sét + sóng xung + title. Tag world boss = cấm bay.
 */
public class ChapHanhGia extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(ChapHanhGia.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DESCEND = SynchedEntityData.defineId(ChapHanhGia.class, EntityDataSerializers.INT);
    private static final ResourceLocation HP_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "chg_hp");
    private static final ResourceLocation DMG_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "chg_dmg");
    private static final ResourceLocation SPD_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "chg_speed");
    private static final ResourceLocation ARM_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "chg_armor");
    private static final BossEvent.BossBarColor[] COLORS = {BossEvent.BossBarColor.RED, BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarColor.PURPLE};
    private static final DustParticleOptions TELEGRAPH = new DustParticleOptions(new Vector3f(1f, 0.1f, 0.1f), 1.4f);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final int idx;
    private final Def def;
    private final ServerBossEvent bar;
    private int skillCd = 80, windup, pending = -1, last = -1, invulTicks, appliedPlayers = 1;
    private double landY;
    private Vec3 pendingAt = Vec3.ZERO;

    public ChapHanhGia(EntityType<? extends Monster> t, Level l) {
        super(t, l);
        int i = 1;
        for (int k = 1; k <= ChgKit.COUNT; k++) if (t == ChgRegistry.type(k)) i = k;
        idx = i;
        def = ChgKit.def(i);
        bar = new ServerBossEvent(Component.translatable("entity.hukhong.chg_" + i), COLORS[i - 1], BossEvent.BossBarOverlay.NOTCHED_10);
        addTag(com.hukhong.flight.FlightBans.WORLD_BOSS_TAG);
        xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attrs(int idx) {
        Def d = ChgKit.def(idx);
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, d.hp()).add(Attributes.ATTACK_DAMAGE, d.dmg()).add(Attributes.ARMOR, d.armor())
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.MOVEMENT_SPEED, d.speed())
                .add(Attributes.FOLLOW_RANGE, 56);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PHASE, 1);
        b.define(DESCEND, 0);
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

    public int idx() { return idx; }
    @Override public Component getDisplayName() { return com.hukhong.entity.BossLevel.tag(super.getDisplayName(), def.level()); }
    public int phase() { return entityData.get(PHASE); }
    public boolean descending() { return entityData.get(DESCEND) > 0; }

    @Override public boolean isPushable() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }
    @Override public boolean isImmobile() { return super.isImmobile() || descending() || windup > 0; }

    @Override public boolean hurt(DamageSource src, float amt) {
        if ((descending() || invulTicks > 0) && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(src, amt);
    }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }
    @Override public void remove(RemovalReason r) { super.remove(r); bar.removeAllPlayers(); }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ChgPhase", phase());
        tag.putInt("ChgDescend", entityData.get(DESCEND));
        tag.putDouble("ChgLandY", landY);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ChgPhase")) entityData.set(PHASE, Math.max(1, Math.min(3, tag.getInt("ChgPhase"))));
        entityData.set(DESCEND, tag.getInt("ChgDescend"));
        landY = tag.contains("ChgLandY") ? tag.getDouble("ChgLandY") : getY();
        applyPhaseEffects(phase());
        bar.setName(barName());
    }

    private Component barName() {
        return Component.translatable("hukhong.chg.bar", com.hukhong.entity.BossLevel.tag(Component.translatable("entity.hukhong.chg_" + idx), def.level()), phase());
    }

    // ------------------------------------------------------------ giáng lâm
    /** Gọi ngay sau khi spawn tại chân hạ cánh: nhấc lên cao, rơi xuống trong DESCENT_TICKS. */
    public void beginDescent() {
        landY = getY();
        entityData.set(DESCEND, ChgKit.DESCENT_TICKS);
        setNoGravity(true);
        setPos(getX(), landY + ChgKit.DESCENT_HEIGHT, getZ());
        bar.setName(barName());
        applyPhaseEffects(1);
    }

    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && descending()) descentTick(sl);
        else if (level() instanceof ServerLevel sl && isAlive() && windup > 0 && pending >= 0) stepWindup(sl);
        else if (level().isClientSide && descending()) {
            for (int i = 0; i < 3; i++)
                level().addParticle(ParticleTypes.END_ROD, getX() + random.nextGaussian() * 0.6, getY() + random.nextDouble() * 12,
                        getZ() + random.nextGaussian() * 0.6, 0, -0.4, 0);
        }
    }

    private void descentTick(ServerLevel sl) {
        int d = entityData.get(DESCEND) - 1;
        entityData.set(DESCEND, d);
        double frac = d / (double) ChgKit.DESCENT_TICKS;
        setDeltaMovement(Vec3.ZERO);
        setPos(getX(), landY + ChgKit.DESCENT_HEIGHT * frac * frac, getZ());
        bar.setProgress(1f);
        if (d == ChgKit.DESCENT_TICKS - 1) {
            for (ServerPlayer p : sl.players()) if (p.distanceToSqr(this) < 160 * 160)
                p.playNotifySound(SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.HOSTILE, 4f, 0.5f);
        }
        for (double y = landY; y < getY(); y += 2.5)  // cột sáng giáng lâm
            sl.sendParticles(ParticleTypes.END_ROD, getX(), y, getZ(), 1, 0.4, 0, 0.4, 0.0);
        if (d == 0) land(sl);
    }

    private void land(ServerLevel sl) {
        setNoGravity(false);
        setPos(getX(), landY, getZ());
        LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) { bolt.moveTo(getX(), landY, getZ()); bolt.setVisualOnly(true); sl.addFreshEntity(bolt); }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), landY + 1, getZ(), 1, 0, 0, 0, 0);
        for (int i = 0; i < 72; i++) {
            double a = i * Math.PI * 2 / 72;
            sl.sendParticles(ParticleTypes.END_ROD, getX() + Math.cos(a) * 8, landY + 0.3, getZ() + Math.sin(a) * 8, 1, 0.1, 0.1, 0.1, 0.05);
        }
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.5f);
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 3f, 0.6f);
        double dmg = getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5;
        for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(8, 3, 8))) {
            if (p.isSpectator()) continue;
            p.hurt(damageSources().mobAttack(this), (float) dmg);
            Vec3 away = p.position().subtract(position()).multiply(1, 0, 1).normalize();
            p.setDeltaMovement(p.getDeltaMovement().add(away.x * 1.4, 0.5, away.z * 1.4));
            p.hurtMarked = true;
        }
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) > 128 * 128) continue;
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("entity.hukhong.chg_" + idx)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("hukhong.chg.line." + idx)));
        }
    }

    // ------------------------------------------------------------ server AI
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel sl = (ServerLevel) level();
        if (descending()) { getNavigation().stop(); return; }
        bar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 100 == 1) rescale(sl);
        if (phase() >= 3 && def.passive() == ChgKit.Passive.REGEN && tickCount % 20 == 0)
            heal((float) (getMaxHealth() * ChgKit.REGEN_FRAC_PER_SEC));

        int ph = BossScaling.phase(getHealth() / getMaxHealth());
        if (ph > phase()) enterPhase(sl, ph);
        if (invulTicks > 0) {
            invulTicks--;
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 4, 1.0, 1.5, 1.0, 0.05);
            return;
        }
        if (phase() >= 2 && tickCount % 10 == 0)
            sl.sendParticles(phase() == 3 ? ParticleTypes.SOUL_FIRE_FLAME : ModParticles.HU_KHONG.get(),
                    getX(), getY() + getBbHeight() * 0.5, getZ(), 6, 1.2, 1.2, 1.2, 0.02);

        LivingEntity t = getTarget();
        if (pending >= 0) { stepWindup(sl); return; }
        if (skillCd > 0) { skillCd--; return; }
        if (t == null || !t.isAlive() || distanceToSqr(t) > 24 * 24) return;
        int n = ChgKit.skillsAvailable(def, phase());
        int pick = random.nextInt(n);
        if (n > 1 && pick == last) pick = (pick + 1) % n;
        Skill k = def.skills()[pick];
        if (k.shape() == ChgKit.Shape.SUMMON && minionCount(sl) >= 6) pick = 0;
        k = def.skills()[pick];
        last = pick;
        pending = pick;
        windup = k.windup();
        pendingAt = t.position();
        triggerAnim("skill", k.anim());
        playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 1.5f, 0.6f);
    }

    /** isImmobile() (windup > 0) làm LivingEntity.aiStep bỏ qua customServerAiStep => windup phải giảm ở tick(), không thì kẹt vĩnh viễn. */
    private void stepWindup(ServerLevel sl) {
        LivingEntity t = getTarget();
        getNavigation().stop();
        if (t != null) getLookControl().setLookAt(t, 30, 30);
        Skill k = def.skills()[pending];
        if (windup % 2 == 0) telegraph(sl, k);
        if (--windup <= 0) {
            fire(sl, k);
            pending = -1;
            skillCd = ChgKit.cooldown(def, phase());
        }
    }

    private int minionCount(ServerLevel sl) {
        return sl.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(24), e -> e != this && e instanceof com.hukhong.entity.HuMob).size();
    }

    private void telegraph(ServerLevel sl, Skill k) {
        switch (k.shape()) {
            case RAIN -> ring(sl, pendingAt, k.radius());
            case DASH -> ring(sl, pendingAt, k.radius());
            case SUMMON -> sl.sendParticles(ModParticles.HU_KHONG.get(), getX(), getY() + 0.2, getZ(), 8, 1.5, 0.1, 1.5, 0.0);
            default -> ring(sl, position(), k.radius());
        }
    }

    private void ring(ServerLevel sl, Vec3 c, double r) {
        int pts = (int) Math.min(48, 12 + r * 3);
        for (int i = 0; i < pts; i++) {
            double a = i * Math.PI * 2 / pts;
            sl.sendParticles(TELEGRAPH, c.x + Math.cos(a) * r, c.y + 0.15, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    private void hit(ServerLevel sl, Player p, double dmg, ChgKit.Fx fx, Vec3 from) {
        p.hurt(damageSources().mobAttack(this), (float) dmg);
        HuSkill.hu(p, 120);
        Vec3 away = p.position().subtract(from).multiply(1, 0, 1).normalize();
        switch (fx) {
            case KNOCK -> { p.setDeltaMovement(p.getDeltaMovement().add(away.x * 1.0, 0.7, away.z * 1.0)); p.hurtMarked = true; }
            case SLOW -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            case BLIND -> p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            case WEAK -> p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            case NAUSEA -> p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            default -> { }
        }
    }

    private void fire(ServerLevel sl, Skill k) {
        double dmg = getAttributeValue(Attributes.ATTACK_DAMAGE) * k.dmgMul();
        double r = k.radius();
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.7f);
        switch (k.shape()) {
            case SUMMON -> {
                for (int i = 0; i < ChgKit.summonCount(phase()); i++) {
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
            }
            case RAIN -> {
                LivingEntity t = getTarget();
                Vec3 c = t != null ? t.position() : pendingAt;
                for (int i = 0; i < ChgKit.rainStrikes(phase()); i++) {
                    double a = random.nextDouble() * Math.PI * 2, d = random.nextDouble() * r;
                    strike(sl, c.add(Math.cos(a) * d, 0, Math.sin(a) * d), 2.2, dmg, k.fx());
                }
                strike(sl, c, 2.2, dmg, k.fx());
            }
            case DASH -> {
                Vec3 to = pendingAt;
                Vec3 dir = to.subtract(position()).multiply(1, 0, 1);
                Vec3 stop = dir.lengthSqr() < 4 ? position() : to.subtract(dir.normalize().scale(1.5));
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1.5, getZ(), 6, 1, 0.5, 1, 0);
                teleportTo(stop.x, to.y, stop.z);
                burst(sl, position(), r, dmg, k.fx());
            }
            case HEAL -> {
                heal((float) (getMaxHealth() * 0.04));
                burst(sl, position(), r, dmg, k.fx());
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.5, getZ(), 30, 1.5, 1.5, 1.5, 0.1);
            }
            case DRAIN -> {
                int n = burst(sl, position(), r, dmg, k.fx());
                heal((float) (dmg * 0.5 * n) * com.hukhong.entity.HuHoa.ehpScale(this));
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 1.5, getZ(), 20, 1, 1, 1, 0.1);
            }
            default -> burst(sl, position(), r, dmg, k.fx());  // AOE
        }
    }

    /** AoE tròn; trả về số người trúng. */
    private int burst(ServerLevel sl, Vec3 c, double r, double dmg, ChgKit.Fx fx) {
        int n = 0;
        for (Player p : sl.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(c, c).inflate(r, 3, r))) {
            if (p.isSpectator() || p.position().distanceToSqr(c) > r * r) continue;
            hit(sl, p, dmg, fx, c);
            n++;
        }
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48;
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0.1, 0.1, 0.1, 0.0);
        }
        sl.sendParticles(ModParticles.HU_KHONG.get(), c.x, c.y + 0.5, c.z, 40, r * 0.4, 0.3, r * 0.4, 0.05);
        return n;
    }

    private void strike(ServerLevel sl, Vec3 c, double r, double dmg, ChgKit.Fx fx) {
        sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 2, 0.4, 0.2, 0.4, 0);
        sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 3, c.z, 12, 0.1, 3, 0.1, 0.0);
        for (Player p : sl.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(c, c).inflate(r, 3, r)))
            if (!p.isSpectator() && p.position().distanceToSqr(c) <= r * r) hit(sl, p, dmg, fx, c);
    }

    private void enterPhase(ServerLevel sl, int ph) {
        entityData.set(PHASE, ph);
        applyPhaseEffects(ph);
        bar.setName(barName());
        invulTicks = ChgKit.PHASE_INVUL_TICKS;
        pending = -1;
        windup = 0;
        skillCd = ChgKit.PHASE_INVUL_TICKS + 20;
        triggerAnim("skill", "phase");
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
        sl.sendParticles(ModParticles.HU_KHONG.get(), getX(), getY() + 1, getZ(), 80, 2.5, 1.5, 2.5, 0.1);
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 3f, 0.7f);
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(this) < 80 * 80) p.displayClientMessage(Component.translatable("hukhong.chg.phase", Component.translatable("entity.hukhong.chg_" + idx), ph), true);
    }

    private void applyPhaseEffects(int ph) {
        setModifier(Attributes.MOVEMENT_SPEED, SPD_MOD, ChgKit.speedModifier(ph));
        AttributeInstance arm = getAttribute(Attributes.ARMOR);
        if (arm != null) {
            arm.removeModifier(ARM_MOD);
            if (ph >= 3 && def.passive() == ChgKit.Passive.ARMOR)
                arm.addPermanentModifier(new AttributeModifier(ARM_MOD, ChgKit.ARMOR_BONUS, AttributeModifier.Operation.ADD_VALUE));
        }
        rescaleDamage(Math.max(1, appliedPlayers), ph);
    }

    private void rescaleDamage(int n, int ph) { setModifier(Attributes.ATTACK_DAMAGE, DMG_MOD, ChgKit.damageModifier(n, ph)); }

    private void rescale(ServerLevel sl) {
        int n = BossScaling.clamp(sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(64), p -> !p.isSpectator()).size());
        if (n == appliedPlayers) return;
        appliedPlayers = n;
        float frac = getHealth() / getMaxHealth();
        setModifier(Attributes.MAX_HEALTH, HP_MOD, ChgKit.hpModifier(n));
        rescaleDamage(n, phase());
        setHealth(Math.max(1f, getMaxHealth() * frac));
    }

    private void setModifier(Holder<Attribute> a, ResourceLocation id, double v) {
        AttributeInstance inst = getAttribute(a);
        if (inst == null) return;
        inst.removeModifier(id);
        inst.addPermanentModifier(new AttributeModifier(id, v, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    /** Sau khi spawn: tính sẵn theo số người chơi quanh đó, đầy máu. */
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
            if (phase() >= 3 && def.passive() == ChgKit.Passive.LEECH)
                heal((float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * ChgKit.LEECH_FRAC) * com.hukhong.entity.HuHoa.ehpScale(this));
        }
        return r;
    }

    // ------------------------------------------------------------ chết: cờ + rơi
    @Override public void die(DamageSource src) {
        super.die(src);
        if (!(level() instanceof ServerLevel sl)) return;
        int bit = ChgKit.defeatedBit(idx);
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) > 128 * 128) continue;
            StoryData d = StoryService.get(p);
            if ((d.lair() & bit) == 0) p.setData(StoryRegistry.STORY.get(), d.withLair(bit));
            StoryService.sync(p);
            p.displayClientMessage(Component.translatable("hukhong.chg.fall." + idx), false);
            p.displayClientMessage(Component.translatable("hukhong.chg.slain", Component.translatable("entity.hukhong.chg_" + idx)), false);
        }
    }

    @Override protected void dropCustomDeathLoot(ServerLevel sl, DamageSource src, boolean recentlyHit) {
        super.dropCustomDeathLoot(sl, src, recentlyHit);
        drop(sl, new ItemStack(ChgRegistry.CORES.get(idx - 1).get(), 1 + (random.nextFloat() < 0.3f ? 1 : 0)));
        drop(sl, new ItemStack(com.hukhong.item.ModItems.HON_TINH.get(), 12 + 4 * idx + random.nextInt(6)));
        drop(sl, com.hukhong.skillbook.SkillBookRewards.stack(new java.util.Random(random.nextLong()),
                com.hukhong.skillbook.SkillBookRewards.chgGrade(idx, random.nextDouble()), com.hukhong.skillbook.SkillBookRewards.killerPaths(src)));
    }

    private void drop(ServerLevel sl, ItemStack st) { sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.5, getZ(), st)); }

    // ------------------------------------------------------------ client + GeckoLib
    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide && !descending() && phase() >= 2 && tickCount % 2 == 0)
            level().addParticle(phase() == 3 ? ParticleTypes.SOUL_FIRE_FLAME : ModParticles.HU_KHONG.get(),
                    getRandomX(1.0), getRandomY(), getRandomZ(1.0), 0, 0.08, 0);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        String n = "animation.chg_" + idx + ".";
        c.add(new AnimationController<>(this, "move", 4, s -> s.setAndContinue(s.isMoving()
                ? RawAnimation.begin().thenLoop(n + "walk") : RawAnimation.begin().thenLoop(n + "idle"))));
        AnimationController<ChapHanhGia> sk = new AnimationController<>(this, "skill", 0, s -> PlayState.STOP);
        for (String a : new String[]{"attack", "skill_a", "skill_b", "skill_c", "phase"})
            sk.triggerableAnim(a, RawAnimation.begin().then(n + a, Animation.LoopType.PLAY_ONCE));
        c.add(sk);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
