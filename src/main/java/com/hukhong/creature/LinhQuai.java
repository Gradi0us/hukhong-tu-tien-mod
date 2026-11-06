package com.hukhong.creature;

import com.hukhong.entity.HuHoa;
import com.hukhong.entity.HuMob;
import com.hukhong.entity.PoisonSpit;
import com.hukhong.events.CorruptBuff;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * Sinh vat thu dich: Doc Van Xa (doc), Sa Trung (chui cat, tro len co bao truoc), Bang Hon (lam cham/dong bang vung),
 * Moc Yeu (dung yen nhu cay, troi re), Doc Thiem (phun doc tam xa), Quan Hon (doi hinh 3-5, thuong tran).
 */
public class LinhQuai extends Monster implements GeoEntity, HuMob {
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(LinhQuai.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BURROWED = SynchedEntityData.defineId(LinhQuai.class, EntityDataSerializers.BOOLEAN);
    /** Năm tuổi Yêu Thú (ordinal YeuThuAge), -1 = không phải Yêu Thú. */
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(LinhQuai.class, EntityDataSerializers.INT);
    public static final net.minecraft.resources.ResourceLocation AGE_MOD = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hukhong", "yeu_thu_age");
    public static final int EMERGE_WARN_TICKS = 20, BURROW_IDLE_TICKS = 100;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final CreatureKind kind;
    private int auraCd, emerge, idle;
    /** Hệ số chia sát thương nhận vào khi máu theo năm tuổi vượt trần máu của game (>= 1). */
    private float toughness = 1f;
    /** Trạng thái hồn kỹ Yêu Thú (YeuThuSkills). */
    int skillCd = 60, windup, charge, chargeLeft;
    Vec3 chargeDir = Vec3.ZERO;
    /** Mục tiêu của hồn kỹ đang tung (giữ cả khi AI đổi/bỏ mục tiêu). */
    LivingEntity skillTarget;
    final java.util.Set<Integer> chargeHits = new java.util.HashSet<>();

    public float toughness() { return toughness; }

    @Override public boolean hurt(DamageSource s, float amount) {
        return super.hurt(s, toughness > 1f && !s.is(DamageTypeTags.BYPASSES_INVULNERABILITY) ? amount / toughness : amount);
    }

    public LinhQuai(EntityType<? extends LinhQuai> t, Level l) {
        super(t, l);
        kind = CreatureKind.of(t);
    }

    public CreatureKind kind() { return kind; }

    public boolean burrowed() { return entityData.get(BURROWED); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(LEVEL, 1).define(BURROWED, false).define(AGE, -1);
    }

    @Override protected void registerGoals() {
        CreatureKind k = CreatureKind.of(getType());
        goalSelector.addGoal(0, new FloatGoal(this));
        if (k == CreatureKind.DOC_THIEM) goalSelector.addGoal(1, new SpitGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, k == CreatureKind.DOC_VAN_XA ? 1.3 : 1.1, false));
        if (k != CreatureKind.MOC_YEU) goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8)); // Moc Yeu dung yen nhu cay
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l, DifficultyInstance d, MobSpawnType t, SpawnGroupData g) {
        SpawnGroupData r = super.finalizeSpawn(l, d, t, g);
        entityData.set(LEVEL, CreatureCommon.scaleOnSpawn(this, kind, l));
        if (kind.beast() && age() == null) {
            double depth = com.hukhong.world.yeulam.DaiSamLam.depth(l.getLevel().getSeed(), getX(), getZ());
            YeuThuAge a = YeuThuAge.roll(depth, getRandom());
            if (kind.mini() && a.ordinal() < YeuThuAge.Y1000.ordinal()) a = YeuThuAge.Y1000; // boss Yêu Thú: ít nhất thiên niên
            setAge(a);
        }
        return r;
    }

    /** Năm tuổi (null nếu không phải Yêu Thú). */
    public YeuThuAge age() { int a = entityData.get(AGE); return a < 0 ? null : YeuThuAge.of(a); }

    /** Đặt năm tuổi: đổi máu / công / giáp / kích thước theo bậc (ghi đè bậc cũ), hồi đầy máu. */
    public void setAge(YeuThuAge a) {
        entityData.set(AGE, a.ordinal());
        var hpInst = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (hpInst != null) hpInst.removeModifier(AGE_MOD);
        double wanted = getMaxHealth() * a.hpMul;
        var mul = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        var add = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
        ageMod(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, a.hpMul - 1, mul);
        ageMod(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, a.dmgMul - 1, mul);
        ageMod(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, a.armor, add);
        ageMod(net.minecraft.world.entity.ai.attributes.Attributes.SCALE, CreatureKind.BEAST_SCALE * a.scale - 1, add);
        ageMod(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, Math.min(1, a.ordinal() * 0.2), add);
        if (a.ordinal() >= YeuThuAge.Y10000.ordinal()) setPersistenceRequired();
        toughness = (float) Math.max(1.0, wanted / Math.max(1, getMaxHealth())); // máu vượt trần 1024 -> giảm sát thương nhận
        refreshDimensions();
        setHealth(getMaxHealth());
    }

    private void ageMod(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> at, double v,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation op) {
        var inst = getAttribute(at);
        if (inst == null) return;
        inst.removeModifier(AGE_MOD);
        if (v != 0) inst.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(AGE_MOD, v, op));
    }

    // ---- ky nang ----
    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            YeuThuAge ag = age();
            if (ag != null && tickCount % (ag.ordinal() >= 2 ? 3 : 8) == 0) {
                int c = ag.color;
                var dust = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f), 1.2f);
                double rr = getBbWidth() * 0.9 + 0.4, t = tickCount * 0.15;
                for (int i = 0; i < 2 + ag.ordinal(); i++) {
                    double a = t + i * Math.PI * 2 / (2 + ag.ordinal());
                    level().addParticle(dust, getX() + Math.cos(a) * rr, getY() + 0.1 + getBbHeight() * 0.5 * (i % 2), getZ() + Math.sin(a) * rr, 0, 0, 0);
                }
            }
            if (kind == CreatureKind.BANG_HON && tickCount % 4 == 0)
                level().addParticle(ParticleTypes.SNOWFLAKE, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
            return;
        }
        if (kind.mini()) MiniBossSkills.tick(this);
        if (kind.beast() && !kind.mini()) YeuThuSkills.tick(this);
        if (kind == CreatureKind.BANG_HON) freezeAura();
        if (kind == CreatureKind.SA_TRUNG) burrowTick();
    }

    /** Bang Hon: moi 1.5 giay, nguoi choi trong 5 block bi cham + dong bang dan. */
    private void freezeAura() {
        if (--auraCd > 0) return;
        auraCd = 30;
        boolean hit = false;
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(5), e -> !e.isSpectator() && !e.isCreative())) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze() + 40, p.getTicksFrozen() + 60));
            hit = true;
        }
        if (hit && level() instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1, getZ(), 24, 2.5, 0.8, 2.5, 0.02);
    }

    /** Sa Trung: chui cat khi khong co muc tieu; co nguoi lai gan -> bao truoc 1 giay (bui + cat) roi tro len. */
    private void burrowTick() {
        boolean near = level().getNearestPlayer(this, 7) instanceof Player np && !np.isSpectator() && !np.isCreative();
        if (burrowed()) {
            getNavigation().stop();
            setTarget(null);
            if (near) {
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(blockPosition().below())),
                            getX(), getY() + 0.1, getZ(), 6, 0.5, 0.1, 0.5, 0.05);
                }
                if (++emerge >= EMERGE_WARN_TICKS) setBurrowed(false);
            } else emerge = 0;
        } else {
            if (getTarget() != null || near || !onGround()) { idle = 0; return; }
            if (++idle >= BURROW_IDLE_TICKS && level().getBlockState(blockPosition().below()).is(BlockTags.SAND)) setBurrowed(true);
        }
    }

    private void setBurrowed(boolean v) {
        entityData.set(BURROWED, v);
        setInvisible(v);
        setSilent(v);
        emerge = 0;
        idle = 0;
        if (level() instanceof ServerLevel sl)
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(blockPosition().below())),
                    getX(), getY() + 0.2, getZ(), 20, 0.5, 0.2, 0.5, 0.08);
    }

    @Override public boolean isInvulnerableTo(DamageSource s) {
        return (burrowed() && !s.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) || super.isInvulnerableTo(s);
    }

    @Override public boolean isPushable() { return !burrowed() && super.isPushable(); }

    @Override public boolean doHurtTarget(Entity e) {
        boolean r = super.doHurtTarget(e);
        if (r && !level().isClientSide) {
            triggerAnim("attack", "attack");
            if (e instanceof LivingEntity le) switch (kind) {
                case DOC_VAN_XA, DOC_THIEM -> le.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
                case BANG_HON -> le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                case MOC_YEU -> { // troi re: dong cung gan nhu 2 giay
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 6));
                    if (level() instanceof ServerLevel sl)
                        sl.sendParticles(ParticleTypes.COMPOSTER, le.getX(), le.getY() + 0.2, le.getZ(), 12, 0.4, 0.1, 0.4, 0.02);
                }
                case QUAN_HON -> le.knockback(0.6, getX() - le.getX(), getZ() - le.getZ()); // thuong tran day lui
                case HU_LANG, HU_LANG_VUONG -> le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0)); // cắn xé: suy yếu
                case THACH_QUY, THACH_CU_NHAN -> { le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)); le.push(0, 0.35, 0); le.hurtMarked = true; }
                case AM_HON_SU -> le.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
                case HOA_TINH -> le.igniteForSeconds(4);
                case HUYET_YEU_VUONG -> heal(4f); // hút huyết
                case XICH_VAN_HO -> le.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0)); // vuốt xé: chảy máu
                case HAC_GIAP_TE -> { le.knockback(1.2, getX() - le.getX(), getZ() - le.getZ()); le.push(0, 0.25, 0); le.hurtMarked = true; } // húc
                case CUONG_LUC_VIEN -> { le.push(0, 0.6, 0); le.hurtMarked = true; le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0)); } // đấm hất
                case BICH_NHAN_THIEM -> { le.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1)); le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)); }
                case NGAN_LAN_MANG -> le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 4)); // siết
                case HUYET_NHA_TRU -> { le.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0)); le.knockback(0.5, getX() - le.getX(), getZ() - le.getZ()); }
                case U_ANH_BAO -> le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0)); // vồ trong bóng tối
                case THIET_GIAP_TRI_CHU -> le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                case BAO_VIEM_TICH -> le.igniteForSeconds(3);
                case THU_YEU, VAN_NIEN_THU_VUONG -> { le.knockback(0.9, getX() - le.getX(), getZ() - le.getZ()); le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)); }
                case THUC_NHAN_HOA -> heal(2f); // nuốt: hút máu
                case DANG_XA -> le.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                case QUY_CO -> le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0)); // bào tử gây choáng váng
                default -> { }
            }
        }
        return r;
    }

    /** Doc Thiem: phun nuoc bot doc khi muc tieu cach 3-14 block. */
    private static final class SpitGoal extends Goal {
        private final LinhQuai m;
        private int cd = 40;

        SpitGoal(LinhQuai m) { this.m = m; setFlags(EnumSet.of(Flag.LOOK)); }

        @Override public boolean canUse() {
            LivingEntity t = m.getTarget();
            if (t == null || !t.isAlive()) return false;
            double d = m.distanceToSqr(t);
            return --cd <= 0 && d >= 9 && d <= 196 && m.getSensing().hasLineOfSight(t);
        }

        @Override public void start() {
            cd = 60;
            LivingEntity t = m.getTarget();
            m.triggerAnim("attack", "skill");
            PoisonSpit.launch(m, t, (float) HuHoa.skillDamage(m.huLevel(), 0.6), 2.0);
        }
    }

    @Override protected void dropCustomDeathLoot(ServerLevel sl, DamageSource src, boolean recent) {
        super.dropCustomDeathLoot(sl, src, recent);
        CreatureCommon.rollDrops(this, kind);
        YeuThuAge ag = age();
        if (ag != null && ag.boneChance > 0 && com.hukhong.gates.RewardGate.canReward(this) && getRandom().nextDouble() < ag.boneChance) {
            var bones = com.hukhong.honcot.Bone.values();
            spawnAtLocation(com.hukhong.honcot.HonCotRegistry.stack(bones[getRandom().nextInt(bones.length)], ag.boneTier,
                    40 + ag.ordinal() * 10 + getRandom().nextInt(25)));
            if (src.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp)
                sp.sendSystemMessage(Component.translatable("hukhong.yeu_thu.bone_drop", Component.translatable(ag.langKey()), getTypeName()).withColor(ag.color == 0x2A2A2A ? 0x9A9A9A : ag.color));
        }
    }

    @Override public Component getDisplayName() {
        YeuThuAge ag = age();
        if (ag == null) return HuHoa.name(huLevel(), getTypeName());
        return Component.empty().append(Component.literal("[").append(Component.translatable(ag.langKey())).append("] ")
                .withColor(ag.color == 0x2A2A2A ? 0x6E6E6E : ag.color)).append(HuHoa.name(huLevel(), getTypeName()));
    }
    @Override public boolean shouldShowName() { return !burrowed(); }

    // ---- mini boss: thanh máu ----
    private net.minecraft.server.level.ServerBossEvent bossBar;

    net.minecraft.server.level.ServerBossEvent bar() {
        if (bossBar == null && kind.mini()) {
            bossBar = new net.minecraft.server.level.ServerBossEvent(getDisplayName(),
                    kind == CreatureKind.HUYET_YEU_VUONG ? net.minecraft.world.BossEvent.BossBarColor.RED
                            : kind == CreatureKind.HU_LANG_VUONG ? net.minecraft.world.BossEvent.BossBarColor.PURPLE : net.minecraft.world.BossEvent.BossBarColor.WHITE,
                    net.minecraft.world.BossEvent.BossBarOverlay.NOTCHED_10);
        }
        return bossBar;
    }

    @Override public void startSeenByPlayer(net.minecraft.server.level.ServerPlayer p) { super.startSeenByPlayer(p); if (bar() != null) bar().addPlayer(p); }

    @Override public void stopSeenByPlayer(net.minecraft.server.level.ServerPlayer p) { super.stopSeenByPlayer(p); if (bossBar != null) bossBar.removePlayer(p); }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (bossBar != null) bossBar.setProgress(getHealth() / getMaxHealth());
    }

    @Override public boolean removeWhenFarAway(double d) { YeuThuAge a = age(); return !kind.mini() && (a == null || a.ordinal() < YeuThuAge.Y10000.ordinal()) && super.removeWhenFarAway(d); }
    @Override public boolean isSunBurnTick() { return false; }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(HuHoa.TAG_LEVEL, huLevel());
        tag.putBoolean("Burrowed", burrowed());
        tag.putInt("YeuThuAge", entityData.get(AGE));
        tag.putFloat("YeuThuTough", toughness);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(HuHoa.TAG_LEVEL)) entityData.set(LEVEL, tag.getInt(HuHoa.TAG_LEVEL));
        if (tag.getBoolean("Burrowed")) { entityData.set(BURROWED, true); setInvisible(true); }
        if (tag.contains("YeuThuAge")) entityData.set(AGE, tag.getInt("YeuThuAge"));
        if (tag.contains("YeuThuTough")) toughness = Math.max(1f, tag.getFloat("YeuThuTough"));
    }

    @Override public int huLevel() { return entityData.get(LEVEL); }
    @Override public void setHuLevel(int level) { entityData.set(LEVEL, Math.max(1, level)); }
    @Override public double huWeight() { YeuThuAge a = age(); return CreatureCommon.weight(kind) * (a == null ? 1 : a.expMul); }
    @Override public float honTinhChance() { YeuThuAge a = age(); return (float) Math.min(1.0, CreatureCommon.honTinh(kind) * (a == null ? 1 : 1 + a.ordinal())); }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) { CreatureCommon.controllers(this, kind.id, c); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
