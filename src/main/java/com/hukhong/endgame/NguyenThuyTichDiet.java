package com.hukhong.endgame;

import com.hukhong.HuKhong;
import com.hukhong.endgame.EndgameKit.Skill;
import com.hukhong.entity.BossLevel;
import com.hukhong.entity.HuSkill;
import com.hukhong.entity.ModEntities;
import com.hukhong.events.BossScaling;
import com.hukhong.flight.FlightBans;
import com.hukhong.item.ModItems;
import com.hukhong.vfx.Fx;
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
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
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
import net.minecraft.util.Mth;
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
 * Hai dạng (kiểu Radagon -> Elden Beast): DẠNG 1 = tu sĩ bị Hư Không chiếm xác (model nttd_form1, cận chiến búa nhanh);
 * hạ dạng 1 -> bất tử + cutscene thân xác nứt vỡ -> DẠNG 2 = Nguyên Thủy Tịch Diệt nguyên bản (4 pha, đầy máu).
 * Nguyên Thủy Tịch Diệt (Bước 25): boss cuối, 4 pha, 6 chiêu độc nhất (kéo, đập, tia Vòng Hở, xóa nét, triệu hồi, thiên thạch).
 * Pha 3: Dạ Khuyết xuất hiện. Có tag world boss (cấm bay). Scale theo số người chơi.
 */
