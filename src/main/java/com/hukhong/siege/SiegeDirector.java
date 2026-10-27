package com.hukhong.siege;

import com.hukhong.HuKhong;
import com.hukhong.city.CityBuilder;
import com.hukhong.city.CityMarket;
import com.hukhong.city.WorldBuildData;
import com.hukhong.city.ambient.AmbientNpc;
import com.hukhong.city.ambient.AmbientRegistry;
import com.hukhong.entity.HuMob;
import com.hukhong.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Dao dien man "thanh bi vay": chuong bao dong -> linh mac giap / len tuong / chan cong -> dan chay vao nha, thuong nhan dong quay
 * -> chao lua + cot sang tren tuong -> khe nut Hu Khong mo ngoai tuong, quai tro ra -> thien thach xe troi, khoi lua tai cho trung
 * -> chien thang: khe nut khep, linh reo ho, phao hoa tren quang truong, dan cu tro ve.
 * Cung dung cho bao ve tong mon khi Hu Khong Dot Kich do bo gan tong (de tu tran kiem + truong lao dung ket gioi phu chu).
 *
 * Tat ca la hieu ung thuan server (hat/am thanh vanilla), khong pha/khong dat block; ban chat chien dau do {@link AmbientNpc}.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class SiegeDirector {
    private SiegeDirector() {}

    public static final int ALARM_TICKS = 120;
    public static final int RIFT_OPEN_TICKS = 50;
    private static final String HIDDEN_TAG = "hukhong_siege_hidden";
    private static final int MIN_GUARDS = 10;
    private static final int TIMEOUT = 13000;

    private enum Phase { ALARM, BATTLE, VICTORY }

    // ------------------------------------------------------------------ lich tac vu nho
    private record Task(long due, Runnable run) {}
    private static final List<Task> TASKS = new ArrayList<>();
    private static long clock;

    /** Chay {@code r} sau {@code delay} tick (>=1). */
    public static void schedule(ServerLevel l, int delay, Runnable r) { TASKS.add(new Task(clock + Math.max(1, delay), r)); }

    // ------------------------------------------------------------------ trang thai
    private static final class Siege {
        final ServerLevel level;
        final BlockPos center;
        final int half;
        final boolean auto;
        Phase phase = Phase.ALARM;
        int age, phaseAge, wave, nextMeteor = ALARM_TICKS + 140;
        boolean lit = true;
        final List<AmbientNpc> guards = new ArrayList<>(), citizens = new ArrayList<>(), extras = new ArrayList<>();
        final List<WanderingTrader> traders = new ArrayList<>();
        final List<Rift> rifts = new ArrayList<>();
        final List<Vec3> braziers = new ArrayList<>(), beacons = new ArrayList<>();
        final List<BlockPos> doors = new ArrayList<>();
        // che do tu sinh quai (lenh thu thanh khac / demo)
        final List<Mob> autoMobs = new ArrayList<>();
        int autoToSpawn, autoWave;

        Siege(ServerLevel l, BlockPos c, int half, boolean auto) { level = l; center = c; this.half = half; this.auto = auto; }
    }

    private static final class Meteor {
        final ServerLevel l;
        final Vec3 from, to;
        int age;
        static final int LIFE = 34;
        Meteor(ServerLevel l, Vec3 from, Vec3 to) { this.l = l; this.from = from; this.to = to; }
    }

    private static final class Smoulder {
        final ServerLevel l;
        final Vec3 p;
        int age;
        Smoulder(ServerLevel l, Vec3 p) { this.l = l; this.p = p; }
    }

    private static final class SectDef {
        final ServerLevel l;
        final UUID owner;
        final Vec3 c;
        final double domeR;
        int age, endAge = -1;
        final List<AmbientNpc> disciples = new ArrayList<>(), elders = new ArrayList<>(), extras = new ArrayList<>(), others = new ArrayList<>();
        SectDef(ServerLevel l, UUID owner, Vec3 c, double r) { this.l = l; this.owner = owner; this.c = c; domeR = r; }
    }

    private static Siege cur;
    private static final List<Rift> RIFTS = new ArrayList<>();
    private static final List<Meteor> METEORS = new ArrayList<>();
    private static final List<Smoulder> SMOULDERS = new ArrayList<>();
    private static final List<SectDef> SECTS = new ArrayList<>();

    public static boolean active() { return cur != null && cur.phase != Phase.VICTORY; }
    public static boolean inVictory() { return cur != null && cur.phase == Phase.VICTORY; }
    /** Co bat ky man vay thanh / bao ve tong nao dang chay. */
    public static boolean running() { return cur != null || !SECTS.isEmpty(); }

    // ================================================================== BAT DAU
    public static void begin(ServerLevel l, BlockPos center) { begin(l, center, CityBuilder.HALF, true, false); }

    /**
     * @param half      nua canh tuong thanh (CityBuilder.HALF)
     * @param reinforce bo sung linh cho du quan so neu thanh chua sinh du
     * @param auto      Director tu sinh quai qua khe nut va tu ket thuc (khong dung InvasionManager)
     */
    public static void begin(ServerLevel l, BlockPos center, int half, boolean reinforce, boolean auto) {
        if (cur != null) abort();
        Siege s = new Siege(l, center, half, auto);
        cur = s;
        scanDoors(s);
        Vec3 cv = Vec3.atCenterOf(center);

        // vi tri chao lua + cot sang
        for (Direction d : Direction.Plane.HORIZONTAL) {
            Direction al = d.getClockWise();
            for (int sg = -1; sg <= 1; sg += 2) {
                s.braziers.add(surface(l, center.getX() + d.getStepX() * half + al.getStepX() * 4 * sg,
                        center.getZ() + d.getStepZ() * half + al.getStepZ() * 4 * sg, center.getY()));
                s.braziers.add(surface(l, center.getX() + d.getStepX() * (half - 2) + al.getStepX() * (half - 2) * sg,
                        center.getZ() + d.getStepZ() * (half - 2) + al.getStepZ() * (half - 2) * sg, center.getY()));
            }
        }
        for (int i = 0; i < s.braziers.size(); i++) if (i % 4 == 1) s.beacons.add(s.braziers.get(i));

        // dan cu
        AABB box = new AABB(center).inflate(half + 30, 30, half + 30);
        for (AmbientNpc n : l.getEntitiesOfClass(AmbientNpc.class, box)) {
            if (n.siegeMode() != AmbientNpc.SIEGE_NONE) continue;
            if (n.role() == AmbientNpc.GUARD) s.guards.add(n); else s.citizens.add(n);
        }
        if (reinforce) {
            for (int i = 0; s.guards.size() < MIN_GUARDS && i < MIN_GUARDS; i++) {
                AmbientNpc g = spawnNpc(l, AmbientNpc.GUARD, gatePost(l, center, half, i + 2), center, half + 30);
                if (g == null) break;
                s.guards.add(g);
                s.extras.add(g);
            }
        }
        for (WanderingTrader t : l.getEntitiesOfClass(WanderingTrader.class, box, t -> t.getTags().contains(CityMarket.TAG))) s.traders.add(t);

        // ---- bao dong: chuong 5 hoi, giam dan cao do ----
        for (int i = 0; i < 5; i++) {
            final int k = i;
            schedule(l, 1 + i * 26, () -> {
                if (cur != s) return;
                float pitch = 0.85f - k * 0.06f;
                SiegeFx.bell(l, cv.add(0, 6, 0), pitch);
                for (Direction d : Direction.Plane.HORIZONTAL)
                    SiegeFx.bell(l, cv.add(d.getStepX() * (half - 2), 5, d.getStepZ() * (half - 2)), pitch + 0.15f);
            });
        }
        // phao hieu do tren 4 goc tuong
        for (int i = 0; i < 4; i++) {
            final Vec3 b = s.braziers.get(i * 4 + 1);
            schedule(l, 4 + i * 6, () -> {
                if (cur == s) SiegeFx.rocket(l, b.add(0, 1, 0), 0xFF3B2A, 0xFFB347, FireworkExplosion.Shape.SMALL_BALL, 2);
            });
        }
        msg(s, Component.translatable("hukhong.siege.alarm"));

        // linh: mac giap lech nhau de khong dong bo may moc; 1/3 la cung thu tren tuong
        int archerIdx = 0, meleeIdx = 0;
        for (int i = 0; i < s.guards.size(); i++) {
            final AmbientNpc g = s.guards.get(i);
            final boolean archer = i % 3 == 2;
            final BlockPos post = archer ? wallPost(l, center, half, archerIdx++) : gatePost(l, center, half, meleeIdx++);
            schedule(l, 4 + l.random.nextInt(36), () -> {
                if (cur != s || !g.isAlive() || g.isRemoved()) return;
                g.siegeFight(post, archer);
                SiegeFx.puff(l, g.position().add(0, 1, 0));
            });
        }
        // dan thuong / tre em: hoang loan lan dan
        for (AmbientNpc c : s.citizens) {
            BlockPos door = nearestDoor(s, c.blockPosition());
            schedule(l, 10 + l.random.nextInt(60), () -> {
                if (cur != s || !c.isAlive() || c.isRemoved()) return;
                c.siegeShelter(door);
            });
        }
        // thuong nhan don hang, dong quay
        for (WanderingTrader t : s.traders) {
            schedule(l, 20 + l.random.nextInt(50), () -> {
                if (cur != s || t.isRemoved()) return;
                t.addTag(HIDDEN_TAG);
                t.setInvisible(true);
                t.setCustomNameVisible(false);
                SiegeFx.puff(l, t.position().add(0, 1, 0));
                l.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.NEUTRAL, 1f, 0.8f);
            });
        }
        if (auto) { s.autoWave = 0; s.autoToSpawn = 0; }
    }

    // ================================================================== QUA TRINH
    /** InvasionManager goi khi dot quai moi bat dau (dot 1 do Director tu mo khi het bao dong). */
    public static void waveStart(int wave) {
        Siege s = cur;
        if (s == null || s.phase != Phase.BATTLE) return;
        openWaveRifts(s, wave);
    }

    private static void openWaveRifts(Siege s, int wave) {
        for (Rift r : s.rifts) r.close();
        s.rifts.clear();
        s.wave = wave;
        ServerLevel l = s.level;
        int n = wave >= 3 ? 3 : 2;
        double base = l.random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < n; i++) {
            double a = base + i * Math.PI * 2 / n + (l.random.nextDouble() - 0.5) * 0.4;
            double d = s.half + 8;
            int x = s.center.getX() + (int) Math.round(Math.cos(a) * d), z = s.center.getZ() + (int) Math.round(Math.sin(a) * d);
            if (!l.hasChunk(x >> 4, z >> 4)) continue;
            Vec3 g = surface(l, x, z, s.center.getY());
            if (!l.getFluidState(BlockPos.containing(g).below()).isEmpty()) continue;
            Rift r = new Rift(l, g, new Vec3(s.center.getX() - x, 0, s.center.getZ() - z), 1.7 + wave * 0.15);
            RIFTS.add(r);
            s.rifts.add(r);
        }
        msg(s, Component.translatable("hukhong.siege.rift"));
        // phao hieu chu y huong dich
        for (Rift r : s.rifts) {
            Vec3 p = r.base.add(0, 1, 0);
            schedule(l, 15, () -> { if (cur == s) SiegeFx.rocket(l, p, 0xB36BFF, 0x5A2BC9, FireworkExplosion.Shape.STAR, 1); });
        }
    }

    /** Diem quai tro ra tu khe nut dang san sang; null = chua co khe nao san sang (cho). */
    public static Vec3 riftSpawnPoint() {
        Siege s = cur;
        if (s == null) return null;
        List<Rift> ready = new ArrayList<>();
        for (Rift r : s.rifts) if (r.ready()) ready.add(r);
        if (ready.isEmpty()) return null;
        return ready.get(s.level.random.nextInt(ready.size())).spawnPoint();
    }

    /** Quai vua nhao ra khoi khe nut. */
    public static void emerge(ServerLevel l, Vec3 p) {
        l.sendParticles(ParticleTypes.PORTAL, p.x, p.y + 1, p.z, 24, 0.4, 0.8, 0.4, 0.8);
        l.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y + 0.6, p.z, 10, 0.3, 0.5, 0.3, 0.15);
        l.sendParticles(SiegeFx.VOID_DUST, p.x, p.y + 0.2, p.z, 6, 0.5, 0.05, 0.5, 0);
        l.playSound(null, p.x, p.y, p.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.5f, 0.55f + l.random.nextFloat() * 0.2f);
    }

    private static void tickSiege(Siege s) {
        ServerLevel l = s.level;
        s.age++;
        s.phaseAge++;
        if (s.age > TIMEOUT) { abort(); return; }
        Vec3 cv = Vec3.atCenterOf(s.center);

        // chao lua + cot sang tren tuong (chi hat)
        if (s.lit) {
            if (s.age % 4 == 0) for (Vec3 b : s.braziers) l.sendParticles(ParticleTypes.FLAME, b.x, b.y + 1.1, b.z, 2, 0.1, 0.1, 0.1, 0.01);
            if (s.age % 12 == 0) for (Vec3 b : s.braziers) l.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, b.x, b.y + 1.3, b.z, 0, 0, 1, 0, 0.05);
            if (s.age % 6 == 0) for (Vec3 b : s.beacons) SiegeFx.beam(l, b.add(0, 1, 0), 26, s.phase == Phase.ALARM ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME);
        }

        switch (s.phase) {
            case ALARM -> {
                if (s.phaseAge >= ALARM_TICKS) {
                    s.phase = Phase.BATTLE;
                    s.phaseAge = 0;
                    openWaveRifts(s, 1);
                    if (s.auto) { s.autoWave = 1; s.autoToSpawn = 5; }
                }
            }
            case BATTLE -> {
                if (s.age % 10 == 0) assistTargets(s);
                if (s.age >= s.nextMeteor && METEORS.size() < 2) {
                    s.nextMeteor = s.age + 160 + l.random.nextInt(110);
                    launchMeteor(s);
                }
                if (s.auto) tickAuto(s);
            }
            case VICTORY -> tickVictory(s, cv);
        }
    }

    /** Quai gan linh thi quay sang danh linh (khong bo qua nguoi choi han). */
    private static void assistTargets(Siege s) {
        AABB box = new AABB(s.center).inflate(s.half + 40, 30, s.half + 40);
        for (Mob m : s.level.getEntitiesOfClass(Mob.class, box, e -> e instanceof HuMob && e.isAlive())) {
            LivingEntity t = m.getTarget();
            if (t instanceof AmbientNpc && t.isAlive()) continue;
            if (t != null && s.level.random.nextInt(3) != 0) continue;
            AmbientNpc best = null;
            double bd = 9 * 9;
            for (AmbientNpc g : s.guards) {
                if (!g.isAlive() || g.downed() || g.siegeMode() != AmbientNpc.SIEGE_FIGHT) continue;
                double d = m.distanceToSqr(g);
                if (d < bd) { bd = d; best = g; }
            }
            if (best != null) m.setTarget(best);
        }
    }

    private static void tickAuto(Siege s) {
        s.autoMobs.removeIf(m -> m.isRemoved() || !m.isAlive());
        ServerLevel l = s.level;
        if (s.autoToSpawn > 0) {
            if (s.age % 12 == 0 && s.autoMobs.size() < 14) {
                Vec3 p = riftSpawnPoint();
                if (p != null) {
                    Mob m = (l.random.nextInt(3) == 0 ? ModEntities.HU_COT.get() : ModEntities.HU_THI.get()).create(l);
                    if (m != null) {
                        m.moveTo(p.x, p.y, p.z, l.random.nextFloat() * 360f, 0f);
                        m.finalizeSpawn(l, l.getCurrentDifficultyAt(BlockPos.containing(p)), MobSpawnType.EVENT, null);
                        m.setPersistenceRequired();
                        l.addFreshEntity(m);
                        s.autoMobs.add(m);
                        s.autoToSpawn--;
                        emerge(l, p);
                    }
                }
            }
        } else if (s.autoMobs.isEmpty()) {
            if (s.autoWave >= 2) { victory(); return; }
            s.autoWave++;
            s.autoToSpawn = 7;
            openWaveRifts(s, s.autoWave);
        }
        if (s.age % 20 == 0)
            for (Mob m : s.autoMobs) if (m.getTarget() == null) m.getNavigation().moveTo(s.center.getX() + 0.5, s.center.getY(), s.center.getZ() + 0.5, 1.0);
    }

    // ------------------------------------------------------------------ thien thach
    private static void launchMeteor(Siege s) {
        ServerLevel l = s.level;
        double ang = l.random.nextDouble() * Math.PI * 2;
        double rd = s.half + 1 + l.random.nextDouble() * 4;
        int x = s.center.getX() + (int) Math.round(Math.cos(ang) * rd), z = s.center.getZ() + (int) Math.round(Math.sin(ang) * rd);
        if (!l.hasChunk(x >> 4, z >> 4)) return;
        Vec3 to = surface(l, x, z, s.center.getY());
        double back = ang + (l.random.nextDouble() - 0.5) * 0.8;
        Vec3 from = to.add(Math.cos(back) * 26, 44, Math.sin(back) * 26);
        METEORS.add(new Meteor(l, from, to));
        SiegeFx.sound(l, to.add(0, 14, 0), SoundEvents.FIRECHARGE_USE, 3f, 0.5f);
        SiegeFx.sound(l, to.add(0, 14, 0), SoundEvents.GHAST_SHOOT, 1.5f, 0.45f);
    }

    private static boolean tickMeteor(Meteor m) {
        ServerLevel l = m.l;
        m.age++;
        double f = Math.pow(m.age / (double) Meteor.LIFE, 1.5);
        Vec3 p = m.from.lerp(m.to, Math.min(1, f));
        Vec3 pb = m.from.lerp(m.to, Math.max(0, Math.min(1, Math.pow((m.age - 1) / (double) Meteor.LIFE, 1.5))));
        l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 6, 0.25, 0.25, 0.25, 0.02);
        l.sendParticles(ParticleTypes.PORTAL, p.x, p.y, p.z, 8, 0.3, 0.3, 0.3, 0.4);
        for (int i = 0; i < 3; i++) {
            Vec3 q = pb.lerp(p, i / 3.0);
            l.sendParticles(ParticleTypes.LARGE_SMOKE, q.x, q.y, q.z, 1, 0.1, 0.1, 0.1, 0.01);
        }
        l.sendParticles(SiegeFx.VOID_DUST, pb.x, pb.y, pb.z, 3, 0.2, 0.2, 0.2, 0);
        if (m.age < Meteor.LIFE) return false;
        // va cham: chi hieu ung, khong pha block
        Vec3 t = m.to;
        l.sendParticles(ParticleTypes.EXPLOSION, t.x, t.y + 0.5, t.z, 1, 0, 0, 0, 0);
        l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, t.x, t.y + 0.4, t.z, 40, 0.2, 0.2, 0.2, 0.28);
        l.sendParticles(ParticleTypes.LARGE_SMOKE, t.x, t.y + 0.6, t.z, 20, 0.5, 0.4, 0.5, 0.05);
        l.sendParticles(ParticleTypes.ELECTRIC_SPARK, t.x, t.y + 0.5, t.z, 24, 0.3, 0.3, 0.3, 0.5);
        SiegeFx.sound(l, t, SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.7f);
        SiegeFx.sound(l, t, SoundEvents.AMETHYST_BLOCK_BREAK, 2f, 0.5f);
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(t, t).inflate(3.5), e -> e instanceof HuMob))
            e.hurt(l.damageSources().magic(), 8f);
        SMOULDERS.add(new Smoulder(l, t));
        waveRing(l, t);
        return true;
    }

    private static void waveRing(ServerLevel l, Vec3 t) {
        for (int k = 1; k <= 6; k++) {
            final int kk = k;
            schedule(l, k, () -> SiegeFx.shockwave(l, t, 4.5, kk, 6, ParticleTypes.CLOUD));
        }
    }

    private static boolean tickSmoulder(Smoulder sm) {
        sm.age++;
        ServerLevel l = sm.l;
        var r = l.random;
        if (sm.age % 3 == 0) l.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, sm.p.x + (r.nextDouble() - 0.5) * 1.6, sm.p.y + 0.2,
                sm.p.z + (r.nextDouble() - 0.5) * 1.6, 0, 0, 1, 0, 0.06);
        if (sm.age % 10 == 0) {
            l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, sm.p.x, sm.p.y + 0.2, sm.p.z, 2, 0.6, 0.05, 0.6, 0.01);
            l.sendParticles(ParticleTypes.LARGE_SMOKE, sm.p.x, sm.p.y + 0.5, sm.p.z, 1, 0.4, 0.2, 0.4, 0.02);
        }
        return sm.age >= 120;
    }

    // ------------------------------------------------------------------ chien thang / ket thuc
    /** Chien thang: khe nut khep, linh reo ho, phao hoa, dan tro ve. */
    public static void victory() {
        Siege s = cur;
        if (s == null || s.phase == Phase.VICTORY) return;
        s.phase = Phase.VICTORY;
        s.phaseAge = 0;
        s.lit = false;
        ServerLevel l = s.level;
        for (Rift r : s.rifts) r.close();
        s.rifts.clear();
        for (AmbientNpc g : s.guards) if (g.isAlive()) g.siegeCheer();
        for (Mob m : s.autoMobs) if (m.isAlive()) m.discard();
        s.autoMobs.clear();
        Vec3 cv = Vec3.atCenterOf(s.center);
        SiegeFx.sound(l, cv.add(0, 6, 0), SoundEvents.BELL_BLOCK, 4f, 1.2f);
        SiegeFx.sound(l, cv, SoundEvents.PLAYER_LEVELUP, 1f, 0.7f);
        msg(s, Component.translatable("hukhong.siege.victory"));
        int[][] cols = {{0xFFD54A, 0xFF8A00}, {0x6FE7FF, 0x2B7BFF}, {0xFFFFFF, 0xBFD8FF}, {0xFF5A5A, 0xFFB347}, {0xC9A8FF, 0x6D3BE0}};
        FireworkExplosion.Shape[] shapes = {FireworkExplosion.Shape.LARGE_BALL, FireworkExplosion.Shape.STAR, FireworkExplosion.Shape.BURST,
                FireworkExplosion.Shape.LARGE_BALL, FireworkExplosion.Shape.STAR};
        for (int i = 0; i < 7; i++) {
            final int k = i;
            schedule(l, 8 + i * 14, () -> {
                if (cur != s) return;
                double a = k * 2.4, d = 3 + (k % 3) * 2.5;
                Vec3 g = surface(l, s.center.getX() + (int) (Math.cos(a) * d), s.center.getZ() + (int) (Math.sin(a) * d), s.center.getY());
                int[] c = cols[k % cols.length];
                SiegeFx.rocket(l, g.add(0, 0.5, 0), c[0], c[1], shapes[k % shapes.length], 1 + k % 2);
            });
        }
    }

    private static void tickVictory(Siege s, Vec3 cv) {
        ServerLevel l = s.level;
        if (s.phaseAge == 70) {
            for (AmbientNpc c : s.citizens) schedule(l, 1 + l.random.nextInt(60), () -> { if (!c.isRemoved()) c.siegeEnd(); });
            for (WanderingTrader t : s.traders) schedule(l, 10 + l.random.nextInt(50), () -> { if (!t.isRemoved()) showTrader(t); });
        }
        if (s.phaseAge == 190) {
            for (AmbientNpc g : s.guards) if (!g.isRemoved()) g.siegeEnd();
            for (AmbientNpc g : s.extras) if (!g.isRemoved()) { SiegeFx.puff(l, g.position().add(0, 1, 0)); g.discard(); }
            cur = null;
        }
    }

    /** Ket thuc tuc thi, khong an mung (thua, huy, het gio, tat may chu). */
    public static void abort() {
        Siege s = cur;
        cur = null;
        if (s == null) return;
        restore(s);
        for (Rift r : s.rifts) r.close();
        for (Mob m : s.autoMobs) if (!m.isRemoved()) m.discard();
    }

    private static void restore(Siege s) {
        for (AmbientNpc g : s.guards) if (!g.isRemoved()) g.siegeEnd();
        for (AmbientNpc c : s.citizens) if (!c.isRemoved()) c.siegeEnd();
        for (AmbientNpc g : s.extras) if (!g.isRemoved()) g.discard();
        for (WanderingTrader t : s.traders) if (!t.isRemoved()) showTrader(t);
    }

    private static void showTrader(WanderingTrader t) {
        t.removeTag(HIDDEN_TAG);
        t.setInvisible(false);
        t.setCustomNameVisible(true);
        if (t.level() instanceof ServerLevel sl) SiegeFx.puff(sl, t.position().add(0, 1, 0));
    }

    /** Thuong nhan con "dong quay" (luu giua tran) ma khong con vay thanh -> mo lai. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof WanderingTrader t) || !t.getTags().contains(HIDDEN_TAG)) return;
        if (cur != null && cur.traders.contains(t)) return;
        t.removeTag(HIDDEN_TAG);
        t.setInvisible(false);
        t.setCustomNameVisible(true);
    }

    public static void shutdown() {
        abort();
        for (SectDef d : new ArrayList<>(SECTS)) endSect(d, true);
        SECTS.clear();
        RIFTS.clear();
        METEORS.clear();
        SMOULDERS.clear();
        TASKS.clear();
    }

    // ================================================================== HOOK HU KHONG DOT KICH
    public static Rift openRift(ServerLevel l, BlockPos ground, Vec3 facing, double radius) {
        Rift r = new Rift(l, Vec3.atBottomCenterOf(ground), facing, radius);
        RIFTS.add(r);
        return r;
    }

    /** Mini-boss xuat hien: set danh gia ao + cot linh hoa + xung kich + tieng gam. */
    public static void bossEntrance(ServerLevel l, Vec3 p) {
        LightningBolt lb = EntityType.LIGHTNING_BOLT.create(l);
        if (lb != null) { lb.moveTo(p.x, p.y, p.z); lb.setVisualOnly(true); l.addFreshEntity(lb); }
        SiegeFx.sound(l, p, SoundEvents.WITHER_SPAWN, 0.45f, 1.5f);
        SiegeFx.sound(l, p, SoundEvents.ENDER_DRAGON_GROWL, 0.8f, 0.6f);
        for (int k = 1; k <= 14; k++) {
            final int kk = k;
            schedule(l, k, () -> {
                SiegeFx.shockwave(l, p, 7.0, kk, 14, SiegeFx.VOID_DUST);
                double a = kk * 0.9;
                for (int i = 0; i < 3; i++) {
                    double aa = a + i * 2.094, y = kk * 0.35;
                    l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x + Math.cos(aa) * 1.1, p.y + y, p.z + Math.sin(aa) * 1.1, 1, 0, 0, 0, 0.01);
                }
            });
        }
    }

    /** Hieu am "nhac hieu" khi dot kich dem bat dau. */
    public static void raidSting(ServerPlayer p) {
        ServerLevel l = (ServerLevel) p.level();
        l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.4f, 0.7f);
        schedule(l, 14, () -> l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.4f, 0.7f));
        schedule(l, 40, () -> l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.2f, 0.6f));
    }

    // ================================================================== TONG MON
    public static WorldBuildData.Settlement sectNear(ServerLevel l, Vec3 pos, double margin) {
        for (var s : WorldBuildData.get(l).settlements().values())
            if ("sect".equals(s.kind()) && pos.x >= s.minX() - margin && pos.x <= s.maxX() + margin && pos.z >= s.minZ() - margin && pos.z <= s.maxZ() + margin) return s;
        return null;
    }

    /** De tu tran kiem + truong lao dung ket gioi phu chu quanh tong mon trong suot dot kich cua {@code owner}. */
    public static void sectDefense(ServerLevel l, WorldBuildData.Settlement st, UUID owner) {
        for (SectDef d : SECTS) if (d.owner.equals(owner)) return;
        Vec3 c = new Vec3(st.cx() + 0.5, st.y(), st.cz() + 0.5);
        double dr = Math.max(10, Math.min(16, Math.min(st.maxX() - st.minX(), st.maxZ() - st.minZ()) / 2.0 - 1));
        SectDef d = new SectDef(l, owner, c, dr);
        SECTS.add(d);
        BlockPos cb = BlockPos.containing(c);
        AABB box = new AABB(st.minX() - 8, st.y() - 12, st.minZ() - 8, st.maxX() + 9, st.y() + 30, st.maxZ() + 9);
        List<BlockPos> doors = new ArrayList<>();
        scanDoorsIn(l, st.minX(), st.minZ(), st.maxX(), st.maxZ(), st.y(), doors);
        for (AmbientNpc n : l.getEntitiesOfClass(AmbientNpc.class, box)) {
            if (n.siegeMode() != AmbientNpc.SIEGE_NONE) continue;
            if (n.role() == AmbientNpc.DISCIPLE || n.role() == AmbientNpc.GUARD) {
                if (d.disciples.size() < 12) d.disciples.add(n);
            } else d.others.add(n);
        }
        for (int i = 0; d.disciples.size() < 8 && i < 8; i++) {
            AmbientNpc n = spawnNpc(l, AmbientNpc.DISCIPLE, surface3(l, cb, 6 + i, i), cb, 40);
            if (n == null) break;
            d.disciples.add(n);
            d.extras.add(n);
        }
        // 2 truong lao dung gan tam, cac de tu con lai xep vong tran kiem
        int ring = 0, n = Math.max(1, d.disciples.size() - 2);
        for (int i = 0; i < d.disciples.size(); i++) {
            AmbientNpc a = d.disciples.get(i);
            final int idx = i;
            if (i < 2) {
                BlockPos post = surface3(l, cb, 2 + 0, i == 0 ? 0 : 4);
                d.elders.add(a);
                schedule(l, 5 + i * 6, () -> { if (!a.isRemoved() && a.isAlive()) { a.siegeElder(post); SiegeFx.puff(l, a.position().add(0, 1, 0)); } });
            } else {
                double ang = ring++ * Math.PI * 2 / n;
                double rr = dr * 0.6;
                BlockPos post = BlockPos.containing(surface(l, (int) Math.round(c.x + Math.cos(ang) * rr), (int) Math.round(c.z + Math.sin(ang) * rr), st.y()));
                schedule(l, 10 + idx * 3, () -> {
                    if (a.isRemoved() || !a.isAlive()) return;
                    a.siegeFight(post, false);
                    // ao de tu: bo giap sat, cam kiem sang
                    a.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    a.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                    a.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
                    a.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
                    a.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    a.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                });
            }
        }
        for (AmbientNpc o : d.others) {
            BlockPos door = nearest(doors, o.blockPosition());
            schedule(l, 10 + l.random.nextInt(50), () -> { if (!o.isRemoved() && o.isAlive()) o.siegeShelter(door); });
        }
        // vung phong ho mo ra
        SiegeFx.sound(l, c, SoundEvents.BEACON_ACTIVATE, 2f, 0.8f);
        for (int k = 1; k <= 20; k++) {
            final int kk = k;
            schedule(l, k, () -> SiegeFx.shockwave(l, c.add(0, 0.2, 0), dr, kk, 20, SiegeFx.TALISMAN_DUST));
        }
        for (ServerPlayer p : l.players()) if (p.getUUID().equals(owner)) p.displayClientMessage(Component.translatable("hukhong.siege.sect"), true);
    }

    public static void sectDefenseEnd(UUID owner) {
        for (SectDef d : SECTS) if (d.owner.equals(owner) && d.endAge < 0) d.endAge = 0;
    }

    private static void endSect(SectDef d, boolean now) {
        for (AmbientNpc n : d.disciples) if (!n.isRemoved()) n.siegeEnd();
        for (AmbientNpc n : d.others) if (!n.isRemoved()) n.siegeEnd();
        for (AmbientNpc n : d.extras) if (!n.isRemoved()) { SiegeFx.puff(d.l, n.position().add(0, 1, 0)); n.discard(); }
        if (!now) {
            SiegeFx.sound(d.l, d.c, SoundEvents.BEACON_DEACTIVATE, 2f, 0.8f);
            d.l.sendParticles(ParticleTypes.END_ROD, d.c.x, d.c.y + 2, d.c.z, 40, d.domeR * 0.5, 2, d.domeR * 0.5, 0.05);
        }
    }

    private static boolean tickSect(SectDef d) {
        ServerLevel l = d.l;
        d.age++;
        if (d.age > 16000 && d.endAge < 0) d.endAge = 0;
        double fade = 1;
        if (d.endAge >= 0) {
            d.endAge++;
            fade = Math.max(0, 1 - d.endAge / 40.0);
            if (d.endAge == 1) {
                // thang: reo ho
                for (AmbientNpc n : d.disciples) if (n.isAlive()) n.siegeCheer();
            }
            if (d.endAge >= 60) { endSect(d, false); return true; }
        }
        var r = l.random;
        // vom ket gioi phu chu: ~10 diem ngau nhien tren nua cau
        if (d.age % 3 == 0) {
            int cnt = (int) Math.round(10 * fade);
            for (int i = 0; i < cnt; i++) {
                double a = r.nextDouble() * Math.PI * 2, th = Math.acos(r.nextDouble());
                double rr = d.domeR;
                l.sendParticles(i % 3 == 0 ? ParticleTypes.ENCHANT : SiegeFx.TALISMAN_DUST,
                        d.c.x + Math.sin(th) * Math.cos(a) * rr, d.c.y + Math.cos(th) * rr * 0.9, d.c.z + Math.sin(th) * Math.sin(a) * rr, 1, 0, 0, 0, 0.3);
            }
        }
        if (d.age % 8 == 0) SiegeFx.ring(l, ParticleTypes.END_ROD, d.c.add(0, 0.15, 0), d.domeR, 28, d.age * 0.02);
        if (d.age % 4 == 0) SiegeFx.ring(l, ParticleTypes.ENCHANT, d.c.add(0, 0.2, 0), d.domeR * 0.6, 12, -d.age * 0.05);
        // duong kiem khi noi cac de tu thanh tran phap
        if (d.age % 5 == 0) {
            List<AmbientNpc> ring = new ArrayList<>();
            for (AmbientNpc n : d.disciples) if (n.isAlive() && !n.downed() && !d.elders.contains(n) && n.siegeMode() == AmbientNpc.SIEGE_FIGHT) ring.add(n);
            ring.sort((a, b) -> Double.compare(Math.atan2(a.getZ() - d.c.z, a.getX() - d.c.x), Math.atan2(b.getZ() - d.c.z, b.getX() - d.c.x)));
            for (int i = 0; i < ring.size() && ring.size() > 1; i++) {
                AmbientNpc a = ring.get(i), b = ring.get((i + 1) % ring.size());
                Vec3 pa = a.position().add(0, 1.1, 0), pb = b.position().add(0, 1.1, 0);
                if (pa.distanceToSqr(pb) > 24 * 24) continue;
                for (int k = 1; k <= 3; k++) {
                    Vec3 q = pa.lerp(pb, k / 4.0);
                    l.sendParticles(k == 2 ? ParticleTypes.ELECTRIC_SPARK : SiegeFx.SPIRIT_DUST, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0);
                }
            }
        }
        // truong lao niem chu
        if (d.age % 30 == 0) for (AmbientNpc e : d.elders) if (e.isAlive() && !e.downed()) {
            e.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            l.sendParticles(SiegeFx.TALISMAN_DUST, e.getX(), e.getY() + 2.3, e.getZ(), 6, 0.4, 0.3, 0.4, 0);
            l.sendParticles(ParticleTypes.ENCHANT, e.getX(), e.getY() + 2.0, e.getZ(), 8, 0.4, 0.4, 0.4, 0.5);
        }
        // ket gioi day quai Hu Hoa dang lao vao bien gioi
        if (d.age % 4 == 0 && d.endAge < 0) {
            AABB box = new AABB(d.c, d.c).inflate(d.domeR + 4, 14, d.domeR + 4);
            for (LivingEntity m : l.getEntitiesOfClass(LivingEntity.class, box, e -> e instanceof HuMob)) {
                double dx = m.getX() - d.c.x, dz = m.getZ() - d.c.z, h = Math.sqrt(dx * dx + dz * dz);
                if (h < d.domeR - 1.5 || h > d.domeR + 1.0 || h < 1e-3) continue;
                m.setDeltaMovement(m.getDeltaMovement().add(dx / h * 0.55, 0.18, dz / h * 0.55));
                m.hurtMarked = true;
                l.sendParticles(ParticleTypes.ELECTRIC_SPARK, m.getX(), m.getY() + 1, m.getZ(), 8, 0.3, 0.5, 0.3, 0.2);
                l.sendParticles(SiegeFx.TALISMAN_DUST, m.getX(), m.getY() + 1, m.getZ(), 5, 0.3, 0.5, 0.3, 0);
                if (r.nextInt(3) == 0) l.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1f, 0.6f);
            }
        }
        return false;
    }

    // ================================================================== TICK
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        clock++;
        if (!TASKS.isEmpty()) {
            List<Runnable> due = new ArrayList<>();
            for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) { Task t = it.next(); if (t.due <= clock) { due.add(t.run); it.remove(); } }
            for (Runnable r : due) { try { r.run(); } catch (RuntimeException ex) { com.mojang.logging.LogUtils.getLogger().warn("siege task failed", ex); } }
        }
        if (!RIFTS.isEmpty()) RIFTS.removeIf(Rift::tick);
        if (!METEORS.isEmpty()) METEORS.removeIf(SiegeDirector::tickMeteor);
        if (!SMOULDERS.isEmpty()) SMOULDERS.removeIf(SiegeDirector::tickSmoulder);
        if (!SECTS.isEmpty()) SECTS.removeIf(SiegeDirector::tickSect);
        if (cur != null) tickSiege(cur);
    }

    // ================================================================== TIEN ICH
    private static void msg(Siege s, Component c) {
        Vec3 cv = Vec3.atCenterOf(s.center);
        for (ServerPlayer p : s.level.players()) if (p.distanceToSqr(cv) < 160 * 160) p.displayClientMessage(c, true);
    }

    private static Vec3 surface(ServerLevel l, int x, int z, int fallbackY) {
        if (!l.hasChunk(x >> 4, z >> 4)) return new Vec3(x + 0.5, fallbackY + 1, z + 0.5);
        return new Vec3(x + 0.5, l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5);
    }

    private static BlockPos surface3(ServerLevel l, BlockPos c, int dx, int dz) {
        return BlockPos.containing(surface(l, c.getX() + dx, c.getZ() + dz, c.getY()));
    }

    /** Hang chan cong: ben trong tuong canh 4 cong, xep hang doc theo tuong. */
    private static BlockPos gatePost(ServerLevel l, BlockPos c, int half, int idx) {
        Direction d = Direction.from2DDataValue(Math.floorMod(idx, 4));
        Direction al = d.getClockWise();
        int[] offs = {3, -3, 6, -6, 8, -8};
        int off = offs[(idx / 4) % offs.length];
        return BlockPos.containing(surface(l, c.getX() + d.getStepX() * (half - 4) + al.getStepX() * off,
                c.getZ() + d.getStepZ() * (half - 4) + al.getStepZ() * off, c.getY()));
    }

    /** Cung thu: tren dinh tuong (do cao mat tuong). */
    private static BlockPos wallPost(ServerLevel l, BlockPos c, int half, int idx) {
        Direction d = Direction.from2DDataValue(Math.floorMod(idx, 4));
        Direction al = d.getClockWise();
        int[] offs = {7, -7, 12, -12};
        int off = offs[(idx / 4) % offs.length];
        return BlockPos.containing(surface(l, c.getX() + d.getStepX() * (half - 1) + al.getStepX() * off,
                c.getZ() + d.getStepZ() * (half - 1) + al.getStepZ() * off, c.getY()));
    }

    private static AmbientNpc spawnNpc(ServerLevel l, int role, BlockPos at, BlockPos home, int radius) {
        AmbientNpc n = AmbientRegistry.AMBIENT.get().create(l);
        if (n == null) return null;
        n.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, l.random.nextFloat() * 360f, 0f);
        n.setup(role, l.random.nextInt(AmbientRegistry.SKINS), home, radius);
        n.finalizeSpawn(l, l.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        l.addFreshEntity(n);
        SiegeFx.puff(l, Vec3.atCenterOf(at));
        return n;
    }

    private static void scanDoors(Siege s) {
        scanDoorsIn(s.level, s.center.getX() - s.half, s.center.getZ() - s.half, s.center.getX() + s.half, s.center.getZ() + s.half, s.center.getY(), s.doors);
    }

    private static void scanDoorsIn(ServerLevel l, int x0, int z0, int x1, int z1, int y, List<BlockPos> out) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1 && out.size() < 48; x++)
            for (int z = z0; z <= z1 && out.size() < 48; z++) {
                if (!l.hasChunk(x >> 4, z >> 4)) continue;
                for (int yy = y - 1; yy <= y + 8; yy++) {
                    BlockState bs = l.getBlockState(m.set(x, yy, z));
                    if (bs.is(BlockTags.WOODEN_DOORS) && bs.hasProperty(DoorBlock.HALF) && bs.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) out.add(m.immutable());
                }
            }
    }

    private static BlockPos nearestDoor(Siege s, BlockPos from) { return nearest(s.doors, from); }

    private static BlockPos nearest(List<BlockPos> doors, BlockPos from) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : doors) { double d = p.distSqr(from); if (d < bd) { bd = d; best = p; } }
        return best;
    }

}
