package com.hukhong.endgame;

import com.hukhong.flight.FlightBans;
import com.hukhong.item.ModItems;
import com.hukhong.vfx.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Dạ Khuyết (Bước 25): xuất hiện ở pha 3 của Nguyên Thủy Tịch Diệt. "Đêm Khuyết" (mù + yếu diện rộng)
 * và "Dịch Lời" (phiên dịch ý chí Hư Không: hồi máu cho boss chính). Hạ hắn trước thì boss mất nguồn hồi.
 */
public class DaKhuyet extends Monster implements GeoEntity, com.hukhong.boss.phase.PhasedBoss {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> PHASE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(DaKhuyet.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar = new ServerBossEvent(com.hukhong.entity.BossLevel.tag(Component.translatable("entity.hukhong.da_khuyet"), EndgameKit.BOSS_LEVEL),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
    private int skillCd = 80;
    private final com.hukhong.boss.phase.BossPhaseDirector director = new com.hukhong.boss.phase.BossPhaseDirector(this);

    public DaKhuyet(EntityType<? extends Monster> t, Level l) {
        super(t, l);
        addTag(FlightBans.WORLD_BOSS_TAG);
        xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attrs() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6000).add(Attributes.ATTACK_DAMAGE, 34).add(Attributes.ARMOR, 18)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PHASE, 1);
        b.define(AVATAR, false);
    }

    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DkPhase", phase());
        tag.putBoolean("DkAlly", ally);
    }

    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (getTags().contains("hk_nemesis_avatar")) entityData.set(AVATAR, true);
        if (tag.contains("DkPhase")) entityData.set(PHASE, Math.max(1, Math.min(3, tag.getInt("DkPhase"))));
        ally = tag.getBoolean("DkAlly");
    }

    public int phase() { return entityData.get(PHASE); }

    /** Phân thân trong tuyến truyện (story/nemesis): không đấu trường, không nhạc, không thanh boss cuối, không đổi pha. */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> AVATAR =
            net.minecraft.network.syncher.SynchedEntityData.defineId(DaKhuyet.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    public boolean avatar() { return entityData.get(AVATAR) || getTags().contains("hk_nemesis_avatar"); }

    /** Đánh dấu phân thân (đồng bộ xuống client để tên không gắn cấp boss cuối). */
    public void markAvatar() { addTag("hk_nemesis_avatar"); entityData.set(AVATAR, true); }

    // ------------------------------------------------------------ PhasedBoss (moi pha la mot mat trang khuyet dan)
    @Override public String phaseId() { return "da_khuyet"; }
    @Override public com.hukhong.boss.phase.BossPhaseDirector director() { return director; }
    @Override public int currentPhase() { return phase(); }
    @Override public void setCurrentPhase(int p) { entityData.set(PHASE, Math.max(1, Math.min(3, p))); }
    @Override public void applyPhase(ServerLevel sl, int ph) {
        bar.setColor(ph == 1 ? BossEvent.BossBarColor.BLUE : ph == 2 ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED);
        if (director != null && director.transitioning()) DaKhuyetMoves.transformFx(sl, this, ph); // chỉ lúc lộ diện, không khi nạp save
    }
    @Override public boolean startOwnMove(ServerLevel sl, com.hukhong.boss.phase.BossPhaseConfig.Move m, LivingEntity target) {
        if (ally || !DaKhuyetMoves.handles(m.id())) return false;
        return startMove(sl, m.id(), target);
    }
    @Override public java.util.List<com.hukhong.boss.phase.CamPath.Key> introCamera(int T) { return DaKhuyetMoves.introCam(T); }
    @Override public java.util.List<com.hukhong.boss.phase.BossPhaseConfig.SubLine> introLines(int T) { return DaKhuyetMoves.introSubs(T); }
    @Override public boolean idleForMove() { return mv == null; }
    @Override public void cancelSkill() { endMove(); skillCd = 40; }
    @Override public void playMoveAnim(boolean ultimate) { triggerAnim("skill", ultimate ? "rise" : "skill_a"); }
    @Override public float moveDamage(double mult) { return (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * mult); }

    @Override public boolean hurt(DamageSource src, float amt) {
        if (ally && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false; // đồng minh cốt truyện: không chết
        if (director != null && director.invulnerable() && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(src, amt);
    }

    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide) com.hukhong.boss.phase.PhaseClientFx.aura(this, this);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false) { // có BossBrain: brain lo di chuyển + đòn thường
            @Override public boolean canUse() { return brainProfile() == null && super.canUse(); }
            @Override public boolean canContinueToUse() { return brainProfile() == null && super.canContinueToUse(); }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public boolean isPushable() { return false; }
    @Override public Component getDisplayName() { if (avatar()) return super.getDisplayName(); return com.hukhong.entity.BossLevel.tag(super.getDisplayName(), EndgameKit.BOSS_LEVEL); }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }
    @Override public boolean isImmobile() { return super.isImmobile() || mv != null || (director != null && director.frozen()); }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); if (!avatar() && !ally) bar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }
    @Override public void remove(RemovalReason r) { super.remove(r); bar.removeAllPlayers(); }

    @Override public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide && !avatar()) { com.hukhong.audio.BossMusic.watch(this, "da_khuyet"); com.hukhong.arena.BossArena.attach(this, EndgameKit.MAIN_RADIUS - 1, null, false, new net.minecraft.core.BlockPos(0, EndgameKit.CENTER_Y, 0)); }
    }

    /** Xuất hiện: thoại + title cho người chơi quanh đó. */
    public void begin(ServerLevel sl, NguyenThuyTichDiet boss) {
        setHealth(getMaxHealth());
        endMove();
        pools.clear();
        ally = true;
        allyIntro = ALLY_INTRO_TICKS;
        bar.removeAllPlayers();
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) > 100 * 100) continue;
            p.displayClientMessage(Component.translatable("hukhong.endgame.dakhuyet.ally.1"), false);
            p.displayClientMessage(Component.translatable("hukhong.endgame.dakhuyet.ally.3"), false);
        }
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 40, 1, 1.5, 1, 0.05);
        playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 3f, 0.6f);
        // cutscene: Dạ Khuyết hiện ra, quay lưng với Hư Không
        int T = ALLY_INTRO_TICKS;
        com.hukhong.boss.phase.BossPhaseDirector.sendCutscene(sl, this, T, com.hukhong.boss.phase.BossPhaseDirector.introCam(T, 1.4), java.util.List.of(
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(6, 40, "entity.hukhong.da_khuyet"),
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(40, 80, "hukhong.endgame.dakhuyet.ally.1"),
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(80, 120, "hukhong.endgame.dakhuyet.ally.2"),
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(120, T - 4, "hukhong.endgame.dakhuyet.ally.3")));
    }

    // ------------------------------------------------------------ phe người chơi (trận cuối, pha 3)
    /** Đồng minh: không bị người chơi nhắm/đánh, không thanh boss, không chết; đánh Nguyên Thủy Tịch Diệt + Hư mob, hồi máu/che chắn người chơi. */
    private boolean ally;
    private int allyIntro, allyCd = 60;
    public static final int ALLY_INTRO_TICKS = 160;
    public boolean ally() { return ally; }

    @Override public boolean canAttack(LivingEntity t) {
        if (ally && t instanceof Player) return false;
        return super.canAttack(t);
    }

    private LivingEntity allyTarget(ServerLevel sl) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (NguyenThuyTichDiet b : sl.getEntitiesOfClass(NguyenThuyTichDiet.class, getBoundingBox().inflate(80), e -> e.isAlive())) {
            double d = distanceToSqr(b);
            if (d < bd) { bd = d; best = b; }
        }
        if (best != null) return best;
        for (net.minecraft.world.entity.monster.Monster m : sl.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, getBoundingBox().inflate(24),
                e -> e.isAlive() && e instanceof com.hukhong.entity.HuMob)) {
            double d = distanceToSqr(m);
            if (d < bd) { bd = d; best = m; }
        }
        return best;
    }

    private void allyStep(ServerLevel sl) {
        if (allyIntro > 0) {
            allyIntro--;
            getNavigation().stop();
            if (allyIntro % 5 == 0) sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 6, 0.8, 1.2, 0.8, 0.02);
            return;
        }
        LivingEntity t = allyTarget(sl);
        if (getTarget() != t) setTarget(t);
        if (allyCd-- > 0) return;
        allyCd = 70 + random.nextInt(30);
        int pick = random.nextInt(3);
        if (pick == 0 && t != null) { // Nguyệt Nhận: chém trăng khuyết vào boss
            float dmg = (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * 3.0);
            t.hurt(damageSources().mobAttack(this), dmg);
            net.minecraft.world.phys.Vec3 a = position().add(0, 1.6, 0), b = t.position().add(0, t.getBbHeight() * 0.5, 0);
            for (int i = 0; i <= 16; i++) {
                net.minecraft.world.phys.Vec3 q = a.lerp(b, i / 16.0);
                sl.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 3, 0.1, 0.1, 0.1, 0.01);
            }
            sl.sendParticles(ParticleTypes.SONIC_BOOM, b.x, b.y, b.z, 1, 0, 0, 0, 0);
            triggerAnim("skill", "skill_a");
            playSound(SoundEvents.TRIDENT_RIPTIDE_3.value(), 2f, 0.8f);
        } else if (pick == 1) { // Nguyệt Quang: hồi máu + giải hiệu ứng xấu cho người chơi quanh đó
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32), q -> !q.isSpectator())) {
                p.heal(p.getMaxHealth() * 0.15f);
                p.removeEffect(MobEffects.BLINDNESS); p.removeEffect(MobEffects.WEAKNESS); p.removeEffect(MobEffects.POISON);
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 12, 0.5, 0.8, 0.5, 0.05);
            }
            triggerAnim("skill", "skill_b");
            playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 3f, 1.2f);
        } else { // Khuyết Nguyệt Thuẫn: lá chắn cho người chơi
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32), q -> !q.isSpectator())) {
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 3));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1));
                sl.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 30, 0.6, 1, 0.6, 0.3);
            }
            triggerAnim("skill", "skill_b");
            playSound(SoundEvents.BEACON_POWER_SELECT, 2f, 0.8f);
        }
    }

    /** Sau khi boss ngã: dừng đánh, đứng yên cho cutscene kết. */
    public void standDown() { allyIntro = Integer.MAX_VALUE; setTarget(null); }

    // ================================================================ chiến đấu kiểu Malenia (xem DaKhuyetMoves)
    /** Chiêu đang thi triển (id trong BossPhases "da_khuyet" hoặc dk_dem_khuyet), null = rảnh. */
    String mv;
    int mt, stage;
    net.minecraft.world.phys.Vec3 aim = new net.minecraft.world.phys.Vec3(0, 0, 1), spot, from, to;
    LivingEntity mTarget;
    final java.util.Set<java.util.UUID> hitOnce = new java.util.HashSet<>();
    /** Vũng máu (Huyết Nguyệt Khai Hoa / Thủy Điểu Vũ máu): gây sát thương theo nhịp tới khi cạn. */
    final java.util.List<DaKhuyetMoves.Pool> pools = new java.util.ArrayList<>();
    int aeoniaCd = 400;
    /** Hồi máu = LIFESTEAL x sát thương mỗi khi đòn của Dạ Khuyết trúng (như Malenia). */
    public static final float LIFESTEAL = 1.5f;

    public String currentMove() { return mv; }
    public int moveTick() { return mt; }
    public int poolCount() { return pools.size(); }

    /** Bắt đầu chiêu riêng; false nếu đang bận. */
    public boolean startMove(ServerLevel sl, String id, LivingEntity target) {
        if (mv != null || !isAlive()) return false;
        mv = id; mt = 0; stage = 0; mTarget = target; spot = null; from = null; to = null;
        hitOnce.clear();
        getNavigation().stop();
        DaKhuyetMoves.begin(sl, this);
        return true;
    }

    void endMove() {
        if (mv == null) return;
        mv = null; mTarget = null;
        setNoGravity(false);
    }

    /** Đòn của Dạ Khuyết trúng người chơi: sát thương + hồi máu (lifesteal) + hiệu ứng hút máu. */
    public boolean strike(LivingEntity p, float dmg) {
        boolean ok = p.hurt(damageSources().mobAttack(this), dmg);
        if (ok && !ally) {
            heal(dmg * LIFESTEAL);
            if (level() instanceof ServerLevel sl) DaKhuyetMoves.drainFx(sl, this, p);
        }
        return ok;
    }

    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity e) {
        float before = e instanceof LivingEntity le ? le.getHealth() : 0;
        boolean ok = super.doHurtTarget(e);
        if (ok && !ally && e instanceof LivingEntity le) {
            heal(Math.max(0, before - le.getHealth()) * LIFESTEAL);
            if (level() instanceof ServerLevel sl) DaKhuyetMoves.drainFx(sl, this, le);
        }
        return ok;
    }

    /** isImmobile() (mv != null) làm LivingEntity.aiStep bỏ qua customServerAiStep => chiêu phải chạy ở tick(). */
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl) || !isAlive()) return;
        if (!avatar() && !ally) director.tick(sl);
        else if (!ally) com.hukhong.boss.ai.BossBrain.tickFor(sl, this, this); // phân thân: không director nhưng vẫn có brain
        if (!ally) brainFollowUps(sl);
        if (mv != null) {
            if (ally || director.transitioning()) endMove();
            else if (!DaKhuyetMoves.step(sl, this, mt++)) { endMove(); skillCd = (int) Math.round(com.hukhong.events.BossBuff.cd(70) * cdMul()); }
        }
        if (!pools.isEmpty()) DaKhuyetMoves.tickPools(sl, this);
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel sl = (ServerLevel) level();
        if (ally) { allyStep(sl); return; }
        bar.setProgress(getHealth() / getMaxHealth());
        if (aeoniaCd > 0) aeoniaCd--;
        if (mv != null || (director != null && director.frozen())) return;
        if (skillCd > 0) { skillCd--; return; }
        if (brainProfile() != null) return; // BossBrain chọn đòn thường + xin chiêu riêng qua brainBigMove()
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive() || distanceToSqr(t) > 24 * 24) return;
        String id = DaKhuyetMoves.pick(random, phase(), aeoniaCd <= 0 && !avatar(), distanceTo(t));
        if (id.equals("dk_huyet_hoa")) aeoniaCd = 900;
        startMove(sl, id, t);
    }
    @Override public void die(DamageSource src) {
        super.die(src);
        if (!(level() instanceof ServerLevel sl) || avatar()) return;
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(this) < 100 * 100) p.displayClientMessage(Component.translatable("hukhong.endgame.dakhuyet.fall"), false);
        pools.clear();
        if (!ally) DaKhuyetMoves.deathScene(sl, this);
    }

    @Override protected void dropCustomDeathLoot(ServerLevel sl, DamageSource src, boolean recentlyHit) {
        super.dropCustomDeathLoot(sl, src, recentlyHit);
        if (avatar()) return;
        com.hukhong.currency.Wallet.grantHonTinhBoss(sl, position(), src, 20 + random.nextInt(8));
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        String n = "animation.da_khuyet.";
        c.add(com.hukhong.boss.ai.BrainAnims.controller(this, DaKhuyetBrain.PROFILE, n, DaKhuyetBrain.ANIMS, "attack", "walk", 4));
        c.add(new AnimationController<>(this, "move", 4, s -> s.setAndContinue(s.isMoving()
                ? RawAnimation.begin().thenLoop(n + "walk") : RawAnimation.begin().thenLoop(n + "idle"))));
        AnimationController<DaKhuyet> sk = new AnimationController<>(this, "skill", 0, s -> PlayState.STOP);
        for (String a : new String[]{"attack", "skill_a", "skill_b", "skill_c", "phase", "thrust", "flurry", "rise", "dive"})
            sk.triggerableAnim(a, RawAnimation.begin().then(n + a, Animation.LoopType.PLAY_ONCE));
        c.add(sk);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ================================================================ BossBrain (PLAN-v1 §2, xem DaKhuyetBrain)
    @Override public com.hukhong.boss.ai.BossProfile brainProfile() { return ally ? null : DaKhuyetBrain.PROFILE; }
    @Override public com.hukhong.boss.ai.GapClose brainGapClose(ServerLevel sl, LivingEntity t, double dist) { return DaKhuyetBrain.gapClose(phase(), random); }
    @Override public com.hukhong.boss.ai.BossProfile.PhaseVals brainPhaseVals(com.hukhong.boss.ai.BossProfile.PhaseVals base) {
        return phase() >= 3 && getHealth() < getMaxHealth() * 0.3f ? DaKhuyetBrain.ENRAGED : base;
    }
    @Override public boolean brainStrike(LivingEntity victim, DamageSource src, float dmg) { return strike(victim, dmg); }
    @Override public boolean brainBigMove(ServerLevel sl, LivingEntity t) {
        if (mv != null || distanceToSqr(t) > 24 * 24) return false;
        String id = DaKhuyetMoves.pick(random, phase(), aeoniaCd <= 0 && !avatar(), distanceTo(t));
        return startMove(sl, id, t);
    }

    private com.hukhong.boss.ai.BossBrain.State brainPrev;
    private boolean leapDone;
    /** Số lần backflip/né xa nối Nguyệt Luân (gametest). */
    public int evadeCrescents;

    /** Nối chiêu sau brain: backflip / né đạn từ xa -> Nguyệt Luân; Nhảy Chém bật lên lúc lấy đà. */
    private void brainFollowUps(ServerLevel sl) {
        com.hukhong.boss.ai.BossBrain b = com.hukhong.boss.ai.BossBrain.get(this);
        if (b == null) return;
        com.hukhong.boss.ai.BossBrain.State s = b.state();
        LivingEntity t = getTarget();
        if (brainPrev == com.hukhong.boss.ai.BossBrain.State.EVADE && s == com.hukhong.boss.ai.BossBrain.State.STRAFE && mv == null && t != null && b.allowAttacks) {
            boolean far = distanceTo(t) > 8;
            if (b.lastEvade() == com.hukhong.boss.ai.BossProfile.Evade.BACKFLIP || far && random.nextFloat() < 0.4f) {
                if (startMove(sl, "dk_nguyet_luan", t)) evadeCrescents++;
            }
        }
        brainPrev = s;
        if ("nhay_chem".equals(b.comboId()) && b.hitIndex() == 0 && b.hitStage() == 0) {
            if (!leapDone && onGround()) {
                leapDone = true;
                setDeltaMovement(getDeltaMovement().x, 0.62, getDeltaMovement().z);
                hasImpulse = true;
                sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 8, 0.4, 0.05, 0.4, 0.03);
            }
        } else if (!"nhay_chem".equals(b.comboId())) leapDone = false;
    }
}
