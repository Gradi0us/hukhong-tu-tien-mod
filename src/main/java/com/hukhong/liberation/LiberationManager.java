package com.hukhong.liberation;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.entity.HuHoa;
import com.hukhong.entity.HuMob;
import com.hukhong.entity.HuVuong;
import com.hukhong.events.CorruptionData;
import com.hukhong.events.CorruptionManager;
import com.hukhong.item.ModItems;
import com.hukhong.story.objective.ObjectiveAttachments;
import com.hukhong.story.objective.ObjectiveData;
import com.hukhong.story.objective.Objectives;
import com.hukhong.voidblock.VoidBlocks;
import com.hukhong.world.SafeZones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Giai phong vung Hu Hoa: moi Tam Hu Hoa (CorruptionData.hearts) la dau truong co boss gac (HuVuong scale theo cap vung).
 * Vao vung -> nhiem vu phu "Giai phong vung dat" (Objectives neu chua co muc tieu, nguoc lai dong actionbar); gan Tam -> dung dau truong + boss.
 * Boss chet -> CorruptionManager.onBossKilled goi {@link #onLiberated}: tay vung (hang doi hoan nguyen), troi quang, thuong, hoan thanh nhiem vu.
 * Trong vung nhiem: manh vo bay, mach nut dap, mach phun Hu Khong (server-side, bo sung cho am thanh/hieu ung client).
 * Nhac boss: HuVuong tu goi BossMusic.watch("hu_vuong") (chua co track rieng).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class LiberationManager {
    private LiberationManager() {}

    public static final String OBJECTIVE_ID = "liberation";
    public static final int REGION = 112, BUILD_R = 64, GUARD_R = 52, LEAVE_R = 176;
    private static final int GUARD_COOLDOWN = 1200, MAX_GEYSERS = 6, WARN = 20, ERUPT = 36;
    private static final ResourceLocation ZONE_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "liberation_zone");

    private static final class Geyser {
        final BlockPos pos;
        int age;
        final Set<UUID> hit = new HashSet<>();
        Geyser(BlockPos p) { pos = p; }
    }

    private static final List<Geyser> GEYSERS = new ArrayList<>();
    private static final Map<Long, Long> GUARD_AT = new HashMap<>();
    private static final Map<Long, List<int[]>> CRACKS = new HashMap<>();
    private static final Set<UUID> IN_REGION = new HashSet<>();

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        GEYSERS.clear(); GUARD_AT.clear(); CRACKS.clear(); IN_REGION.clear();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { IN_REGION.remove(e.getEntity().getUUID()); }

    // ------------------------------------------------------------------ thuan
    /** Cap vung = cap quai tai Tam (theo khoang cach toi spawn, khong jitter, co thuong tuoi the gioi). */
    public static int zoneLevel(ServerLevel ow, BlockPos heart) {
        BlockPos sp = ow.getSharedSpawnPos();
        double d = Math.hypot(heart.getX() - sp.getX(), heart.getZ() - sp.getZ());
        return HuHoa.levelFor(d, HuHoa.ageBonus(ow.getGameTime() / 24000L), 0);
    }

    /** Thuan: he so boss theo cap vung so voi cap goc 30 cua Hu Vuong, kep [0.6, 3.5]. */
    public static double bossMul(int zoneLevel) { return Math.max(0.6, Math.min(3.5, zoneLevel / 30.0)); }

    public static boolean inRegion(BlockPos heart, double x, double z) {
        double dx = x - heart.getX(), dz = z - heart.getZ();
        return dx * dx + dz * dz <= (double) REGION * REGION;
    }

    private static BlockPos surface(ServerLevel ow, BlockPos heart) {
        int y = ow.hasChunk(heart.getX() >> 4, heart.getZ() >> 4) ? ow.getHeight(Heightmap.Types.WORLD_SURFACE, heart.getX(), heart.getZ()) : ow.getSeaLevel();
        return new BlockPos(heart.getX(), y, heart.getZ());
    }

    // ------------------------------------------------------------------ tick
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null || ow.players().isEmpty()) return;
        long t = ow.getGameTime();
        CorruptionData cd = CorruptionData.get(ow);
        if (t % 20 == 3) regionScan(ow, cd, t);
        if (t % 10 == 5) ambience(ow, cd, t);
        if (t % 2 == 0 && !GEYSERS.isEmpty()) tickGeysers(ow);
    }

    private static void regionScan(ServerLevel ow, CorruptionData cd, long t) {
        LiberationData ld = LiberationData.get(ow);
        for (BlockPos h : new ArrayList<>(cd.hearts())) {
            for (ServerPlayer p : ow.players()) {
                if (p.isSpectator() || !p.isAlive()) continue;
                double dx = p.getX() - h.getX(), dz = p.getZ() - h.getZ(), d = Math.sqrt(dx * dx + dz * dz);
                if (d <= REGION) enter(ow, p, h, d, t);
                else if (d > LEAVE_R) leave(p, h);
                if (d <= BUILD_R && !ld.hasArena(h) && HeartArena.build(ow, h)) ld.markArena(h);
                if (d <= GUARD_R) ensureGuardian(ow, h, t);
            }
        }
    }

    private static boolean targets(ServerPlayer p, BlockPos h) {
        ObjectiveData od = p.getData(ObjectiveAttachments.OBJECTIVE.get());
        return OBJECTIVE_ID.equals(od.id()) && od.target().isPresent()
                && od.target().get().getX() == h.getX() && od.target().get().getZ() == h.getZ();
    }

    private static void enter(ServerLevel ow, ServerPlayer p, BlockPos h, double dist, long t) {
        BlockPos target = surface(ow, h);
        boolean first = IN_REGION.add(p.getUUID());
        String cur = Objectives.currentId(p);
        if (cur == null) {
            Objectives.set(p, OBJECTIVE_ID, Component.translatableWithFallback("hukhong.liberation.objective",
                    "Giải phóng vùng đất: hạ Hộ Vệ Tâm Hư Hóa"), new BlockPos(target.getX(), target.getY(), target.getZ()), Level.OVERWORLD);
            if (first) p.sendSystemMessage(Component.translatableWithFallback("hukhong.liberation.enter",
                    "Bạn bước vào vùng Hư Hóa. Hạ Hộ Vệ ở Tâm Hư Hóa để giải phóng vùng đất."));
        } else if (first || t % 100 == 3) {
            // objective dang co (cua nhiem vu khac, hoac chinh vung nay): chi dong tracker o actionbar neu khong phai nhiem vu giai phong
            if (!OBJECTIVE_ID.equals(cur))
                p.displayClientMessage(Component.translatableWithFallback("hukhong.liberation.tracker",
                        "Giải phóng vùng đất: Tâm Hư Hóa cách %s block, hướng %s", (int) dist, direction(p, target)), true);
        }
    }

    private static void leave(ServerPlayer p, BlockPos h) {
        IN_REGION.remove(p.getUUID());
        if (targets(p, h)) Objectives.clear(p);
    }

    /** Huong la ban 8 phuong (D, DN, N, TN, T, TB, B, DB) tu nguoi choi toi dich. */
    static String direction(ServerPlayer p, BlockPos to) {
        double ang = Math.toDegrees(Math.atan2(to.getZ() - p.getZ(), to.getX() - p.getX())); // 0 = Dong, 90 = Nam
        String[] n = {"Đông", "Đông Nam", "Nam", "Tây Nam", "Tây", "Tây Bắc", "Bắc", "Đông Bắc"};
        return n[(int) Math.floorMod(Math.round(ang / 45.0), 8L)];
    }

    // ------------------------------------------------------------------ boss gac
    public static void ensureGuardian(ServerLevel ow, BlockPos h, long t) {
        if (!ow.hasChunk(h.getX() >> 4, h.getZ() >> 4)) return;
        long key = LiberationData.key(h);
        if (GUARD_AT.getOrDefault(key, -GUARD_COOLDOWN * 2L) + GUARD_COOLDOWN > t) return;
        AABB box = new AABB(h).inflate(96, 256, 96);
        if (!ow.getEntitiesOfClass(HuVuong.class, box).isEmpty()) return;
        GUARD_AT.put(key, t);
        HuVuong b = CorruptionManager.spawnBoss(ow, surface(ow, h));
        if (b == null) return;
        int zl = zoneLevel(ow, h);
        double mul = bossMul(zl) - 1;
        for (var a : List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE)) {
            var inst = b.getAttribute(a);
            if (inst == null) continue;
            inst.removeModifier(ZONE_MOD);
            inst.addPermanentModifier(new AttributeModifier(ZONE_MOD, mul, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        b.setHealth(b.getMaxHealth());
        b.getPersistentData().putInt("hk_zone_level", zl);
    }

    // ------------------------------------------------------------------ giai phong
    /** Goi khi Hu Vuong gac Tam bi ha (truoc khi Tam chuyen di). */
    public static void onLiberated(ServerLevel ow, BlockPos heart) {
        LiberationData ld = LiberationData.get(ow);
        ld.clearArena(heart);
        ld.addLiberated();
        com.hukhong.story.world.HuAnh.addNear(ow, heart, -15);
        GUARD_AT.remove(LiberationData.key(heart));
        CRACKS.remove(LiberationData.key(heart));
        GEYSERS.removeIf(g -> Math.hypot(g.pos.getX() - heart.getX(), g.pos.getZ() - heart.getZ()) < REGION + 16);
        int zl = zoneLevel(ow, heart);
        BlockPos s = surface(ow, heart);
        ow.setWeatherParameters(24000, 0, false, false); // troi quang
        for (ServerPlayer p : ow.players()) {
            double d = Math.hypot(p.getX() - heart.getX(), p.getZ() - heart.getZ());
            boolean tracked = targets(p, heart);
            if (d <= REGION + 48) {
                long exp = HuHoa.exp(30.0, zl, CultivationService.get(p).level());
                if (exp > 0) CultivationService.addExp(p, exp);
                int n = 12 + ow.random.nextInt(9);
                com.hukhong.currency.Wallet.grantHonTinh(p, n);
                p.sendSystemMessage(Component.translatableWithFallback("hukhong.liberation.done",
                        "Vùng đất đã được giải phóng! Hư Hóa tan dần, trời quang trở lại. +%s EXP, +%s Hồn Tinh", exp, n));
                ow.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.2f);
            }
            if (tracked) Objectives.complete(p, OBJECTIVE_ID);
            IN_REGION.remove(p.getUUID());
        }
        for (int i = 0; i < 6; i++)
            ow.sendParticles(ParticleTypes.END_ROD, s.getX() + 0.5, s.getY() + 1 + i * 4, s.getZ() + 0.5, 40, 3, 2, 3, 0.05);
    }

    // ------------------------------------------------------------------ hieu ung
    private static void ambience(ServerLevel ow, CorruptionData cd, long t) {
        BlockState deb = VoidBlocks.DA.get().defaultBlockState();
        for (ServerPlayer p : ow.players()) {
            if (p.isSpectator() || !cd.isCorrupted(p.blockPosition())) continue;
            // manh vo bay: da Hu Khong troi len cham
            ow.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR, deb), p.getX(), p.getY() + 0.5, p.getZ(), 16, 9, 2.5, 9, 0.2);
            ow.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 10, 8, 3, 8, 0.08);
            if (t % 60 == 5 && GEYSERS.size() < MAX_GEYSERS && ow.random.nextInt(3) == 0) spawnGeyser(ow, cd, p);
        }
        // mach nut dap quanh Tam
        for (BlockPos h : cd.hearts()) {
            boolean near = false;
            for (ServerPlayer p : ow.players()) if (p.distanceToSqr(h.getX(), p.getY(), h.getZ()) < 72 * 72) { near = true; break; }
            if (!near || !ow.hasChunk(h.getX() >> 4, h.getZ() >> 4)) continue;
            List<int[]> cr = CRACKS.computeIfAbsent(LiberationData.key(h), k -> HeartArena.cracks(h));
            int beat = (int) ((t / 10) % 6);  // nhip 0..5: sang len roi tat
            int pulse = beat < 3 ? beat + 1 : 6 - beat;
            for (int i = beat % 2; i < cr.size(); i += 2) {
                if (ow.random.nextInt(3) != 0) continue;
                int[] c = cr.get(i);
                int x = h.getX() + c[0], z = h.getZ() + c[1];
                int y = ow.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                ow.sendParticles(ParticleTypes.DRAGON_BREATH, x + 0.5, y + 0.2, z + 0.5, pulse, 0.2, 0.1, 0.2, 0.01);
                if (pulse >= 3) ow.sendParticles(ParticleTypes.PORTAL, x + 0.5, y + 0.8, z + 0.5, 3, 0.2, 0.5, 0.2, 0.3);
            }
        }
    }

    private static void spawnGeyser(ServerLevel ow, CorruptionData cd, ServerPlayer p) {
        double a = ow.random.nextDouble() * Math.PI * 2, r = 8 + ow.random.nextDouble() * 14;
        int x = (int) (p.getX() + Math.cos(a) * r), z = (int) (p.getZ() + Math.sin(a) * r);
        if (!ow.hasChunk(x >> 4, z >> 4) || !cd.isCorrupted(x >> 4, z >> 4)) return;
        BlockPos g = new BlockPos(x, ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        if (!ow.getFluidState(g.below()).isEmpty() || SafeZones.isSafe(ow, g)) return;
        GEYSERS.add(new Geyser(g));
    }

    private static void tickGeysers(ServerLevel ow) {
        for (Iterator<Geyser> it = GEYSERS.iterator(); it.hasNext(); ) {
            Geyser g = it.next();
            g.age += 2;
            double x = g.pos.getX() + 0.5, y = g.pos.getY(), z = g.pos.getZ() + 0.5;
            if (g.age < WARN) {
                ow.sendParticles(ParticleTypes.SMOKE, x, y + 0.1, z, 4, 0.4, 0.05, 0.4, 0.02);
                ow.sendParticles(ParticleTypes.DRAGON_BREATH, x, y + 0.1, z, 2, 0.5, 0.05, 0.5, 0.01);
            } else if (g.age < WARN + ERUPT) {
                for (int h = 0; h < 10; h += 2) ow.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + h, z, 5, 0.3, 0.4, 0.3, 0.15);
                ow.sendParticles(ParticleTypes.PORTAL, x, y + 1, z, 8, 0.4, 1.5, 0.4, 0.6);
                if (g.age == WARN) ow.playSound(null, g.pos, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.5f, 1.8f);
                for (LivingEntity le : ow.getEntitiesOfClass(LivingEntity.class, new AABB(g.pos).inflate(1.6, 5, 1.6), e -> e.isAlive() && !(e instanceof HuMob))) {
                    le.setDeltaMovement(le.getDeltaMovement().x, 0.75, le.getDeltaMovement().z);
                    le.hurtMarked = true;
                    le.fallDistance = 0;
                    if (le instanceof ServerPlayer sp) sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false));
                    if (g.hit.add(le.getUUID())) le.hurt(ow.damageSources().source(DamageTypes.MAGIC), 2.0f);
                }
            } else it.remove();
        }
    }
}
