package com.hukhong.phaptu;

import com.hukhong.core.Path;
import com.hukhong.endgame.NguyenThuyTichDiet;
import com.hukhong.gates.DamageRules;
import com.hukhong.stats.StatService;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.UUID;

/**
 * Tháp Thổ (Pháp Tu - Thổ): đứng yên, bắn kẻ địch gần nhất và khiêu khích quái (trừ boss cuối).
 * Sát thương = % sát thương của chủ, máu = % máu tối đa của chủ (xem {@link PhapMath}). Không lưu vào thế giới.
 */
public class PhapTurret extends PathfinderMob implements software.bernie.geckolib.animatable.GeoEntity {
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache geoCache = software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    @Override public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar c) {}
    @Override public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }

    private static final EntityDataAccessor<Integer> ELEM = SynchedEntityData.defineId(PhapTurret.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MERGED = SynchedEntityData.defineId(PhapTurret.class, EntityDataSerializers.BOOLEAN);
    /** Tiến độ laser 0..1000 (đồng bộ để client biết độ dày tia / hạt). */
    private static final EntityDataAccessor<Integer> RAMP = SynchedEntityData.defineId(PhapTurret.class, EntityDataSerializers.INT);
    /** Máu ảo (từ Địa Thứ Lĩnh Vực) x1000 / máu tối đa, đồng bộ để client vẽ khiên. */
    private static final EntityDataAccessor<Integer> SHIELD = SynchedEntityData.defineId(PhapTurret.class, EntityDataSerializers.INT);
    private static final double RANGE = 14;
    /** Trụ cấp 3 (Thái Sơn Thần Tháp) / đang có người nhập điều khiển. */
    private static final EntityDataAccessor<Boolean> TITAN = SynchedEntityData.defineId(PhapTurret.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SPIKE = SynchedEntityData.defineId(PhapTurret.class, EntityDataSerializers.BOOLEAN);
    private Vec3 spikeFrom;
    private static final EntityDataAccessor<Boolean> CONTROL = SynchedEntityData.defineId(PhapTurret.class, EntityDataSerializers.BOOLEAN);
    private boolean ended;
    private int riseFrom;
    private float shield;
    private int shieldTicks;
    /** Tick client bắt đầu hiệu ứng trồi lên (khi sinh ra / khi Hợp Thể). */
    public int riseStart;

    private UUID owner;
    /** Tham chiếu chủ (không lưu) để tháp vẫn sống khi chủ không nằm trong PlayerList (người chơi giả / chuyển chiều). */
    private ServerPlayer ownerRef;
    private float shotDamage;
    private int life = PhapMath.THO_LIFE_TICKS, empowered, shotTicks = PhapMath.THO_SHOT_TICKS;
    /** Hợp Thể: laser đơn mục tiêu. */
    private UUID laserTarget;
    private int lockTicks;

    public PhapTurret(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ELEM, PhapElement.THO.ordinal());
        b.define(MERGED, false);
        b.define(RAMP, 0);
        b.define(SHIELD, 0);
        b.define(TITAN, false);
        b.define(CONTROL, false);
        b.define(SPIKE, false);
    }

    public PhapElement element() { return PhapElement.values()[Math.floorMod(entityData.get(ELEM), PhapElement.values().length)]; }

    public UUID ownerId() { return owner; }

    public boolean isEmpowered() { return empowered > 0; }

    /** Cấu hình lúc triệu hồi. */
    public void setup(ServerPlayer owner, float hp, float shot, int shotTicks) {
        this.shotTicks = Math.max(4, shotTicks);
        this.owner = owner.getUUID();
        this.ownerRef = owner;
        this.shotDamage = shot;
        var inst = getAttribute(Attributes.MAX_HEALTH);
        if (inst != null) inst.setBaseValue(hp);
        setHealth(hp);
    }


    // ------------------------------------------------------------------ Hợp Thể

    public boolean isMerged() { return entityData.get(MERGED); }

    /** Tiến độ laser 0..1 (client dùng cho độ dày tia). */
    public float rampFraction() { return entityData.get(RAMP) / 1000f; }

    public int lifeLeft() { return life; }

    /** Tỉ lệ máu ảo 0..1 (client vẽ khiên). */
    public float shieldFraction() { return entityData.get(SHIELD) / 1000f; }

    public float shield() { return shield; }

    /** Số tick đã khoá cùng một mục tiêu (0 khi chưa có). */
    public int lockTicks() { return lockTicks; }

    public UUID laserTargetId() { return laserTarget; }

    public boolean isTitan() { return entityData.get(TITAN); }

    public boolean isControlled() { return entityData.get(CONTROL); }

    public void setControlled(boolean v) { entityData.set(CONTROL, v); }

    public boolean isSpike() { return entityData.get(SPIKE); }

    public void setLife(int ticks) { life = ticks; }

    float shotDamage() { return shotDamage; }

    /** Gai đá của R trụ cấp 3: máu hp, sát thương mỗi nhịp dmg, sống life tick, đẩy lùi ra xa tâm from. */
    void makeSpike(ServerPlayer o, float hp, float dmg, int life, Vec3 from) {
        setup(o, hp, dmg, 20);
        entityData.set(SPIKE, true);
        this.life = life;
        this.spikeFrom = from;
        riseFrom = tickCount;
    }

    /** Nâng thành trụ cấp 3: máu hp, sống life tick, trồi lên lại. */
    public void makeTitan(float hp, int life) {
        entityData.set(MERGED, false);
        entityData.set(TITAN, true);
        entityData.set(RAMP, 0);
        var inst = getAttribute(Attributes.MAX_HEALTH);
        if (inst != null) inst.setBaseValue(hp);
        setHealth(hp);
        this.life = life;
        laserTarget = null;
        riseFrom = tickCount;
        refreshDimensions();
    }

    /** Gỡ trụ không nổ, không báo. */
    void discardQuiet() { ended = true; discard(); }

    public boolean isEnemy(LivingEntity e) { return enemy(e); }

    @Override
    protected net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        var d = super.getDefaultDimensions(pose);
        return isTitan() ? d.scale((float) PhapMath.THO_TITAN_SCALE) : isMerged() ? d.scale((float) PhapMath.THO_MERGED_SCALE) : d;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if ((MERGED.equals(key) || TITAN.equals(key)) && (isMerged() || isTitan())) { refreshDimensions(); riseStart = tickCount; }
    }

    /**
     * Gộp một trụ nữa (còn otherLife tick, máu otherHp) vào trụ này tại midpoint: to hơn, thời gian sống = tổng, máu = tổng, chuyển sang laser.
     * Không gộp tiếp trụ đã Hợp Thể.
     */
    public void absorb(int otherLife, float otherHp, Vec3 mid) {
        life += Math.max(0, otherLife);
        var inst = getAttribute(Attributes.MAX_HEALTH);
        float hp = (inst == null ? 20f : (float) inst.getBaseValue()) + otherHp;
        if (inst != null) inst.setBaseValue(hp);
        setHealth(Math.min(hp, getHealth() + otherHp));
        entityData.set(MERGED, true);
        refreshDimensions();
        moveTo(mid.x, mid.y, mid.z, getYRot(), 0);
        lockTicks = 0;
        laserTarget = null;
        entityData.set(RAMP, 0);
    }

    /** Laser kiểu Inferno Tower: khoá MỘT mục tiêu, sát thương tăng dần theo thời gian khoá, đổi mục tiêu thì về mức khởi đầu. */
    private void laser(ServerLevel sl, ServerPlayer o) {
        LivingEntity t = null;
        if (laserTarget != null && sl.getEntity(laserTarget) instanceof LivingEntity le && enemy(le) && le.distanceToSqr(this) <= RANGE * RANGE) t = le;
        if (t == null) {
            t = nearestEnemy(sl);
            lockTicks = 0;
            laserTarget = t == null ? null : t.getUUID();
            if (t == null) { if (entityData.get(RAMP) != 0) entityData.set(RAMP, 0); return; }
        }
        lockTicks++;
        double mult = PhapMath.thoLaserMult(lockTicks);
        float k = (float) ((mult - PhapMath.THO_LASER_MIN) / (PhapMath.THO_LASER_MAX - PhapMath.THO_LASER_MIN));
        entityData.set(RAMP, Math.round(k * 1000));
        if (tickCount % PhapMath.THO_LASER_PULSE_TICKS != 0) return;
        Vec3 from = position().add(0, 1.55 * PhapMath.THO_MERGED_SCALE, 0), to = t.getBoundingBox().getCenter();
        float dmg = PhapMath.thoLaserPulse(shotDamage * (empowered > 0 ? 2f : 1f), shotTicks, lockTicks);
        t.invulnerableTime = 0;
        hitNoKnock(o, t, dmg, 10, 2);
        int core = lerpRgb(0xC49A5C, 0xFFE9A0, k);
        Fx.ribbon(sl, o, from, to, 0, 5, 0, 0.10f + 0.42f * k, core);                 // tia dày dần theo độ nạp
        Fx.ribbon(sl, o, from, to, 0, 5, 0, 0.04f + 0.14f * k, 0xFFFFFF);              // lõi trắng
        Fx.ribbonWave(sl, o, from, to, 0, 0.5 + 0.8 * k, 5, 0, 0.06f + 0.10f * k, 0xC47A2A); // xoắn đất quanh tia
        if (tickCount % (PhapMath.THO_LASER_PULSE_TICKS * 2) == 0) {
            Fx.rune(sl, o, to.subtract(from).normalize().scale(-0.4).add(to), 0.5f + 0.7f * k, core, 10, 5); // ấn trận tại điểm chạm
            Fx.qi(sl, from, to.subtract(from).normalize(), 0.5f + 0.5f * k, core, 0, 6);                     // chớp ở tinh thể
        }
        if (k > 0.7f && tickCount % 8 == 0) Fx.groundRing(sl, o, PhapSkills.ground(sl, to).add(0, 0.06, 0), 1.0f + k, core, 10);
        sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState()),
                to.x, to.y, to.z, 2 + Math.round(8 * k), 0.25, 0.25, 0.25, 0.1 + 0.15 * k);
        if (lockTicks % 20 == 0) sl.playSound(null, to.x, to.y, to.z, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, 0.6f + 0.9f * k);
    }

    private static int lerpRgb(int a, int b, float k) {
        int r = Math.round(((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k), g = Math.round(((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k),
                bl = Math.round((a & 255) * (1 - k) + (b & 255) * k);
        return (r << 16) | (g << 8) | bl;
    }

    private LivingEntity nearestEnemy(ServerLevel sl) {
        return sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(RANGE, 5, RANGE), this::enemy).stream()
                .filter(e -> e.distanceToSqr(this) <= RANGE * RANGE)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(this))).orElse(null);
    }
    /** Thiên Địa Phong Tháp: sát thương x2 trong ticks tick. */
    public void empower(int ticks) { empowered = Math.max(empowered, ticks); }

    @Override
    public boolean shouldBeSaved() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.getEntity() instanceof Player || src.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) return false;
        if (shieldTicks > 0 && !level().isClientSide) {
            amount *= (float) (1 - PhapMath.THO_FIELD_ARMOR); // giáp ảo
            float soak = Math.min(shield, amount);
            shield -= soak;
            amount -= soak;
            syncShield();
            if (soak > 0 && level() instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.WAX_OFF, getX(), getY() + 1.2 * scaleK(), getZ(), 6, 0.5, 0.6, 0.5, 0.05);
            if (amount <= 0) { hurtTime = 6; return true; }
        }
        return super.hurt(src, amount);
    }

    @Override
    protected void registerGoals() { }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel fs && tickCount % 10 == 1) com.hukhong.world.TreeFell.fellIn(fs, getBoundingBox().inflate(0.4), 4); // trụ đá trồi lên / lớn lên: làm đổ cây chạm vào
        if (!(level() instanceof ServerLevel sl)) {
            if (tickCount % 4 == 0) level().addParticle(ParticleTypes.CRIT, getX(), getY() + 1.2, getZ(), 0, 0.05, 0);
            return;
        }
        ServerPlayer o = owner == null ? null : sl.getServer().getPlayerList().getPlayer(owner);
        if (o == null && ownerRef != null && ownerRef.isAlive() && !ownerRef.hasDisconnected()) o = ownerRef;
        if (o == null || !o.isAlive() || !o.getData(com.hukhong.core.ModAttachments.CULTIVATION).paths().contains(Path.PHAP)
                || --life <= 0) {
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.6, getZ(), 12, 0.3, 0.4, 0.3, 0.02);
            if (isTitan() && o != null && !ended) { ended = true; ThoTitan.onEnd(sl, o, this, false); }
            discard();
            return;
        }
        if (empowered > 0) empowered--;
        if (isSpike()) { spikeTick(sl, o); return; }
        if (tickCount - riseFrom < 14) rising(sl, tickCount - riseFrom);
        tickFieldShield(sl, o);
        if (tickCount % 20 == 0) {
            taunt(sl);
            if (getHealth() < getMaxHealth() && o.distanceToSqr(this) < 16 * 16) heal(getMaxHealth() * 0.02f); // chủ ở gần thì tháp tự phục hồi 2%/s
        }
        if (isTitan()) ThoTitan.tick(sl, o, this, shotDamage * (empowered > 0 ? 2f : 1f));
        else if (isMerged()) laser(sl, o);
        else if (tickCount % shotTicks == 0) shoot(sl, o);
    }

    /** Trúng đạn trụ: không đẩy lùi, thay bằng làm chậm (amp 1 = -30%, amp 2 = -45%). */
    static void hitNoKnock(ServerPlayer o, LivingEntity t, float dmg, int slowTicks, int amp) {
        Vec3 v = t.getDeltaMovement();
        DamageRules.hurtBySkill(o, t, dmg);
        t.setDeltaMovement(v.x * 0.3, Math.min(v.y, 0), v.z * 0.3);
        t.hurtMarked = true;
        t.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, slowTicks, amp, false, false, true));
    }

    private float scaleK() { return isTitan() ? (float) PhapMath.THO_TITAN_SCALE : isMerged() ? (float) PhapMath.THO_MERGED_SCALE : 1f; }

    /** Gai đá Địa Thứ Hộ Tháp (R trụ cấp 3): đứng yên, có máu riêng, đẩy lùi + gây sát thương quanh mình. */
    private void spikeTick(ServerLevel sl, ServerPlayer o) {
        if (tickCount - riseFrom < 8) {
            sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.POINTED_DRIPSTONE.defaultBlockState()),
                    getX(), getY() + 0.2, getZ(), 6, 0.4, 0.1, 0.4, 0.2);
        }
        if (tickCount % 10 != 0) return;
        double r = 2.2;
        boolean any = false;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r, 2, r), x -> enemy(x) && x.distanceToSqr(this) <= (r + 0.5) * (r + 0.5))) {
            any = true;
            e.invulnerableTime = 0;
            DamageRules.hurtBySkill(o, e, shotDamage);
            Vec3 away = e.position().subtract(spikeFrom == null ? position() : spikeFrom).multiply(1, 0, 1);
            if (away.lengthSqr() < 1e-4) away = e.position().subtract(position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) away = away.normalize();
            e.setDeltaMovement(away.x * 1.1, 0.35, away.z * 1.1);
            e.hurtMarked = true;
            Fx.ribbon(sl, o, position().add(0, 0.3, 0), e.getBoundingBox().getCenter(), 0, 2, 0, 0.14f, PhapElement.THO.rgb);
        }
        if (any) {
            Fx.elem(sl, o, Fx.E_SPIKES, position(), Vec3.ZERO, 0.6f, PhapElement.THO.rgb, 16);
            sl.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.POINTED_DRIPSTONE_HIT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.6f);
        }
    }

    private void syncShield() {
        int v = shieldTicks > 0 ? Math.round(1000 * Math.min(1f, shield / Math.max(1f, getMaxHealth()))) : 0;
        if (entityData.get(SHIELD) != v) entityData.set(SHIELD, v);
    }

    /** Trụ trồi khỏi mặt đất: đất đá bắn tung, vết nứt, cột bụi dâng theo chiều cao trụ. */
    private void rising(ServerLevel sl, int age) {
        float k = scaleK();
        double h = age / 14.0 * 2.2 * k;
        var dirt = new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.ROOTED_DIRT.defaultBlockState());
        var stone = new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState());
        sl.sendParticles(dirt, getX(), getY() + 0.1, getZ(), 10, 0.6 * k, 0.05, 0.6 * k, 0.25);
        sl.sendParticles(stone, getX(), getY() + h, getZ(), 6, 0.35 * k, 0.1, 0.35 * k, 0.15);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.2, getZ(), 2, 0.7 * k, 0.05, 0.7 * k, 0.01);
        if (age == 1) {
            Fx.groundCrack(sl, null, position().add(0, 0.06, 0), 2.6f * k, PhapElement.THO.rgb, 8, 40);
            Fx.groundRing(sl, null, position().add(0, 0.06, 0), 2.2f * k, 0xFFD27A, 16);
            Fx.shake(sl, position(), 0.35f * k, 8, 14f);
            sl.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.DRIPSTONE_BLOCK_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.5f);
        }
        if (age == 12) {
            Fx.elem(sl, null, Fx.E_SPIKES, position(), Vec3.ZERO, 1.2f * k, PhapElement.THO.rgb, 26);
            sl.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.ANVIL_PLACE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.6f);
        }
    }

    /** Trong (hoặc chạm mép) Địa Thứ Lĩnh Vực của chủ: nhận máu ảo + giáp ảo, nạp lại mỗi giây. */
    private void tickFieldShield(ServerLevel sl, ServerPlayer o) {
        double r = ThoService.fieldRadius(o);
        Vec3 c = r <= 0 ? null : ThoService.fieldCenter(o);
        boolean in = c != null && Math.hypot(getX() - c.x, getZ() - c.z) <= r + 1.0 + getBbWidth() / 2 && Math.abs(getY() - c.y) < 6;
        if (in) {
            if (shieldTicks == 0) {
                sl.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.5f);
                Fx.groundRing(sl, null, position().add(0, 0.06, 0), 1.8f * scaleK(), 0xFFD27A, 14);
            }
            shieldTicks = 30;
            if (tickCount % 20 == 0 || shield <= 0) shield = Math.max(shield, (float) (getMaxHealth() * PhapMath.THO_FIELD_SHIELD_PCT));
            if (tickCount % 10 == 0) sl.sendParticles(ParticleTypes.WAX_ON, getX(), getY() + 1.1 * scaleK(), getZ(), 3, 0.6 * scaleK(), 0.8 * scaleK(), 0.6 * scaleK(), 0.02);
        } else if (shieldTicks > 0 && --shieldTicks == 0) shield = 0;
        syncShield();
    }

    /** Trụ bị phá: hút kẻ địch gần vào tâm rồi nổ đá. Hết thời gian thì không nổ. */
    @Override
    public void die(DamageSource src) {
        if (level() instanceof ServerLevel sl && !isRemoved() && !dead) {
            if (isSpike()) {
                sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.DRIPSTONE_BLOCK.defaultBlockState()),
                        getX(), getY() + 0.8, getZ(), 30, 0.4, 0.6, 0.4, 0.2);
                super.die(src);
                return;
            }
            burst(sl);
            if (isTitan() && !ended) {
                ended = true;
                ServerPlayer o = owner == null ? null : sl.getServer().getPlayerList().getPlayer(owner);
                if (o == null) o = ownerRef;
                if (o != null) ThoTitan.onEnd(sl, o, this, true);
            }
        }
        super.die(src);
    }

    private void burst(ServerLevel sl) {
        ServerPlayer o = owner == null ? null : sl.getServer().getPlayerList().getPlayer(owner);
        if (o == null) o = ownerRef;
        if (o == null) return;
        float k = scaleK();
        Vec3 c = position();
        double pull = PhapMath.THO_BURST_PULL * k + getBbWidth() / 2, rad = PhapMath.THO_BURST_RADIUS * k;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(rad + 1, 3, rad + 1), this::enemy)) {
            double d = Math.hypot(e.getX() - c.x, e.getZ() - c.z);
            if (d <= pull + e.getBbWidth() / 2 && d > 0.6) { // hút vào sát chân trụ
                Vec3 to = new Vec3(c.x + (e.getX() - c.x) / d * 0.6, e.getY(), c.z + (e.getZ() - c.z) / d * 0.6);
                Fx.ribbon(sl, o, e.position().add(0, 0.5, 0), to.add(0, 0.5, 0), 0, 3, 0, 0.12f, 0x8A6A3A);
                e.setPos(to.x, to.y, to.z);
                d = 0.6;
            }
            if (d <= rad + e.getBbWidth() / 2) {
                e.invulnerableTime = 0;
                DamageRules.hurtBySkill(o, e, (float) (shotDamage * PhapMath.THO_BURST_COEF * (isMerged() ? PhapMath.THO_BURST_MERGED : 1)));
                e.push((e.getX() - c.x) * 0.1, 0.45, (e.getZ() - c.z) * 0.1);
            }
        }
        Fx.elem(sl, o, Fx.E_SPIKES, c, Vec3.ZERO, 1.8f * k, PhapElement.THO.rgb, 30);
        Fx.groundCrack(sl, o, c.add(0, 0.06, 0), (float) rad + 0.8f, PhapElement.THO.rgb, 10, 50);
        Fx.groundRing(sl, o, c.add(0, 0.06, 0), (float) rad, 0xFFD27A, 14);
        Fx.pool(sl, o, c.add(0, 0.05, 0), (float) rad * 0.8f, 0x6B4A26, 40);
        Fx.shake(sl, c, 0.9f * k, 12, 22f);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.8, c.z, 1, 0, 0, 0, 0);
        sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.DEEPSLATE_BRICKS.defaultBlockState()),
                c.x, c.y + 1, c.z, 60, 0.8 * k, 1.0 * k, 0.8 * k, 0.35);
        sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.RAW_GOLD_BLOCK.defaultBlockState()),
                c.x, c.y + 1.5 * k, c.z, 30, 0.5, 0.5, 0.5, 0.3);
        sl.playSound(null, c.x, c.y, c.z, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.4f, 0.7f);
    }

    private boolean enemy(LivingEntity e) {
        return e != this && e.isAlive() && !(e instanceof Player) && !(e instanceof PhapTurret) && !com.hukhong.endgame.DaKhuyet.isStoryAlly(e)
                && !e.isInvulnerable() && !(e instanceof net.minecraft.world.entity.npc.Npc)
                && !(e instanceof net.minecraft.world.entity.animal.IronGolem) && !(e instanceof net.minecraft.world.entity.TamableAnimal ta && ta.isTame())
                && (e instanceof Enemy || (e instanceof Mob m && (m.getTarget() instanceof Player || m.getTarget() instanceof PhapTurret)));
    }

    private void taunt(ServerLevel sl) {
        int taunted = 0;
        for (Mob m : sl.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(PhapMath.THO_TAUNT_RANGE, 4, PhapMath.THO_TAUNT_RANGE),
                e -> enemy(e) && !(e instanceof NguyenThuyTichDiet))) {
            LivingEntity t = m.getTarget();
            if (t == null || t instanceof Player) { m.setTarget(this); taunted++; }
        }
        if (taunted > 0 && tickCount % 40 == 0) { // xung khiêu khích: vòng đá nở quanh trụ
            Fx.groundRing(sl, null, position().add(0, 0.06, 0), 3.5f, PhapElement.THO.rgb, 14);
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.2, getZ(), 8, 1.2, 0.1, 1.2, 0.01);
        }
    }

    private void shoot(ServerLevel sl, ServerPlayer o) {
        LivingEntity best = sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(RANGE, 5, RANGE), this::enemy).stream()
                .filter(e -> e.distanceToSqr(this) <= RANGE * RANGE)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(this))).orElse(null);
        if (best == null) return;
        Vec3 from = position().add(0, 1.8, 0), to = best.getBoundingBox().getCenter();
        float dmg = shotDamage * (empowered > 0 ? 2f : 1f);
        Vec3 core = position().add(0, 2.3, 0);
        Vec3 d = to.subtract(core);
        int flight = Math.max(2, (int) Math.round(d.length() / 3.5));
        Fx.qi(sl, core.add(d.normalize().scale(0.5)), d.normalize(), 0.6f, 0xFFC36A, 0, 6);             // chớp nạp ở lõi
        Fx.elem(sl, o, Fx.E_SHOT, core, d, 1.0f, PhapElement.THO.rgb, flight);                         // tảng đá nung bay vòng cung, đuôi lửa
        sl.sendParticles(ParticleTypes.CRIT, core.x, core.y, core.z, 6, 0.15, 0.15, 0.15, 0.25);
        sl.playSound(null, core.x, core.y, core.z, net.minecraft.sounds.SoundEvents.BLAZE_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 0.8f);
        final LivingEntity tgt = best;
        final ServerPlayer own = o;
        com.hukhong.skill.SkillScheduler.later(flight, () -> {
            if (!tgt.isAlive() || !own.isAlive()) return;
            tgt.invulnerableTime = 0;
            hitNoKnock(own, tgt, dmg, 30, 1);
            Vec3 at = tgt.getBoundingBox().getCenter();
            Vec3 g = PhapSkills.ground(sl, at.add(0, 0.5, 0));
            if (at.y - g.y < 2.5) {
                Fx.groundCrack(sl, own, g.add(0, 0.06, 0), 1.3f, PhapElement.THO.rgb, 6, 24);
                Fx.groundRing(sl, own, g.add(0, 0.06, 0), 1.1f, 0xFFB35A, 8);
            }
            sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState()),
                    at.x, at.y, at.z, 16, 0.3, 0.3, 0.3, 0.2);
            sl.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 2, 0.2, 0.2, 0.2, 0);
            sl.playSound(null, at.x, at.y, at.z, net.minecraft.sounds.SoundEvents.BASALT_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 0.6f);
        });
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); }
}
