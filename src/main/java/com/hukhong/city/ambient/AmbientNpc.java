package com.hukhong.city.ambient;

import com.hukhong.entity.HuMob;
import com.hukhong.siege.SiegeFx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Dan cu trang tri (phan lon chi de cho song dong): di dao trong ranh gioi khu dan cu, linh tuan tra, phu khuan thung,
 * tho cang, de tu luyen/thien, tre em. Bat tu voi nguoi choi, khong giao dich. Sinh/xoa boi {@link AmbientSpawner}.
 * Khi thanh/tong bi tan cong ({@link com.hukhong.siege.SiegeDirector}) linh/de tu chien dau that, dan thuong chay vao nha.
 */
public class AmbientNpc extends PathfinderMob {
    public static final int CITIZEN = 0, GUARD = 1, COURIER = 2, DOCK = 3, DISCIPLE = 4, CHILD = 5;
    public static final String TAG = "hukhong_ambient";
    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(AmbientNpc.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(AmbientNpc.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MEDITATING = SynchedEntityData.defineId(AmbientNpc.class, EntityDataSerializers.BOOLEAN);

    // ---- trang thai vay thanh (khong luu): 0 = binh thuong ----
    public static final int SIEGE_NONE = 0, SIEGE_SHELTER = 1, SIEGE_FIGHT = 2, SIEGE_CHEER = 3;
    private static final ResourceLocation SIEGE_HP = ResourceLocation.fromNamespaceAndPath("hukhong", "siege_hp");
    private int siegeMode;
    private boolean archer, hidden, gearOn;
    private int downed, stuck;
    private BlockPos post, shelter;

    private BlockPos home = BlockPos.ZERO;
    private int radius = 40;
    private BlockPos waypoint;

    public AmbientNpc(EntityType<? extends AmbientNpc> t, Level l) {
        super(t, l);
        addTag(TAG);
    }

    public static AttributeSupplier.Builder attrs() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ROLE, CITIZEN);
        b.define(SKIN, 0);
        b.define(MEDITATING, false);
    }

    public int role() { return entityData.get(ROLE); }
    public int skin() { return entityData.get(SKIN); }
    public boolean meditating() { return entityData.get(MEDITATING); }

    /** Gan vai tro + ranh gioi di dao (tam home, ban kinh radius) + do dung. */
    public void setup(int role, int skin, BlockPos home, int radius) {
        entityData.set(ROLE, role);
        entityData.set(SKIN, skin);
        this.home = home;
        this.radius = radius;
        restrictTo(home, radius);
        dress(role);
    }

