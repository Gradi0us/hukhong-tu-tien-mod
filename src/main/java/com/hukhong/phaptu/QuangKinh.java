package com.hukhong.phaptu;

import com.hukhong.core.Aptitude;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quang Ảnh Kính (Pháp Quang — R, 2026-11-06): tấm kính ánh sáng lơ lửng.
 * <ul>
 *   <li>Số kính tối đa theo tư chất: D/C 1, B/A/S 2, SS/SSS 3; đặt quá thì kính cũ nhất vỡ.</li>
 *   <li>Hút sự chú ý: mỗi giây quái trong 8 ô chuyển mục tiêu sang kính. Chịu 10 đòn từ quái thì vỡ; người chơi không phá được.</li>
 *   <li>Lặp chiêu: khi chủ dùng Q (tia), E (choáng) hoặc T (tia lớn), mỗi kính bắn lại chiêu đó về cùng điểm ngắm với 40% sát thương.</li>
 *   <li>Tồn tại 45 giây.</li>
 * </ul>
 */
public class QuangKinh extends LivingEntity {
    public static final int MAX_HITS = 10, LIFE = 900, TAUNT_RADIUS = 8;
    public static final double ECHO = 0.40, PLACE_RANGE = 12;
    private static final EntityDataAccessor<Integer> HITS = SynchedEntityData.defineId(QuangKinh.class, EntityDataSerializers.INT);
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.92f, 0.55f), 0.9f);
    private static final Map<UUID, List<QuangKinh>> BY_OWNER = new HashMap<>();

    private UUID owner;
    private int life = LIFE;

    public QuangKinh(EntityType<? extends QuangKinh> t, Level l) {
        super(t, l);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder attributes() { return LivingEntity.createLivingAttributes(); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(HITS, 0); }

    public int hits() { return entityData.get(HITS); }

    public UUID owner() { return owner; }

    // ---------------------------------------------------------------- số lượng

    public static int maxMirrors(Aptitude a) {
        return switch (a) { case D, C -> 1; case B, A, S -> 2; case SS, SSS -> 3; };
    }

    public static List<QuangKinh> of(ServerPlayer p) {
        List<QuangKinh> l = BY_OWNER.computeIfAbsent(p.getUUID(), k -> new ArrayList<>());
        l.removeIf(k -> k.isRemoved() || !k.isAlive());
        return l;
    }

    /** R: đặt kính tại điểm ngắm (tối đa 12 ô) — trả kính mới. */
    public static QuangKinh place(ServerLevel lv, ServerPlayer p, double burst) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        var r = lv.clip(new ClipContext(eye, eye.add(look.scale(PLACE_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 at = r.getType() == HitResult.Type.MISS ? eye.add(look.scale(PLACE_RANGE)) : r.getLocation().subtract(look.scale(0.6));
        at = new Vec3(at.x, groundY(lv, at) + 0.4, at.z);
        List<QuangKinh> l = of(p);
        int max = maxMirrors(PhapTu.aptitude(p));
        while (l.size() >= max) { l.get(0).shatter(); l.remove(0); }
        QuangKinh k = PhapTuRegistry.KINH.get().create(lv);
        if (k == null) return null;
        k.owner = p.getUUID();
        k.moveTo(at.x, at.y, at.z, p.getYRot(), 0);
        lv.addFreshEntity(k);
        l.add(k);
        Fx.play(lv, FxType.LIGHT_PILLAR, at, Vec3.ZERO, 0.8f, PhapElement.QUANG.rgb, 20);
        Fx.groundRing(lv, p, at.add(0, -0.3, 0), 3f, PhapElement.QUANG.rgb, 18);
        lv.playSound(null, k.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.4f);
        lv.playSound(null, k.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.8f);
        if (burst > 0) for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, k.getBoundingBox().inflate(3), x -> PhapRuntime.valid(p, x)))
            PhapSkills.hit(p, e, burst);
        p.displayClientMessage(net.minecraft.network.chat.Component.translatable("hukhong.phap.quang.kinh", l.size(), max).withColor(PhapElement.QUANG.rgb), true);
        return k;
    }

    private static double groundY(ServerLevel lv, Vec3 at) {
        var r = lv.clip(new ClipContext(at.add(0, 1, 0), at.add(0, -6, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()));
        return r.getType() == HitResult.Type.MISS ? at.y : r.getLocation().y;
    }

    /** Tâm phát chiêu của kính. */
    public Vec3 core() { return position().add(0, 1.1, 0); }

    // ---------------------------------------------------------------- lặp chiêu

    /** Điểm chủ đang ngắm (chạm khối hoặc 16 ô). */
    static Vec3 aimPoint(ServerLevel lv, ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), to = eye.add(p.getLookAngle().scale(range));
        var r = lv.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return r.getType() == HitResult.Type.MISS ? to : r.getLocation();
    }

    /** Hướng bắn của kính: tới điểm ngắm; nếu điểm ngắm quá gần kính thì theo hướng nhìn của chủ. */
    public Vec3 aimDir(Vec3 aim, Vec3 fallback) {
        Vec3 d = aim.subtract(core());
        return d.lengthSqr() < 4 ? fallback : d.normalize();
    }

    /** Q / E của chủ: mỗi kính lặp lại. Trả số kính đã lặp. */
    public static int echo(ServerLevel lv, ServerPlayer p, int slot, double c) {
        List<QuangKinh> l = of(p);
        if (l.isEmpty()) return 0;
        Vec3 aim = aimPoint(lv, p, 16);
        for (QuangKinh k : l) {
            Vec3 from = k.core();
            switch (slot) {
                case 0 -> {
                    Vec3 dir = k.aimDir(aim, p.getLookAngle());
                    for (LivingEntity t : PhapSkills.line(lv, p, from, dir, 16, 0.6)) PhapSkills.hit(p, t, c * ECHO);
                    Fx.ribbon(lv, p, from, from.add(dir.scale(16)), 0, 3, 1, 0.07f, PhapElement.QUANG.rgb);
                    lv.sendParticles(ParticleTypes.END_ROD, from.x, from.y, from.z, 4, 0.1, 0.1, 0.1, 0.03);
                }
                case 1 -> {
                    for (LivingEntity t : lv.getEntitiesOfClass(LivingEntity.class, k.getBoundingBox().inflate(3), x -> PhapRuntime.valid(p, x)))
                        PhapRuntime.stun(t, THIEM_STUN);
                    Fx.groundRing(lv, p, k.position().add(0, -0.3, 0), 3f, 0xFFF4C0, 12);
                    lv.sendParticles(ParticleTypes.FLASH, from.x, from.y, from.z, 1, 0, 0, 0, 0);
                }
                default -> { }
            }
        }
        lv.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.9f);
        return l.size();
    }

    /** T (tia lớn): mỗi nhịp sát thương, mỗi kính bắn tia nhỏ về điểm ngắm. */
    public static void echoBeam(ServerLevel lv, ServerPlayer p, double pulseDmg, double range) {
        List<QuangKinh> l = of(p);
        if (l.isEmpty()) return;
        Vec3 aim = aimPoint(lv, p, range);
        for (QuangKinh k : l) {
            Vec3 from = k.core(), dir = k.aimDir(aim, p.getLookAngle());
            for (LivingEntity t : PhapSkills.line(lv, p, from, dir, range, 1.0)) PhapSkills.hit(p, t, pulseDmg * ECHO);
            Fx.ribbon(lv, p, from, from.add(dir.scale(Math.min(range, aim.distanceTo(from) + 2))), 0, 4, 0, 0.18f, PhapElement.QUANG.rgb);
        }
    }

    /** Thiểm Bộ (E): choáng quanh người chủ (giây). */
    public static final double THIEM_STUN = 1.5, THIEM_RADIUS = 3.5;

    // ---------------------------------------------------------------- vòng đời

    @Override public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (!(level() instanceof ServerLevel lv)) return;
        if (--life <= 0 || owner == null) { shatter(); return; }
        ServerPlayer p = lv.getServer().getPlayerList().getPlayer(owner);
        if (p == null || p.level() != lv || p.distanceToSqr(this) > 64 * 64) { shatter(); return; }
        if (tickCount % 20 == 0) for (Mob m : lv.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(TAUNT_RADIUS),
                m -> m.isAlive() && (m instanceof Enemy || m.getTarget() == p) && !(m instanceof com.hukhong.phaptu.PhapTurret) && m.distanceToSqr(this) <= TAUNT_RADIUS * TAUNT_RADIUS))
            m.setTarget(this); // hấp thu sự chú ý của kẻ địch
        if (tickCount % 4 == 0) {
            double a = tickCount * 0.2;
            lv.sendParticles(GOLD, getX() + Math.cos(a) * 0.7, getY() + 1.1 + Math.sin(a * 0.7) * 0.6, getZ() + Math.sin(a) * 0.7, 1, 0, 0, 0, 0);
        }
    }

    @Override public boolean hurt(DamageSource s, float amount) {
        if (level().isClientSide || isRemoved()) return false;
        if (!(s.getEntity() instanceof LivingEntity a) || a instanceof Player) return false; // chỉ quái phá được
        int h = hits() + 1;
        entityData.set(HITS, h);
        if (level() instanceof ServerLevel lv) {
            lv.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.1, getZ(), 6, 0.3, 0.5, 0.3, 0.05);
            lv.playSound(null, blockPosition(), SoundEvents.GLASS_HIT, SoundSource.PLAYERS, 0.9f, 1.4f);
        }
        if (h >= MAX_HITS) shatter();
        return true;
    }

    /** Vỡ kính (hết hạn / đủ 10 đòn / thay kính mới). */
    public void shatter() {
        if (isRemoved()) return;
        if (level() instanceof ServerLevel lv) {
            lv.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.WHITE_STAINED_GLASS.defaultBlockState()),
                    getX(), getY() + 1.1, getZ(), 30, 0.4, 0.6, 0.4, 0.1);
            lv.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1f, 1.2f);
            for (Mob m : lv.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(TAUNT_RADIUS + 4), m -> m.getTarget() == this)) m.setTarget(null);
        }
        discard();
    }

    @Override public boolean isPushable() { return false; }
    @Override public boolean isPickable() { return true; }
    @Override public boolean isAttackable() { return true; }
    @Override public boolean shouldShowName() { return false; }
    @Override public boolean canBeSeenAsEnemy() { return true; }
    @Override protected void doPush(net.minecraft.world.entity.Entity e) { }

    @Override public Iterable<ItemStack> getArmorSlots() { return List.of(); }
    @Override public ItemStack getItemBySlot(EquipmentSlot s) { return ItemStack.EMPTY; }
    @Override public void setItemSlot(EquipmentSlot s, ItemStack st) { }
    @Override public HumanoidArm getMainArm() { return HumanoidArm.RIGHT; }

    @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); }
    @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); }
    @Override public boolean shouldBeSaved() { return false; } // không lưu: chủ thoát game là mất
}
