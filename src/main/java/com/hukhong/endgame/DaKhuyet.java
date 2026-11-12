package com.hukhong.endgame;

import com.hukhong.flight.FlightBans;
import com.hukhong.item.ModItems;
import com.hukhong.vfx.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.entity.MobSpawnType;
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

    /** Trận khảo hạch (cổng Endgame, lần đầu): máu ≈ 90% NTTD (510000), nhân theo số người như NTTD; thua không chết mà cải tà. */
    public static final double TRIAL_HP = 459000, TRIAL_DMG = 47, TRIAL_ARMOR = 14;
    public static final int REDEEM_TICKS = 300, FADE_TICKS = 50;
    private static final int MOON_RED = 0xB01E28, MOON_WHITE = 0xF4F4FF;
    private boolean trial, redeeming;
    private int trialPlayers, redeemT, fadeT;

    public DaKhuyet(EntityType<? extends Monster> t, Level l) {
        super(t, l);
        addTag(FlightBans.WORLD_BOSS_TAG);
        xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attrs() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, TRIAL_HP).add(Attributes.ATTACK_DAMAGE, TRIAL_DMG).add(Attributes.ARMOR, TRIAL_ARMOR)
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
        tag.putBoolean("DkTrial", trial);
    }

    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (getTags().contains("hk_nemesis_avatar")) entityData.set(AVATAR, true);
        if (tag.contains("DkPhase")) entityData.set(PHASE, Math.max(1, Math.min(3, tag.getInt("DkPhase"))));
        ally = tag.getBoolean("DkAlly");
        trial = tag.getBoolean("DkTrial");
    }

    // ------------------------------------------------------------ trận khảo hạch (story)
    public boolean trial() { return trial; }

    /** Đổi sang chỉ số khảo hạch (máu/công/giáp). Chỉ gọi cho trận khảo hạch. */
    public void markTrial() {
        trial = true;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(TRIAL_HP);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(TRIAL_DMG);
        getAttribute(Attributes.ARMOR).setBaseValue(TRIAL_ARMOR);
        setHealth(getMaxHealth());
    }

    /** Scale máu khảo hạch theo số người chơi gần (như NTTD: hpMul), giữ tỉ lệ máu hiện tại. */
    private void rescaleTrial(ServerLevel sl) {
        int n = com.hukhong.events.BossScaling.clamp(sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(80), p -> !p.isSpectator()).size());
        if (n == trialPlayers) return;
        trialPlayers = n;
        float frac = getHealth() / getMaxHealth();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(TRIAL_HP * com.hukhong.events.BossScaling.hpMul(n));
        setHealth(Math.max(1f, getMaxHealth() * frac));
    }

    /** Dạ Khuyết khảo hạch đang tồn tại trong thế giới này (null nếu không có). */
    public static DaKhuyet findTrial(ServerLevel sl) {
        var l = sl.getEntitiesOfClass(DaKhuyet.class, new AABB(-400, -64, -400, 400, 400, 400), d -> d.trial && d.isAlive());
        return l.isEmpty() ? null : l.get(0);
    }

    /** Triệu hồi Dạ Khuyết khảo hạch tại {@code at}. */
    public static DaKhuyet spawnTrial(ServerLevel sl, Vec3 at, float yaw) {
        DaKhuyet d = EndgameRegistry.DA_KHUYET.get().create(sl);
        if (d == null) return null;
        d.moveTo(at.x, at.y, at.z, yaw, 0f);
        d.finalizeSpawn(sl, sl.getCurrentDifficultyAt(d.blockPosition()), MobSpawnType.EVENT, null);
        d.markTrial();
        sl.addFreshEntity(d);
        return d;
    }

    /** Thua (máu chạm 1): không chết. Quỳ, cutscene cải tà, rồi cờ dk_redeemed và tan biến. */
    /** Dev demo: chạy thẳng cutscene cải tà. */
    public void devRedeem() { startRedeem(); }

    private void startRedeem() {
        if (redeeming || !(level() instanceof ServerLevel sl)) return;
        redeeming = true;
        redeemT = REDEEM_TICKS;
        setHealth(1f);
        endMove();
        pools.clear();
        setTarget(null);
        getNavigation().stop();
        bar.setColor(BossEvent.BossBarColor.WHITE);
        triggerAnim("skill", "phase"); // quỳ: đứng yên qua isImmobile(), không có anim quỳ riêng
        playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 3f, 0.6f);
        int T = REDEEM_TICKS;
        float sc = (float) Math.max(2.5, getBbHeight() * 0.5);
        var cam = java.util.List.of(
                new com.hukhong.boss.phase.CamPath.Key(0, 0, 0.6f * sc, 2.6f * sc, 0, 0, 60, 0.6f * sc),
                new com.hukhong.boss.phase.CamPath.Key(T / 3, 2.4f * sc, 1.4f * sc, 1.6f * sc, 0, 0, 65, 0.4f * sc),
                new com.hukhong.boss.phase.CamPath.Key(T * 2 / 3, -2.2f * sc, 0.8f * sc, 2.4f * sc, 0, 0, 58, 0.5f * sc),
                new com.hukhong.boss.phase.CamPath.Key(T, 0, 2.6f * sc, 5.0f * sc, 0, 8, 75, 0.3f * sc));
        var subs = new java.util.ArrayList<com.hukhong.boss.phase.BossPhaseConfig.SubLine>();
        for (int i = 1; i <= 6; i++) {
            int from = i == 1 ? 6 : 6 + (i - 1) * 48, to = i == 6 ? T - 4 : 6 + i * 48 - 2;
            subs.add(new com.hukhong.boss.phase.BossPhaseConfig.SubLine(from, to, "hukhong.dialog.dakhuyet.redeem." + i));
        }
        com.hukhong.boss.phase.BossPhaseDirector.sendCutscene(sl, this, T, cam, subs);
    }

    /** Màu trăng máu -> trăng bạc theo k (0..1). */
    private static DustParticleOptions moonDust(float k) {
        int a = MOON_RED, b = MOON_WHITE;
        float r = ((a >> 16) & 0xFF) + ((((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * k);
        float g = ((a >> 8) & 0xFF) + ((((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * k);
        float bl = (a & 0xFF) + (((b & 0xFF) - (a & 0xFF)) * k);
        return new DustParticleOptions(new Vector3f(r / 255f, g / 255f, bl / 255f), 1.6f);
    }

    private void tickRedeem(ServerLevel sl) {
        if (redeemT > 0) {
            redeemT--;
            int el = REDEEM_TICKS - redeemT;
            if (el % 4 == 0) sl.sendParticles(moonDust(el / (float) REDEEM_TICKS), getX(), getY() + 2.2, getZ(), 10, 0.8, 1.2, 0.8, 0);
            if (el % 10 == 0) sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 3, getZ(), 8, 1, 1.5, 1, 0.03);
            if (redeemT == 0) {
                for (ServerPlayer p : sl.players()) {
                    if (p.isSpectator() || p.distanceToSqr(this) > 64 * 64) continue;
                    var q = com.hukhong.story.npc.DialogueService.quest(p);
                    com.hukhong.story.npc.DialogueService.setQuest(p, q.withFlag(EndgameService.F_DK_REDEEMED));
                }
                fadeT = FADE_TICKS;
                playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 3f, 1.2f);
            }
            return;
        }
        if (fadeT > 0) {
            fadeT--;
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 6, 0.6, 1.2, 0.6, 0.04);
            if (fadeT == 0) {
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 60, 0.8, 1.5, 0.8, 0.08);
                discard();
            }
        }
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
        if (trial) {
            if (redeeming) return false;
            // khảo hạch: không để máu xuống dưới 1; chỉ cải tà khi đòn thật sự trúng (không tính khung bất tử)
            boolean ok = super.hurt(src, Math.max(0f, Math.min(amt, getHealth() - 1f)));
            if (ok && getHealth() <= 1.01f) startRedeem();
            return ok;
        }
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
    @Override public boolean isImmobile() { return super.isImmobile() || mv != null || redeeming || (director != null && director.frozen()); }
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
    /** Đồng minh đã xong màn xuất hiện, sẵn sàng chắn chiêu. */
    public boolean allyReady() { return ally && isAlive() && allyIntro == 0; }
    /** Đồng minh cốt truyện: trụ/pháp thuật của người chơi không được nhắm. */
    public static boolean isStoryAlly(net.minecraft.world.entity.LivingEntity e) {
        return (e instanceof DaKhuyet d && d.ally) || (e != null && e.getTags().contains(NttdAllies.MO_TAG));
    }

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
        if (guarding > 0) { guarding--; getNavigation().stop(); return; } // đang giữ kết giới đỡ chiêu tất sát
        if (t != null && distanceToSqr(t) > 14 * 14 && tickCount % 10 == 0) getNavigation().moveTo(t, 1.1);
        if (allyCd-- > 0) return;
        allyCd = 45 + random.nextInt(25);
        allyBeat++;
        if (allyBeat % 5 == 4) { // Nguyệt Quang: hồi máu + giải hiệu ứng xấu
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32), q -> !q.isSpectator())) {
                p.heal(p.getMaxHealth() * 0.12f);
                p.removeEffect(MobEffects.BLINDNESS); p.removeEffect(MobEffects.WEAKNESS); p.removeEffect(MobEffects.POISON);
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 12, 0.5, 0.8, 0.5, 0.05);
            }
            DaKhuyetMoves.wingsFx(sl, this, DaKhuyetMoves.MOON, 30);
            triggerAnim("skill", "skill_b");
            playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 3f, 1.2f);
            return;
        }
        if (t == null) return;
        allyAttack(sl, t, random.nextInt(3));
    }

    /** Đếm nhịp chiêu đồng minh; kết giới đang giữ (tick). */
    private int allyBeat, guarding;

    /**
     * Đòn đồng minh vào Nguyên Thủy Tịch Diệt / Hư mob, mượn hình ảnh chiêu thật (DaKhuyetMoves): sát thương THẤP cố định
     * 0.3-0.5% máu tối đa mục tiêu (boss) để không cướp công người chơi. NTTD bỏ qua Dạ Khuyết khi chọn mục tiêu (setTarget).
     */
    private void allyAttack(ServerLevel sl, LivingEntity t, int kind) {
        net.minecraft.world.phys.Vec3 a = position().add(0, 1.4, 0), b = t.position().add(0, t.getBbHeight() * 0.5, 0);
        net.minecraft.world.phys.Vec3 dir = b.subtract(a).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1e-4) dir = new net.minecraft.world.phys.Vec3(0, 0, 1);
        dir = dir.normalize();
        aim = dir;
        getLookControl().setLookAt(t, 60, 60);
        float frac = t instanceof NguyenThuyTichDiet ? 0.003f + random.nextFloat() * 0.002f : 0.08f;
        float dmg = t.getMaxHealth() * frac;
        switch (kind) {
            case 0 -> { // Nguyệt Luân: ba lưỡi trăng khuyết
                for (int i = -1; i <= 1; i++) {
                    double ang = Math.toRadians(i * 12), c = Math.cos(ang), sn = Math.sin(ang);
                    net.minecraft.world.phys.Vec3 d = new net.minecraft.world.phys.Vec3(dir.x * c - dir.z * sn, 0, dir.x * sn + dir.z * c);
                    com.hukhong.vfx.Fx.qi(sl, a, d.scale(Math.min(18, a.distanceTo(b))), 2.0f, DaKhuyetMoves.MOON, i * 30, 12);
                }
                triggerAnim("skill", "skill_b");
                playSound(SoundEvents.TRIDENT_THROW.value(), 2.5f, 0.6f);
            }
            case 1 -> { // Thủy Điểu: lướt tới chém liên hoàn
                net.minecraft.world.phys.Vec3 to = t.position().subtract(dir.scale(Math.max(3, t.getBbWidth() * 0.6 + 2)));
                com.hukhong.vfx.Fx.ribbon(sl, null, a, to.add(0, 1.2, 0), 0.6, 6, 0, 0.35f, DaKhuyetMoves.MOON);
                teleportTo(to.x, Math.max(to.y, getY()), to.z);
                for (int i = 0; i < 3; i++) com.hukhong.vfx.Fx.qi(sl, to.add(0, 1.3, 0), dir.scale(4), 2.4f, DaKhuyetMoves.MOON, -40 + i * 40, 8);
                triggerAnim("skill", "skill_a");
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 0.8f);
            }
            default -> { // Đâm Xuyên: tia trăng thẳng vào boss
                com.hukhong.vfx.Fx.ribbonHelix(sl, null, a, b, 0.5, 0, 6, 0, 0.18f, DaKhuyetMoves.MOON);
                sl.sendParticles(ParticleTypes.SONIC_BOOM, b.x, b.y, b.z, 1, 0, 0, 0, 0);
                triggerAnim("skill", "skill_a");
                playSound(SoundEvents.TRIDENT_RIPTIDE_3.value(), 2f, 0.8f);
            }
        }
        t.hurt(damageSources().mobAttack(this), dmg);
        sl.sendParticles(ParticleTypes.END_ROD, b.x, b.y, b.z, 12, 0.6, 0.6, 0.6, 0.08);
    }

    /** Chiêu tất sát của boss: lướt tới chắn trước, dựng Khuyết Nguyệt Kết Giới bán kính r trong {@code hold} tick. */
    public void guardBarrier(ServerLevel sl, net.minecraft.world.phys.Vec3 at, LivingEntity boss, float r, int hold) {
        com.hukhong.vfx.Fx.ribbon(sl, null, position().add(0, 1.2, 0), at.add(0, 1.2, 0), 0.8, 6, 0, 0.4f, DaKhuyetMoves.MOON);
        teleportTo(at.x, at.y, at.z);
        getNavigation().stop();
        getLookControl().setLookAt(boss, 90, 90);
        guarding = hold;
        triggerAnim("skill", "rise");
        DaKhuyetMoves.wingsFx(sl, this, DaKhuyetMoves.MOON, hold);
        com.hukhong.vfx.Fx.groundRing(sl, null, at, r, DaKhuyetMoves.MOON, hold);
        com.hukhong.vfx.Fx.rune(sl, null, at.add(0, 0.1, 0), r * 0.8f, DaKhuyetMoves.MOON, hold, 8);
        com.hukhong.vfx.Fx.elem(sl, null, com.hukhong.vfx.Fx.E_HALOS, at.add(0, 2, 0), net.minecraft.world.phys.Vec3.ZERO, r * 0.5f, DaKhuyetMoves.MOON, hold);
        playSound(SoundEvents.BEACON_ACTIVATE, 3f, 0.7f);
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
        if (redeeming) { tickRedeem(sl); return; }
        if (trial && (trialPlayers == 0 || tickCount % 100 == 1)) rescaleTrial(sl);
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
        if (redeeming) return;
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
        if (trial) { setHealth(1f); startRedeem(); return; } // khảo hạch không bao giờ chết
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
