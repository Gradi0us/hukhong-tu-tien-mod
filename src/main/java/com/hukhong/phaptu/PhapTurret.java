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
    private static final double RANGE = 14;

    private UUID owner;
    /** Tham chiếu chủ (không lưu) để tháp vẫn sống khi chủ không nằm trong PlayerList (người chơi giả / chuyển chiều). */
    private ServerPlayer ownerRef;
    private float shotDamage;
    private int life = PhapMath.THO_LIFE_TICKS, empowered, shotTicks = PhapMath.THO_SHOT_TICKS;

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
        if (tickCount % shotTicks == 0) shoot(sl, o);
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