    private void dress(int role) {
        switch (role) {
            case GUARD -> {
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            }
            case COURIER -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(random.nextBoolean() ? Items.BARREL : Items.CHEST));
            case DOCK -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(random.nextBoolean() ? Items.BARREL : Items.HAY_BLOCK));
            case DISCIPLE -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(random.nextInt(3) == 0 ? Items.STICK : Items.WOODEN_SWORD));
            case CHILD -> {
                var sc = getAttribute(Attributes.SCALE);
                if (sc != null) sc.setBaseValue(0.6);
                var sp = getAttribute(Attributes.MOVEMENT_SPEED);
                if (sp != null) sp.setBaseValue(0.28);
            }
            default -> { }
        }
        for (EquipmentSlot s : EquipmentSlot.values()) setDropChance(s, 0f);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new Downed(this));
        goalSelector.addGoal(0, new Hidden(this));
        goalSelector.addGoal(0, new Shelter(this));
        goalSelector.addGoal(0, new SiegeCombat(this));
        goalSelector.addGoal(0, new Defend(this));
        goalSelector.addGoal(0, new Cheer(this));
        goalSelector.addGoal(1, new Practice(this));
        goalSelector.addGoal(2, new Shuttle(this));
        goalSelector.addGoal(3, new Wander(this));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6f));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    // ---- bat tu voi moi thu tru bypass (vd /kill, void) + khong day ra khoi ranh gioi bang knockback ----
    // Ngoai le khi vay thanh: linh dang chien dau co the bi QUAI (khong phai nguoi choi) lam bi thuong / ha guc.
    @Override public boolean isInvulnerableTo(DamageSource s) {
        if (s.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return !(siegeMode == SIEGE_FIGHT && downed <= 0 && s.getEntity() instanceof Mob);
    }
    @Override public boolean removeWhenFarAway(double d) { return true; }
    @Override public boolean canBeLeashed() { return false; }
    @Override public boolean shouldShowName() { return false; }
    @Override public boolean isPushable() { return !hidden && super.isPushable(); }
    @Override public boolean isPickable() { return !hidden && super.isPickable(); }

    /** Chi mat mau toi muc nguy kich thi "ha guc" nam yen roi hoi phuc, khong chet. */
    @Override
    public boolean hurt(DamageSource s, float amount) {
        if (!level().isClientSide && siegeMode == SIEGE_FIGHT && downed <= 0 && !isInvulnerableTo(s) && amount >= getHealth() - 1f) {
            downed = 160;
            setHealth(Math.max(4f, getMaxHealth() * 0.25f));
            setTarget(null);
            getNavigation().stop();
            setShiftKeyDown(true);
            if (s.getEntity() != null) knockback(0.7, s.getEntity().getX() - getX(), s.getEntity().getZ() - getZ());
            playSound(SoundEvents.PLAYER_HURT, 1f, 0.9f);
            if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 1, getZ(), 12, 0.3, 0.4, 0.3, 0.02);
            return true;
        }
        return super.hurt(s, amount);
    }

    // ------------------------------------------------------------------ API vay thanh (SiegeDirector)
    public int siegeMode() { return siegeMode; }
    public boolean downed() { return downed > 0; }
    public boolean hiddenInside() { return hidden; }
    public boolean archer() { return archer; }
    public BlockPos post() { return post; }
    public void setPost(BlockPos p) { post = p; }
    public BlockPos homePos() { return home; }

    /** Linh: trang bi day du, ve vi tri (cung thu len tuong). */
    public void siegeFight(BlockPos post, boolean archer) {
        this.post = post;
        this.archer = archer;
        siegeMode = SIEGE_FIGHT;
        downed = 0;
        stuck = 0;
        gearOn = true;
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(archer ? Items.BOW : Items.DIAMOND_SWORD));
        setItemSlot(EquipmentSlot.OFFHAND, archer ? ItemStack.EMPTY : new ItemStack(Items.SHIELD));
        for (EquipmentSlot s : EquipmentSlot.values()) setDropChance(s, 0f);
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && !hp.hasModifier(SIEGE_HP)) hp.addTransientModifier(new AttributeModifier(SIEGE_HP, 20, AttributeModifier.Operation.ADD_VALUE));
        setHealth(getMaxHealth());
        restrictTo(post, 64);
        playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1f, 0.9f + random.nextFloat() * 0.2f);
    }

    /** Truong lao: ao vang + bua chu, dung giu vi tri, ban phu quang. */
    public void siegeElder(BlockPos post) {
        siegeFight(post, true);
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.PAPER));
    }

    /** Dan thuong: chay vao nha (cua) roi an. door co the null (chay tan loan roi an). */
    public void siegeShelter(BlockPos door) {
        if (siegeMode != SIEGE_NONE) return;
        shelter = door;
        siegeMode = SIEGE_SHELTER;
        stuck = 0;
    }

    public void siegeCheer() {
        if (siegeMode != SIEGE_FIGHT) return;
        siegeMode = SIEGE_CHEER;
        downed = 0;
        setShiftKeyDown(false);
        getNavigation().stop();
    }

    /** Tro lai binh thuong: hien hinh, bo trang bi, bo gioi han di chuyen. */
    public void siegeEnd() {
        boolean wasHidden = hidden;
        siegeMode = SIEGE_NONE;
        downed = 0;
        archer = false;
        post = null;
        shelter = null;
        setHidden(false);
        setShiftKeyDown(false);
        setTarget(null);
        getNavigation().stop();
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.removeModifier(SIEGE_HP);
        if (gearOn) {
            gearOn = false;
            for (EquipmentSlot s : EquipmentSlot.values()) setItemSlot(s, ItemStack.EMPTY);
            dress(role());
        }
        restrictTo(home, radius);
        if (wasHidden && level() instanceof ServerLevel sl) SiegeFx.puff(sl, position().add(0, 1, 0));
    }

    private void setHidden(boolean h) {
        hidden = h;
        setInvisible(h);
        setSilent(h);
        noPhysics = h;
        setNoGravity(h);
        if (h) setDeltaMovement(0, 0, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (downed > 0) {
            if (--downed == 0) {
                setShiftKeyDown(false);
                setHealth(getMaxHealth());
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.4, getZ(), 4, 0.3, 0.3, 0.3, 0);
            } else if (downed % 20 == 0 && level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.6, getZ(), 3, 0.25, 0.1, 0.25, 0.01);
            }
        }
        // an toan: het vay thanh ma van o trang thai vay thanh (vd tai nap the gioi) -> tro lai binh thuong
        if ((siegeMode != SIEGE_NONE || hidden || gearOn) && tickCount % 40 == 0 && !SiegeFx.sieging()) siegeEnd();
    }

    /** Khoa muc tieu Hu Hoa gan nhat (giu muc tieu hien tai neu con gan). */
    private void acquire() {
        LivingEntity cur = getTarget();
        if (cur != null && cur.isAlive() && distanceToSqr(cur) < 20 * 20 && random.nextInt(4) != 0) return;
        double r = archer ? 30 : 24;
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r, 10, r), e -> e instanceof HuMob && e.isAlive())) {
            double d = distanceToSqr(e);
            if (d < bd) { bd = d; best = e; }
        }
        setTarget(best);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("Role", role());
        t.putInt("Skin", skin());
        t.putIntArray("Home", new int[]{home.getX(), home.getY(), home.getZ()});
        t.putInt("Radius", radius);
        if (hidden || gearOn) t.putBoolean("SiegeState", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        int[] h = t.getIntArray("Home");
        if (h.length == 3) home = new BlockPos(h[0], h[1], h[2]);
        radius = t.contains("Radius") ? t.getInt("Radius") : 40;
        entityData.set(ROLE, t.getInt("Role"));
        entityData.set(SKIN, t.getInt("Skin"));
        restrictTo(home, radius);
        addTag(TAG);
        if (role() == CHILD) { var sc = getAttribute(Attributes.SCALE); if (sc != null) sc.setBaseValue(0.6); }
        if (t.getBoolean("SiegeState")) { gearOn = true; siegeEnd(); } // luu giua tran vay thanh: dua ve binh thuong
    }

    // ------------------------------------------------------------------ goals

    /** Di dao ngau nhien trong ranh gioi; linh canh di xa hon (tuan tra), tre em chay nhanh. */
    private static final class Wander extends Goal {
        private final AmbientNpc m;
        private int cooldown;
        Wander(AmbientNpc m) { this.m = m; setFlags(EnumSet.of(Flag.MOVE)); }

        @Override public boolean canUse() {
            if (m.meditating() || m.role() == DISCIPLE && m.random.nextInt(3) != 0) return false;
            if (--cooldown > 0) return false;
            cooldown = 20 + m.random.nextInt(60);
            return m.getNavigation().isDone();
        }
        @Override public boolean canContinueToUse() { return !m.getNavigation().isDone(); }
        @Override public void start() {
            int range = m.role() == GUARD ? 24 : m.role() == CHILD ? 8 : 14;
            Vec3 v = DefaultRandomPos.getPos(m, range, 4);
            if (v != null) m.getNavigation().moveTo(v.x, v.y, v.z, m.role() == CHILD ? 1.15 : m.role() == GUARD ? 0.85 : 0.75);
        }
    }

    /** Khuan vac / tho cang / linh: di giua hai diem cach nhau 20-40 o, nghi ngan o moi dau. */
    private static final class Shuttle extends Goal {
        private final AmbientNpc m;
        private BlockPos a, b;
        private boolean toB = true;
        private int pause;
        Shuttle(AmbientNpc m) { this.m = m; setFlags(EnumSet.of(Flag.MOVE)); }

        @Override public boolean canUse() {
            int r = m.role();
            if (r != COURIER && r != DOCK && r != GUARD) return false;
            if (r == GUARD && m.random.nextInt(4) != 0) return false; // linh: xen ke di dao
            return true;
        }
        @Override public boolean canContinueToUse() { return true; }
        @Override public void start() {
            a = m.blockPosition();
            Vec3 v = null;
            for (int i = 0; i < 6 && v == null; i++) v = DefaultRandomPos.getPos(m, m.role() == GUARD ? 40 : 30, 4);
            b = v == null ? a : BlockPos.containing(v);
            toB = true;
        }
        @Override public void tick() {
            if (pause > 0) { pause--; return; }
            BlockPos t = toB ? b : a;
            if (m.getNavigation().isDone()) {
                if (m.blockPosition().distSqr(t) <= 9) { toB = !toB; pause = 60 + m.random.nextInt(60); m.getNavigation().stop(); return; }
                m.getNavigation().moveTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5, 0.7);
                if (m.getNavigation().isDone()) { toB = !toB; pause = 40; } // khong toi duoc -> doi huong
            }
        }
    }

    /** De tu: luc ngoi thien (ngoi/khom, dung yen), luc luyen kiem (vung tay lien tuc). */
    private static final class Practice extends Goal {
        private final AmbientNpc m;
        private int left;
        private boolean sit;
        Practice(AmbientNpc m) { this.m = m; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() { return m.role() == DISCIPLE && m.random.nextInt(60) == 0; }
        @Override public boolean canContinueToUse() { return left > 0; }
        @Override public void start() {
            left = 200 + m.random.nextInt(300);
            sit = m.random.nextBoolean();
            m.getNavigation().stop();
            if (sit) { m.setShiftKeyDown(true); m.entityData.set(MEDITATING, true); }
        }
        @Override public void tick() {
            left--;
            if (!sit && left % 14 == 0) m.swing(InteractionHand.MAIN_HAND);
            if (!sit && left % 40 == 0) m.setYRot(m.getYRot() + 45f);
        }
        @Override public void stop() {
            m.setShiftKeyDown(false);
            m.entityData.set(MEDITATING, false);
        }
    }

    // ------------------------------------------------------------------ goals vay thanh

    /** Bi ha guc: nam yen (cui nguoi) cho hoi phuc. */
    private static final class Downed extends Goal {
        private final AmbientNpc m;
        Downed(AmbientNpc m) { this.m = m; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean canUse() { return m.downed > 0; }
        @Override public boolean canContinueToUse() { return m.downed > 0; }
        @Override public void start() { m.getNavigation().stop(); }
    }

    /** Dang an trong nha: khong lam gi. */
    private static final class Hidden extends Goal {
        private final AmbientNpc m;
        Hidden(AmbientNpc m) { this.m = m; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean canUse() { return m.hidden; }
        @Override public boolean canContinueToUse() { return m.hidden; }
    }

    /** Hoang loan chay ve cua nha gan nhat (hoac tan vao hem neu khong co cua), roi an. */
    private static final class Shelter extends Goal {
        private final AmbientNpc m;
        private int t;
        Shelter(AmbientNpc m) { this.m = m; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { return m.siegeMode == SIEGE_SHELTER && !m.hidden; }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void start() { t = 0; }
        @Override public void tick() {
            t++;
            double sp = m.role() == CHILD ? 1.5 : 1.35;
            if (m.shelter != null) {
                if (t % 10 == 1 || m.getNavigation().isDone()) m.getNavigation().moveTo(m.shelter.getX() + 0.5, m.shelter.getY(), m.shelter.getZ() + 0.5, sp);
                if (m.distanceToSqr(m.shelter.getX() + 0.5, m.shelter.getY(), m.shelter.getZ() + 0.5) < 4.5) { hide(); return; }
            } else if (m.getNavigation().isDone()) {
                Vec3 v = DefaultRandomPos.getPos(m, 10, 3);
                if (v != null) m.getNavigation().moveTo(v.x, v.y, v.z, sp);
            }
            if (t % 8 == 0 && m.level() instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.SPLASH, m.getX(), m.getEyeY() + 0.3, m.getZ(), 2, 0.2, 0.05, 0.2, 0);
            if (t > (m.shelter != null ? 360 : 90)) hide();
        }
        private void hide() {
            if (m.level() instanceof ServerLevel sl) SiegeFx.puff(sl, m.position().add(0, 1, 0));
            if (m.shelter != null) m.setPos(m.shelter.getX() + 0.5, m.shelter.getY(), m.shelter.getZ() + 0.5);
            m.getNavigation().stop();
            m.setHidden(true);
        }
    }

    /** Linh/cung thu/truong lao: ve vi tri, khoa muc tieu Hu Hoa gan nhat, can chien hoac ban phu quang. */
    /**
     * Tự vệ ngoài công thành: lính canh + đệ tử tự đánh quái Hư Không lọt vào khu (đột kích đêm, quái lạc vào),
     * không cần SiegeDirector ra lệnh. Không đuổi quá xa nhà (bán kính khu + 24).
     */
    private static final class Defend extends Goal {
        private final AmbientNpc m;
        private int cd, t, scan;
        Defend(AmbientNpc m) { this.m = m; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        private boolean fighter() { return m.role() == GUARD || m.role() == DISCIPLE; }
        private boolean nearHome(LivingEntity e) {
            double lim = m.radius + 24.0;
            return e.distanceToSqr(Vec3.atCenterOf(m.home)) < lim * lim;
        }
        @Override public boolean canUse() {
            if (m.siegeMode != SIEGE_NONE || m.downed > 0 || !fighter() || m.hidden) return false;
            if (++scan % 10 != 0) return false;
            m.acquire();
            LivingEntity tg = m.getTarget();
            return tg != null && tg.isAlive() && m.distanceToSqr(tg) < 18 * 18 && nearHome(tg);
        }
        @Override public boolean canContinueToUse() {
            LivingEntity tg = m.getTarget();
            return m.siegeMode == SIEGE_NONE && m.downed <= 0 && tg != null && tg.isAlive() && nearHome(tg) && m.distanceToSqr(tg) < 28 * 28;
        }
        @Override public void stop() { m.setTarget(null); m.getNavigation().stop(); }
        @Override public void tick() {
            t++;
            if (cd > 0) cd--;
            if (t % 20 == 0) m.acquire();
            LivingEntity tg = m.getTarget();
            if (tg == null) return;
            m.getLookControl().setLookAt(tg, 40f, 40f);
            double d2 = m.distanceToSqr(tg);
            if (d2 > 3.2 * 3.2) { if (t % 5 == 0 || m.getNavigation().isDone()) m.getNavigation().moveTo(tg, 1.2); }
            else if (cd <= 0) { cd = 18; m.swing(InteractionHand.MAIN_HAND); guardHit(m, tg); }
        }
    }

    /** Đòn của lính/đệ tử: 5 + 6% máu tối đa mục tiêu (để còn ý nghĩa với quái cấp cao; boss chỉ 1/3). */
    static void guardHit(AmbientNpc m, LivingEntity tg) {
        double pct = com.hukhong.events.BossBuff.isBoss(tg) ? 0.02 : 0.06;
        tg.hurt(m.damageSources().mobAttack(m), (float) (5 + tg.getMaxHealth() * pct));
    }

    private static final class SiegeCombat extends Goal {
        private final AmbientNpc m;
        private int cd, t;
        SiegeCombat(AmbientNpc m) { this.m = m; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { return m.siegeMode == SIEGE_FIGHT && m.downed <= 0; }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void tick() {
            t++;
            if (t % 10 == 0) m.acquire();
            LivingEntity tg = m.getTarget();
            if (tg != null && !tg.isAlive()) { m.setTarget(null); tg = null; }
            BlockPos p = m.post;
            if (cd > 0) cd--;
            if (tg != null) {
                m.getLookControl().setLookAt(tg, 40f, 40f);
                double d2 = m.distanceToSqr(tg);
                if (m.archer) {
                    if (p != null && m.distanceToSqr(p.getX() + 0.5, p.getY(), p.getZ() + 0.5) > 25 && t % 20 == 0) goPost(p);
                    if (cd <= 0 && d2 < 30 * 30 && m.hasLineOfSight(tg)) {
                        cd = 30 + m.random.nextInt(14);
                        m.swing(InteractionHand.MAIN_HAND);
                        SiegeFx.spiritArrow(m, tg, 6f + m.random.nextInt(3));
                    }
                } else {
                    if (d2 > 3.2 * 3.2) { if (t % 5 == 0 || m.getNavigation().isDone()) m.getNavigation().moveTo(tg, 1.15); }
                    else if (cd <= 0) { cd = 18; m.swing(InteractionHand.MAIN_HAND); guardHit(m, tg); }
                }
                return;
            }
            // khong co dich: giu vi tri hang
            if (p == null) return;
            double d = m.distanceToSqr(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            if (d > 2.5 * 2.5) {
                if (t % 20 == 0 || m.getNavigation().isDone()) goPost(p);
                if (m.getNavigation().isDone() && ++m.stuck > 4 && d > 4) { // ket (thang/tuong): dich chuyen len vi tri
                    if (m.level() instanceof ServerLevel sl) SiegeFx.puff(sl, m.position().add(0, 1, 0));
                    m.teleportTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                    m.stuck = 0;
                }
            } else {
                m.getNavigation().stop();
                Vec3 out = Vec3.atCenterOf(p).subtract(Vec3.atCenterOf(m.home)).multiply(1, 0, 1);
                if (out.lengthSqr() > 1e-4) out = out.normalize().scale(6);
                m.getLookControl().setLookAt(p.getX() + 0.5 + out.x, p.getY() + 1.5, p.getZ() + 0.5 + out.z);
            }
        }
        private void goPost(BlockPos p) { m.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 1.1); }
    }

    /** Mung chien thang: nhay mua, vung vu khi. */
    private static final class Cheer extends Goal {
        private final AmbientNpc m;
        private int t;
        Cheer(AmbientNpc m) { this.m = m; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean canUse() { return m.siegeMode == SIEGE_CHEER; }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void tick() {
            t++;
            if (t % 14 == 0) m.swing(InteractionHand.MAIN_HAND);
            if (t % 24 == 0 && m.onGround()) m.setDeltaMovement(m.getDeltaMovement().add(0, 0.38, 0));
            if (t % 30 == 0 && m.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, m.getX(), m.getY() + 2, m.getZ(), 3, 0.3, 0.2, 0.3, 0);
        }
    }
}
