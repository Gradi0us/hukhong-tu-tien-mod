package com.hukhong.city.ambient;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
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
 */
public class AmbientNpc extends PathfinderMob {
    public static final int CITIZEN = 0, GUARD = 1, COURIER = 2, DOCK = 3, DISCIPLE = 4, CHILD = 5;
    public static final String TAG = "hukhong_ambient";
    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(AmbientNpc.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(AmbientNpc.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MEDITATING = SynchedEntityData.defineId(AmbientNpc.class, EntityDataSerializers.BOOLEAN);

    private BlockPos home = BlockPos.ZERO;
    private int radius = 40;
    private BlockPos waypoint;

    public AmbientNpc(EntityType<? extends AmbientNpc> t, Level l) {
        super(t, l);
        addTag(TAG);
    }

    public static AttributeSupplier.Builder attrs() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.FOLLOW_RANGE, 48);
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
        goalSelector.addGoal(1, new Practice(this));
        goalSelector.addGoal(2, new Shuttle(this));
        goalSelector.addGoal(3, new Wander(this));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6f));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    // ---- bat tu voi moi thu tru bypass (vd /kill, void) + khong day ra khoi ranh gioi bang knockback ----
    @Override public boolean isInvulnerableTo(DamageSource s) { return !s.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY); }
    @Override public boolean removeWhenFarAway(double d) { return true; }
    @Override public boolean canBeLeashed() { return false; }
    @Override public boolean shouldShowName() { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("Role", role());
        t.putInt("Skin", skin());
        t.putIntArray("Home", new int[]{home.getX(), home.getY(), home.getZ()});
        t.putInt("Radius", radius);
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
}
