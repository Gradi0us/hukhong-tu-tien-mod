package com.hukhong.entity;

import com.hukhong.HuKhong;
import com.hukhong.events.BossBuff;
import com.hukhong.events.BossScaling;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
 * Boss the gioi theo bioma (v3). Khung chung: 2 pha (pha 2 khi mau &lt; 50%, them ky nang + cuong hoa toc do),
 * windup -&gt; telegraph -&gt; fire, co gian theo so nguoi choi, bossbar, nhac, san dau truong BossArena (rao chan, giu dia hinh tu nhien).
 * Lop con chi khai bao bo ky nang (skillCount / windup / anim / cooldown / fire).
 */
public abstract class WorldBoss extends Monster implements GeoEntity, HuMob, com.hukhong.boss.phase.PhasedBoss {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(WorldBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(WorldBoss.class, EntityDataSerializers.INT);
    private static final ResourceLocation HP_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "wboss_players_hp");
    private static final ResourceLocation DMG_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "wboss_players_dmg");
    private static final ResourceLocation SPD_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "wboss_enrage_speed");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar;
    protected int skillCd = 80, windup, pending = -1, last = -1, appliedPlayers = 1;
    protected Vec3 pendingAt = Vec3.ZERO;
    private final com.hukhong.boss.phase.BossPhaseDirector director = new com.hukhong.boss.phase.BossPhaseDirector(this);

    protected WorldBoss(EntityType<? extends Monster> t, Level l, BossEvent.BossBarColor color) {
        super(t, l);
        bar = new ServerBossEvent(BossLevel.tag(Component.translatable(t.getDescriptionId()), 40), color, BossEvent.BossBarOverlay.NOTCHED_10);
        addTag(FlightBans.WORLD_BOSS_TAG);
        xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attrs(double speed) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 140).add(Attributes.ATTACK_DAMAGE, 6).add(Attributes.ARMOR, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9).add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    // ------------------------------------------------------------ kit (lop con)
    protected abstract String id();
    /** Id nhac (tai su dung cac track co san). */
    protected abstract String track();
    /** So ky nang dung duoc o pha {@code phase} (1..2). */
    protected abstract int skillCount(int phase);
    protected abstract int windupOf(int skill);
    /** Ten trigger animation ("skill_a", "skill_b", "skill_c", "attack"). */
    protected abstract String animOf(int skill);
    protected abstract int cooldown(int phase);
    protected abstract double radiusOf(int skill);
    protected abstract void fire(ServerLevel sl, int skill, LivingEntity target);
    protected double hpMul() { return 2.0; }
    protected double dmgMul() { return 0.6; }
    protected ParticleTypesHolder telegraphParticle() { return () -> ModParticles.HU_KHONG.get(); }

    /** Chon diem don (vd vi tri muc tieu) khi bat dau windup. */
    protected void onBegin(ServerLevel sl, int skill, LivingEntity t) { pendingAt = t.position(); }

    @FunctionalInterface
    protected interface ParticleTypesHolder { net.minecraft.core.particles.ParticleOptions get(); }

    // ------------------------------------------------------------ co ban
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PHASE, 1);
        b.define(LEVEL, 40);
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

    // ------------------------------------------------------------ PhasedBoss (3 pha: cutscene + biến đổi + chiêu mới)
    @Override public String phaseId() { return id(); }
    @Override public com.hukhong.boss.phase.BossPhaseDirector director() { return director; }
    @Override public int currentPhase() { return phase(); }
    @Override public void setCurrentPhase(int p) { entityData.set(PHASE, Math.max(1, Math.min(3, p))); }
    @Override public void applyPhase(ServerLevel sl, int ph) { applyPhaseEffects(ph); }
    @Override public boolean idleForMove() { return pending < 0; }
    @Override public void cancelSkill() { pending = -1; windup = 0; skillCd = 40; }
    @Override public void playMoveAnim(boolean ultimate) { triggerAnim("skill", ultimate ? "phase" : "skill_b"); }
    @Override public float moveDamage(double mult) { return skillDmg(mult); }

    @Override public boolean isImmobile() { return super.isImmobile() || (director != null && director.frozen()); }

    @Override public boolean hurt(DamageSource src, float amt) {
        if (director != null && director.invulnerable() && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(src, amt);
    }

    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && isAlive()) director.tick(sl);
    }

    @Override public int huLevel() { return entityData.get(LEVEL); }
    @Override public void setHuLevel(int level) { entityData.set(LEVEL, Math.max(1, level)); }
    @Override public double huWeight() { return 30.0; }
    @Override public float honTinhChance() { return 0f; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }
    @Override public Component getDisplayName() { return BossLevel.tag(super.getDisplayName(), huLevel()); }

    /** Dat cap + chi so. Goi ngay sau khi sinh (trong finalizeSpawn hoac thu cong). */
    public void initBoss(ServerLevel sl, int level) {
        entityData.set(LEVEL, level);
        HuHoa.applyStats(this, level, hpMul(), dmgMul());
        bar.setName(BossLevel.tag(Component.translatable(getType().getDescriptionId()), level));
        appliedPlayers = 0;
        rescale(sl);
        setHealth(getMaxHealth());
    }

    @Override public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor l,
            net.minecraft.world.DifficultyInstance d, net.minecraft.world.entity.MobSpawnType t, net.minecraft.world.entity.SpawnGroupData g) {
        net.minecraft.world.entity.SpawnGroupData r = super.finalizeSpawn(l, d, t, g);
        int lv = Math.max(20, HuHoa.levelFor(l, blockPosition(), getRandom()));
        entityData.set(LEVEL, lv);
        HuHoa.applyStats(this, lv, hpMul(), dmgMul());
        bar.setName(BossLevel.tag(Component.translatable(getType().getDescriptionId()), lv));
        return r;
    }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }
    @Override public void remove(RemovalReason r) { super.remove(r); bar.removeAllPlayers(); }

    @Override public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide) {
            com.hukhong.audio.BossMusic.watch(this, track());
            com.hukhong.arena.BossArena.attach(this, 24, null, false, null);
        }
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("WbPhase", phase());
        tag.putInt(HuHoa.TAG_LEVEL, huLevel());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("WbPhase")) entityData.set(PHASE, Math.max(1, Math.min(3, tag.getInt("WbPhase"))));
        if (tag.contains(HuHoa.TAG_LEVEL)) entityData.set(LEVEL, tag.getInt(HuHoa.TAG_LEVEL));
        bar.setName(BossLevel.tag(Component.translatable(getType().getDescriptionId()), huLevel()));
        applyPhaseEffects(phase());
    }

    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            HuHoa.particles(this);
            com.hukhong.boss.phase.PhaseClientFx.aura(this, this);
            if (phase() >= 2 && tickCount % 2 == 0)
                level().addParticle(ModParticles.HU_KHONG.get(), getRandomX(1.0), getRandomY(), getRandomZ(1.0), 0, 0.08, 0);
        }
    }

    // ------------------------------------------------------------ server logic
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel sl = (ServerLevel) level();
        bar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 100 == 1) rescale(sl);

        LivingEntity t = getTarget();
        if (pending >= 0) {
            getNavigation().stop();
            if (t != null) getLookControl().setLookAt(t, 30, 30);
            if (windup % 2 == 0) telegraph(sl, pending);
            if (--windup <= 0) {
                int sk = pending;
                pending = -1;
                LivingEntity tg = t != null && t.isAlive() ? t : null;
                fire(sl, sk, tg);
                skillCd = (int) Math.round(BossBuff.cd(cooldown(phase())) * (phase() >= 3 ? 0.8 : 1.0));
            }
            return;
        }
        if (skillCd > 0) { skillCd--; return; }
        if (t == null || !t.isAlive() || distanceToSqr(t) > 26 * 26) return;
        int n = Math.max(1, skillCount(phase()));
        int pick = random.nextInt(n);
        if (n > 1 && pick == last) pick = (pick + 1) % n;
        last = pick;
        begin(sl, pick, t);
    }

    /** Vao windup cua ky nang (cong khai de GameTest kich hoat). */
    public void begin(ServerLevel sl, int skill, LivingEntity t) {
        pending = skill;
        windup = windupOf(skill);
        onBegin(sl, skill, t);
        triggerAnim("skill", animOf(skill));
        playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 1.5f, 0.6f);
    }

    protected void telegraph(ServerLevel sl, int skill) {
        double r = radiusOf(skill);
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2 / 16;
            sl.sendParticles(telegraphParticle().get(), getX() + Math.cos(a) * r, getY() + 0.2, getZ() + Math.sin(a) * r, 1, 0.1, 0.05, 0.1, 0.0);
        }
    }

    // ------------------------------------------------------------ tien ich cho lop con
    protected float skillDmg(double mult) { return (float) HuHoa.skillDamage(huLevel(), mult); }

    /** Gay sat thuong dien rong quanh {@code c} cho nguoi choi; tra danh sach da trung. */
    protected java.util.List<Player> hitPlayers(ServerLevel sl, Vec3 c, double r, double mult, java.util.function.Consumer<Player> then) {
        java.util.List<Player> out = new java.util.ArrayList<>();
        for (Player p : sl.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(c, c).inflate(r, 4, r))) {
            if (p.isSpectator() || p.distanceToSqr(c.x, p.getY(), c.z) > r * r) continue;
            if (p.hurt(damageSources().mobAttack(this), skillDmg(mult))) out.add(p);
            if (then != null) then.accept(p);
        }
        return out;
    }

    protected void ringFx(ServerLevel sl, Vec3 c, double r, net.minecraft.core.particles.ParticleOptions pt) {
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48;
            sl.sendParticles(pt, c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    /** Huong ngang (don vi) ve phia muc tieu, hoac huong nhin neu khong con muc tieu. */
    protected Vec3 aim(LivingEntity t) {
        Vec3 d = t != null ? t.position().subtract(position()).multiply(1, 0, 1) : getViewVector(1f).multiply(1, 0, 1);
        return d.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : d.normalize();
    }

    /** Ban {@code n} nuoc bot xoe quat {@code spreadDeg} do ve phia muc tieu. */
    protected void spitFan(ServerLevel sl, LivingEntity t, int n, double spreadDeg, double mult, double aoe, boolean frost) {
        Vec3 d = aim(t);
        double base = Math.atan2(d.z, d.x);
        Vec3 from = position().add(0, getBbHeight() * 0.7, 0);
        for (int i = 0; i < n; i++) {
            double a = base + Math.toRadians(n == 1 ? 0 : (i - (n - 1) / 2.0) * spreadDeg / Math.max(1, n - 1) * 2);
            PoisonSpit sp = PoisonSpit.launchDir(this, from, new Vec3(Math.cos(a), 0.12, Math.sin(a)), skillDmg(mult), aoe, 1.2f);
            if (frost) sp.frost();
        }
    }

    protected int minions(ServerLevel sl) {
        return sl.getEntitiesOfClass(HuTrung.class, getBoundingBox().inflate(28)).size();
    }

    protected void summonSwarm(ServerLevel sl, LivingEntity t, int n) {
        for (int i = 0; i < n && minions(sl) < 10; i++) {
            HuTrung c = V3Entities.HU_TRUNG.get().create(sl);
            if (c == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            c.moveTo(getX() + Math.cos(a) * 3.5, getY(), getZ() + Math.sin(a) * 3.5, random.nextFloat() * 360f, 0f);
            c.finalizeSpawn(sl, sl.getCurrentDifficultyAt(c.blockPosition()), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);
            if (t != null) c.setTarget(t);
            sl.addFreshEntity(c);
        }
        sl.sendParticles(ModParticles.HU_KHONG.get(), getX(), getY() + 1, getZ(), 40, 2, 1, 2, 0.05);
    }

    private void applyPhaseEffects(int ph) {
        bar.setColor(ph == 1 ? bar.getColor() : ph == 2 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.PURPLE);
        AttributeInstance spd = getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) {
            spd.removeModifier(SPD_MOD);
            if (ph >= 2) spd.addPermanentModifier(new AttributeModifier(SPD_MOD, ph >= 3 ? 0.4 : 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

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
        if (level() instanceof ServerLevel sl)
            for (ServerPlayer p : sl.players())
                if (p.distanceToSqr(this) < 128 * 128) p.displayClientMessage(Component.translatable("hukhong.boss.slain"), false);
    }

    @Override protected void dropCustomDeathLoot(ServerLevel sl, DamageSource src, boolean recentlyHit) {
        super.dropCustomDeathLoot(sl, src, recentlyHit);
        com.hukhong.currency.Wallet.grantHonTinhBoss(sl, position(), src, 6 + random.nextInt(5));
        if (random.nextDouble() < com.hukhong.skillbook.SkillBookRewards.HU_VUONG_CHANCE)
            drop(sl, com.hukhong.skillbook.SkillBookRewards.stack(new java.util.Random(random.nextLong()),
                    com.hukhong.skillbook.SkillBookRewards.huVuongGrade(random.nextDouble()), com.hukhong.skillbook.SkillBookRewards.killerPaths(src)));
        var herb = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID,
                random.nextFloat() < 0.5f ? "huyet_tham" : "linh_thao"));
        if (herb != net.minecraft.world.item.Items.AIR) drop(sl, new ItemStack(herb, 1 + random.nextInt(3)));
    }

    private void drop(ServerLevel sl, ItemStack st) { sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.5, getZ(), st)); }

    // ------------------------------------------------------------ GeckoLib
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        String id = id();
        c.add(new AnimationController<>(this, "move", 4, s -> s.setAndContinue(s.isMoving()
                ? RawAnimation.begin().thenLoop("animation." + id + ".walk")
                : RawAnimation.begin().thenLoop("animation." + id + ".idle"))));
        AnimationController<WorldBoss> sk = new AnimationController<>(this, "skill", 0, s -> PlayState.STOP);
        for (String a : new String[]{"attack", "skill_a", "skill_b", "skill_c", "phase"})
            sk.triggerableAnim(a, RawAnimation.begin().then("animation." + id + "." + a, Animation.LoopType.PLAY_ONCE));
        c.add(sk);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
