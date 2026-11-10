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
public class SuDo extends Monster implements GeoEntity, com.hukhong.boss.phase.PhasedBoss {
    public static final String[] IDS = {"su_do_kiem", "su_do_chu", "su_do_te"};
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(SuDo.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(SuDo.class, EntityDataSerializers.INT);
    private final com.hukhong.boss.phase.BossPhaseDirector director = new com.hukhong.boss.phase.BossPhaseDirector(this);

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
        b.define(PHASE, 1);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false) { // có BossBrain: brain lo áp sát + đòn thường
            @Override public boolean canUse() { return brainProfile() == null && super.canUse(); }
            @Override public boolean canContinueToUse() { return brainProfile() == null && super.canContinueToUse(); }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override public boolean canUse() { return (brainProfile() == null || getTarget() == null) && super.canUse(); }
        });
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
        if (!level().isClientSide && !level().getEntitiesOfClass(SuDo.class, getBoundingBox().inflate(32), e -> e != this).isEmpty())
            addTag(com.hukhong.boss.phase.BossPhaseDirector.INTRO_TAG); // nhóm Sứ Đồ: chỉ một cutscene giới thiệu
        if (!level().isClientSide) { com.hukhong.audio.BossMusic.watch(this, "su_do"); com.hukhong.arena.BossArena.attach(this, 16, com.hukhong.arena.BossArena.Theme.SU_DO, false, null); }
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SuDoLevel", suLevel());
        tag.putInt("SuDoPhase", currentPhase());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SuDoLevel")) { entityData.set(LEVEL, Math.max(1, Math.min(StoryService.MAX_LEVEL, tag.getInt("SuDoLevel")))); applyLevel(); }
        if (getCustomName() != null) bar.setName(getCustomName());
        if (tag.contains("SuDoPhase")) entityData.set(PHASE, Math.max(1, Math.min(3, tag.getInt("SuDoPhase"))));
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
        if (ultCd > 0) ultCd--;
        if (mv != null || director.frozen()) return;
        // BossBrain lo di chuyển + đòn thường + chiêu Margit (brainBigMove). Dạ Kiếm không còn gì khác; Chú Sư / Tế Tư vẫn niệm
        // phép riêng (tức thời) nhưng không chen giữa chuỗi đòn / né của brain.
        boolean brain = brainProfile() != null;
        if (brain && (variant == 0 || com.hukhong.boss.ai.BossBrain.busy(this) || tauntT > 0)) return;
        if (skillCd > 0) { skillCd--; return; }
        LivingEntity t = getTarget();
        if (!brain && t != null && t.isAlive() && distanceToSqr(t) < 20 * 20 && random.nextInt(3) > 0) { // bộ chiêu Margit (SuDoMoves)
            String id = SuDoMoves.pick(random, currentPhase(), ultCd <= 0, distanceTo(t));
            if (startMove(sl, id, t)) return;
        }
        if (variant == 2) { // Huyết Tế Tư: hồi máu bản thân + đồng bọn
            boolean hurt = false;
            for (SuDo s : sl.getEntitiesOfClass(SuDo.class, getBoundingBox().inflate(12), SuDo::isAlive)) {
                if (s.getHealth() < s.getMaxHealth()) hurt = true;
                s.heal(s.getMaxHealth() * 0.08f);
                sl.sendParticles(ParticleTypes.HEART, s.getX(), s.getY() + 1.5, s.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
            }
            if (hurt || t != null) { triggerAnim("skill", "cast"); playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1f, 0.8f); }
            skillCd = com.hukhong.events.BossBuff.cd(100);
            if (phase2() && t != null) swarm(sl, t);
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
            skillCd = com.hukhong.events.BossBuff.cd(60);
            if (phase2()) spitFan(sl, t);
        } else if (variant == 0 && d2 > 16 && d2 <= 100) { // Dạ Kiếm: lao tới
            Vec3 dir = t.position().subtract(position()).multiply(1, 0, 1).normalize();
            setDeltaMovement(dir.x * 0.9, 0.25, dir.z * 0.9);
            hurtMarked = true;
            triggerAnim("skill", "attack");
            skillCd = com.hukhong.events.BossBuff.cd(80);
        } else if (variant == 0 && phase2() && d2 <= 36) { // Da Kiem pha 2: Kiem Khi Vong (soc dia r=5)
            for (net.minecraft.world.entity.player.Player pl : sl.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, getBoundingBox().inflate(5, 2, 5))) {
                if (pl.isSpectator()) continue;
                pl.hurt(damageSources().mobAttack(this), (float) (4 + suLevel() * 0.25));
                pl.setDeltaMovement(pl.getDeltaMovement().add(0, 0.5, 0));
                pl.hurtMarked = true;
            }
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.3, getZ(), 40, 2.5, 0.3, 2.5, 0.05);
            triggerAnim("skill", "attack");
            skillCd = com.hukhong.events.BossBuff.cd(90);
        }
    }

    /** Pha 2: duoi 50% mau -> ky nang moi (Da Kiem: Kiem Khi Vong; Chu Su: xoe nuoc bot doc; Te Tu: goi Hu Trung). */
    public boolean phase2() { return getHealth() < getMaxHealth() * 0.5f; }

    private void spitFan(ServerLevel sl, LivingEntity t) {
        Vec3 d = t.position().subtract(position()).multiply(1, 0, 1);
        double a0 = Math.atan2(d.z, d.x);
        for (int i = -1; i <= 1; i++)
            com.hukhong.entity.PoisonSpit.launchDir(this, position().add(0, 1.6, 0), new Vec3(Math.cos(a0 + i * 0.25), 0.12, Math.sin(a0 + i * 0.25)),
                    (float) (3 + suLevel() * 0.25), 2.5, 1.2f);
    }

    private void swarm(ServerLevel sl, LivingEntity t) {
        for (int i = 0; i < 2; i++) {
            com.hukhong.entity.HuTrung c = com.hukhong.entity.V3Entities.HU_TRUNG.get().create(sl);
            if (c == null) continue;
            c.moveTo(getX() + (i == 0 ? 2 : -2), getY(), getZ(), random.nextFloat() * 360f, 0f);
            c.finalizeSpawn(sl, sl.getCurrentDifficultyAt(c.blockPosition()), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);
            c.setTarget(t);
            sl.addFreshEntity(c);
        }
    }

    // ================================================================ PhasedBoss + bộ chiêu riêng kiểu Margit (SuDoMoves)
    String mv;
    int mt, ultCd = 300;
    LivingEntity mTarget;
    Vec3 aim = new Vec3(0, 0, 1), spot, from, to;
    final java.util.Set<java.util.UUID> hitOnce = new java.util.HashSet<>();

    public String currentMove() { return mv; }
    public int moveTick() { return mt; }

    public boolean startMove(ServerLevel sl, String id, LivingEntity target) {
        if (mv != null || !isAlive() || !SuDoMoves.handles(id)) return false;
        mv = id; mt = 0; mTarget = target; spot = null; from = null; to = null;
        hitOnce.clear();
        getNavigation().stop();
        SuDoMoves.begin(sl, this);
        return true;
    }

    void endMove() {
        if (mv == null) return;
        mv = null; mTarget = null;
        setNoGravity(false);
    }

    @Override public String phaseId() { return "su_do"; }
    @Override public com.hukhong.boss.phase.BossPhaseDirector director() { return director; }
    @Override public int currentPhase() { return entityData.get(PHASE); }
    @Override public void setCurrentPhase(int p) { entityData.set(PHASE, Math.max(1, Math.min(3, p))); }
    @Override public void applyPhase(ServerLevel sl, int ph) {
        bar.setColor(ph == 1 ? BossEvent.BossBarColor.PURPLE : ph == 2 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.RED);
        if (director.transitioning()) SuDoMoves.transformFx(sl, this, ph);
    }
    @Override public boolean startOwnMove(ServerLevel sl, com.hukhong.boss.phase.BossPhaseConfig.Move m, LivingEntity target) {
        return SuDoMoves.handles(m.id()) && startMove(sl, m.id(), target);
    }
    @Override public java.util.List<com.hukhong.boss.phase.CamPath.Key> introCamera(int T) { return SuDoMoves.introCam(T); }
    @Override public java.util.List<com.hukhong.boss.phase.BossPhaseConfig.SubLine> introLines(int T) { return SuDoMoves.introSubs(T); }
    @Override public boolean idleForMove() { return mv == null; }
    /** Brain nhường quyền khi đang chiêu riêng hoặc đang đứng khiêu khích (khoảng nghỉ lừa nhịp). */
    @Override public boolean ownMoveActive() { return mv != null || tauntT > 0; }

    /**
     * Hồ sơ BossBrain (PLAN-v1 §4, kiểu Margit) cho cả 3 biến thể: đi vòng 3-6 ô, thỉnh thoảng đứng khiêu khích; né = lùi / nhảy
     * lùi + ném dao; áp sát = bước gậy dài; trừng phạt hồi máu = ném dao ngay rồi Đâm Lướt.
     * Trượng: nhanh - TRỄ - nhanh - TRỄ (nhát cuối triệu búa: nặng, không đỡ được). Sau nhát 1: người chơi lùi > 2.5 ô -> Đâm Lướt;
     * đi ngang > 50° -> Quét Ngang.
     */
    static final com.hukhong.boss.ai.BossProfile BRAIN = com.hukhong.boss.ai.BossProfile.builder()
            .phase(1, new com.hukhong.boss.ai.BossProfile.PhaseVals(3, 6, 0.18, 16f, 0.25, 50, 26, 40))
            .phase(2, new com.hukhong.boss.ai.BossProfile.PhaseVals(3, 6, 0.2, 18f, 0.35, 40, 20, 32))
            .phase(3, new com.hukhong.boss.ai.BossProfile.PhaseVals(2.5, 5.5, 0.22, 20f, 0.4, 35, 14, 26))
            .weight(1.0).accel(5, 4).moveSpeed(0.26).delayExtra(12)
            .gapClose(com.hukhong.boss.ai.GapClose.DASH, 9)
            .combo(new com.hukhong.boss.ai.BossProfile.Combo("truong", java.util.List.of(
                    com.hukhong.boss.ai.BossProfile.hit(9, 3, 5, 2.8, 110, 0.8, 0.8),
                    new com.hukhong.boss.ai.BossProfile.Hit(10, 4, 6, 3.0, 120, 1.0, 1.0, 0, true, false, false),
                    com.hukhong.boss.ai.BossProfile.hit(7, 3, 5, 2.8, 110, 0.8, 0.6),
                    new com.hukhong.boss.ai.BossProfile.Hit(12, 5, 16, 3.4, 140, 1.5, 1.2, 0.5, true, true, true)),
                    0, 3.8, 3, 1, java.util.List.of(
                            new com.hukhong.boss.ai.BossProfile.Branch(0, com.hukhong.boss.ai.BossProfile.Cond.RETREAT, 2.5, "dam"),
                            new com.hukhong.boss.ai.BossProfile.Branch(0, com.hukhong.boss.ai.BossProfile.Cond.STRAFE, 50, "quet")), false))
            .combo(new com.hukhong.boss.ai.BossProfile.Combo("dam", java.util.List.of(
                    new com.hukhong.boss.ai.BossProfile.Hit(12, 4, 8, 2.5, 50, 1.1, 3.5, 0, false, false, false),
                    new com.hukhong.boss.ai.BossProfile.Hit(8, 4, 16, 2.8, 60, 1.3, 2.0, 0.6, false, true, true)),
                    3, 8, 2, 1, java.util.List.of(), false))
            .combo(new com.hukhong.boss.ai.BossProfile.Combo("quet", java.util.List.of(
                    new com.hukhong.boss.ai.BossProfile.Hit(9, 4, 14, 3.4, 270, 1.1, 0.4, 0, false, true, false)),
                    0, 4, 0, 1, java.util.List.of(), true))
            .punishHeal("dam").bigMoveEvery(2)
            .build();

    @Override public com.hukhong.boss.ai.BossProfile brainProfile() { return BRAIN; }

    /** Animation brain có trong cả 3 file su_do_*.animation.json (tools/gen_b5_brain_anims.py). */
    static final java.util.Set<String> BRAIN_ANIMS = java.util.Set.of("strafe_l", "strafe_r", "backstep", "sidestep_l", "sidestep_r",
            "backflip", "dash", "combo_truong_1", "combo_truong_2", "combo_truong_3", "combo_truong_4", "combo_dam_1", "combo_dam_2", "combo_quet_1");

    // ---- phản ứng riêng nối vào brain: khiêu khích, ném dao sau nhảy lùi, ném dao trừng phạt hồi máu
    int tauntT, tauntCd = 160, daggerIn = -1, seenEvades, seenReacts;
    public int daggersThrown, taunts;

    private void brainReactions(ServerLevel sl) {
        if (tauntCd > 0) tauntCd--;
        if (tauntT > 0) { tauntT--; getNavigation().stop(); }
        com.hukhong.boss.ai.BossBrain b = com.hukhong.boss.ai.BossBrain.get(this);
        LivingEntity t = getTarget();
        if (b == null || t == null || !t.isAlive()) { daggerIn = -1; return; }
        if (b.evades > seenEvades) {
            seenEvades = b.evades;
            com.hukhong.boss.ai.BossProfile.Evade k = b.lastEvade();
            if (k == com.hukhong.boss.ai.BossProfile.Evade.BACKFLIP || k == com.hukhong.boss.ai.BossProfile.Evade.BACKSTEP) daggerIn = 5;
        }
        if (daggerIn > 0 && --daggerIn == 0) { daggerIn = -1; SuDoMoves.quickDagger(sl, this, t); }
        if (b.reacts > seenReacts) { // trừng phạt hồi máu: phi đao ngay, brain nối Đâm Lướt
            seenReacts = b.reacts;
            if (b.state() == com.hukhong.boss.ai.BossBrain.State.REACT) SuDoMoves.quickDagger(sl, this, t);
        }
        // khoảng nghỉ khiêu khích: chỉ khi đang đi vòng ở tầm trung
        if (tauntT == 0 && tauntCd == 0 && mv == null && b.state() == com.hukhong.boss.ai.BossBrain.State.STRAFE
                && distanceToSqr(t) > 9 && random.nextInt(60) == 0) startTaunt(sl);
    }

    /** Đứng khiêu khích 1.5 giây (brain nhường quyền) — cửa sổ phản công. Public cho gametest. */
    public void startTaunt(ServerLevel sl) {
        tauntT = 30; tauntCd = 200 + random.nextInt(120); taunts++;
        triggerAnim("skill", "taunt");
        SuDoMoves.tauntLine(sl, this);
        playSound(SoundEvents.EVOKER_CELEBRATE, 1.2f, 0.8f);
    }
    public int tauntTicks() { return tauntT; }

    @Override public boolean brainBigMove(ServerLevel sl, LivingEntity t) {
        String id = SuDoMoves.pick(random, currentPhase(), ultCd <= 0, distanceTo(t));
        return id != null && startMove(sl, id, t);
    }
    @Override public void cancelSkill() { endMove(); skillCd = 40; }
    @Override public void playMoveAnim(boolean ultimate) { triggerAnim("skill", ultimate ? "cast" : "attack"); }
    @Override public float moveDamage(double mult) { return (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * mult); }

    @Override public boolean hurt(DamageSource src, float amt) {
        if (director.invulnerable() && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(src, amt);
    }

    @Override public boolean isImmobile() { return super.isImmobile() || mv != null || director.frozen(); }

    /** isImmobile() làm aiStep bỏ customServerAiStep => chiêu chạy ở tick(). */
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl) || !isAlive()) return;
        director.tick(sl);
        if (brainProfile() != null && !director.frozen()) brainReactions(sl);
        if (mv != null) {
            if (director.transitioning()) endMove();
            else if (!SuDoMoves.step(sl, this, mt++)) { endMove(); skillCd = (int) Math.round(com.hukhong.events.BossBuff.cd(50) * cdMul()); }
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
        if (level() instanceof ServerLevel sl1 && src.getEntity() instanceof Player) SuDoMoves.deathScene(sl1, this);
        if (level() instanceof ServerLevel sl0) com.hukhong.story.world.HuAnh.addNear(sl0, blockPosition(), -2);
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
                .triggerableAnim("cast", RawAnimation.begin().then("animation." + id + ".cast", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("taunt", RawAnimation.begin().then("animation." + id + ".taunt", Animation.LoopType.PLAY_ONCE)));
        if (brainProfile() != null) // blend 4 tick (M5)
            c.add(com.hukhong.boss.ai.BrainAnims.controller(this, brainProfile(), "animation." + id + ".", BRAIN_ANIMS, "attack", "walk", 4));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
