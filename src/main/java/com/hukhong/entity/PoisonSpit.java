package com.hukhong.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Nuoc bot doc cua quai Hu Hoa: bay cong nhe, trung dau no doc ban kinh nho (thay mui ten). */
public class PoisonSpit extends Projectile {
    private float dmg = 4f;
    private double aoe = 2.5;
    private int poisonTicks = 100, poisonAmp = 1, life;
    private boolean frost;

    public PoisonSpit(EntityType<? extends PoisonSpit> t, Level l) { super(t, l); }

    /** Phong tu {@code owner} ve phia {@code target}, sat thuong {@code dmg}. */
    public static PoisonSpit launch(net.minecraft.world.entity.Mob owner, LivingEntity target, float dmg, double aoe) {
        PoisonSpit s = new PoisonSpit(V3Entities.POISON_SPIT.get(), owner.level());
        s.setOwner(owner);
        s.dmg = dmg;
        s.aoe = aoe;
        s.setPos(owner.getX(), owner.getEyeY() - 0.2, owner.getZ());
        Vec3 d = new Vec3(target.getX() - owner.getX(), target.getY(0.5) - s.getY(), target.getZ() - owner.getZ());
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        s.shoot(d.x, d.y + h * 0.12, d.z, 1.3f, 0.8f);
        owner.level().addFreshEntity(s);
        return s;
    }

    /** Phong theo huong tuy y (dan tia / mua). */
    public static PoisonSpit launchDir(net.minecraft.world.entity.Mob owner, Vec3 from, Vec3 dir, float dmg, double aoe, float speed) {
        PoisonSpit s = new PoisonSpit(V3Entities.POISON_SPIT.get(), owner.level());
        s.setOwner(owner);
        s.dmg = dmg;
        s.aoe = aoe;
        s.setPos(from.x, from.y, from.z);
        s.shoot(dir.x, dir.y, dir.z, speed, 0.0f);
        owner.level().addFreshEntity(s);
        return s;
    }

    public PoisonSpit withPoison(int ticks, int amp) { poisonTicks = ticks; poisonAmp = amp; return this; }

    /** Bien the bang: lam cham + dong bang thay vi doc. */
    public PoisonSpit frost() { frost = true; return this; }

    public float damage() { return dmg; }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {}

    @Override public void tick() {
        super.tick();
        Vec3 mv = getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) hitTargetOrDeflectSelf(hit);
        if (isRemoved()) return;
        setPos(getX() + mv.x, getY() + mv.y, getZ() + mv.z);
        setDeltaMovement(mv.scale(0.99).add(0, -0.03, 0));
        if (level() instanceof ServerLevel sl) {
            if (tickCount % 2 == 0) {
                sl.sendParticles(frost ? ParticleTypes.SNOWFLAKE : ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.0);
                sl.sendParticles(com.hukhong.vfx.ModParticles.HU_KHONG.get(), getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.0);
            }
            if (++life > 100) discard();
        }
    }

    @Override protected void onHit(HitResult r) {
        super.onHit(r);
        if (!(level() instanceof ServerLevel sl)) return;
        Entity owner = getOwner();
        sl.sendParticles(frost ? ParticleTypes.SNOWFLAKE : ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(), 24, aoe * 0.4, aoe * 0.3, aoe * 0.4, 0.05);
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY(), getZ(), 16, aoe * 0.4, aoe * 0.3, aoe * 0.4, 0.02);
        playSound(SoundEvents.SLIME_SQUISH, 1.0f, 0.7f);
        for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(aoe))) {
            if (v == owner || v instanceof HuMob || v.isSpectator() || v.distanceToSqr(this) > aoe * aoe + 1) continue;
            v.hurt(damageSources().indirectMagic(this, owner), dmg);
            if (frost) {
                v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                v.setTicksFrozen(v.getTicksRequiredToFreeze() + 80);
            } else v.addEffect(new MobEffectInstance(MobEffects.POISON, poisonTicks, poisonAmp));
            if (v instanceof Player p) HuSkill.hu(p, 100);
        }
        discard();
    }

    @Override public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putFloat("Dmg", dmg);
        t.putDouble("Aoe", aoe);
        t.putBoolean("Frost", frost);
    }

    @Override public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.contains("Dmg")) dmg = t.getFloat("Dmg");
        if (t.contains("Aoe")) aoe = t.getDouble("Aoe");
        frost = t.getBoolean("Frost");
    }
}
