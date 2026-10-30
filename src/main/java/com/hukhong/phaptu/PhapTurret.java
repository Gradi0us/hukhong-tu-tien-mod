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
public class PhapTurret extends PathfinderMob {
    private static final EntityDataAccessor<Integer> ELEM = SynchedEntityData.defineId(PhapTurret.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MERGED = SynchedEntityData.defineId(PhapTurret.class, EntityDataSerializers.BOOLEAN);
    /** Tiến độ laser 0..1000 (đồng bộ để client biết độ dày tia / hạt). */
    private static final EntityDataAccessor<Integer> RAMP = SynchedEntityData.defineId(PhapTurret.class, EntityDataSerializers.INT);
    private static final double RANGE = 14;

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

    /** Số tick đã khoá cùng một mục tiêu (0 khi chưa có). */
    public int lockTicks() { return lockTicks; }

    public UUID laserTargetId() { return laserTarget; }

    @Override
    protected net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        var d = super.getDefaultDimensions(pose);
        return isMerged() ? d.scale((float) PhapMath.THO_MERGED_SCALE) : d;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (MERGED.equals(key)) refreshDimensions();
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
        DamageRules.hurtBySkill(o, t, dmg);
        int core = lerpRgb(0xC49A5C, 0xFFE9A0, k);
        Fx.ribbon(sl, o, from, to, 0, 5, 0, 0.10f + 0.42f * k, core);                 // tia dày dần theo độ nạp
        Fx.ribbon(sl, o, from, to, 0, 5, 0, 0.04f + 0.14f * k, 0xFFFFFF);              // lõi trắng
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
        return super.hurt(src, amount);
    }

    @Override
    protected void registerGoals() { }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) {
            if (tickCount % 4 == 0) level().addParticle(ParticleTypes.CRIT, getX(), getY() + 1.2, getZ(), 0, 0.05, 0);
            return;
        }
        ServerPlayer o = owner == null ? null : sl.getServer().getPlayerList().getPlayer(owner);
        if (o == null && ownerRef != null && ownerRef.isAlive() && !ownerRef.hasDisconnected()) o = ownerRef;
        if (o == null || !o.isAlive() || !o.getData(com.hukhong.core.ModAttachments.CULTIVATION).paths().contains(Path.PHAP)
                || --life <= 0) {
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.6, getZ(), 12, 0.3, 0.4, 0.3, 0.02);
            discard();
            return;
        }
        if (empowered > 0) empowered--;
        if (tickCount % 20 == 0) {
            taunt(sl);
            if (getHealth() < getMaxHealth() && o.distanceToSqr(this) < 16 * 16) heal(getMaxHealth() * 0.02f); // chủ ở gần thì tháp tự phục hồi 2%/s
        }
        if (isMerged()) laser(sl, o);
        else if (tickCount % shotTicks == 0) shoot(sl, o);
    }

    private boolean enemy(LivingEntity e) {
        return e != this && e.isAlive() && !(e instanceof Player) && !(e instanceof PhapTurret)
                && (e instanceof Enemy || (e instanceof Mob m && m.getTarget() != null));
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
        Vec3 from = position().add(0, 1.1, 0), to = best.getBoundingBox().getCenter();
        float dmg = shotDamage * (empowered > 0 ? 2f : 1f);
        best.invulnerableTime = 0;
        DamageRules.hurtBySkill(o, best, dmg);
        Fx.ribbon(sl, o, from.add(0, 0.5, 0), to, 0.8, 4, 0, 0.16f, 0xB08A4F); // tảng đá bắn theo cung, không phải tia sáng
        sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
                to.x, to.y, to.z, 8, 0.25, 0.25, 0.25, 0.12);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); }
}