public class NguyenThuyTichDiet extends Monster implements GeoEntity, com.hukhong.boss.ai.BossBrain.Host {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(NguyenThuyTichDiet.class, EntityDataSerializers.INT);
    /** 1 = hoá thân (tu sĩ bị chiếm), 2 = Nguyên Thủy Tịch Diệt thật. Client đổi model/texture theo giá trị này. */
    private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(NguyenThuyTichDiet.class, EntityDataSerializers.INT);
    /** Tag bóng tạm trong cutscene Lv10 (không nhạc boss, không đấu trường, không thanh máu, không AI). */
    public static final String LV10_TAG = "hk_lv10_shade";
    public boolean lv10Shade() { return getTags().contains(LV10_TAG); }
    private static final ResourceLocation F1_HP = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "ntd_form1_hp");
    /** Dạng 1 có 40% lượng máu của dạng 2. */
    public static final double FORM1_HP_FRAC = 0.4;
    /** Cutscene biến hình: tổng tick và tick lộ dạng 2. */
    public static final int MORPH_TICKS = 180, MORPH_REVEAL = 110;
    private static final int GOLD = 0xFFD866, VOIDC = 0xA050FF, PALE = 0xE8D8FF;
    private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(new Vector3f(1f, 0.85f, 0.4f), 1.6f);
    private static final ResourceLocation HP_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "ntd_hp");
    private static final ResourceLocation DMG_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "ntd_dmg");
    private static final ResourceLocation SPD_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "ntd_speed");
    private static final DustParticleOptions TELEGRAPH = new DustParticleOptions(new Vector3f(0.7f, 0.2f, 1f), 1.5f);
    private static final BossEvent.BossBarColor[] COLORS = {BossEvent.BossBarColor.PURPLE, BossEvent.BossBarColor.PINK,
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarColor.RED};

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar = new ServerBossEvent(BossLevel.tag(Component.translatable("entity.hukhong.nguyen_thuy_tich_diet"), EndgameKit.BOSS_LEVEL),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_20);
    private int skillCd = 100, windup, pending = -1, last = -1, invulTicks, intro, appliedPlayers = 1, dkDelay = -1;
    private boolean daKhuyetSpawned;
    /** Tất sát (dạng 2, có Dạ Khuyết): hồi chiêu, tick gồng còn lại, tâm kết giới. */
    private int sureCd = 1300, sureT;
    private Vec3 barrierAt;
    public static final int SURE_TELE = 60;
    public static final float BARRIER_R = 7f;
    /** Dạ Khuyết đã hy sinh ở cutscene vào pha 4; Mộ Dao đã đến (pha 4). */
    private boolean dkSacrificed, moSpawned;
    private int moBeat;
    public static final int GRAND_T = 240;
    public boolean daKhuyetSacrificed() { return dkSacrificed; }
    private Vec3 pendingAt = Vec3.ZERO, pendingDir = new Vec3(0, 0, 1);
    /** Dạng 1: tick đã trôi của chiêu đang chạy; morph = tick còn lại của biến hình; cine = bất tử trong cutscene tối thượng. */
    private int actT, morph, cine, f1Ults;
    private final java.util.List<Vec3> marks = new java.util.ArrayList<>();
    private final java.util.List<Sched> sched = new java.util.ArrayList<>();
    final NttdExtraMoves.State xs = new NttdExtraMoves.State();
    private final java.util.List<Orb> orbs = new java.util.ArrayList<>();

    /** Việc hẹn giờ (theo tickCount) - chạy trong tick(), không lưu save; xoá khi biến hình/chết. */
    private record Sched(int at, Runnable r) {}

    /** Cầu sáng truy hồn (dạng 2): lơ lửng 20 tick rồi đuổi mục tiêu với góc lái giới hạn; tự tan sau 120 tick. */
    private static final class Orb {
        Vec3 pos, vel = Vec3.ZERO;
        final java.util.UUID tgt;
        int age;
        Orb(Vec3 p, java.util.UUID t) { pos = p; tgt = t; }
    }

    public NguyenThuyTichDiet(EntityType<? extends Monster> t, Level l) {
        super(t, l);
        addTag(FlightBans.WORLD_BOSS_TAG);
        xpReward = 0;
        setPersistenceRequired();
        setF1Hp(true);
        setHealth(getMaxHealth());
    }

    public static AttributeSupplier.Builder attrs() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, EndgameKit.BOSS_HP).add(Attributes.ATTACK_DAMAGE, EndgameKit.BOSS_DMG).add(Attributes.ARMOR, EndgameKit.BOSS_ARMOR)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 64);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PHASE, 1);
        b.define(FORM, 1);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // không MeleeAttackGoal: BossBrain lo di chuyển + đòn thường (PLAN-v1 §3)
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int phase() { return entityData.get(PHASE); }
    public int form() { return entityData.get(FORM); }
    public boolean morphing() { return morph > 0; }

    @Override protected EntityDimensions getDefaultDimensions(Pose pose) {
        return form() == 1 ? EntityDimensions.scalable(1.4f, 2.9f) : super.getDefaultDimensions(pose);
    }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (FORM.equals(key)) refreshDimensions();
    }

    private void setF1Hp(boolean on) {
        AttributeInstance inst = getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return;
        inst.removeModifier(F1_HP);
        if (on) inst.addPermanentModifier(new AttributeModifier(F1_HP, FORM1_HP_FRAC - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    @Override public Component getDisplayName() { return BossLevel.tag(super.getDisplayName(), EndgameKit.BOSS_LEVEL); }

    @Override public boolean isPushable() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }
    @Override public boolean isImmobile() { return super.isImmobile() || windup > 0 || intro > 0 || morph > 0 || sureT > 0; }

    /** Đồng minh cốt truyện (Dạ Khuyết, Mộ Dao) không bao giờ thành mục tiêu: không cướp aggro của người chơi. */
    @Override public void setTarget(LivingEntity t) {
        if (t != null && DaKhuyet.isStoryAlly(t)) return;
        super.setTarget(t);
    }

    @Override public boolean hurt(DamageSource src, float amt) {
        if ((intro > 0 || invulTicks > 0 || morph > 0 || cine > 0) && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (form() == 2 && isInvisible() && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false; // đang lặn trong Hư Không
        boolean r = super.hurt(src, amt);
        if (r && !level().isClientSide && form() == 1 && src.getEntity() != null) onHurtStreak();
        return r;
    }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); if (!lv10Shade()) bar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }
    @Override public void remove(RemovalReason r) {
        super.remove(r);
        bar.removeAllPlayers();
        if (r == RemovalReason.DISCARDED && level() instanceof ServerLevel sl && !lv10Shade()) NttdAllies.dismissMo(sl, position()); // trận bị huỷ: Mộ Dao rời đi
    }

    @Override public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide && !lv10Shade()) { com.hukhong.audio.BossMusic.watch(this, "final_boss"); com.hukhong.arena.BossArena.attach(this, EndgameKit.MAIN_RADIUS - 1, null, false, (level().dimension().location().getPath().equals("hu_khong_ban_nguyen") ? new net.minecraft.core.BlockPos(0, EndgameKit.CENTER_Y, 0) : blockPosition())); }
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("NtdPhase", phase());
        tag.putBoolean("NtdDak", daKhuyetSpawned);
        tag.putInt("NtdForm", form());
        tag.putInt("NtdF1Ults", f1Ults);
        tag.putBoolean("NtdDkSac", dkSacrificed);
        tag.putBoolean("NtdMo", moSpawned);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NtdPhase")) entityData.set(PHASE, Math.max(1, Math.min(EndgameKit.PHASES, tag.getInt("NtdPhase"))));
        daKhuyetSpawned = tag.getBoolean("NtdDak");
        entityData.set(FORM, tag.contains("NtdForm") ? Math.max(1, Math.min(2, tag.getInt("NtdForm"))) : 2);
        f1Ults = tag.getInt("NtdF1Ults");
        dkSacrificed = tag.getBoolean("NtdDkSac");
        moSpawned = tag.getBoolean("NtdMo");
        setF1Hp(form() == 1);
        setNoGravity(form() == 2);
        applyPhaseEffects(phase());
        bar.setName(barName());
    }

    private Component barName() {
        if (form() == 1) return Component.translatable("hukhong.endgame.boss.bar_form1", BossLevel.tag(Component.translatable("entity.hukhong.nguyen_thuy_tich_diet.form1"), EndgameKit.BOSS_LEVEL));
        return Component.translatable("hukhong.endgame.boss.bar", BossLevel.tag(Component.translatable("entity.hukhong.nguyen_thuy_tich_diet"), EndgameKit.BOSS_LEVEL), phase());
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
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("entity.hukhong.nguyen_thuy_tich_diet.form1")));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("hukhong.endgame.boss.line.1")));
        }
        // cutscene xuất hiện: camera từ xa lia vào Vòng Tròn Hở
        int T = EndgameKit.INTRO_TICKS;
        double k = Math.max(2.0, getBbHeight() * 0.5);
        com.hukhong.boss.phase.BossPhaseDirector.sendCutscene(sl, this, T, com.hukhong.boss.phase.BossPhaseDirector.introCam(T, k), java.util.List.of(
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(8, 40, "entity.hukhong.nguyen_thuy_tich_diet.form1"),
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(40, 85, "hukhong.dialog.nttd.intro.1"),
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(85, T - 4, "hukhong.dialog.nttd.intro.2")));
        Fx.rift(sl, null, position().add(0, 1.5, 0).subtract(pendingDir.scale(2)), new Vec3(1, 0, 0), 2.5f, VOIDC, T);
    }

    // ------------------------------------------------------------ server AI
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel sl = (ServerLevel) level();
        bar.setProgress(getHealth() / getMaxHealth());
        if (intro > 0 || morph > 0) return;
        if (tickCount % 100 == 1) rescale(sl);
        if (form() == 1) { form1Ai(sl); return; }

        int ph = EndgameKit.phase(getHealth() / getMaxHealth());
        if (ph > phase()) enterPhase(sl, ph);
        if (dkDelay > 0 && --dkDelay == 0) { dkDelay = -1; if (!daKhuyetSpawned) spawnDaKhuyet(sl); }
        if (invulTicks > 0) {
            invulTicks--;
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 3, getZ(), 6, 1.5, 3, 1.5, 0.05);
            return;
        }
        if (tickCount % 10 == 0)
            sl.sendParticles(phase() >= 3 ? ParticleTypes.SOUL_FIRE_FLAME : ModParticles.HU_KHONG.get(),
                    getX(), getY() + 3, getZ(), 8, 1.6, 2.5, 1.6, 0.02);

        LivingEntity t = getTarget();
        if (pending >= 0) { stepWindup(sl); return; }
        // tất sát: chỉ khi Dạ Khuyết đồng minh còn ở đây để chắn (pha 3)
        DaKhuyet dk = allyDk(sl);
        if (dk != null && phase() < 4 && own == O_NONE && sureT == 0 && !com.hukhong.boss.ai.BossBrain.busy(this) && --sureCd <= 0) { startSureKill(sl, dk); return; }
        if (skillCd > -1000) skillCd--;
        if (t == null || !t.isAlive() || distanceToSqr(t) > 40 * 40) return;
        // chiêu lớn do BossBrain xin qua brainBigMove(); dự phòng nếu lâu không được xin (brain bận / không có mục tiêu hợp lệ)
        if (skillCd <= -160 && own == O_NONE && !com.hukhong.boss.ai.BossBrain.busy(this)) startF2Skill(sl, t, -1);
    }

    /** Bắt đầu một chiêu EndgameKit (dạng 2); forced &lt; 0 = chọn ngẫu nhiên theo pha. */
    private void startF2Skill(ServerLevel sl, LivingEntity t, int forced) {
        int pick;
        if (forced >= 0) pick = forced;
        else {
            int n = EndgameKit.skillsAvailable(phase());
            pick = random.nextInt(n);
            if (n > 1 && pick == last) pick = (pick + 1) % n;
            if (EndgameKit.SKILLS[pick].shape() == EndgameKit.Shape.SUMMON && minionCount(sl) >= 8) pick = 1;
        }
        Skill k = EndgameKit.SKILLS[pick];
        last = pick;
        dbgBigMoves.add(k.id());
        pending = pick;
        windup = k.windup();
        pendingAt = t.position();
        Vec3 d = t.position().subtract(position()).multiply(1, 0, 1);
        pendingDir = d.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : d.normalize();
        triggerAnim("skill", k.anim());
        playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2f, 0.5f);
        startTelegraph(sl, k);
    }

    /**
     * isImmobile() (intro/windup &gt; 0) khiến LivingEntity.aiStep BỎ QUA serverAiStep -&gt; customServerAiStep không chạy,
     * nên bộ đếm intro/windup phải giảm ở tick() (luôn chạy). Trước đây đếm trong customServerAiStep =&gt; intro kẹt 80,
     * hurt() chặn mãi: người chơi không thể gây sát thương (và windup kẹt: boss đứng im sau chiêu đầu).
     */
    @Override public void tick() {
        super.tick();
        if (lv10Shade()) return; // bóng cutscene Lv10: đứng yên, không chạy brain/chiêu
        if (level() instanceof ServerLevel sl && isAlive()) {
            if (cine > 0) cine--;
            runSched();
            if (!orbs.isEmpty()) tickOrbs(sl);
            if (morph > 0) { bar.setProgress(getHealth() / getMaxHealth()); stepMorph(sl); }
            else if (intro > 0) { bar.setProgress(getHealth() / getMaxHealth()); stepIntro(sl); }
            else if (windup > 0 && pending >= 0) { if (form() == 1) stepF1(sl); else stepWindup(sl); }
            if (sureT > 0) stepSureKill(sl);
            if (form() == 2 && morph == 0 && intro == 0) moAllyTick(sl);
            if (morph == 0 && intro == 0) brainSideTick(sl);
            com.hukhong.boss.ai.BossBrain.tickFor(sl, this, this); // tự nhường khi frozen()/ownMoveActive(); profile đổi theo DẠNG
        }
    }

    private void stepIntro(ServerLevel sl) {
        int el = EndgameKit.INTRO_TICKS - intro;
        intro--;
        getNavigation().stop();
        sl.sendParticles(ModParticles.HU_KHONG.get(), getX(), getY() + getBbHeight() * 0.5, getZ(), 8, 1.2, 1.5, 1.2, 0.04);
        if (el == 40) Fx.rune(sl, null, position().add(0, 0.1, 0), 5f, VOIDC, 80, 5);
        if (el == 85) {
            Fx.groundCrack(sl, null, position(), 7f, VOIDC, 6, 50);
            Fx.shake(sl, position(), 2f, 12, 40);
            playSound(SoundEvents.WARDEN_ROAR, 3f, 0.8f);
        }
    }

    // ------------------------------------------------------------ hẹn giờ, tiện ích chung
    void later(int delay, Runnable r) { sched.add(new Sched(tickCount + Math.max(1, delay), r)); }

    private void runSched() {
        if (sched.isEmpty()) return;
        java.util.List<Sched> due = new java.util.ArrayList<>();
        sched.removeIf(x -> { if (x.at() <= tickCount) { due.add(x); return true; } return false; });
        for (Sched x : due) if (isAlive()) x.r().run();
    }

    java.util.List<Player> players(ServerLevel sl, Vec3 c, double r) {
        return sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r, 6, r),
                p -> !p.isSpectator() && !p.isCreative() && p.position().multiply(1, 0, 1).distanceToSqr(c.multiply(1, 0, 1)) <= r * r);
    }

    double atk(double mul) { return getAttributeValue(Attributes.ATTACK_DAMAGE) * mul; }

    private Vec3 faceTo(Vec3 at) {
        Vec3 d = at.subtract(position()).multiply(1, 0, 1);
        return d.lengthSqr() < 1e-4 ? pendingDir : d.normalize();
    }

    private static Vec3 rotY(Vec3 d, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(d.x * c - d.z * s, 0, d.x * s + d.z * c);
    }

    /** Sóng sáng/hư lan ra từ c (nhảy để né): trúng người đứng đất trong dải ±1.1 quanh bán kính hiện tại, mỗi người 1 lần. */
    private void wave(ServerLevel sl, Vec3 c, int delay, double maxR, double speed, int rgb, double dmg) {
        int steps = (int) Math.ceil(maxR / speed);
        java.util.Set<java.util.UUID> done = new java.util.HashSet<>();
        later(delay, () -> {
            Fx.groundRing(sl, null, c, (float) maxR, rgb, steps);
            playSound(SoundEvents.BEACON_DEACTIVATE, 3f, 1.6f);
        });
        for (int i = 0; i <= steps; i++) {
            double r = 1.5 + speed * i;
            boolean puff = i % 2 == 0;
            later(delay + i, () -> {
                if (puff) {
                    int pts = (int) Math.min(72, 10 + r * 3);
                    for (int j = 0; j < pts; j++) {
                        double a = j * Math.PI * 2 / pts;
                        sl.sendParticles(rgb == GOLD ? GOLD_DUST : TELEGRAPH, c.x + Math.cos(a) * r, c.y + 0.3, c.z + Math.sin(a) * r, 1, 0, 0.1, 0, 0);
                    }
                }
                for (Player p : players(sl, c, r + 1.2)) {
                    if (done.contains(p.getUUID()) || !p.onGround() || Math.abs(p.getY() - c.y) > 2.5) continue;
                    double d = Math.sqrt(p.position().multiply(1, 0, 1).distanceToSqr(c.multiply(1, 0, 1)));
                    if (Math.abs(d - r) < 1.1) { done.add(p.getUUID()); hit(p, dmg, c, true); }
                }
            });
        }
    }

    // ------------------------------------------------------------ DẠNG 1: hoá thân bị chiếm (moveset kiểu Radagon)
    private static final int A_SLAM = 0, A_COMBO = 1, A_TELE = 2, A_PILLAR = 3, A_ULT = 4;
    private static final int[] A_LEN = {44, 48, 36, 40, 112, 40, 44, 42};   // 5-7: NttdExtraMoves (chain/star/shock)
    private static final String[] A_ANIM = {"f1_slam", "f1_combo", "f1_teleport", "f1_pillars", "f1_ult", "f1_pillars", "f1_slam", "f1_combo"};

    private void form1Ai(ServerLevel sl) {
        if (tickCount % 8 == 0)
            sl.sendParticles(ModParticles.HU_KHONG.get(), getX(), getY() + 1.6, getZ(), 3, 0.5, 0.9, 0.5, 0.02);
        LivingEntity t = getTarget();
        if (pending >= 0) { stepF1(sl); return; }
        if (skillCd > -1000) skillCd--;
        if (t == null || !t.isAlive() || distanceToSqr(t) > 40 * 40) return;
        float hp = getHealth() / getMaxHealth();
        boolean free = own == O_NONE && !com.hukhong.boss.ai.BossBrain.busy(this);
        if (free && ((f1Ults == 0 && hp < 0.5f) || (f1Ults == 1 && hp < 0.2f))) { f1Ults++; beginF1(sl, t, A_ULT); return; }
        if (free && skillCd <= -160) startF1Skill(sl, t); // dự phòng khi brain lâu không xin chiêu lớn
    }

    /** Chiêu riêng dạng 1 (không gồm tối thượng) - brain xin qua brainBigMove(). */
    private void startF1Skill(ServerLevel sl, LivingEntity t) {
        double d = Math.sqrt(distanceToSqr(t));
        int r = random.nextInt(100), a;
        if (d > 12) a = r < 60 ? A_TELE : A_PILLAR;
        else if (d < 6) a = r < 40 ? A_SLAM : r < 85 ? A_COMBO : A_PILLAR;
        else a = r < 35 ? A_TELE : r < 60 ? A_SLAM : r < 85 ? A_PILLAR : A_COMBO;
        a = NttdExtraMoves.f1Swap(random, d, a);   // ~35%: chiêu bổ sung (xích / sao / chấn)
        if (a == last) a = (a + 1 + random.nextInt(3)) % 4;
        beginF1(sl, t, a);
    }

    private void beginF1(ServerLevel sl, LivingEntity t, int a) {
        dbgBigMoves.add(A_ANIM[a]);
        last = a;
        pending = a;
        actT = 0;
        windup = A_LEN[a];
        pendingAt = t.position();
        pendingDir = faceTo(t.position());
        marks.clear();
        triggerAnim("skill", A_ANIM[a]);
        playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2f, 0.7f);
    }

    private void stepF1(ServerLevel sl) {
        LivingEntity t = getTarget();
        getNavigation().stop();
        int at = ++actT;
        if (t != null && pending != A_TELE) getLookControl().setLookAt(t, 30, 30);
        switch (pending) {
            case A_SLAM -> f1Slam(sl, at);
            case A_COMBO -> f1Combo(sl, t, at);
            case A_TELE -> f1Teleport(sl, t, at);
            case A_PILLAR -> f1Pillars(sl, at);
            case A_ULT -> f1Ult(sl, at);
            case NttdExtraMoves.A_CHAIN, NttdExtraMoves.A_STAR, NttdExtraMoves.A_SHOCK -> NttdExtraMoves.f1(sl, this, pending, at, pendingAt, marks);
            default -> { }
        }
        if (--windup <= 0) {
            pending = -1;
            windup = 0;
            setInvisible(false);
            skillCd = com.hukhong.events.BossBuff.cd(getHealth() / getMaxHealth() < 0.5f ? 16 + random.nextInt(10) : 26 + random.nextInt(15));
        }
    }

    /** "Trì Chùy" - giơ búa giữ lâu (đòn trễ kiểu Radagon) rồi đập; 12 tick sau nổ hư không lớn hơn tại cùng chỗ. */
    private void f1Slam(ServerLevel sl, int at) {
        Vec3 c = position().add(pendingDir.scale(3.0));
        if (at == 1) Fx.telegraphCircle(sl, null, c, 4.5f, 27);
        if (at == 28) {
            for (Player p : players(sl, c, 4.5)) hit(p, atk(1.3), c, true);
            Fx.groundCrack(sl, null, c, 5f, VOIDC, 6, 40);
            Fx.shake(sl, c, 2.5f, 10, 24);
            sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 3, 1, 0.2, 1, 0);
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.6f);
            Fx.telegraphCircle(sl, null, c, 7f, 12, VOIDC);
        }
        if (at == 40) {
            for (Player p : players(sl, c, 7)) hit(p, atk(0.9), c, true);
            Fx.groundRing(sl, null, c, 7.5f, VOIDC, 20);
            Fx.elem(sl, null, Fx.E_HALOS, c.add(0, 0.5, 0), Vec3.ZERO, 6f, VOIDC, 20);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1, c.z, 80, 3, 1, 3, 0.3);
            playSound(SoundEvents.WARDEN_SONIC_BOOM, 3f, 0.7f);
        }
    }

    /** "Liên Hoàn Chùy" - 3 nhát quạt (nhịp nhanh-nhanh-trễ), mỗi nhát quay theo mục tiêu và lướt tới; nhát 3 xé không gian. */
    private void f1Combo(ServerLevel sl, LivingEntity t, int at) {
        int[] hitAt = {12, 22, 36};
        for (int i = 0; i < 3; i++) {
            int lead = i == 2 ? 12 : 10;
            if (at == hitAt[i] - lead) {
                if (t != null) pendingDir = faceTo(t.position());
                Fx.telegraphCone(sl, null, position(), pendingDir, i == 2 ? 6.5f : 5.5f, i == 2 ? 75f : 65f, lead);
            }
            if (at == hitAt[i]) {
                double r = i == 2 ? 6.5 : 5.5, half = i == 2 ? 75 : 65;
                for (Player p : players(sl, position(), r)) {
                    Vec3 rel = p.position().subtract(position()).multiply(1, 0, 1);
                    if (rel.lengthSqr() < 1 || Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, rel.normalize().dot(pendingDir))))) <= half)
                        hit(p, atk(i == 2 ? 1.0 : 0.7), position(), i == 2);
                }
                setDeltaMovement(getDeltaMovement().add(pendingDir.scale(0.6)));
                hurtMarked = true;
                Fx.qi(sl, position().add(0, 1.5, 0), pendingDir, (float) r, VOIDC, i == 0 ? 30 : i == 1 ? -30 : 90, 10);
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2f, 0.6f);
                if (i == 2) {
                    Fx.rift(sl, null, position().add(pendingDir.scale(4)).add(0, 0.2, 0), pendingDir.scale(8), 1.2f, VOIDC, 30);
                    Fx.groundCrack(sl, null, position().add(pendingDir.scale(3)), 4f, VOIDC, 4, 30);
                }
            }
        }
    }

    /** "Thuấn Hư Kích" - tan vào khe hư không, hiện sau lưng mục tiêu, vòng cảnh báo 14 tick rồi bổ xuống. */
    private void f1Teleport(ServerLevel sl, LivingEntity t, int at) {
        if (at == 1) {
            Fx.rift(sl, null, position().add(0, 1.4, 0), rotY(pendingDir, 90), 1.8f, VOIDC, 24);
            sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.4, getZ(), 60, 0.6, 1.2, 0.6, 0.6);
        }
        if (at == 8) setInvisible(true);
        if (at == 12) {
            Vec3 tp = t != null ? t.position() : pendingAt;
            Vec3 back = t != null ? Vec3.directionFromRotation(0, t.getYRot()).multiply(1, 0, 1) : pendingDir;
            back = back.lengthSqr() < 1e-4 ? pendingDir : back.normalize();
            Vec3 land = tp.subtract(back.scale(2.6));
            if (!sl.noCollision(this, getBoundingBox().move(land.subtract(position())))) {
                Vec3 away = position().subtract(tp).multiply(1, 0, 1);
                land = away.lengthSqr() < 1e-4 ? position() : tp.add(away.normalize().scale(2.6));
            }
            teleportTo(land.x, land.y, land.z);
            pendingDir = faceTo(tp);
            pendingAt = land.add(pendingDir.scale(1.6));
            setInvisible(false);
            Fx.rift(sl, null, land.add(0, 1.4, 0), rotY(pendingDir, 90), 1.8f, VOIDC, 20);
            Fx.telegraphCircle(sl, null, pendingAt, 3.5f, 14);
            playSound(SoundEvents.ENDERMAN_TELEPORT, 2f, 0.5f);
        }
        if (at == 26) {
            for (Player p : players(sl, pendingAt, 3.5)) hit(p, atk(1.1), pendingAt, true);
            Fx.groundRing(sl, null, pendingAt, 4f, VOIDC, 16);
            Fx.qi(sl, pendingAt.add(0, 1.5, 0), pendingDir, 4f, VOIDC, 90, 10);
            sl.sendParticles(ParticleTypes.SONIC_BOOM, pendingAt.x, pendingAt.y + 1, pendingAt.z, 1, 0, 0, 0, 0);
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.8f);
        }
    }

    /** "Hư Trụ Phún" - cắm búa xuống, cột hư không phun lên dưới chân từng người chơi (+3 điểm ngẫu nhiên). */
    private void f1Pillars(ServerLevel sl, int at) {
        if (at == 1) {
            for (Player p : players(sl, position(), 30)) marks.add(p.position());
            for (int i = 0; i < 3; i++) {
                double a = random.nextDouble() * Math.PI * 2, d = 4 + random.nextDouble() * 9;
                marks.add(position().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            }
            for (Vec3 m : marks) Fx.telegraphCircle(sl, null, m, 2.5f, 29);
        }
        if (at == 30) {
            Fx.groundCrack(sl, null, position(), 3f, VOIDC, 5, 30);
            Fx.shake(sl, position(), 1.5f, 8, 30);
            for (Vec3 m : marks) {
                Fx.elem(sl, null, Fx.E_SPIKES, m, Vec3.ZERO, 2.5f, VOIDC, 24);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, m.x, m.y + 2, m.z, 25, 0.5, 2, 0.5, 0.05);
                for (Player p : players(sl, m, 2.5)) {
                    hit(p, atk(0.9), m, false);
                    p.setDeltaMovement(p.getDeltaMovement().add(0, 1.0, 0));
                    p.hurtMarked = true;
                }
            }
            playSound(SoundEvents.EVOKER_FANGS_ATTACK, 3f, 0.6f);
        }
    }

    /** Tối thượng dạng 1 "Hư Chùy Thiên Phạt": cutscene; cự chùy hư không giáng xuống chỗ từng người, rồi 2 sóng hư (nhảy né). */
    private void f1Ult(ServerLevel sl, int at) {
        if (at == 1) {
            cine = 50;
            ultCutscene(sl, 50, "ult1");
            playSound(SoundEvents.WITHER_SPAWN, 3f, 0.6f);
        }
        if (at == 10) Fx.rune(sl, null, position().add(0, 12, 0), 8f, VOIDC, 80, 6);
        if (at < 50 && at % 3 == 0) sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 2, getZ(), 30, 6, 3, 6, 0.6);
        if (at == 50) {
            for (Player p : players(sl, position(), 40)) marks.add(p.position());
            if (marks.isEmpty()) marks.add(position().add(pendingDir.scale(5)));
            for (Vec3 m : marks) {
                Fx.telegraphCircle(sl, null, m, 5f, 30);
                Fx.giantSword(sl, null, m, 4f, VOIDC, 30, 50);
            }
        }
        if (at == 80) {
            for (Vec3 m : marks) {
                for (Player p : players(sl, m, 5)) hit(p, atk(2.0), m, true);
                Fx.groundCrack(sl, null, m, 6f, VOIDC, 8, 50);
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, m.x, m.y + 0.5, m.z, 1, 0, 0, 0, 0);
            }
            Fx.shake(sl, position(), 4f, 20, 50);
            Fx.telegraphCircle(sl, null, position(), 3f, 6, VOIDC);
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 4f, 0.4f);
            wave(sl, position(), 6, 24, 1.2, VOIDC, atk(0.8));
            wave(sl, position(), 20, 24, 1.2, VOIDC, atk(0.8));
        }
    }

    /** Cutscene ngắn cho chiêu tối thượng: cận -> lùi rộng, 2 dòng hukhong.dialog.nttd.&lt;key&gt;.1/2. */
    private void ultCutscene(ServerLevel sl, int T, String key) {
        float s = (float) Math.max(2.0, getBbHeight() * 0.5);
        var cam = java.util.List.of(
                new com.hukhong.boss.phase.CamPath.Key(0, 0.6f * s, 0.5f * s, 1.5f * s, 0, 0, 50, 0.9f * s),
                new com.hukhong.boss.phase.CamPath.Key(T / 2, -1.4f * s, 0.2f * s, 2.2f * s, 0, -8, 60, 1.0f * s),
                new com.hukhong.boss.phase.CamPath.Key(T, 0, 1.6f * s, 4.5f * s, 0, 4, 78, 0.8f * s));
        com.hukhong.boss.phase.BossPhaseDirector.sendCutscene(sl, this, T, cam, java.util.List.of(
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(4, T / 2, "hukhong.dialog.nttd." + key + ".1"),
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(T / 2, T - 2, "hukhong.dialog.nttd." + key + ".2")));
    }

    // ------------------------------------------------------------ BIẾN HÌNH dạng 1 -> 2
    /** Bắt đầu biến hình (thường do dạng 1 hết máu; GameTest/lệnh có thể gọi trực tiếp). */
    public void startMorph(ServerLevel sl) {
        if (form() != 1 || morph > 0) return;
        morph = MORPH_TICKS;
        own = O_NONE; queued = O_NONE; bellyT = 0;
        pending = -1;
        windup = 0;
        intro = 0;
        sched.clear();
        orbs.clear();
        setInvisible(false);
        setHealth(1f);
        getNavigation().stop();
        triggerAnim("skill", "f1_transform");
        playSound(SoundEvents.WARDEN_DEATH, 4f, 0.5f);
        for (Monster m : sl.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(40), e -> e instanceof com.hukhong.entity.HuMob)) m.discard();
        int T = MORPH_TICKS;
        var cam = java.util.List.of(
                new com.hukhong.boss.phase.CamPath.Key(0, 0.8f, 1.6f, 3.2f, 0, 0, 50, 1.8f),
                new com.hukhong.boss.phase.CamPath.Key(50, -2.8f, 1.0f, 3.6f, 0, -6, 55, 2.0f),
                new com.hukhong.boss.phase.CamPath.Key(MORPH_REVEAL - 6, 0, 0.8f, 6.0f, 0, -10, 62, 2.6f),
                new com.hukhong.boss.phase.CamPath.Key(MORPH_REVEAL + 20, 3.5f, 2.5f, 10.5f, 0, -4, 74, 4.0f),
                new com.hukhong.boss.phase.CamPath.Key(T, 0, 4.0f, 14.0f, 0, 0, 78, 4.2f));
        com.hukhong.boss.phase.BossPhaseDirector.sendCutscene(sl, this, T, cam, java.util.List.of(
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(6, 45, "hukhong.dialog.nttd.transform.1"),
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(45, 85, "hukhong.dialog.nttd.transform.2"),
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(85, MORPH_REVEAL + 15, "hukhong.dialog.nttd.transform.3"),
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(MORPH_REVEAL + 15, T - 4, "hukhong.dialog.nttd.transform.4")));
    }

    private void stepMorph(ServerLevel sl) {
        int el = MORPH_TICKS - morph;
        morph--;
        getNavigation().stop();
        Vec3 c = position();
        if (el < MORPH_REVEAL) {
            // thân xác nứt: khe sáng mở dần, ánh hư không tuôn ra
            if (el % 8 == 0) {
                Vec3 dir = rotY(new Vec3(1, 0, 0), random.nextInt(360)).add(0, random.nextDouble() - 0.5, 0);
                Fx.rift(sl, null, c.add(0, 0.8 + random.nextDouble() * 1.6, 0), dir.scale(0.8 + el / 60.0), 0.3f + el / 120f, PALE, 30);
            }
            sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1.5, c.z, 2 + el / 12, 0.4, 0.8, 0.4, 0.06 + el * 0.002);
            if (el % 4 == 0) sl.sendParticles(ModParticles.HU_KHONG.get(), c.x, c.y + 1.4, c.z, 10, 0.6, 1, 0.6, 0.12);
            if (el == 20) { Fx.groundCrack(sl, null, c, 8f, VOIDC, 8, 90); Fx.shake(sl, c, 1.5f, 30, 40); }
            if (el == 60) { Fx.rune(sl, null, c.add(0, 14, 0), 10f, GOLD, 120, 8); playSound(SoundEvents.BEACON_ACTIVATE, 4f, 0.5f); }
            if (el >= 70 && el % 6 == 0) Fx.ribbon(sl, null, c.add(0, 1.5, 0), c.add(random.nextGaussian() * 2, 16, random.nextGaussian() * 2), 0, 10, 0, 0.4f, GOLD);
            if (el == MORPH_REVEAL - 20) Fx.shake(sl, c, 3f, 20, 50);
        } else if (el == MORPH_REVEAL) {
            reveal(sl);
        } else if (el % 5 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 3, c.z, 10, 2, 3, 2, 0.05);
        }
        if (morph == 0) {
            skillCd = 40;
            bar.setName(barName());
        }
    }

    private void reveal(ServerLevel sl) {
        entityData.set(FORM, 2);
        entityData.set(PHASE, 1);
        setNoGravity(true); // dạng 2 không đi bộ: trôi lơ lửng (Locomotion.hover)
        setInvisible(false);
        refreshDimensions();
        setF1Hp(false);
        setHealth(getMaxHealth());
        applyPhaseEffects(1);
        bar.setName(barName());
        last = -1;
        triggerAnim("skill", "phase");
        Vec3 c = position();
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 2, c.z, 2, 1, 1, 1, 0);
        sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 3, c.z, 200, 3, 4, 3, 0.4);
        Fx.groundRing(sl, null, c, 18f, GOLD, 40);
        Fx.rune(sl, null, c.add(0, 0.1, 0), 12f, GOLD, 100, 8);
        Fx.shake(sl, c, 4f, 25, 60);
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 5f, 0.4f);
        // đẩy người chơi đứng sát ra ngoài để không kẹt trong hitbox lớn của dạng 2
        for (Player p : players(sl, c, 5)) {
            Vec3 away = p.position().subtract(c).multiply(1, 0, 1);
            away = away.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : away.normalize();
            p.setDeltaMovement(away.x * 1.4, 0.5, away.z * 1.4);
            p.hurtMarked = true;
        }
    }

    // ------------------------------------------------------------ DẠNG 2: cầu truy hồn + telegraph Fx
    private void tickOrbs(ServerLevel sl) {
        var it = orbs.iterator();
        while (it.hasNext()) {
            Orb o = it.next();
            o.age++;
            Entity tg = sl.getEntity(o.tgt);
            if (o.age < 20 || !(tg instanceof Player pt) || !pt.isAlive()) {
                o.vel = o.vel.scale(0.8).add(0, o.age < 20 ? 0.02 : 0, 0);
            } else {
                Vec3 want = pt.position().add(0, 0.9, 0).subtract(o.pos).normalize().scale(0.34);
                o.vel = o.vel.scale(0.88).add(want.scale(0.12));
                if (o.vel.length() > 0.34) o.vel = o.vel.normalize().scale(0.34);
            }
            o.pos = o.pos.add(o.vel);
            sl.sendParticles(GOLD_DUST, o.pos.x, o.pos.y, o.pos.z, 3, 0.12, 0.12, 0.12, 0);
            sl.sendParticles(ParticleTypes.END_ROD, o.pos.x, o.pos.y, o.pos.z, 1, 0.05, 0.05, 0.05, 0.01);
            boolean boom = false;
            for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(o.pos, o.pos).inflate(1.3), p -> !p.isSpectator() && !p.isCreative())) {
                hit(p, atk(0.7), o.pos, false);
                boom = true;
            }
            if (boom || o.age > 120 || !sl.getBlockState(net.minecraft.core.BlockPos.containing(o.pos)).isAir()) {
                sl.sendParticles(ParticleTypes.FLASH, o.pos.x, o.pos.y, o.pos.z, 1, 0, 0, 0, 0);
                if (boom) Fx.groundRing(sl, null, o.pos.subtract(0, 0.9, 0), 2f, GOLD, 10);
                it.remove();
            }
        }
    }

    /** Telegraph Fx một lần lúc bắt đầu gồng (dạng 2). */
    private void startTelegraph(ServerLevel sl, Skill k) {
        int w = windup;
        switch (k.shape()) {
            case SWEEP -> Fx.telegraphCone(sl, null, position(), pendingDir, (float) k.radius(), 60f, w);
            case WAVES -> { Fx.telegraphCircle(sl, null, position(), (float) k.radius(), w, GOLD); Fx.rune(sl, null, position().add(0, 0.1, 0), 4f, GOLD, w, 6); }
            case ORBS -> Fx.rune(sl, null, position().add(0, getBbHeight() + 2, 0), 3f, GOLD, w + 10, 5);
            case STARS -> Fx.rune(sl, null, position().add(0, 18, 0), 9f, GOLD, w + 60, 7);
            case NOVA -> {
                cine = 50;
                ultCutscene(sl, 50, "nova");
                Fx.rune(sl, null, position().add(0, 20, 0), 14f, GOLD, w + 20, 8);
                later(50, () -> {
                    Fx.telegraphCircle(sl, null, position(), 7f, w - 50);
                    Fx.telegraphCircle(sl, null, position(), (float) k.radius(), w - 50, GOLD);
                });
            }
            case METEOR -> Fx.telegraphCircle(sl, null, pendingAt, (float) k.radius(), w);
            case SLAM -> Fx.telegraphCircle(sl, null, position(), (float) k.radius(), w);
            default -> { }
        }
    }


    private void stepWindup(ServerLevel sl) {
        LivingEntity t = getTarget();
        getNavigation().stop();
        EndgameKit.Shape sh = EndgameKit.SKILLS[pending].shape();
        if (t != null && sh != EndgameKit.Shape.BEAM && sh != EndgameKit.Shape.SWEEP) getLookControl().setLookAt(t, 30, 30);
        Skill k = EndgameKit.SKILLS[pending];
        if (windup % 2 == 0) telegraph(sl, k);
        if (--windup <= 0) {
            fire(sl, k);
            pending = -1;
            skillCd = com.hukhong.events.BossBuff.cd(EndgameKit.cooldown(phase()));
        }
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
            case SWEEP, ORBS, STARS -> sl.sendParticles(GOLD_DUST, getX(), getY() + getBbHeight() * 0.7, getZ(), 6, 1.5, 1.5, 1.5, 0.02);
            case WAVES, NOVA -> sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 6, 2, 1, 2, 0.05);
            default -> ring(sl, position(), k.radius());
        }
    }

    void hit(Player p, double dmg, Vec3 from, boolean knock) {
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
            case SWEEP -> {  // "Quét Quang": tia sáng quét quạt 120° trong 15 tick
                java.util.Set<java.util.UUID> done = new java.util.HashSet<>();
                Vec3 base = pendingDir;
                for (int i = 0; i <= 14; i++) {
                    double ang = -60 + i * (120.0 / 14);
                    later(i + 1, () -> {
                        Vec3 d = rotY(base, ang);
                        Vec3 from = position().add(0, getBbHeight() * 0.6, 0);
                        Fx.ribbon(sl, null, from, position().add(d.scale(r)).add(0, 0.4, 0), 0, 3, 0, 0.5f, GOLD);
                        for (double s = 2; s < r; s += 1.5)
                            sl.sendParticles(ParticleTypes.END_ROD, getX() + d.x * s, getY() + 0.5, getZ() + d.z * s, 1, 0.1, 0.1, 0.1, 0);
                        for (Player p : players(sl, position(), r)) {
                            if (done.contains(p.getUUID())) continue;
                            Vec3 rel = p.position().subtract(position()).multiply(1, 0, 1);
                            double along = rel.dot(d), side = Math.abs(rel.x * d.z - rel.z * d.x);
                            if (along > 0 && side < 1.8) { done.add(p.getUUID()); hit(p, dmg, position(), true); }
                        }
                    });
                }
                playSound(SoundEvents.BEACON_POWER_SELECT, 3f, 0.6f);
            }
            case WAVES -> {  // "Sóng Quang Luân": 3 vòng sóng sáng nở ra - nhảy đúng nhịp
                for (int i = 0; i < 3; i++) wave(sl, position(), 1 + i * 12, r, 1.0, GOLD, dmg);
                Fx.shake(sl, position(), 1.5f, 10, 30);
            }
            case ORBS -> {  // "Cầu Truy Hồn": 5 cầu sáng lơ lửng rồi đuổi theo người chơi
                java.util.List<Player> ps = players(sl, position(), r);
                if (!ps.isEmpty()) for (int i = 0; i < 5; i++) {
                    double a = i * Math.PI * 2 / 5;
                    Vec3 at = position().add(Math.cos(a) * 3, getBbHeight() * 0.8, Math.sin(a) * 3);
                    orbs.add(new Orb(at, ps.get(i % ps.size()).getUUID()));
                    Fx.groundRing(sl, null, at, 1f, GOLD, 10);
                }
                playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 3f, 0.6f);
            }
            case STARS -> {  // "Mưa Tinh Tú": 16 sao rơi lần lượt quanh người chơi, mỗi sao có vòng cảnh báo 18 tick
                java.util.List<Player> ps = players(sl, position(), 40);
                for (int i = 0; i < 16 && !ps.isEmpty(); i++) {
                    Player tp = ps.get(i % ps.size());
                    int idx = i;
                    later(1 + i * 3, () -> {
                        Vec3 at = tp.position().add(random.nextGaussian() * 2.5, 0, random.nextGaussian() * 2.5);
                        Fx.telegraphCircle(sl, null, at, (float) r, 18, GOLD);
                        Fx.ribbon(sl, null, at.add(random.nextGaussian() * 4, 20, random.nextGaussian() * 4), at, 0, 6, 12, 0.35f, GOLD);
                        later(18, () -> {
                            for (Player p : players(sl, at, r)) hit(p, dmg, at, idx % 4 == 3);
                            Fx.groundRing(sl, null, at, (float) r + 0.4f, GOLD, 14);
                            sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.5, at.z, 20, 0.6, 0.6, 0.6, 0.2);
                            sl.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, net.minecraft.sounds.SoundSource.HOSTILE, 2f, 0.6f);
                        });
                    });
                }
            }
            case NOVA -> {  // tối thượng "Kim Hoàn Diệt": nổ tâm 7 block + 3 vòng nhẫn vàng lan tới 30 block (nhảy né)
                for (Player p : players(sl, position(), 7)) hit(p, dmg * 1.5, position(), true);
                Fx.elem(sl, null, Fx.E_NUKE, position().add(0, 1, 0), Vec3.ZERO, 7f, GOLD, 30);
                Fx.groundRing(sl, null, position(), 8f, GOLD, 20);
                Fx.shake(sl, position(), 4f, 25, 60);
                sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 3, getZ(), 3, 1, 1, 1, 0);
                for (int i = 0; i < 3; i++) wave(sl, position(), 4 + i * 10, r, 1.4, GOLD, dmg * 0.7);
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
        sureT = 0;
        DaKhuyet dk = ph == 4 ? allyDk(sl) : null;
        if (dk != null) { invulTicks = GRAND_T; grandSacrifice(sl, dk); } // pha 3 -> 4: Dạ Khuyết đỡ đại chiêu, hy sinh; Mộ Dao đến
        else {
            phaseCutscene(sl, ph);
            if (ph == 4 && !moSpawned) later(EndgameKit.PHASE_INVUL_TICKS - 10, () -> arriveMo(sl, true));
        }
        pending = -1;
        windup = 0;
        skillCd = invulTicks + 20;
        triggerAnim("skill", "phase");
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 2, getZ(), 1, 0, 0, 0, 0);
        sl.sendParticles(ModParticles.HU_KHONG.get(), getX(), getY() + 2, getZ(), 120, 3, 2.5, 3, 0.12);
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 4f, 0.5f);
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) > 100 * 100) continue;
            p.displayClientMessage(Component.translatable("hukhong.endgame.boss.phase", ph), true);
            if (ph == 4) p.displayClientMessage(Component.translatable("hukhong.endgame.boss.line.4"), false);
        }
        if (ph >= EndgameKit.DA_KHUYET_PHASE && !daKhuyetSpawned && dkDelay < 0) dkDelay = EndgameKit.PHASE_INVUL_TICKS + 6; // đợi cutscene pha xong
    }

    /** Cutscene đổi pha: cận mặt -> vòng quanh -> lùi xa, 2 dòng thoại hukhong.endgame.boss.phase.&lt;pha&gt;.1/2. */
    private void phaseCutscene(ServerLevel sl, int ph) {
        int T = EndgameKit.PHASE_INVUL_TICKS;
        float s = (float) Math.max(2.0, getBbHeight() * 0.5);
        var cam = java.util.List.of(
                new com.hukhong.boss.phase.CamPath.Key(0, 0, 0.5f * s, 1.6f * s, 0, 0, 45, 0.9f * s),
                new com.hukhong.boss.phase.CamPath.Key(T / 3, 1.8f * s, 0.3f * s, 1.4f * s, 0, -5, 55, 0.8f * s),
                new com.hukhong.boss.phase.CamPath.Key(T * 2 / 3, -1.6f * s, 1.2f * s, -1.0f * s, 0, 4, 62, 0.7f * s),
                new com.hukhong.boss.phase.CamPath.Key(T, 0, 1.3f * s, 3.6f * s, 0, 0, 74, 0.6f * s));
        com.hukhong.boss.phase.BossPhaseDirector.sendCutscene(sl, this, T, cam, java.util.List.of(
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(6, T / 2, "hukhong.dialog.nttd.phase." + ph + ".1"),
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(T / 2, T - 4, "hukhong.dialog.nttd.phase." + ph + ".2")));
    }

    private void spawnDaKhuyet(ServerLevel sl) {
        if (phase() >= 4) { daKhuyetSpawned = true; return; } // nhảy thẳng qua pha 3: không còn kịp đồng hành
        if (!redeemedNear(sl)) return; // chỉ đồng hành khi người chơi quanh đây đã cải tà Dạ Khuyết (cờ dk_redeemed)
        daKhuyetSpawned = true;
        DaKhuyet d = EndgameRegistry.DA_KHUYET.get().create(sl);
        if (d == null) return;
        d.moveTo(getX() + 9, getY(), getZ() + 3, 90f, 0f);
        d.finalizeSpawn(sl, sl.getCurrentDifficultyAt(d.blockPosition()), MobSpawnType.EVENT, null);
        d.begin(sl, this);
        sl.addFreshEntity(d);
    }

    private boolean redeemedNear(ServerLevel sl) {
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(this) <= 120 * 120 && com.hukhong.story.npc.DialogueService.quest(p).has(EndgameService.F_DK_REDEEMED)) return true;
        return false;
    }

    // ------------------------------------------------------------ đồng minh cốt truyện: tất sát / hy sinh / Mộ Dao
    private DaKhuyet allyDk(ServerLevel sl) {
        for (DaKhuyet d : sl.getEntitiesOfClass(DaKhuyet.class, getBoundingBox().inflate(80), DaKhuyet::allyReady)) return d;
        return null;
    }

    /** Hướng từ boss về phía người chơi (trung bình), dùng đặt kết giới / Mộ Dao. */
    private Vec3 towardPlayers(ServerLevel sl) {
        Vec3 c = Vec3.ZERO;
        int n = 0;
        for (Player p : players(sl, position(), 48)) { c = c.add(p.position()); n++; }
        Vec3 d = n == 0 ? pendingDir : c.scale(1.0 / n).subtract(position()).multiply(1, 0, 1);
        return d.lengthSqr() < 1e-3 ? new Vec3(0, 0, 1) : d.normalize();
    }

    private Vec3 groundAt(ServerLevel sl, Vec3 v) {
        net.minecraft.core.BlockPos g = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.core.BlockPos.containing(v));
        return new Vec3(v.x, Math.min(g.getY(), getY() + 4), v.z);
    }

    private void startSureKill(ServerLevel sl, DaKhuyet dk) {
        sureT = SURE_TELE;
        sureCd = 1200 + random.nextInt(600); // 60-90s
        barrierAt = null;
        getNavigation().stop();
        triggerAnim("skill", "phase");
        playSound(SoundEvents.WITHER_SPAWN, 4f, 0.5f);
        Fx.telegraphCircle(sl, null, position(), 40f, SURE_TELE, 0xFF2030);
        Fx.rune(sl, null, position().add(0, 22, 0), 16f, 0xFF2030, SURE_TELE + 10, 9);
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) > 80 * 80) continue;
            p.connection.send(new ClientboundSetTitlesAnimationPacket(4, SURE_TELE, 10));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("hukhong.endgame.surekill.title")));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("hukhong.endgame.surekill.sub")));
        }
    }

    /** Tất Sát "Tịch Diệt Chung Yên": gồng 3s -> Dạ Khuyết lướt tới dựng kết giới -> nổ: ngoài kết giới chết chắc, trong kết giới vô sự. */
    private void stepSureKill(ServerLevel sl) {
        sureT--;
        getNavigation().stop();
        if (sureT % 2 == 0) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 3, getZ(), 30, 8, 4, 8, 0.8);
            sl.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.1f, 0.15f), 2f), getX(), getY() + getBbHeight() * 0.7, getZ(), 10, 2, 2, 2, 0.02);
        }
        DaKhuyet dk = allyDk(sl);
        if (sureT == SURE_TELE - 15) {
            if (dk == null) { sureT = 0; return; } // không có người chắn: huỷ, không dùng chiêu
            barrierAt = groundAt(sl, position().add(towardPlayers(sl).scale(Math.max(7, getBbWidth() * 0.5 + 5))));
            dk.guardBarrier(sl, barrierAt, this, BARRIER_R, sureT + 20);
            Fx.shake(sl, barrierAt, 1.5f, 12, 40f);
            NttdAllies.say(sl, this, "hukhong.endgame.dakhuyet.guard." + (1 + random.nextInt(3)));
        }
        if (barrierAt != null && sureT % 4 == 0) Fx.groundRing(sl, null, barrierAt, BARRIER_R, 0x8FA8FF, 6);
        if (sureT > 0) return;
        if (barrierAt == null) return; // bị huỷ trước khi có kết giới
        Fx.elem(sl, null, Fx.E_NUKE, position(), Vec3.ZERO, 14f, 0xFF2030, 0);
        Fx.shake(sl, position(), 5f, 25, 80f);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 2, getZ(), 4, 6, 1, 6, 0);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 5f, 0.4f);
        for (Player p : players(sl, position(), 48)) {
            if (p.isCreative() || p.isSpectator()) continue;
            double dx = p.getX() - barrierAt.x, dz = p.getZ() - barrierAt.z;
            if (dx * dx + dz * dz <= (BARRIER_R + 0.5) * (BARRIER_R + 0.5)) {
                sl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 20, 0.5, 1, 0.5, 0.1);
                continue; // kết giới trăng khuyết hoá giải hoàn toàn
            }
            p.hurt(damageSources().indirectMagic(this, this), (p.getMaxHealth() + p.getAbsorptionAmount()) * 4f);
        }
        Fx.elem(sl, null, Fx.E_HALOS, barrierAt.add(0, 1.5, 0), Vec3.ZERO, BARRIER_R, 0x8FA8FF, 0);
        sl.sendParticles(ParticleTypes.END_ROD, barrierAt.x, barrierAt.y + 2, barrierAt.z, 80, BARRIER_R * 0.5, 2, BARRIER_R * 0.5, 0.1);
        barrierAt = null;
        skillCd = 60;
    }

    /** Cutscene lớn cuối pha 3: Nguyên Thủy tung đại chiêu, Dạ Khuyết lấy thân chắn rồi tan biến; Mộ Dao xuất hiện. */
    private void grandSacrifice(ServerLevel sl, DaKhuyet dk) {
        int T = GRAND_T;
        Vec3 dir = towardPlayers(sl);
        float yaw = (float) (Math.atan2(-dir.x, dir.z) * 180 / Math.PI);
        setYRot(yaw); yBodyRot = yaw; yHeadRot = yaw;
        float s = (float) Math.max(2.0, getBbHeight() * 0.5);
        double front = Math.max(7, getBbWidth() * 0.5 + 5);
        Vec3 guard = groundAt(sl, position().add(dir.scale(front)));
        float f = (float) front;
        var cam = java.util.List.of(
                new com.hukhong.boss.phase.CamPath.Key(0, 0, 0.6f * s, 1.8f * s, 0, 0, 50, 0.9f * s),
                new com.hukhong.boss.phase.CamPath.Key(50, 1.2f * s, 1.4f * s, 2.4f * s, 0, -6, 62, 1.0f * s),
                new com.hukhong.boss.phase.CamPath.Key(90, 3.2f, 1.8f, f + 5, 0, 0, 62, 2f),
                new com.hukhong.boss.phase.CamPath.Key(135, 1.8f, 1.4f, f + 4.5f, 0, 0, 56, 1.8f),
                new com.hukhong.boss.phase.CamPath.Key(180, -2.2f, 1.4f, f + 3.2f, 0, 0, 48, 1.6f),
                new com.hukhong.boss.phase.CamPath.Key(T, 0, 4.5f, f + 12, 0, 0, 72, 2f));
        com.hukhong.boss.phase.BossPhaseDirector.sendCutscene(sl, this, position(), yaw, T, cam, java.util.List.of(
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(6, 50, "hukhong.dialog.nttd.grand.1"),
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(50, 95, "hukhong.dialog.nttd.grand.2"),
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(95, 135, "hukhong.dialog.nttd.grand.3"),
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(135, 175, "hukhong.dialog.nttd.grand.4"),
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(175, 210, "hukhong.dialog.nttd.grand.5"),
                new com.hukhong.boss.phase.BossPhaseConfig.SubLine(210, T - 4, "hukhong.dialog.nttd.grand.6")));
        Fx.rune(sl, null, position().add(0, 24, 0), 22f, VOIDC, 140, 9);
        later(30, () -> Fx.telegraphCircle(sl, null, position(), 36f, 80, 0xFF2030));
        later(70, () -> { if (dk.isAlive()) dk.guardBarrier(sl, guard, this, 9f, 120); Fx.shake(sl, guard, 1.5f, 10, 40f); });
        for (int i = 0; i < 6; i++) {
            int at = 110 + i * 10;
            later(at, () -> {
                Fx.ribbonHelix(sl, null, position().add(0, getBbHeight() * 0.6, 0), guard.add(0, 1.5, 0), 1.2, at * 0.3, 8, 0, 0.6f, VOIDC);
                sl.sendParticles(ParticleTypes.SONIC_BOOM, guard.x, guard.y + 1.5, guard.z, 2, 1.5, 1, 1.5, 0);
                Fx.shake(sl, guard, 2.5f, 8, 50f);
            });
        }
        later(110, () -> { Fx.elem(sl, null, Fx.E_NUKE, guard, Vec3.ZERO, 8f, VOIDC, 0); playSound(SoundEvents.GENERIC_EXPLODE.value(), 5f, 0.4f); });
        later(160, () -> Fx.groundCrack(sl, null, guard, 9f, 0x8FA8FF, 12, 80));
        later(175, () -> {
            dkSacrificed = true;
            if (dk.isAlive()) {
                Vec3 p = dk.position();
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, p.x, p.y + 1.5, p.z, 220, 1.5, 2, 1.5, 0.08);
                sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1.5, p.z, 120, 0.8, 2.5, 0.8, 0.12);
                Fx.groundRing(sl, null, p, 6f, 0x8FA8FF, 60);
                Fx.ribbon(sl, null, p.add(0, 1.5, 0), p.add(0, 18, 0), 0, 30, 0, 0.5f, 0x8FA8FF);
                dk.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 4f, 0.5f);
                dk.discard();
            }
            NttdAllies.say(sl, this, "hukhong.endgame.dakhuyet.sacrifice");
        });
        later(212, () -> arriveMo(sl, false));
    }

    /** Mộ Dao đến trợ chiến pha 4. */
    private void arriveMo(ServerLevel sl, boolean announce) {
        if (!isAlive() || moSpawned) return;
        Vec3 at = groundAt(sl, position().add(towardPlayers(sl).scale(11)));
        if (NttdAllies.spawnMo(sl, at, position()) == null) return;
        moSpawned = true;
        Fx.shake(sl, at, 1f, 10, 30f);
        if (announce) NttdAllies.say(sl, this, "hukhong.dialog.nttd.grand.6");
        NttdAllies.say(sl, this, "hukhong.endgame.modao.ally");
    }

    /** Pha 4: Mộ Dao đánh / hộ thể theo nhịp 3.5s. */
    private void moAllyTick(ServerLevel sl) {
        if (!moSpawned || phase() < 4 || invulTicks > 0 || tickCount % 70 != 0) return;
        com.hukhong.story.npc.StoryNpc mo = NttdAllies.findMo(sl, position());
        if (mo == null) { moSpawned = false; arriveMo(sl, false); return; } // mất (dỡ chunk) -> gọi lại
        NttdAllies.moTick(sl, this, mo, moBeat++);
    }

    private void applyPhaseEffects(int ph) {
        setModifier(Attributes.MOVEMENT_SPEED, SPD_MOD, EndgameKit.speedModifier(ph) + (form() == 1 ? 0.2 : 0));
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
            triggerAnim("skill", form() == 1 ? "f1_attack" : "attack");
            if (e instanceof LivingEntity le) HuSkill.hu(le, 120);
        }
        return r;
    }

    // ------------------------------------------------------------ chết
    @Override public void die(DamageSource src) {
        // dạng 1 không chết: thân xác vỡ, Nguyên Thủy Tịch Diệt thật lộ ra (chỉ /kill - BYPASSES_INVULNERABILITY - mới giết thẳng)
        if (form() == 1 && level() instanceof ServerLevel sl && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            setHealth(1f);
            startMorph(sl);
            return;
        }
        sched.clear();
        orbs.clear();
        if (level() instanceof ServerLevel sl0) {
            Fx.groundRing(sl0, null, position(), 24f, GOLD, 60);
            Fx.rune(sl0, null, position().add(0, 0.1, 0), 10f, GOLD, 160, 8);
            Fx.shake(sl0, position(), 3f, 30, 60);
        }
        super.die(src);
        if (level() instanceof ServerLevel sl) EndgameService.onBossDefeated(sl, this);
    }

    @Override protected void dropCustomDeathLoot(ServerLevel sl, DamageSource src, boolean recentlyHit) {
        super.dropCustomDeathLoot(sl, src, recentlyHit);
        com.hukhong.currency.Wallet.grantHonTinhBoss(sl, position(), src, 40 + random.nextInt(16));
    }

    // ------------------------------------------------------------ BOSS BRAIN (PLAN-v1 §3): di chuyển + đòn thường theo DẠNG
    private static final int O_NONE = 0, O_STRUGGLE = 1, O_ESCAPE = 2, O_BURST = 3, O_THROW = 4,
            O_CHARGE = 11, O_DIVE = 12, O_CRUSH = 13, O_ANQUANG = 14, O_STARBEAM = 15;
    private static final int ORBS_IDX = 5; // EndgameKit "cau_truy_hon"
    private int own, ownT, queued, bellyT, f2Rot;
    private Vec3 ownDir = new Vec3(0, 0, 1), ownAt = Vec3.ZERO;
    private boolean gapLeap;
    private final java.util.ArrayDeque<Integer> hurtAt = new java.util.ArrayDeque<>();
    private final java.util.Set<java.util.UUID> ownHit = new java.util.HashSet<>();
    /** Số liệu cho gametest / debug. */
    public int dbgCracks, dbgBlinks, dbgLeaps, dbgStruggles, dbgTpStrikes, dbgPurple, dbgLaunch, dbgBursts, dbgThrows,
            dbgDives, dbgEmerges, dbgCharges, dbgCrushes, dbgRings, dbgBeams;
    public final java.util.List<String> dbgBigMoves = new java.util.ArrayList<>();

    /** Dạng 1 áp sát: xa > 12 nhảy búa (cao ~6 ô), còn lại Thuấn Hư bộ 5-8 ô. */
    private static final com.hukhong.boss.ai.GapClose F1_GAP = new com.hukhong.boss.ai.GapClose() {
        public String anim() { return "leap"; }
        public boolean step(ServerLevel sl, Mob b, com.hukhong.boss.ai.BossMovement mv, LivingEntity t, int i) {
            return b instanceof NguyenThuyTichDiet n && n.f1Gap(sl, mv, t, i);
        }
    };
    /** Dạng 2: người chơi chạy xa > 20 ô -> lặn Hư Không, trồi lên cạnh người chơi. */
    private static final com.hukhong.boss.ai.DiveEmerge F2_GAP = new com.hukhong.boss.ai.DiveEmerge(
            com.hukhong.boss.ai.DiveEmerge.Where.NEAR_TARGET, 12, 10, 8, "dive_under",
            (sl, b, at) -> { if (b instanceof NguyenThuyTichDiet n) n.emergeBlast(sl, at, true); });
    /** Dạng 2 nhịp trận: lặn xuống, trồi lên phía bên kia sân. */
    private static final com.hukhong.boss.ai.DiveEmerge F2_DIVE_ACROSS = new com.hukhong.boss.ai.DiveEmerge(
            com.hukhong.boss.ai.DiveEmerge.Where.ACROSS_ARENA, 14, 12, 10, "dive_under",
            (sl, b, at) -> { if (b instanceof NguyenThuyTichDiet n) n.emergeBlast(sl, at, false); });

    /** §3.1 Dạng 1: búa nặng, chậm, cam kết; 3-6 ô, xoay 10°/tick khi vung, né 15%, nghỉ 18-28. */
    public static final com.hukhong.boss.ai.BossProfile BRAIN_F1 = com.hukhong.boss.ai.BossProfile.builder()
            .phase(1, new com.hukhong.boss.ai.BossProfile.PhaseVals(3, 6, 0.13, 10f, 0.15, 60, 18, 28))
            .weight(2.5).accel(7, 5).moveSpeed(0.2)
            .locomotion(com.hukhong.boss.ai.Locomotion.circleTarget(22f))
            .gapClose(F1_GAP, 8)
            .combo(new com.hukhong.boss.ai.BossProfile.Combo("tam_giang", java.util.List.of(
                    new com.hukhong.boss.ai.BossProfile.Hit(14, 4, 8, 3.6, 150, 0.8, 0.6, 0, false, false, false),   // quét ngang
                    new com.hukhong.boss.ai.BossProfile.Hit(16, 3, 10, 3.4, 60, 1.0, 0.8, 0, false, true, false),    // bổ dọc
                    new com.hukhong.boss.ai.BossProfile.Hit(10, 3, 22, 3.2, 100, 1.2, 0.4, 0, false, true, true)),   // giật cục: ra sau lưng bổ
                    0, 4.5, 3, 1, java.util.List.of(), false))
            .combo(new com.hukhong.boss.ai.BossProfile.Combo("keo_bua", java.util.List.of(
                    new com.hukhong.boss.ai.BossProfile.Hit(8, 10, 2, 2.8, 70, 0.5, 3.0, 0, false, false, false),    // kéo búa 3 ô
                    new com.hukhong.boss.ai.BossProfile.Hit(10, 3, 20, 3.4, 80, 0.9, 0.5, 0, false, true, false)),   // hất lên
                    2, 6, 2, 1, java.util.List.of(), false))
            .combo(new com.hukhong.boss.ai.BossProfile.Combo("song_the", java.util.List.of(
                    new com.hukhong.boss.ai.BossProfile.Hit(22, 4, 6, 3.4, 120, 0.9, 0.6, 0, false, true, false),    // nhát người: chậm
                    new com.hukhong.boss.ai.BossProfile.Hit(5, 2, 20, 3.6, 120, 1.0, 1.0, 0, false, true, false)),   // nhát Hư Không: nhanh gấp đôi
                    0, 4.5, 2, 1, java.util.List.of(), false))
            .bigMoveEvery(2)
            .build();

    /** §3.2 Dạng 2: trôi vòng quanh tâm sân, 8-16 ô, xoay 6°/tick, không né (dịch chuyển xuyên sân), nghỉ 20-30. */
    public static final com.hukhong.boss.ai.BossProfile BRAIN_F2 = com.hukhong.boss.ai.BossProfile.builder()
            .phase(1, new com.hukhong.boss.ai.BossProfile.PhaseVals(8, 16, 0.16, 6f, 0, 999, 20, 30))
            .weight(3).accel(10, 8).moveSpeed(0.22)
            .locomotion(com.hukhong.boss.ai.Locomotion.orbitCenter(0.55))
            .gapClose(F2_GAP, 20).bigMoveOutOfRange()
            .combo(new com.hukhong.boss.ai.BossProfile.Combo("quet_tinh_ha", java.util.List.of(
                    new com.hukhong.boss.ai.BossProfile.Hit(18, 6, 6, 8, 180, 1.0, 0, 0, false, true, false),
                    new com.hukhong.boss.ai.BossProfile.Hit(10, 6, 20, 8, 180, 1.0, 0, 0, false, true, false)),
                    0, 9, 3, 1, java.util.List.of(), false))
            .bigMoveEvery(1)
            .build();

    // tên animation brain có thật trong file (theo dạng) + bí danh cho tên brain không có animation riêng
    private static final java.util.Set<String> F1_ANIMS = java.util.Set.of("strafe_l", "strafe_r", "backstep", "leap", "struggle", "burst", "throw",
            "combo_tam_giang_1", "combo_tam_giang_2", "combo_tam_giang_3", "combo_keo_bua_1", "combo_keo_bua_2", "combo_song_the_1", "combo_song_the_2");
    private static final java.util.Map<String, String> F1_ALIAS = java.util.Map.of("sidestep_l", "strafe_l", "sidestep_r", "strafe_r",
            "backflip", "backstep", "dash", "leap", "blink", "teleport", "escape", "teleport");
    private static final java.util.Set<String> F2_ANIMS = java.util.Set.of("float", "orbit_l", "orbit_r", "dive_under", "emerge", "charge", "crush",
            "an_quang", "star_beam", "combo_quet_tinh_ha_1", "combo_quet_tinh_ha_2");
    private static final java.util.Map<String, String> F2_ALIAS = java.util.Map.ofEntries(
            java.util.Map.entry("strafe_l", "orbit_l"), java.util.Map.entry("strafe_r", "orbit_r"), java.util.Map.entry("dash", "charge"),
            java.util.Map.entry("sidestep_l", "orbit_l"), java.util.Map.entry("sidestep_r", "orbit_r"), java.util.Map.entry("backstep", "float"),
            java.util.Map.entry("backflip", "float"), java.util.Map.entry("leap", "crush"),
            java.util.Map.entry("gravity_well", "an_quang"), java.util.Map.entry("time_freeze", "star_beam"),
            java.util.Map.entry("mirror_echo", "emerge"));

    private static void brainAnims(AnimationController<NguyenThuyTichDiet> c, String key, String prefix, com.hukhong.boss.ai.BossProfile p,
                                   java.util.Set<String> have, java.util.Map<String, String> alias, String walk) {
        java.util.Set<String> names = new java.util.LinkedHashSet<>(com.hukhong.boss.ai.BrainAnims.names(p));
        names.addAll(have);
        names.addAll(alias.keySet());
        for (String nm : names) {
            String real = have.contains(nm) ? nm : alias.containsKey(nm) ? alias.get(nm) : nm.startsWith("combo_") ? "attack" : walk;
            c.triggerableAnim(key + nm, RawAnimation.begin().then(prefix + real, Animation.LoopType.PLAY_ONCE));
        }
    }

    // ---- BossBrain.Host
    @Override public com.hukhong.boss.ai.BossProfile brainProfile() { return form() == 1 ? BRAIN_F1 : BRAIN_F2; }
    @Override public int currentPhase() { return form() == 1 ? 1 : phase(); }
    @Override public boolean frozen() { return intro > 0 || morph > 0 || cine > 0 || invulTicks > 0 || sureT > 0; }
    @Override public boolean ownMoveActive() { return pending >= 0 || own != O_NONE; }
    @Override public float moveDamage(double mult) { return (float) atk(mult); }
    @Override public void playBrainAnim(String name) { triggerAnim(com.hukhong.boss.ai.BrainAnims.CONTROLLER, (form() == 1 ? "f1_" : "f2_") + name); }

    /** Đang trong cửa sổ đánh tự do (lùi giằng co, dạng 1). */
    public boolean freeWindow() { return own == O_STRUGGLE; }
    /** Chiêu riêng brain đang chạy (0 = không). */
    public int ownMove() { return own; }

    @Override public boolean brainBigMove(ServerLevel sl, LivingEntity t) {
        if (pending >= 0 || own != O_NONE) return false;
        if (form() == 1) {
            if (players(sl, position(), 4).size() >= 2) { startOwn(O_ESCAPE); return true; }  // bị vây: Thuấn Hư thoát ra
            if (random.nextFloat() < 0.3f) { startOwn(O_STRUGGLE); return true; }
            if (skillCd <= 0) { startF1Skill(sl, t); return true; }
            return false;
        }
        double d = distanceTo(t);
        if (random.nextFloat() < 0.3f && NttdExtraMoves.startF2(this, t)) return true;   // hố trọng lực / tia băng / phân thân
        for (int k = 0; k < 5; k++) {   // §3.3 nhịp: Cầu Truy Hồn -> Lao Xuyên -> (Quét Tinh Hà do brain) -> lặn -> Ấn Quang -> chiêu lớn
            switch (f2Rot++ % 5) {
                case 0 -> { if (d >= 8) { startF2Skill(sl, t, ORBS_IDX); return true; } }
                case 1 -> { startOwn(O_CHARGE); return true; }
                case 2 -> { startOwn(O_DIVE); return true; }
                case 3 -> { startOwn(O_ANQUANG); return true; }
                default -> { if (skillCd <= 0) { startF2Skill(sl, t, -1); return true; } }
            }
        }
        return false;
    }

    @Override public boolean brainOnHeal(ServerLevel sl, Player p) {
        queued = form() == 1 ? O_THROW : O_STARBEAM;   // dạng 1 ném búa xoay, dạng 2 chùm tia sao
        return true;
    }

    @Override public void brainHitEvent(ServerLevel sl, String id, int hit, int stage, LivingEntity t) {
        com.hukhong.boss.ai.BossBrain b = com.hukhong.boss.ai.BossBrain.get(this);
        if (b == null || t == null) return;
        com.hukhong.boss.ai.BossMovement mv = b.movement();
        switch (id) {
            case "tam_giang" -> { if (hit == 2 && stage == 0) tpBehind(sl, mv, t); }
            case "song_the" -> { if (hit == 1 && stage == 0) purpleTell(sl, mv); }
            case "keo_bua" -> {
                if (hit == 0 && stage == 1) {
                    Fx.groundCrack(sl, null, position().add(mv.forward().scale(2)), 3f, VOIDC, 4, 30);
                    sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.1, getZ(), 12, 0.6, 0.05, 0.6, 0.2);
                    dbgCracks++;
                }
                if (hit == 1 && stage == 1) launch(sl, mv, t);
            }
            case "quet_tinh_ha" -> {
                if (stage == 1) {
                    Fx.qi(sl, position().add(0, getBbHeight() * 0.4, 0), mv.forward(), 8f, GOLD, hit == 0 ? 90 : -90, 10);
                    playSound(SoundEvents.BEACON_POWER_SELECT, 2f, hit == 0 ? 0.6f : 0.8f);
                }
            }
            default -> { }
        }
    }

    /** Búa Tam Giáng nhát 3: ý chí Hư Không giật cục - dịch chuyển ra sau lưng mục tiêu. */
    private void tpBehind(ServerLevel sl, com.hukhong.boss.ai.BossMovement mv, LivingEntity t) {
        Vec3 away = t.position().subtract(position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 1e-4 ? pendingDir : away.normalize();
        Vec3 land = clampArena(t.position().add(away.scale(2.4)));
        if (!sl.noCollision(this, getBoundingBox().move(land.subtract(position())))) return;
        Fx.rift(sl, null, position().add(0, 1.4, 0), rotY(away, 90), 1.4f, VOIDC, 12);
        teleportTo(land.x, getY(), land.z);
        mv.face(t.position(), 360f);
        Fx.rift(sl, null, land.add(0, 1.4, 0), rotY(away, 90), 1.4f, VOIDC, 14);
        sl.sendParticles(TELEGRAPH, land.x, land.y + 2, land.z, 20, 0.4, 0.8, 0.4, 0);
        playSound(SoundEvents.ENDERMAN_TELEPORT, 2f, 0.5f);
        dbgTpStrikes++;
    }

    /** Song Thể Kích: vệt sáng tím báo hiệu nhát Hư Không nhanh. */
    private void purpleTell(ServerLevel sl, com.hukhong.boss.ai.BossMovement mv) {
        Vec3 f = mv.forward(), r = new Vec3(-f.z, 0, f.x);
        for (int i = 0; i <= 10; i++) {
            Vec3 p = position().add(r.scale(-1.2 + i * 0.24)).add(f.scale(1.0 + Math.sin(i / 10.0 * Math.PI))).add(0, 2.2 - i * 0.1, 0);
            sl.sendParticles(TELEGRAPH, p.x, p.y, p.z, 2, 0.03, 0.03, 0.03, 0);
        }
        sl.sendParticles(ParticleTypes.WITCH, getX(), getY() + 2, getZ(), 12, 0.5, 0.6, 0.5, 0.05);
        playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1.5f, 1.6f);
        dbgPurple++;
    }

    /** Kéo Búa Hất: hất tung (đánh bay) mọi thứ trước mặt. */
    private void launch(ServerLevel sl, com.hukhong.boss.ai.BossMovement mv, LivingEntity t) {
        Vec3 c = position().add(mv.forward().scale(2));
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(3, 2, 3), e -> e != this && (e instanceof Player || e == t))) {
            if (e instanceof Player p && (p.isSpectator() || p.isCreative())) continue;
            e.setDeltaMovement(e.getDeltaMovement().x, 1.1, e.getDeltaMovement().z);
            e.hurtMarked = true;
        }
        Fx.groundCrack(sl, null, c, 3f, VOIDC, 5, 30);
        playSound(SoundEvents.ANVIL_LAND, 1.5f, 0.6f);
        dbgLaunch++;
    }

    private Vec3 clampArena(Vec3 v) {
        double[] a = com.hukhong.arena.BossArena.bounds(this);
        if (a == null) return v;
        Vec3 rel = new Vec3(v.x - a[0], 0, v.z - a[1]);
        double lim = Math.max(2, a[2] - 3);
        return rel.length() <= lim ? v : new Vec3(a[0], v.y, a[1]).add(rel.normalize().scale(lim));
    }

    private boolean f1Gap(ServerLevel sl, com.hukhong.boss.ai.BossMovement mv, LivingEntity t, int i) {
        if (i == 0) {
            gapLeap = distanceTo(t) > 12;
            if (!gapLeap) playBrainAnim("blink");
        }
        return gapLeap ? leapStep(sl, mv, t, i) : blinkStep(sl, mv, t, i, false);
    }

    /** Nhảy búa: bật cao ~6 ô, đáp cạnh mục tiêu, đập nứt đất. */
    private boolean leapStep(ServerLevel sl, com.hukhong.boss.ai.BossMovement mv, LivingEntity t, int i) {
        if (i == 0) {
            Vec3 to = t.position().subtract(position()).multiply(1, 0, 1);
            double h = Math.max(0, to.length() - 2.5);
            mv.face(t.position(), 360f);
            ownDir = to.lengthSqr() < 1e-4 ? Vec3.ZERO : to.normalize().scale(h / 27.0);
            setDeltaMovement(getDeltaMovement().x, 1.05, getDeltaMovement().z);
            hasImpulse = true;
            dbgLeaps++;
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 16, 0.8, 0.1, 0.8, 0.05);
            playSound(SoundEvents.RAVAGER_ROAR, 2f, 0.6f);
            return true;
        }
        mv.drive(ownDir, true);
        if (i > 4 && onGround() || i > 38) {
            mv.freeze();
            Vec3 c = position();
            for (Player p : players(sl, c, 4.5)) hit(p, atk(1.0), c, true);
            Fx.groundCrack(sl, null, c, 5f, VOIDC, 6, 30);
            Fx.shake(sl, c, 2f, 10, 24);
            sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.2, c.z, 2, 1, 0.2, 1, 0);
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.7f);
            dbgCracks++;
            return false;
        }
        return true;
    }

    /** Thuấn Hư bộ: 6 tick tan vào khe, hiện ra cách 5-8 ô (áp sát, hoặc thoát vây khi escape). */
    private boolean blinkStep(ServerLevel sl, com.hukhong.boss.ai.BossMovement mv, LivingEntity t, int i, boolean escape) {
        mv.freeze();
        Vec3 to = t.position().subtract(position()).multiply(1, 0, 1);
        double d = to.length();
        Vec3 n = d < 1e-3 ? pendingDir : to.scale(1 / d);
        if (i == 0) {
            Fx.rift(sl, null, position().add(0, 1.4, 0), rotY(n, 90), 1.4f, VOIDC, 12);
            sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.4, getZ(), 40, 0.5, 1, 0.5, 0.5);
        }
        if (i < 6) return true;
        Vec3 dest = escape ? position().add(rotY(n, 180 + (random.nextDouble() - 0.5) * 80).scale(6.5))
                : position().add(n.scale(Mth.clamp(d - 2.5, 5, 8)));
        dest = clampArena(dest);
        if (sl.noCollision(this, getBoundingBox().move(dest.subtract(position())))) teleportTo(dest.x, getY(), dest.z);
        mv.face(t.position(), 360f);
        Fx.rift(sl, null, position().add(0, 1.4, 0), rotY(n, 90), 1.4f, VOIDC, 12);
        playSound(SoundEvents.ENDERMAN_TELEPORT, 2f, 0.6f);
        dbgBlinks++;
        return false;
    }

    /** Lặn xong trồi lên: sóng nổ. */
    private void emergeBlast(ServerLevel sl, Vec3 at, boolean nearTarget) {
        if (nearTarget) dbgDives++;
        dbgEmerges++;
        playBrainAnim("emerge");
        Vec3 g = new Vec3(at.x, com.hukhong.boss.ai.Locomotion.groundY(sl, at.x, at.y, at.z), at.z);
        for (Player p : players(sl, g, 6)) hit(p, atk(0.9), g, true);
        Fx.groundRing(sl, null, g, 7f, GOLD, 16);
        Fx.shake(sl, g, 2f, 10, 30);
        sl.sendParticles(ParticleTypes.EXPLOSION, g.x, g.y + 0.5, g.z, 3, 1.5, 0.3, 1.5, 0);
        sl.sendParticles(ParticleTypes.END_ROD, g.x, g.y + 1, g.z, 60, 2, 1.5, 2, 0.2);
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.5f, 0.7f);
    }

    private void onHurtStreak() {
        hurtAt.addLast(tickCount);
        while (!hurtAt.isEmpty() && tickCount - hurtAt.peekFirst() > 40) hurtAt.pollFirst();
        if (hurtAt.size() >= 3 && own != O_BURST) { hurtAt.clear(); queued = O_BURST; }  // bị đánh liên tục: nổ sóng (không né)
    }

    private static final String[] OWN_ANIM = new String[24];
    static {
        OWN_ANIM[O_STRUGGLE] = "struggle"; OWN_ANIM[O_ESCAPE] = "escape"; OWN_ANIM[O_BURST] = "burst"; OWN_ANIM[O_THROW] = "throw";
        OWN_ANIM[O_CHARGE] = "charge"; OWN_ANIM[O_DIVE] = "dive_under"; OWN_ANIM[O_CRUSH] = "crush"; OWN_ANIM[O_ANQUANG] = "an_quang";
        OWN_ANIM[O_STARBEAM] = "star_beam";
        OWN_ANIM[NttdExtraMoves.O_GRAVITY] = "gravity_well"; OWN_ANIM[NttdExtraMoves.O_FREEZE] = "time_freeze"; OWN_ANIM[NttdExtraMoves.O_ECHO] = "mirror_echo";
    }

    /** Bắt đầu chiêu di chuyển/phản ứng riêng (public cho gametest). Brain nhường quyền trong lúc chạy. */
    public void startOwn(int kind) {
        own = kind; ownT = 0; ownHit.clear();
        LivingEntity t = getTarget();
        ownAt = t != null ? t.position() : position();
        dbgBigMoves.add(OWN_ANIM[kind]);
        playBrainAnim(OWN_ANIM[kind]);
    }
    public static final int OWN_STRUGGLE = O_STRUGGLE, OWN_BURST = O_BURST, OWN_CHARGE = O_CHARGE, OWN_DIVE = O_DIVE, OWN_CRUSH = O_CRUSH, OWN_ANQUANG = O_ANQUANG;

    /** Mỗi tick server (ngoài intro/biến hình): vết nứt bước chân, lơ lửng, dưới bụng, phản ứng xếp hàng, chiêu riêng. */
    private void brainSideTick(ServerLevel sl) {
        if (isNoGravity() != (form() == 2)) setNoGravity(form() == 2);
        com.hukhong.boss.ai.BossBrain b = com.hukhong.boss.ai.BossBrain.get(this);
        boolean swinging = b != null && b.hitStage() == 1;
        if (queued != O_NONE && own == O_NONE && pending < 0 && !swinging && invulTicks == 0 && cine == 0) { int q = queued; queued = O_NONE; startOwn(q); }
        if (own != O_NONE) stepOwn(sl, b);
        if (form() == 1) {
            // Bước nặng: kéo búa trên đất, để lại vết nứt sáng
            if (own == O_NONE && pending < 0 && onGround() && getDeltaMovement().horizontalDistance() > 0.06 && tickCount % 12 == 0) {
                Vec3 f = getLookAngle().multiply(1, 0, 1);
                Fx.groundCrack(sl, null, position().add(f.scale(0.8)), 1.5f, VOIDC, 3, 30);
                dbgCracks++;
            }
            return;
        }
        if (own != O_CRUSH) com.hukhong.boss.ai.Locomotion.hover(this, 2.5);
        LivingEntity t = getTarget();
        if (t != null && own == O_NONE && pending < 0 && Math.hypot(t.getX() - getX(), t.getZ() - getZ()) < getBbWidth() * 0.5 + 1.2 && t.getY() < getY() + 0.5) {
            if (++bellyT >= 60 && !swinging) { bellyT = 0; startOwn(O_CRUSH); }   // đứng dưới bụng > 3s: bay vọt lên rồi rơi đè
        } else bellyT = 0;
    }

    private void stepOwn(ServerLevel sl, com.hukhong.boss.ai.BossBrain b) {
        LivingEntity t = getTarget();
        com.hukhong.boss.ai.BossMovement mv = b == null ? null : b.movement();
        int i = ownT++;
        boolean done = switch (own) {
            case O_STRUGGLE -> struggle(sl, t, i);
            case O_ESCAPE -> mv == null || t == null || !blinkStep(sl, mv, t, i, true);
            case O_BURST -> burst(sl, t, i);
            case O_THROW -> throwHammer(sl, t, i);
            case O_CHARGE -> charge(sl, t, mv, i);
            case O_DIVE -> mv == null || !F2_DIVE_ACROSS.step(sl, this, mv, t, i);
            case O_CRUSH -> crush(sl, t, i);
            case O_ANQUANG -> anQuang(sl, t, i);
            case O_STARBEAM -> starBeam(sl, t, i);
            default -> NttdExtraMoves.stepF2(this, sl, t, own, i);
        };
        if (done) { if (own == O_DIVE) setInvisible(false); own = O_NONE; }
    }

    /** Lùi giằng co: ôm đầu lùi 2-3 bước (ý chí người giành lại) -> cửa sổ đánh tự do 1.5s. */
    private boolean struggle(ServerLevel sl, LivingEntity t, int i) {
        if (i == 0) { dbgStruggles++; playSound(SoundEvents.WARDEN_HURT, 2f, 0.5f); }
        Vec3 away = t == null ? pendingDir.scale(-1) : position().subtract(t.position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 1e-4 ? pendingDir.scale(-1) : away.normalize();
        Vec3 d = i < 14 ? away.scale(0.19) : Vec3.ZERO;
        if (i < 14 && t != null) setYRot((float) (Mth.atan2(-away.z, -away.x) * Mth.RAD_TO_DEG) - 90f);
        setDeltaMovement(d.x, getDeltaMovement().y, d.z);
        if (i % 4 == 0) sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.4, getZ(), 4, 0.3, 0.3, 0.3, 0.02);
        return i >= 30;
    }

    /** Nổ sóng Hư Không đẩy lùi (thay vì né khi bị đánh liên tục). */
    private boolean burst(ServerLevel sl, LivingEntity t, int i) {
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (i == 0) sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 40, 1, 1, 1, 0.2);
        if (i == 6) {
            Vec3 c = position();
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6, 2, 6), e -> e != this && (e instanceof Player || e == t))) {
                if (e instanceof Player p && (p.isSpectator() || p.isCreative())) continue;
                Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
                away = away.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : away.normalize();
                if (e instanceof Player p) hit(p, atk(0.5), c, false);
                e.setDeltaMovement(away.x * 1.6, 0.5, away.z * 1.6);
                e.hurtMarked = true;
            }
            Fx.groundRing(sl, null, c, 6f, VOIDC, 12);
            playSound(SoundEvents.WARDEN_SONIC_BOOM, 2f, 0.8f);
            dbgBursts++;
        }
        return i >= 14;
    }

    /** Người chơi hồi máu (dạng 1): ném búa xoay về phía người chơi. */
    private boolean throwHammer(ServerLevel sl, LivingEntity t, int i) {
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (i == 10) {
            Vec3 from = position().add(0, 2, 0), to = (t != null ? t.position() : ownAt).add(0, 1, 0);
            java.util.Set<java.util.UUID> done = new java.util.HashSet<>();
            for (int k = 0; k <= 14; k++) {
                Vec3 at = from.lerp(to, k < 8 ? k / 8.0 : (14 - k) / 6.0);
                later(k + 1, () -> {
                    sl.sendParticles(TELEGRAPH, at.x, at.y, at.z, 6, 0.3, 0.3, 0.3, 0);
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0, 0, 0, 0);
                    for (Player p : players(sl, at, 1.8)) if (done.add(p.getUUID())) hit(p, atk(0.8), at, true);
                });
            }
            playSound(SoundEvents.TRIDENT_THROW.value(), 2f, 0.5f);
            dbgThrows++;
        }
        return i >= 26;
    }

    /** Lao xuyên: lao thẳng qua sân bằng thân (đòn di chuyển = đòn đánh), để lại vệt sao. */
    private boolean charge(ServerLevel sl, LivingEntity t, com.hukhong.boss.ai.BossMovement mv, int i) {
        if (i == 0) {
            ownDir = faceTo(t != null ? t.position() : ownAt);
            for (int k = 2; k < 30; k += 2) {
                Vec3 p = position().add(ownDir.scale(k));
                sl.sendParticles(GOLD_DUST, p.x, p.y + 0.2, p.z, 2, 0.6, 0, 0.6, 0);
            }
            playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.5f, 1.0f);
        }
        if (mv != null) mv.face(position().add(ownDir), 360f);
        setYRot((float) (Mth.atan2(ownDir.z, ownDir.x) * Mth.RAD_TO_DEG) - 90f);
        if (i < 10) { setDeltaMovement(0, getDeltaMovement().y, 0); return false; }
        if (i == 10) dbgCharges++;
        double[] a = com.hukhong.arena.BossArena.bounds(this);
        if (a != null) {
            double rx = getX() - a[0], rz = getZ() - a[1];
            if (Math.hypot(rx, rz) > a[2] - 4 && rx * ownDir.x + rz * ownDir.z > 0) { setDeltaMovement(0, getDeltaMovement().y, 0); return true; }
        }
        setDeltaMovement(ownDir.x * 0.85, getDeltaMovement().y, ownDir.z * 0.85);
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 6, 1, 1, 1, 0.02);
        sl.sendParticles(GOLD_DUST, getX(), getY() + 0.5, getZ(), 4, 1, 0.5, 1, 0);
        for (Player p : players(sl, position(), getBbWidth() * 0.5 + 1.5))
            if (ownHit.add(p.getUUID())) hit(p, atk(1.1), position(), true);
        return i >= 40;
    }

    /** Bị áp sát dưới thân > 3s: bay vọt lên cao rồi rơi đè. */
    private boolean crush(ServerLevel sl, LivingEntity t, int i) {
        Vec3 dm = getDeltaMovement();
        if (i == 0) playSound(SoundEvents.ENDER_DRAGON_FLAP, 3f, 0.5f);
        if (i < 14) { setDeltaMovement(dm.x * 0.5, 0.6, dm.z * 0.5); return false; }
        if (i < 26) {
            Vec3 to = t == null ? Vec3.ZERO : t.position().subtract(position()).multiply(1, 0, 1);
            if (to.length() > 0.3) to = to.normalize().scale(Math.min(0.3, to.length()));
            setDeltaMovement(to.x, 0, to.z);
            Vec3 g = new Vec3(getX(), com.hukhong.boss.ai.Locomotion.groundY(sl, getX(), getY(), getZ()), getZ());
            if (i == 14) Fx.telegraphCircle(sl, null, g, 5.5f, 16, GOLD);
            return false;
        }
        setDeltaMovement(0, -1.6, 0);
        if (onGround() || i > 50) {
            Vec3 c = position();
            for (Player p : players(sl, c, 5.5)) hit(p, atk(1.4), c, true);
            if (t != null && !(t instanceof Player) && t.distanceTo(this) < 6) { t.setDeltaMovement(0, 0.8, 0); t.hurtMarked = true; }
            Fx.groundCrack(sl, null, c, 6f, GOLD, 8, 40);
            Fx.shake(sl, c, 3f, 14, 40);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.5, c.z, 1, 0, 0, 0, 0);
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.5f);
            dbgCrushes++;
            return true;
        }
        return false;
    }

    /** Ấn Quang: 3 vòng sáng nổ dưới chân người chơi theo nhịp 1-2-...trễ. */
    private boolean anQuang(ServerLevel sl, LivingEntity t, int i) {
        setDeltaMovement(getDeltaMovement().scale(0.5));
        if (i == 0) {
            for (int delay : new int[]{8, 20, 44}) {
                later(delay, () -> {
                    LivingEntity tt = getTarget();
                    Vec3 at = tt != null ? tt.position() : ownAt;
                    Fx.telegraphCircle(sl, null, at, 3f, 14, GOLD);
                    later(14, () -> {
                        for (Player p : players(sl, at, 3)) hit(p, atk(0.8), at, false);
                        Fx.groundRing(sl, null, at, 3.4f, GOLD, 12);
                        sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.5, at.z, 30, 0.8, 1.5, 0.8, 0.1);
                        sl.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, net.minecraft.sounds.SoundSource.HOSTILE, 2f, 0.7f);
                        dbgRings++;
                    });
                });
            }
        }
        return i >= 16;
    }

    /** Người chơi hồi máu (dạng 2): chùm tia sao. */
    private boolean starBeam(ServerLevel sl, LivingEntity t, int i) {
        if (i == 8) {
            Vec3 to = t != null ? t.position() : ownAt, from = position().add(0, getBbHeight() * 0.8, 0);
            for (int k = 0; k < 5; k++)
                Fx.ribbon(sl, null, from, to.add(random.nextGaussian() * 1.2, 0.2, random.nextGaussian() * 1.2), 0, 6, 0, 0.4f, GOLD);
            for (Player p : players(sl, to, 2.5)) hit(p, atk(1.0), to, false);
            playSound(SoundEvents.BEACON_POWER_SELECT, 3f, 1.4f);
            dbgBeams++;
        }
        return i >= 20;
    }

    /** Gametest/lệnh: bỏ qua cutscene biến hình, vào thẳng dạng 2. */
    public void debugForceForm2(ServerLevel sl) {
        if (form() != 1) return;
        reveal(sl);
        morph = 0;
        skillCd = 40;
    }

    // ------------------------------------------------------------ client + GeckoLib
    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide && tickCount % 2 == 0 && form() == 2)
            level().addParticle(phase() >= 3 ? ParticleTypes.SOUL_FIRE_FLAME : ModParticles.HU_KHONG.get(),
                    getRandomX(1.4), getRandomY(), getRandomZ(1.4), 0, 0.08, 0);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        String n = "animation.nguyen_thuy_tich_diet.", n1 = "animation.nttd_form1.";
        c.add(new AnimationController<>(this, "move", 4, s -> {
            if (s.getAnimatable().form() == 2) return s.setAndContinue(RawAnimation.begin().thenLoop(n + "float"));
            return s.setAndContinue(s.isMoving() ? RawAnimation.begin().thenLoop(n1 + "walk") : RawAnimation.begin().thenLoop(n1 + "idle"));
        }));
        AnimationController<NguyenThuyTichDiet> sk = new AnimationController<>(this, "skill", 0, s -> PlayState.STOP);
        for (String a : new String[]{"attack", "skill_a", "skill_b", "skill_c", "phase"})
            sk.triggerableAnim(a, RawAnimation.begin().then(n + a, Animation.LoopType.PLAY_ONCE));
        for (String a : new String[]{"attack", "slam", "combo", "teleport", "pillars", "ult", "transform"})
            sk.triggerableAnim("f1_" + a, RawAnimation.begin().then(n1 + a, Animation.LoopType.PLAY_ONCE));
        c.add(sk);
        // BossBrain: "f1_<tên>" / "f2_<tên>" (playBrainAnim thêm tiền tố theo dạng); tên thiếu rơi về animation gần nhất
        AnimationController<NguyenThuyTichDiet> br = new AnimationController<>(this, com.hukhong.boss.ai.BrainAnims.CONTROLLER, 4, s -> PlayState.STOP);
        brainAnims(br, "f1_", n1, BRAIN_F1, F1_ANIMS, F1_ALIAS, "walk");
        brainAnims(br, "f2_", n, BRAIN_F2, F2_ANIMS, F2_ALIAS, "float");
        c.add(br);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
