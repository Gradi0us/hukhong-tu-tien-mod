package com.hukhong.zone;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityRegistryData;
import com.hukhong.core.CultivationService;
import com.hukhong.currency.Wallet;
import com.hukhong.entity.HuHoa;
import com.hukhong.events.CorruptionData;
import com.hukhong.liberation.HeartArena;
import com.hukhong.liberation.LiberationData;
import com.hukhong.story.world.Faction;
import com.hukhong.story.world.HuAnh;
import com.hukhong.story.world.Lore;
import com.hukhong.story.world.StoryHooks;
import com.hukhong.story.objective.Objectives;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Vung xam thuc dat san (PLAN-v2 12.4): tao du lieu 1 lan luc khoi dong the gioi, bien doi/quai chi khi chunk nap.
 * Vao vung -> nhiem vu "Giai Phong"; gan tam -> dung dau truong (HeartArena) + boss (ZoneBossFactory); boss chet -> tay sach tu tam ra.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class ZoneManager {
    private ZoneManager() {}

    public static final String OBJECTIVE_ID = "zone_liberation";
    public static final String TAG_ZONE = "hk_zone";
    public static final int BUILD_R = 64, GUARD_R = 52, LEAVE_MARGIN = 64, CLEANSE_PER_SEC = 16, REWARD_R = 128;
    private static final int GUARD_COOLDOWN = 1200, REVERT_PER_TICK = 6, QUEUE_MAX = 4096;

    private static final Set<UUID> INSIDE = new HashSet<>();
    private static final Map<Integer, Long> GUARD_AT = new HashMap<>();
    private static final ConcurrentLinkedQueue<Long> LOADED = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger LOADED_N = new AtomicInteger();

    // ------------------------------------------------------------------ tao du lieu
    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) {
        ServerLevel ow = e.getServer().getLevel(Level.OVERWORLD);
        if (ow == null) return;
        ZoneData d = ZoneData.get(ow);
        if (!d.generated()) generate(ow, d);
        d.rebuildIndex();
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        ZoneData.clearIndex(); INSIDE.clear(); GUARD_AT.clear(); LOADED.clear(); LOADED_N.set(0);
    }

    /** Chi ghi du lieu (khong tai chunk). */
    public static void generate(ServerLevel ow, ZoneData d) {
        BlockPos sp = ow.getSharedSpawnPos();
        CityRegistryData reg = CityRegistryData.get(ow);
        int[][] cities = new int[CityDef.ALL.size()][];
        for (CityDef c : CityDef.ALL) {
            BlockPos ctr = reg.center(c.id());
            cities[c.index()] = ctr != null ? new int[]{ctr.getX(), ctr.getZ()} : new int[]{sp.getX() + c.ax(), sp.getZ() + c.az()};
        }
        for (ZoneMath.Spec s : ZoneMath.place(ow.getSeed(), sp.getX(), sp.getZ(), cities)) {
            int form = s.radius() >= 600 ? 7 : formFor(ow, s.cx(), s.cz());
            d.add(new ZoneData.Zone(s.id(), s.cx(), s.cz(), s.radius(), s.level(), form, s.name()));
        }
        d.markGenerated();
    }

    /** Goi y dang Hu Vuong theo bioma (lay tu bo sinh bioma, khong tai chunk). 0 dong bang,1 nui,2 rung,3 tuyet,4 sa mac,5 dam,6 bien. */
    static int formFor(ServerLevel ow, int x, int z) {
        try {
            Holder<Biome> b = ow.getChunkSource().getGenerator().getBiomeSource()
                    .getNoiseBiome(x >> 2, 16, z >> 2, ow.getChunkSource().randomState().sampler());
            if (b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_BEACH) || b.is(BiomeTags.IS_RIVER)) return 6;
            if (b.is(Biomes.SWAMP) || b.is(Biomes.MANGROVE_SWAMP)) return 5;
            if (b.is(Biomes.DESERT) || b.is(BiomeTags.IS_BADLANDS)) return 4;
            if (b.is(BiomeTags.IS_MOUNTAIN)) return 1;
            if (b.is(BiomeTags.IS_FOREST) || b.is(BiomeTags.IS_JUNGLE) || b.is(BiomeTags.IS_TAIGA)) return 2;
            if (b.value().getBaseTemperature() <= 0.15f) return 3;
        } catch (RuntimeException ex) { /* du phong: dong bang */ }
        return 0;
    }

    // ------------------------------------------------------------------ chunk nap -> hoan nguyen vung da tay sach
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load e) {
        if (e.isNewChunk() || !(e.getLevel() instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) return;
        ChunkPos cp = e.getChunk().getPos();
        if (ZoneData.cleansedAt(cp.getMiddleBlockX(), cp.getMiddleBlockZ()) && LOADED_N.get() < QUEUE_MAX) {
            LOADED.add(cp.toLong()); LOADED_N.incrementAndGet();
        }
    }

    // ------------------------------------------------------------------ tick
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        ServerLevel ow = e.getServer().getLevel(Level.OVERWORLD);
        if (ow == null || ow.players().isEmpty()) return;
        long t = ow.getGameTime();
        if (!LOADED.isEmpty()) {
            CorruptionData cd = CorruptionData.get(ow);
            for (int i = 0; i < REVERT_PER_TICK; i++) {
                Long l = LOADED.poll();
                if (l == null) break;
                LOADED_N.decrementAndGet();
                cd.markRevert(l);
            }
        }
        if (t % 20 != 7) return;
        ZoneData zd = ZoneData.get(ow);
        if (zd.zones().isEmpty()) return;
        scanPlayers(ow, zd, t);
        for (ZoneData.Zone z : zd.zones()) if (z.liberated && z.cleansed < z.radius) cleanseStep(ow, zd, z);
    }

    private static void scanPlayers(ServerLevel ow, ZoneData zd, long t) {
        for (ZoneData.Zone z : zd.zones()) {
            if (z.liberated) continue;
            for (ServerPlayer p : ow.players()) {
                if (p.isSpectator() || !p.isAlive()) continue;
                double d = Math.hypot(p.getX() - z.cx, p.getZ() - z.cz);
                if (d <= z.radius) enter(ow, p, z, d, t);
                else if (d > z.radius + LEAVE_MARGIN) leave(p, z);
                if (d <= BUILD_R && !LiberationData.get(ow).hasArena(center(z)) && HeartArena.build(ow, center(z)))
                    LiberationData.get(ow).markArena(center(z));
                if (d <= GUARD_R) ensureGuardian(ow, z, t);
            }
        }
    }

    public static BlockPos center(ZoneData.Zone z) { return new BlockPos(z.cx, 0, z.cz); }

    private static BlockPos surface(ServerLevel ow, ZoneData.Zone z) {
        int y = ow.hasChunk(z.cx >> 4, z.cz >> 4) ? ow.getHeight(Heightmap.Types.WORLD_SURFACE, z.cx, z.cz) : ow.getSeaLevel();
        return new BlockPos(z.cx, y, z.cz);
    }

    static boolean targets(ServerPlayer p, ZoneData.Zone z) {
        var od = p.getData(com.hukhong.story.objective.ObjectiveAttachments.OBJECTIVE.get());
        return OBJECTIVE_ID.equals(od.id()) && od.target().isPresent()
                && od.target().get().getX() == z.cx && od.target().get().getZ() == z.cz;
    }

    /** Vao vung: nhan nhiem vu Giai Phong (neu chua co muc tieu chinh), nguoc lai dong actionbar. */
    static void enter(ServerLevel ow, ServerPlayer p, ZoneData.Zone z, double dist, long t) {
        boolean first = INSIDE.add(p.getUUID());
        String cur = Objectives.currentId(p);
        if (cur == null) {
            Objectives.set(p, OBJECTIVE_ID, objectiveText(z), surface(ow, z), Level.OVERWORLD);
        } else if (!OBJECTIVE_ID.equals(cur) && (first || t % 100 == 7)) {
            p.displayClientMessage(Component.translatableWithFallback("hukhong.zone.tracker",
                    "Giải Phóng %s: tâm cách %s block, cấp gợi ý %s", z.name, (int) dist, z.level), true);
        }
        if (first) p.sendSystemMessage(Component.translatableWithFallback("hukhong.zone.enter",
                "Bạn bước vào vùng xâm thực %s (cấp %s). Hạ Hư Vương ở tâm vùng để giải phóng.", z.name, z.level));
    }

    public static Component objectiveText(ZoneData.Zone z) {
        return Component.translatableWithFallback("hukhong.zone.objective", "Giải Phóng %s (cấp gợi ý %s)", z.name, z.level);
    }

    static void leave(ServerPlayer p, ZoneData.Zone z) {
        INSIDE.remove(p.getUUID());
        if (targets(p, z)) Objectives.clear(p);
    }

    // ------------------------------------------------------------------ boss gac
    static void ensureGuardian(ServerLevel ow, ZoneData.Zone z, long t) {
        if (z.liberated || !ow.hasChunk(z.cx >> 4, z.cz >> 4)) return;
        if (GUARD_AT.getOrDefault(z.id, -GUARD_COOLDOWN * 2L) + GUARD_COOLDOWN > t) return;
        AABB box = new AABB(z.cx, ow.getMinBuildHeight(), z.cz, z.cx, ow.getMaxBuildHeight(), z.cz).inflate(96, 0, 96);
        if (!ow.getEntitiesOfClass(Mob.class, box, m -> m.getPersistentData().getInt(TAG_ZONE) == z.id).isEmpty()) return;
        GUARD_AT.put(z.id, t);
        Mob b = ZoneBossFactory.Holder.current.spawn(ow, z, surface(ow, z));
        if (b != null) b.getPersistentData().putInt(TAG_ZONE, z.id);
    }

    // ------------------------------------------------------------------ giai phong
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        if (!(v.level() instanceof ServerLevel sl) || !(v instanceof Mob)) return;
        int id = v.getPersistentData().getInt(TAG_ZONE);
        if (id <= 0) return;
        ServerLevel ow = sl.getServer().overworld();
        ZoneData zd = ZoneData.get(ow);
        ZoneData.Zone z = zd.byId(id);
        if (z != null) liberate(ow, zd, z, v.position(), e.getSource());
    }

    /** Danh dau da giai phong (khong sinh lai boss), thuong, goi hook truyen. Tra false neu da giai phong tu truoc. */
    public static boolean liberate(ServerLevel ow, ZoneData zd, ZoneData.Zone z, Vec3 at, DamageSource src) {
        if (z.liberated) return false;
        z.liberated = true;
        z.cleansed = 0;
        zd.setDirty();
        LiberationData ld = LiberationData.get(ow);
        ld.clearArena(center(z));
        ld.addLiberated();
        GUARD_AT.remove(z.id);
        HuAnh.addNear(ow, new BlockPos(z.cx, 64, z.cz), -25);
        long n = ZoneMath.honTinhReward(z.level);
        Wallet.grantHonTinhBoss(ow, at, src, n);
        Lore lore = new Lore(Faction.DA_KHUYET, 0, 99, List.of(), List.of("zone.liberated"), ZoneMath.clueFor(z.id, z.radius), null, null);
        for (ServerPlayer p : ow.players()) {
            if (p.isSpectator() || p.position().distanceToSqr(at) > (double) REWARD_R * REWARD_R) continue;
            long exp = HuHoa.exp(30.0, z.level, CultivationService.get(p).level());
            if (exp > 0) CultivationService.addExp(p, exp);
            StoryHooks.complete(p, lore);
            p.sendSystemMessage(Component.translatableWithFallback("hukhong.zone.liberated",
                    "Vùng %s đã được giải phóng! Hư Hóa tan dần từ tâm. +%s EXP, +%s Hồn Tinh", z.name, exp, n));
            ow.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
        for (ServerPlayer p : ow.players()) {
            if (targets(p, z)) Objectives.complete(p, OBJECTIVE_ID);
            INSIDE.remove(p.getUUID());
        }
        return true;
    }

    /** Moi giay: mo rong vong da tay sach, dua cac chunk da nap quanh nguoi choi trong vong moi vao hang doi hoan nguyen. */
    private static void cleanseStep(ServerLevel ow, ZoneData zd, ZoneData.Zone z) {
        int old = z.cleansed;
        z.cleansed = Math.min(z.radius, old + CLEANSE_PER_SEC);
        zd.setDirty();
        CorruptionData cd = CorruptionData.get(ow);
        for (ServerPlayer p : ow.players()) {
            int pcx = p.blockPosition().getX() >> 4, pcz = p.blockPosition().getZ() >> 4;
            for (int x = pcx - 10; x <= pcx + 10; x++)
                for (int zz = pcz - 10; zz <= pcz + 10; zz++) {
                    double dd = Math.hypot(x * 16 + 8 - z.cx, zz * 16 + 8 - z.cz);
                    if (dd <= z.cleansed + 12 && dd >= old - 24 && dd <= z.radius + 12 && ow.hasChunk(x, zz)) cd.markRevert(ChunkPos.asLong(x, zz));
                }
        }
    }

    /** Cho GameTest/lenh: dat cac the vung (khong dung trong luc choi). */
    public static List<ZoneData.Zone> all(ServerLevel ow) { return new ArrayList<>(ZoneData.get(ow).zones()); }
}
