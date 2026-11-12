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
        tag.putFloat("DkAllyDealt", allyDealt);
    }

    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (getTags().contains("hk_nemesis_avatar")) entityData.set(AVATAR, true);
        if (tag.contains("DkPhase")) entityData.set(PHASE, Math.max(1, Math.min(3, tag.getInt("DkPhase"))));
        ally = tag.getBoolean("DkAlly");
        trial = tag.getBoolean("DkTrial");
        allyDealt = tag.getFloat("DkAllyDealt");
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

    /**
     * Cải tà (PLAN nttd-story-rework §7.1), el = REDEEM_TICKS - redeemT:
     * 0 khựng (stagger) + hit-stop, CRIT, rung · 6 lảo đảo lùi · 14 quỳ một gối cắm katana (kneel -> kneel_idle), nứt đất đỏ
     * 40-120 thoại, trăng máu nhỏ dần · 130 ngẩng lên (look_up), dải sáng từ trời, trăng máu -> bạc
     * 180 dâng kiếm (offer_blade), END_ROD dọc lưỡi · 240 đứng dậy (rise), cánh trăng · T tan (FADE_TICKS)
     */
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
        Player near = sl.getNearestPlayer(this, 48); // quay mặt về người chơi: camera dựng theo hướng thân lúc bắt đầu
        if (near != null) faceTo(near.position());
        triggerAnim(CINE, "stagger");
        playSound(SoundEvents.PLAYER_ATTACK_CRIT, 3f, 0.6f);
        playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 3f, 0.6f);
        sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 2, getZ(), 40, 0.6, 1.0, 0.6, 0.4);
        com.hukhong.vfx.Fx.shake(sl, position(), 2.5f, 10, 32f);
        for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(10), p -> !p.isSpectator())) { // hit-stop 6 tick
            p.setDeltaMovement(Vec3.ZERO); p.hurtMarked = true;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 9, false, false, false));
        }
        int T = REDEEM_TICKS;
        var cam = java.util.List.of(
                ck(0, 0, 2.2f, 6.5f, 0, 62, 2.0f),
                ck(12, 1.2f, 1.2f, 4.2f, 0, 55, 1.6f),
                ck(22, 1.5f, 0.5f, 2.4f, 6, 42, 0.7f),     // cận tay cắm kiếm
                ck(40, 2.0f, 0.8f, 2.9f, 4, 46, 1.0f),
                ck(110, -2.6f, 1.6f, 4.0f, 0, 55, 1.8f),
                ck(130, 0, 1.6f, 2.3f, 0, 38, 2.3f),        // cận mặt khi ngẩng lên
                ck(175, 0.6f, 1.7f, 2.1f, 0, 38, 2.4f),
                ck(188, 1.4f, 1.2f, 3.0f, 0, 48, 1.3f),     // lưỡi kiếm dâng ngang
                ck(232, -1.4f, 1.2f, 3.4f, 0, 50, 1.5f),
                ck(246, 0, 0.3f, 4.4f, 0, 60, 3.0f),        // góc thấp nhìn lên lúc đứng dậy
                ck(T, 0, 2.0f, 8.0f, 0, 72, 2.2f));
        int[][] at = {{16, 62}, {62, 104}, {104, 130}, {130, 180}, {180, 240}, {240, T - 4}};
        var subs = new java.util.ArrayList<com.hukhong.boss.phase.BossPhaseConfig.SubLine>();
        for (int i = 0; i < at.length; i++)
            subs.add(new com.hukhong.boss.phase.BossPhaseConfig.SubLine(at[i][0], at[i][1], "hukhong.dialog.dakhuyet.redeem." + (i + 1)));
        com.hukhong.boss.phase.BossPhaseDirector.sendCutscene(sl, this, T, cam, subs);
    }

    private static com.hukhong.boss.phase.CamPath.Key ck(int t, float dx, float dy, float dz, float pitch, float fov, float lookH) {
        return new com.hukhong.boss.phase.CamPath.Key(t, dx, dy, dz, 0, pitch, fov, lookH);
    }

    private void faceTo(Vec3 p) {
        Vec3 d = p.subtract(position());
        if (d.horizontalDistanceSqr() < 1e-4) return;
        float yaw = (float) (Math.atan2(-d.x, d.z) * 180 / Math.PI);
        setYRot(yaw); yBodyRot = yaw; yHeadRot = yaw;
    }

    private Vec3 fwd() { float a = yBodyRot * ((float) Math.PI / 180f); return new Vec3(-Math.sin(a), 0, Math.cos(a)); }

    /** Một nhịp của cutscene cải tà (el = số tick đã chạy). */
    private void redeemBeat(ServerLevel sl, int el) {
        Vec3 f = fwd(), side = new Vec3(-f.z, 0, f.x);
        float k = Math.min(1f, Math.max(0f, (el - 130) / 40f)); // 0 = trăng máu, 1 = trăng bạc
        switch (el) {
            case 6 -> {
                setDeltaMovement(f.x * -0.55, 0.18, f.z * -0.55); hurtMarked = true;
                sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 10, 0.4, 0.05, 0.4, 0.03);
            }
            case 14 -> {
                triggerAnim(CINE, "kneel");
                Vec3 blade = position().add(f.scale(0.9)).add(side.scale(0.5));
                com.hukhong.vfx.Fx.groundCrack(sl, null, blade, 2.2f, MOON_RED, 7, 60);
                sl.sendParticles(ParticleTypes.CRIT, blade.x, blade.y + 0.2, blade.z, 20, 0.2, 0.1, 0.2, 0.2);
                com.hukhong.vfx.Fx.shake(sl, blade, 1.2f, 6, 24f);
                playSound(SoundEvents.ANVIL_LAND, 1.6f, 0.5f);
            }
            case 40 -> com.hukhong.vfx.Fx.elem(sl, null, com.hukhong.vfx.Fx.E_HALOS, position().add(0, 3.6, 0), Vec3.ZERO, 2.0f, MOON_RED, 45);
            case 85 -> com.hukhong.vfx.Fx.elem(sl, null, com.hukhong.vfx.Fx.E_HALOS, position().add(0, 3.4, 0), Vec3.ZERO, 1.3f, MOON_RED, 45);
            case 130 -> {
                triggerAnim(CINE, "look_up");
                Vec3 head = position().add(0, 2.6, 0);
                com.hukhong.vfx.Fx.ribbon(sl, null, head.add(0, 22, 0), head, 0, 14, 0, 0.7f, MOON_WHITE);
                com.hukhong.vfx.Fx.elem(sl, null, com.hukhong.vfx.Fx.E_HALOS, position().add(0, 3.8, 0), Vec3.ZERO, 2.4f, MOON_WHITE, 160);
                playSound(SoundEvents.BEACON_ACTIVATE, 2.5f, 1.4f);
            }
            case 180 -> {
                triggerAnim(CINE, "offer_blade");
                playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 2f, 0.7f);
            }
            case 240 -> {
                triggerAnim(CINE, "rise");
                DaKhuyetMoves.wingsFx(sl, this, DaKhuyetMoves.MOON, 60);
                com.hukhong.vfx.Fx.groundRing(sl, null, position(), 5f, MOON_WHITE, 40);
                playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 3f, 0.8f);
            }
            default -> {}
        }
        if (el >= 14 && el < 130 && el % 6 == 0) { // trăng máu trên đầu nhỏ dần
            double r = 1.4 * (1 - (el - 14) / 116.0) + 0.3;
            for (int i = 0; i < 10; i++) {
                double a = el * 0.2 + i * Math.PI / 5;
                sl.sendParticles(moonDust(0), getX() + Math.cos(a) * r, getY() + 3.6, getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
        }
        if (el >= 180 && el < 240 && el % 4 == 0) { // ánh bạc dọc lưỡi kiếm dâng ngang
            Vec3 c = position().add(f.scale(1.1)).add(0, 1.15, 0);
            for (int i = -4; i <= 4; i++) {
                Vec3 q = c.add(side.scale(i * 0.22));
                sl.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0.005);
            }
        }
        if (el % 4 == 0) sl.sendParticles(moonDust(k), getX(), getY() + 2.2, getZ(), 6, 0.8, 1.2, 0.8, 0);
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
            redeemBeat(sl, REDEEM_TICKS - redeemT);
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

    /**
     * Đồng minh pha 3 (PLAN nttd-story-rework §4): dùng ĐỦ bộ chiêu boss qua BossBrain (host riêng {@link #allyHost}) nhắm Nguyên Thủy.
     * Ở đây chỉ còn: màn xuất hiện, chọn mục tiêu, lướt tới / giữ kết giới, Nguyệt Quang theo timer, né chiêu Nguyên Thủy.
     */
    private void allyTick(ServerLevel sl) {
        if (allyIntro > 0) {
            allyIntro--;
            getNavigation().stop();
            if (allyIntro % 5 == 0) sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 6, 0.8, 1.2, 0.8, 0.02);
            return;
        }
        if (aeoniaCd > 0) aeoniaCd--;
        if (dashT > 0) { stepDash(sl); return; }
        if (guarding > 0) { guarding--; getNavigation().stop(); return; }
        LivingEntity cur = getTarget();
        LivingEntity t = tickCount % 20 == 0 || cur == null || !cur.isAlive() ? allyTarget(sl) : cur;
        if (cur != t) setTarget(t);
        com.hukhong.boss.ai.BossBrain.tickFor(sl, this, allyHost);
        brainFollowUps(sl);
        if (t == null) return;
        allyDodge(sl, t);
        double edge = com.hukhong.boss.ai.BossBrain.edgeDist(this, t);
        if (edge > 40 && tickCount % 10 == 0) getNavigation().moveTo(t, 1.2);
        if (--allyHealCd <= 0 && mv == null && !com.hukhong.boss.ai.BossBrain.busy(this)) { allyHealCd = 240 + random.nextInt(60); moonlight(sl); }
        else if (edge > 24 && mv == null && --allyCd <= 0) { allyCd = 80; allyAttack(sl, t); }
    }

    /** Nguyệt Quang: hồi 12% máu + giải mù/yếu/độc cho người chơi quanh nàng. */
    private void moonlight(ServerLevel sl) {
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32), q -> !q.isSpectator())) {
            p.heal(p.getMaxHealth() * 0.12f);
            p.removeEffect(MobEffects.BLINDNESS); p.removeEffect(MobEffects.WEAKNESS); p.removeEffect(MobEffects.POISON);
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 12, 0.5, 0.8, 0.5, 0.05);
        }
        DaKhuyetMoves.wingsFx(sl, this, DaKhuyetMoves.MOON, 30);
        triggerAnim("skill", "skill_b");
        playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 3f, 1.2f);
    }

    /** §4.2: Nguyên Thủy bắt đầu chiêu riêng / lấy đà nhát brain gần nàng => báo đe dọa cho brain (né, backflip nối Nguyệt Luân). */
    private void allyDodge(ServerLevel sl, LivingEntity t) {
        com.hukhong.boss.ai.BossBrain me = com.hukhong.boss.ai.BossBrain.get(this);
        if (me == null) return;
        boolean cast = t instanceof NguyenThuyTichDiet n && n.ownMoveActive();
        com.hukhong.boss.ai.BossBrain tb = com.hukhong.boss.ai.BossBrain.get(t);
        boolean swing = tb != null && tb.hitStage() == 0 && com.hukhong.boss.ai.BossBrain.edgeDist(this, t) < 7;
        if (cast && !foeCasting && distanceTo(t) < 28) me.onThreat(com.hukhong.boss.ai.BossBrain.Threat.SKILL);
        else if (swing && !foeSwinging) me.onThreat(com.hukhong.boss.ai.BossBrain.Threat.MELEE);
        foeCasting = cast; foeSwinging = swing;
    }

    private boolean foeCasting, foeSwinging;
    private int allyHealCd = 200;
    /** Phần máu tối đa Nguyên Thủy nàng đã gây (trần {@link #ALLY_TOTAL_CAP}). */
    private float allyDealt;
    /** §4.3: 0.25% máu Nguyên Thủy x hệ số chiêu, trần 0.8%/đòn, tổng 12% cả pha 3; Hư mob 8%/đòn. */
    public static final float ALLY_FRAC = 0.0025f, ALLY_HIT_CAP = 0.008f, ALLY_TOTAL_CAP = 0.12f, ALLY_MOB_FRAC = 0.08f;
    public float allyDealt() { return allyDealt; }

    /** Host brain khi là đồng minh: profile boss đầy đủ (pha 3), nghỉ dài hơn, chỉ chém mục tiêu của mình. */
    private final com.hukhong.boss.ai.BossBrain.Host allyHost = new com.hukhong.boss.ai.BossBrain.Host() {
        public com.hukhong.boss.ai.BossProfile brainProfile() { return DaKhuyetBrain.PROFILE; }
        public int currentPhase() { return 3; }
        public boolean frozen() { return allyIntro > 0 || guarding > 0 || dashT > 0 || redeeming; }
        public boolean ownMoveActive() { return mv != null; }
        public boolean brainBigMove(ServerLevel sl, LivingEntity t) { return allyBigMove(sl, t); }
        public float moveDamage(double mult) { return DaKhuyet.this.moveDamage(mult); }
        public void playBrainAnim(String name) { DaKhuyet.this.playBrainAnim(name); }
        public com.hukhong.boss.ai.GapClose brainGapClose(ServerLevel sl, LivingEntity t, double d) { return DaKhuyetBrain.gapClose(3, random); }
        public com.hukhong.boss.ai.BossProfile.PhaseVals brainVals(com.hukhong.boss.ai.BossProfile p) { return DaKhuyetBrain.ALLY; }
        public boolean brainStrike(LivingEntity v, DamageSource src, float dmg) { return strike(v, dmg); }
        public boolean brainCanHit(LivingEntity e) { return !(e instanceof Player) && !isStoryAlly(e) && (e == getTarget() || e instanceof com.hukhong.entity.HuMob); }
    };

    private boolean allyBigMove(ServerLevel sl, LivingEntity t) {
        if (mv != null) return false;
        double edge = com.hukhong.boss.ai.BossBrain.edgeDist(this, t);
        if (edge > 24) return false;
        String id = DaKhuyetMoves.pick(random, 3, aeoniaCd <= 0, (float) edge);
        return startMove(sl, id, t);
    }

    /** Kết giới đang giữ (tick). */
    private int guarding;

    /** Dự phòng khi Nguyên Thủy ngoài tầm chiêu: ba lưỡi trăng bay tới (qua {@link #strike}, chịu trần sát thương). */
    private void allyAttack(ServerLevel sl, LivingEntity t) {
        Vec3 a = position().add(0, 1.4, 0), b = t.position().add(0, t.getBbHeight() * 0.5, 0);
        Vec3 dir = b.subtract(a).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);
        dir = dir.normalize();
        aim = dir;
        getLookControl().setLookAt(t, 60, 60);
        for (int i = -1; i <= 1; i++) {
            double ang = Math.toRadians(i * 12), c = Math.cos(ang), sn = Math.sin(ang);
            Vec3 d = new Vec3(dir.x * c - dir.z * sn, 0, dir.x * sn + dir.z * c);
            com.hukhong.vfx.Fx.qi(sl, a, d.scale(Math.min(18, a.distanceTo(b))), 2.0f, DaKhuyetMoves.MOON, i * 30, 12);
        }
        triggerAnim("skill", "skill_b");
        playSound(SoundEvents.TRIDENT_THROW.value(), 2.5f, 0.6f);
        strike(t, moveDamage(1.0));
        sl.sendParticles(ParticleTypes.END_ROD, b.x, b.y, b.z, 12, 0.6, 0.6, 0.6, 0.08);
    }

    /** Chiêu tất sát / hy sinh: LƯỚT tới (8 tick, không dịch chuyển), rồi dựng Khuyết Nguyệt Kết Giới bán kính r trong {@code hold} tick. */
    public void guardBarrier(ServerLevel sl, Vec3 at, LivingEntity boss, float r, int hold) {
        endMove();
        com.hukhong.boss.ai.BossBrain.tickFor(sl, this, allyHost); // brain đứng yên ngay (frozen)
        dashFrom = position(); dashTo = at; dashBoss = boss; dashR = r; dashHold = hold; dashT = DASH_TICKS;
        getNavigation().stop();
        setNoGravity(true);
        triggerAnim(com.hukhong.boss.ai.BrainAnims.CONTROLLER, "dash");
        com.hukhong.vfx.Fx.ribbon(sl, null, position().add(0, 1.2, 0), at.add(0, 1.2, 0), 0.8, DASH_TICKS, 0, 0.4f, DaKhuyetMoves.MOON);
        playSound(SoundEvents.PHANTOM_FLAP, 2f, 1.3f);
    }

    public static final int DASH_TICKS = 8;
    private int dashT, dashHold;
    private float dashR;
    private Vec3 dashFrom, dashTo;
    private LivingEntity dashBoss;

    private void stepDash(ServerLevel sl) {
        dashT--;
        double k = 1 - dashT / (double) DASH_TICKS;
        k = k * k * (3 - 2 * k);
        Vec3 p = dashFrom.lerp(dashTo, k);
        setDeltaMovement(Vec3.ZERO);
        faceTo(dashTo.add(dashTo.subtract(dashFrom)));
        moveTo(p.x, p.y, p.z, getYRot(), getXRot());
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 3, 0.3, 0.5, 0.3, 0.01);
        if (dashT > 0) return;
        setNoGravity(false);
        raiseBarrier(sl, dashTo, dashBoss, dashR, dashHold);
    }

    private void raiseBarrier(ServerLevel sl, Vec3 at, LivingEntity boss, float r, int hold) {
        getNavigation().stop();
        if (boss != null) { faceTo(boss.position()); getLookControl().setLookAt(boss, 90, 90); }
        guarding = hold;
        triggerAnim(CINE, "rise");
        DaKhuyetMoves.wingsFx(sl, this, DaKhuyetMoves.MOON, hold);
        com.hukhong.vfx.Fx.groundRing(sl, null, at, r, DaKhuyetMoves.MOON, hold);
        com.hukhong.vfx.Fx.rune(sl, null, at.add(0, 0.1, 0), r * 0.8f, DaKhuyetMoves.MOON, hold, 8);
        com.hukhong.vfx.Fx.elem(sl, null, com.hukhong.vfx.Fx.E_HALOS, at.add(0, 2, 0), Vec3.ZERO, r * 0.5f, DaKhuyetMoves.MOON, hold);
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 12, 0.6, 0.05, 0.6, 0.05);
        playSound(SoundEvents.BEACON_ACTIVATE, 3f, 0.7f);
    }

    // ---- hy sinh pha 3 -> 4 (PLAN §7.2; NguyenThuyTichDiet.grandSacrifice gọi theo dòng thời gian)
    /** t150: quỳ dần dưới áp lực, kết giới nứt. */
    public void sacrificeKneel(ServerLevel sl, Vec3 guard, LivingEntity boss) {
        guarding = Math.max(guarding, 60);
        triggerAnim(CINE, "kneel");
        com.hukhong.vfx.Fx.groundCrack(sl, null, guard, 6f, DaKhuyetMoves.MOON, 10, 50);
        Vec3 f = boss == null ? fwd() : boss.position().subtract(guard).multiply(1, 0, 1);
        f = f.lengthSqr() < 1e-4 ? fwd() : f.normalize();
        Vec3 side = new Vec3(-f.z, 0, f.x);
        com.hukhong.vfx.Fx.rift(sl, null, guard.add(f.scale(2.5)).add(0, 2.2, 0), side.scale(2.5), 2.4f, DaKhuyetMoves.MOON, 30);
        com.hukhong.vfx.Fx.shake(sl, guard, 2f, 10, 40f);
        playSound(SoundEvents.GLASS_BREAK, 3f, 0.5f);
        playSound(SoundEvents.ANVIL_LAND, 1.5f, 0.6f);
    }

    /** t170: quay lại nhìn người chơi lần cuối (vẫn quỳ, ngẩng đầu). */
    public void sacrificeLookBack(ServerLevel sl) {
        Player p = sl.getNearestPlayer(this, 64);
        if (p != null) { faceTo(p.position()); getLookControl().setLookAt(p, 90, 90); }
        triggerAnim(CINE, "look_up");
        com.hukhong.vfx.Fx.elem(sl, null, com.hukhong.vfx.Fx.E_HALOS, position().add(0, 3.6, 0), Vec3.ZERO, 1.6f, MOON_WHITE, 10);
    }

    /** Sau khi boss ngã: dừng đánh, đứng yên cho cutscene kết. */
    public void standDown() { allyIntro = Integer.MAX_VALUE; endMove(); pools.clear(); setTarget(null); }

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
        if (ally) return allyStrike(p, dmg);
        boolean ok = p.hurt(damageSources().mobAttack(this), dmg);
        if (ok && !ally) {
            heal(dmg * LIFESTEAL);
            if (level() instanceof ServerLevel sl) DaKhuyetMoves.drainFx(sl, this, p);
        }
        return ok;
    }

    /** Đòn của đồng minh: không bao giờ trúng người chơi / đồng minh; % máu mục tiêu có trần (§4.3); trúng => hồi 1% máu người chơi gần nhất. */
    private boolean allyStrike(LivingEntity v, float dmg) {
        if (v == this || v instanceof Player || isStoryAlly(v)) return false;
        float frac;
        if (v instanceof NguyenThuyTichDiet) {
            double mult = dmg / Math.max(1.0, getAttributeValue(Attributes.ATTACK_DAMAGE));
            frac = (float) Math.min(ALLY_HIT_CAP, ALLY_FRAC * mult);
            frac = Math.min(frac, ALLY_TOTAL_CAP - allyDealt);
        } else frac = v instanceof com.hukhong.entity.HuMob ? ALLY_MOB_FRAC : 0f;
        if (frac <= 0) { // hết trần: vẫn có hình ảnh trúng đòn
            if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.END_ROD, v.getX(), v.getY() + v.getBbHeight() * 0.5, v.getZ(), 4, 0.4, 0.4, 0.4, 0.05);
            return false;
        }
        if (!v.hurt(damageSources().mobAttack(this), v.getMaxHealth() * frac)) return false;
        if (v instanceof NguyenThuyTichDiet) allyDealt += frac;
        if (level() instanceof ServerLevel sl) {
            Player p = sl.getNearestPlayer(this, 32);
            if (p != null && !p.isSpectator()) {
                p.heal(p.getMaxHealth() * 0.01f);
                sl.sendParticles(moonDust(1f), p.getX(), p.getY() + 1.2, p.getZ(), 3, 0.3, 0.4, 0.3, 0);
            }
        }
        return true;
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
        if (ally) allyTick(sl); // đồng minh: brain với host riêng (allyHost), nhắm Nguyên Thủy
        else {
            if (!avatar()) director.tick(sl);
            else com.hukhong.boss.ai.BossBrain.tickFor(sl, this, this); // phân thân: không director nhưng vẫn có brain
            brainFollowUps(sl);
        }
        if (mv != null) {
            if (ally ? (allyIntro > 0 || guarding > 0 || dashT > 0) : director.transitioning()) endMove();
            else if (!DaKhuyetMoves.step(sl, this, mt++)) { endMove(); skillCd = (int) Math.round(com.hukhong.events.BossBuff.cd(70) * cdMul()); }
        }
        if (!pools.isEmpty()) DaKhuyetMoves.tickPools(sl, this);
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel sl = (ServerLevel) level();
        if (ally) return; // đồng minh chạy trong tick() (allyTick)
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
        // cutscene cải tà / hy sinh (PLAN §7.1-7.2): thêm SAU cùng để đè các controller khác khi đang chạy
        AnimationController<DaKhuyet> cine = new AnimationController<>(this, CINE, 3, s -> PlayState.STOP);
        cine.triggerableAnim("stagger", RawAnimation.begin().then(n + "stagger", Animation.LoopType.HOLD_ON_LAST_FRAME));
        cine.triggerableAnim("kneel", RawAnimation.begin().then(n + "kneel", Animation.LoopType.PLAY_ONCE).thenLoop(n + "kneel_idle"));
        cine.triggerableAnim("look_up", RawAnimation.begin().then(n + "look_up", Animation.LoopType.HOLD_ON_LAST_FRAME));
        cine.triggerableAnim("offer_blade", RawAnimation.begin().then(n + "offer_blade", Animation.LoopType.HOLD_ON_LAST_FRAME));
        cine.triggerableAnim("rise", RawAnimation.begin().then(n + "rise", Animation.LoopType.PLAY_ONCE));
        c.add(cine);
    }

    /** Controller animation cutscene. */
    public static final String CINE = "cine";

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ================================================================ BossBrain (PLAN-v1 §2, xem DaKhuyetBrain)
    @Override public com.hukhong.boss.ai.BossProfile brainProfile() { return DaKhuyetBrain.PROFILE; } // đồng minh cũng có brain (allyHost)
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
