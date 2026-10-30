package com.hukhong.zone.form;

import com.hukhong.HuKhong;
import com.hukhong.arena.BossArena;
import com.hukhong.audio.BossMusic;
import com.hukhong.boss.phase.BossPhaseConfig;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.boss.phase.PhaseClientFx;
import com.hukhong.boss.phase.PhaseMoves;
import com.hukhong.boss.phase.PhasedBoss;
import com.hukhong.entity.BossLevel;
import com.hukhong.entity.HuHoa;
import com.hukhong.entity.HuMob;
import com.hukhong.entity.HuSkill;
import com.hukhong.events.BossBuff;
import com.hukhong.events.BossScaling;
import com.hukhong.flight.FlightBans;
import com.hukhong.honcot.Bone;
import com.hukhong.honcot.HonCotRegistry;
import com.hukhong.vfx.ModParticles;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Hu Vuong nhieu dang (PLAN-v2 12.6): 1 loai thuc the, dang (0..7) dong bo qua SynchedEntityData -> renderer chon khung xuong/texture,
 * server chon bo chieu ({@link HvForm#skills(int)}) + chieu pha ({@code BossPhases "hvf_<dang>"}). Roi Hon Cot 100% cho moi nguoi gay >= 10% sat thuong.
 */
public class HuVuongForm extends Monster implements GeoEntity, HuMob, PhasedBoss {
    private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(HuVuongForm.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(HuVuongForm.class, EntityDataSerializers.INT);
    private static final ResourceLocation HP_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "hvf_players_hp");
    private static final ResourceLocation DMG_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "hvf_players_dmg");
    private static final ResourceLocation SPD_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "hvf_enrage_speed");

    /** Chi so goc (cung Hu Vuong mau). */
    public static final double HP = 9700, DMG = 14, ARMOR = 5;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.hukhong.hu_vuong_form"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private final BossPhaseDirector director = new BossPhaseDirector(this);
    private int formCache, level30 = 30, zoneId, zoneTier;
    private int skillCd = 80, appliedPlayers = 1;
    private HvSkill lastSkill;
    private HvSkills.Cast cast;
    private final List<HvSkills.Hazard> hazards = new ArrayList<>();
    private int dashLeft, mirrorTicks;
    private Vec3 dashDir = Vec3.ZERO;
    private final Set<UUID> dashHit = new HashSet<>();
    private boolean burrowing, bonesGiven;
    private double dashDmg;
    private HvSkill dashSkill;
    /** Sat thuong nguoi choi da gay (sau giap), theo UUID. */
    private final Map<UUID, Float> dealt = new LinkedHashMap<>();
    private final Map<UUID, ServerPlayer> dealers = new LinkedHashMap<>();
    private int fired;

    public HuVuongForm(EntityType<? extends Monster> t, Level l) {
        super(t, l);
        addTag(FlightBans.WORLD_BOSS_TAG);
        xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attrs() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HP).add(Attributes.ATTACK_DAMAGE, DMG).add(Attributes.ARMOR, ARMOR)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9).add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 48).add(Attributes.SCALE, 1.0);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(FORM, 0);
        b.define(PHASE, 1);
    }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> a) {
        super.onSyncedDataUpdated(a);
        if (FORM.equals(a)) { formCache = entityData.get(FORM); refreshDimensions(); }
    }

    @Override public EntityDimensions getDefaultDimensions(Pose pose) {
        HvForm.Skel s = HvForm.byId(formCache).skel;
        return EntityDimensions.scalable(s.w, s.h);
    }

    public HvForm form() { return HvForm.byId(formCache); }

    /** Dat dang (truoc khi them vao the gioi). */
    public void setForm(HvForm f) {
        formCache = f.ordinal();
        entityData.set(FORM, formCache);
        refreshDimensions();
    }

    public void setZoneInfo(int id, int level, int tier) { zoneId = id; level30 = level; zoneTier = tier; }

    public int zoneLevel() { return level30; }

    public int zoneTier() { return zoneTier; }

    public int phase() { return entityData.get(PHASE); }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ------------------------------------------------------------ PhasedBoss
    @Override public String phaseId() { return form().phaseId(); }
    @Override public BossPhaseDirector director() { return director; }
    @Override public int currentPhase() { return phase(); }
    @Override public void setCurrentPhase(int p) { entityData.set(PHASE, Math.max(1, Math.min(3, p))); }
    @Override public void applyPhase(ServerLevel sl, int ph) {
        bar.setColor(ph == 1 ? BossEvent.BossBarColor.PURPLE : ph == 2 ? BossEvent.BossBarColor.PINK : BossEvent.BossBarColor.RED);
        AttributeInstance spd = getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) {
            spd.removeModifier(SPD_MOD);
            if (ph == 3) spd.addPermanentModifier(new AttributeModifier(SPD_MOD, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if (ph > 1) triggerAnim("skill", "phase");
    }
    @Override public boolean idleForMove() { return cast == null && dashLeft == 0 && !burrowing; }
    @Override public void cancelSkill() {
        cast = null; dashLeft = 0; skillCd = 40; hazards.clear();
        setBurrowing(false);
    }
    @Override public void playMoveAnim(boolean ultimate) { triggerAnim("skill", ultimate ? "phase" : "cast"); }
    @Override public float moveDamage(double mult) { return (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * mult); }

    float dmg(double mult) { return (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * mult); }

    // ------------------------------------------------------------ trang thai chieu
    void startDash(Vec3 dir, HvSkill s) { dashDir = dir; dashLeft = 14; dashHit.clear(); dashDmg = s.dmg; dashSkill = s; }
    void setMirror(int t) { mirrorTicks = t; }
    void addHazard(HvSkills.Hazard h) { hazards.add(h); }
    void setBurrowing(boolean b) {
        burrowing = b;
        setInvisible(b);
        if (b && level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 30, getBbWidth() * 0.5, 0.2, getBbWidth() * 0.5, 0.1);
    }

    public boolean burrowing() { return burrowing; }

    @Override public boolean isImmobile() { return super.isImmobile() || (director != null && director.frozen()); }

    @Override public boolean isInvulnerableTo(DamageSource src) { return super.isInvulnerableTo(src) || (burrowing && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)); }

    @Override public boolean hurt(DamageSource src, float amt) {
        if (director != null && director.invulnerable() && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (burrowing && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (mirrorTicks > 0 && src.getEntity() instanceof LivingEntity a && a != this && !level().isClientSide)
            a.hurt(damageSources().thorns(this), amt * 0.3f);  // Guong Bang: phan 30% sat thuong
        return super.hurt(src, amt);
    }

    /** Ghi sat thuong nguoi choi (sau giap, khong qua mau con lai) de chia Hon Cot. */
    @Override protected void actuallyHurt(DamageSource src, float amount) {
        float before = getHealth();
        super.actuallyHurt(src, amount);
        if (src.getEntity() instanceof ServerPlayer p) recordDamage(p, Math.max(0, before - Math.max(0, getHealth())));
    }

    public void recordDamage(ServerPlayer p, float amt) {
        if (amt <= 0) return;
        dealt.merge(p.getUUID(), amt, Float::sum);
        dealers.put(p.getUUID(), p);
    }

    /** So chieu rieng da no (GameTest). */
    public int skillsFired() { return fired; }

    /** Chan AI tu chon chieu trong n tick (GameTest: chi chay chieu duoc goi). */
    public void holdSkills(int ticks) { skillCd = ticks; }

    public Map<UUID, Float> damageDealt() { return dealt; }

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

    @Override public void setCustomName(net.minecraft.network.chat.Component n) {
        super.setCustomName(n);
        bar.setName(getDisplayName());
    }

    @Override public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide) {
            bar.setName(getDisplayName());
            BossMusic.watch(this, "hu_vuong");
            BossArena.attach(this, 20, BossArena.Theme.HU_VUONG, true, null);
        }
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HvForm", formCache);
        tag.putInt("HvPhase", phase());
        tag.putInt("HvZone", zoneId);
        tag.putInt("HvLevel", level30);
        tag.putInt("HvTier", zoneTier);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HvForm")) { formCache = Math.max(0, Math.min(HvForm.COUNT - 1, tag.getInt("HvForm"))); entityData.set(FORM, formCache); refreshDimensions(); }
        if (tag.contains("HvPhase")) entityData.set(PHASE, Math.max(1, Math.min(3, tag.getInt("HvPhase"))));
        zoneId = tag.getInt("HvZone");
        if (tag.contains("HvLevel")) level30 = tag.getInt("HvLevel");
        zoneTier = tag.getInt("HvTier");
        if (level() instanceof ServerLevel sl) applyPhase(sl, phase());
        if (hasCustomName()) bar.setName(getDisplayName());
    }

    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            HuHoa.particles(this);
            PhaseClientFx.aura(this, this);
            if (tickCount % 3 == 0)
                level().addParticle(PhaseMoves.particle(form().pal), getRandomX(1.0), getRandomY(), getRandomZ(1.0), 0, 0.04, 0);
            if (phase() >= 2 && tickCount % 2 == 0)
                level().addParticle(phase() == 3 ? ParticleTypes.SOUL_FIRE_FLAME : ModParticles.HU_KHONG.get(), getRandomX(1.0), getRandomY(), getRandomZ(1.0), 0, 0.08, 0);
        }
    }

    // ------------------------------------------------------------ server logic
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel sl = (ServerLevel) level();
        bar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 100 == 1) rescale(sl);
        if (mirrorTicks > 0) mirrorTicks--;

        for (int i = hazards.size() - 1; i >= 0; i--) if (!HvSkills.tickHazard(sl, this, hazards.get(i))) hazards.remove(i);

        if (dashLeft > 0) {
            dashLeft--;
            setDeltaMovement(dashDir.x * 1.1, getDeltaMovement().y, dashDir.z * 1.1);
            hurtMarked = true;
            sl.sendParticles(PhaseMoves.particle(form().pal), getX(), getY() + 0.5, getZ(), 6, 1, 0.5, 1, 0.05);
            for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(1.2), q -> !q.isSpectator() && q.isAlive()))
                if (dashHit.add(p.getUUID())) {
                    p.hurt(damageSources().mobAttack(this), dmg(dashDmg));
                    PhaseMoves.applyHit(p, dashSkill.hit);
                    Vec3 away = new Vec3(dashDir.x, 0, dashDir.z);
                    p.setDeltaMovement(p.getDeltaMovement().add(away.x * 1.4, 0.5, away.z * 1.4));
                    p.hurtMarked = true;
                }
            return;
        }
        if (director.frozen()) return;

        LivingEntity t = getTarget();
        if (cast != null) {
            getNavigation().stop();
            if (t != null && !burrowing) getLookControl().setLookAt(t, 30, 30);
            HvSkills.tick(sl, this, cast);
            if (--cast.left <= 0) {
                HvSkills.Cast c = cast;
                cast = null;
                HvSkills.fire(sl, this, c);
                fired++;
                skillCd = BossBuff.cd(phase() == 1 ? 100 : phase() == 2 ? 80 : 60);
            }
            return;
        }
        if (skillCd > 0) { skillCd--; return; }
        if (t == null || !t.isAlive() || distanceToSqr(t) > 22 * 22) return;
        List<HvSkill> list = form().skills(phase());
        HvSkill pick = list.get(random.nextInt(list.size()));
        if (list.size() > 1 && pick == lastSkill) pick = list.get((list.indexOf(pick) + 1) % list.size());
        startSkill(sl, pick, t);
    }

    /** Thi trien mot chieu rieng ngay (AI, GameTest). */
    public void startSkill(ServerLevel sl, HvSkill s, LivingEntity target) {
        lastSkill = s;
        cast = HvSkills.begin(sl, this, s, target);
        triggerAnim("skill", switch (s.kind) {
            case DASH, CONE, SWALLOW, TELEPORT -> "attack";
            default -> "cast";
        });
    }

    public boolean casting() { return cast != null; }

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

    // ------------------------------------------------------------ chet -> Hon Cot 100%
    @Override public void die(DamageSource src) {
        super.die(src);
        if (level() instanceof ServerLevel sl) {
            director.reset();
            setBurrowing(false);
            grantBones(sl, src);
            for (ServerPlayer p : sl.players())
                if (p.distanceToSqr(this) < 128 * 128) p.displayClientMessage(Component.translatable("hukhong.boss.slain"), false);
        }
    }

    /** Moi nguoi gay >= 10% tong sat thuong nhan 1 Hon Cot rieng (vao tui, day xuong dat neu day); khong ai du dieu kien -> 1 vien roi tai cho. */
    public void grantBones(ServerLevel sl, DamageSource src) {
        if (bonesGiven) return;
        bonesGiven = true;
        HvForm f = form();
        int tier = HvDrops.tier(f, zoneTier), roll = HvDrops.roll(level30);
        double total = 0;
        for (float v : dealt.values()) total += v;
        List<ServerPlayer> winners = new ArrayList<>();
        for (Map.Entry<UUID, Float> e : dealt.entrySet()) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null) p = dealers.get(e.getKey());  // nguoi vua thoat/FakePlayer: dung ban cuoi con giu
            if (p != null && HvDrops.qualifies(e.getValue(), total)) winners.add(p);
        }
        Random r = new Random(random.nextLong());
        if (winners.isEmpty()) {
            ServerPlayer k = src != null && src.getEntity() instanceof ServerPlayer kp ? kp : null;
            if (k != null) winners.add(k);
        }
        if (winners.isEmpty()) {
            sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.5, getZ(), HonCotRegistry.stack(f.bone, tier, roll)));
            return;
        }
        for (ServerPlayer p : winners) {
            give(p, HonCotRegistry.stack(f.bone, tier, roll));
            Bone b2 = HvDrops.second(f, r);
            if (b2 != null) give(p, HonCotRegistry.stack(b2, tier, roll));
        }
    }

    private void give(ServerPlayer p, ItemStack st) {
        p.sendSystemMessage(Component.translatable("hukhong.hvf.bone", st.getHoverName()));
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    // ------------------------------------------------------------ HuMob
    @Override public int huLevel() { return level30; }
    @Override public void setHuLevel(int level) { level30 = level; }
    @Override public Component getDisplayName() { return BossLevel.tag(super.getDisplayName(), huLevel()); }
    @Override public double huWeight() { return 40.0; }
    @Override public float honTinhChance() { return 0f; }

    // ------------------------------------------------------------ GeckoLib (ten animation chung moi khung: animation.hvf.*)
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "move", 4, s -> s.setAndContinue(s.isMoving()
                ? RawAnimation.begin().thenLoop("animation.hvf.walk")
                : RawAnimation.begin().thenLoop("animation.hvf.idle"))));
        c.add(new AnimationController<>(this, "skill", 0, s -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().then("animation.hvf.attack", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("cast", RawAnimation.begin().then("animation.hvf.cast", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("phase", RawAnimation.begin().then("animation.hvf.phase", Animation.LoopType.PLAY_ONCE)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
