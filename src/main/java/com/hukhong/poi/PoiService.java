package com.hukhong.poi;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.network.MapMarkersPayload;
import com.hukhong.story.world.StoryHooks;
import com.hukhong.travel.TravelRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chay dia diem: kham pha (-> dau ban do), canh giu (don xong -> ruong), hoi sinh, khe nut, doi quan hon ban dem,
 * Linh Tuyen / Thac Linh Khi, NPC tieu nhi. Quet moi giay, chi dia diem trong 160 block cua nguoi choi.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PoiService {
    private PoiService() {}

    public static final int DISCOVER_R = 48, ACTIVE_R = 30, SCAN_R = 160;
    public static final String NPC_TAG = "hukhong_poi_npc";
    private static final Set<UUID> DIRTY = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> MEDITATE_SECS = new HashMap<>();

    // ------------------------------------------------------------------ ban do
    public static boolean takeMarkerDirty(UUID u) { return DIRTY.remove(u); }

    /** Danh sach marker chung + dia diem nguoi choi nay da kham pha. */
    public static List<MapMarkersPayload.Marker> withMarkers(ServerPlayer p, List<MapMarkersPayload.Marker> base) {
        List<MapMarkersPayload.Marker> out = new ArrayList<>(base);
        out.addAll(markersFor(p));
        return out;
    }

    public static List<MapMarkersPayload.Marker> markersFor(ServerPlayer p) {
        List<MapMarkersPayload.Marker> out = new ArrayList<>();
        MinecraftServer s = p.getServer();
        if (s == null) return out;
        PoiSavedData data = PoiSavedData.get(s.overworld());
        for (String key : data.foundBy(p.getUUID())) {
            PoiInstance i = data.get(key);
            if (i != null) out.add(new MapMarkersPayload.Marker(MapMarkersPayload.POI, i.key, i.x, i.z, i.state == PoiInstance.CLEARED));
        }
        return out;
    }

    /** Danh dau da kham pha: dau ban do + (Khach Diem) diem dich chuyen. true neu moi. */
    public static boolean discover(ServerPlayer p, PoiInstance i) {
        ServerLevel ow = p.getServer().overworld();
        PoiSavedData data = PoiSavedData.get(ow);
        if (!data.markFound(p.getUUID(), i.key)) return false;
        DIRTY.add(p.getUUID());
        PoiDef d = PoiDefs.get(i.defId);
        p.displayClientMessage(Component.translatable("hukhong.poi.discovered", Component.translatable(d == null ? "hukhong.poi.n." + i.defId : d.nameKey())), true);
        if (d != null && d.travel()) p.setData(TravelRegistry.TRAVEL.get(), p.getData(TravelRegistry.TRAVEL.get()).with("p:" + i.key));
        return true;
    }

    // ------------------------------------------------------------------ vong doi
    static void afterPlace(ServerLevel ow, PoiInstance i, PoiDef d) {
        if (d.builder().equals("inn")) ensureInnkeeper(ow, i);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) DIRTY.add(p.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        MEDITATE_SECS.remove(e.getEntity().getUUID());
        PoiBoost.clear(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer s = e.getServer();
        if (s.getTickCount() % 20 != 7) return;
        ServerLevel ow = s.overworld();
        PoiSavedData data = PoiSavedData.get(ow);
        if (data.count() == 0) return;
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer p : ow.players()) if (p.isAlive() && !p.isSpectator()) players.add(p);
        if (players.isEmpty()) return;
        long now = ow.getGameTime();
        for (PoiInstance i : new ArrayList<PoiInstance>(asList(data.all()))) {
            ServerPlayer near = null;
            double nd = Double.MAX_VALUE;
            for (ServerPlayer p : players) { double d2 = i.dist2(p.getX(), p.getZ()); if (d2 < nd) { nd = d2; near = p; } }
            if (near == null || nd > (double) SCAN_R * SCAN_R || !ow.hasChunkAt(i.origin())) continue;
            PoiDef d = PoiDefs.get(i.defId);
            if (d == null) continue;
            if (nd <= (double) DISCOVER_R * DISCOVER_R)
                for (ServerPlayer p : players) if (i.dist2(p.getX(), p.getZ()) <= (double) DISCOVER_R * DISCOVER_R) discover(p, i);
            try {
                encounter(ow, data, i, d, nd, now);
                ambient(ow, data, i, d, players, now);
            } catch (Exception ex) {
                com.mojang.logging.LogUtils.getLogger().error("POI {} tick failed: {}", i.key, ex.toString());
            }
        }
    }

    private static <T> List<T> asList(Iterable<T> it) { List<T> l = new ArrayList<>(); for (T t : it) l.add(t); return l; }

    // ------------------------------------------------------------------ canh giu
    private static void encounter(ServerLevel ow, PoiSavedData data, PoiInstance i, PoiDef d, double nd, long now) {
        String enc = d.encounter();
        double a2 = (double) ACTIVE_R * ACTIVE_R;
        switch (enc) {
            case "approach" -> {
                if (i.state == PoiInstance.READY && nd <= a2) {
                    if (PoiMobs.spawnAll(ow, i, d, ow.random) > 0) { i.state = PoiInstance.ACTIVE; i.stateTick = now; data.setDirty(); DIRTY_ALL(ow); }
                } else if (i.state == PoiInstance.ACTIVE && now - i.stateTick > 100 && nd <= 48.0 * 48.0) {
                    if (PoiMobs.alive(ow, i, 48).isEmpty()) clear(ow, data, i, d, now, true);
                } else if (i.state == PoiInstance.CLEARED && now - i.stateTick >= d.respawnTicks() && nd > 64.0 * 64.0) {
                    reset(ow, data, i, d, now);
                }
            }
            case "timed" -> timed(ow, data, i, d, nd, now);
            case "night_squads" -> squads(ow, data, i, d, nd, now);
            default -> { }
        }
        if (("cave".equals(d.builder()) || "wuxing".equals(d.builder())) && i.solved && now - i.stateTick >= d.respawnTicks() && nd > 64.0 * 64.0) resetPuzzle(ow, data, i);
        if ("grave".equals(d.builder()) && i.state == PoiInstance.CLEARED && now - i.stateTick >= d.respawnTicks() && nd > 64.0 * 64.0) reset(ow, data, i, d, now);
    }

    private static void DIRTY_ALL(ServerLevel ow) {
        for (ServerPlayer p : ow.players()) DIRTY.add(p.getUUID());
    }

    static void clear(ServerLevel ow, PoiSavedData data, PoiInstance i, PoiDef d, long now, boolean fillChest) {
        i.state = PoiInstance.CLEARED;
        i.stateTick = now;
        data.setDirty();
        DIRTY_ALL(ow);
        PoiInstance.Act chest = i.firstAct("chest");
        if (fillChest && chest != null) PoiRewards.fillChest(ow, chest.pos(), d, ow.random);
        for (ServerPlayer p : ow.players()) {
            if (i.dist2(p.getX(), p.getZ()) > 60.0 * 60.0) continue;
            PoiRewards.currencies(p, d, ow.random);
            if (chest == null) PoiRewards.giveStacks(p, PoiRewards.itemStacks(d, ow.random, CultivationService.get(p).paths()));
            if (StoryHooks.allowed(p, d.lore())) StoryHooks.complete(p, d.lore());
            p.sendSystemMessage(Component.translatable("hukhong.poi.cleared", Component.translatable(d.nameKey())));
        }
    }

    static void reset(ServerLevel ow, PoiSavedData data, PoiInstance i, PoiDef d, long now) {
        i.state = PoiInstance.READY;
        i.stateTick = now;
        i.extra.putInt("waves", 0);
        for (String k : new ArrayList<>(i.extra.getAllKeys())) if (k.startsWith("cd:") || k.startsWith("claimed:")) i.extra.remove(k);
        PoiInstance.Act chest = i.firstAct("chest");
        if (chest != null) PoiRewards.emptyChest(ow, chest.pos());
        if (d.builder().equals("rift")) PoiBuilders.setRift(ow, i, true);
        data.setDirty();
        DIRTY_ALL(ow);
    }

    static void resetPuzzle(ServerLevel ow, PoiSavedData data, PoiInstance i) {
        i.solved = false;
        i.progress = 0;
        for (String k : new ArrayList<>(i.extra.getAllKeys())) if (k.startsWith("claimed:")) i.extra.remove(k);
        if (i.firstAct("vault") != null) { PoiBuilders.resetLevers(ow, i); PoiBuilders.setSeal(ow, i, false); }
        if (i.firstAct("pillar:0") != null) PoiBuilders.resetPillars(ow, i, i.seed);
        data.setDirty();
    }

    private static void timed(ServerLevel ow, PoiSavedData data, PoiInstance i, PoiDef d, double nd, long now) {
        int total = d.intParam("waves", 3), interval = d.intParam("interval", 200), limit = d.intParam("limit", 6000);
        if (i.state == PoiInstance.READY && nd <= 20.0 * 20.0) {
            i.state = PoiInstance.ACTIVE; i.stateTick = now; i.extra.putInt("waves", 0); data.setDirty();
            for (ServerPlayer p : ow.players()) if (i.dist2(p.getX(), p.getZ()) <= 60.0 * 60.0)
                p.sendSystemMessage(Component.translatable("hukhong.poi.rift.open", limit / 1200));
        }
        if (i.state == PoiInstance.ACTIVE) {
            long el = now - i.stateTick;
            int waves = i.extra.getInt("waves");
            if (waves < total && el >= (long) waves * interval) {
                PoiMobs.spawnAll(ow, i, d, ow.random);
                i.extra.putInt("waves", waves + 1);
                data.setDirty();
            }
            boolean none = PoiMobs.alive(ow, i, 40).isEmpty();
            if (waves >= total && none) {
                PoiBuilders.setRift(ow, i, false);
                clear(ow, data, i, d, now, false);
            } else if (el > limit) {
                PoiMobs.discardAll(ow, i, 40);
                PoiBuilders.setRift(ow, i, false);
                i.state = PoiInstance.CLEARED; i.stateTick = now; data.setDirty();
                for (ServerPlayer p : ow.players()) if (i.dist2(p.getX(), p.getZ()) <= 60.0 * 60.0)
                    p.sendSystemMessage(Component.translatable("hukhong.poi.rift.collapse"));
            }
        } else if (i.state == PoiInstance.CLEARED && now - i.stateTick >= d.respawnTicks() && nd > 64.0 * 64.0) {
            reset(ow, data, i, d, now);
        }
    }

    private static void squads(ServerLevel ow, PoiSavedData data, PoiInstance i, PoiDef d, double nd, long now) {
        long dt = ow.getDayTime() % 24000L;
        boolean night = dt >= 13000 && dt <= 23000;
        if (!night) { if (!PoiMobs.alive(ow, i, 60).isEmpty()) PoiMobs.discardAll(ow, i, 60); return; }
        if (nd > 56.0 * 56.0 || now - i.stateTick < d.intParam("interval", 400)) return;
        if (PoiMobs.alive(ow, i, 60).size() >= d.intParam("cap", 8)) return;
        i.stateTick = now;
        data.setDirty();
        PoiMobs.spawnAll(ow, i, d, ow.random);
    }

    /** Doi quan hon / quai dia diem chet: co the roi Hon Cot + loot. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide || !(v.level() instanceof ServerLevel sl)) return;
        String key = v.getPersistentData().getString(PoiMobs.TAG);
        if (key.isEmpty() || !(e.getSource().getEntity() instanceof ServerPlayer killer)) return;
        PoiDef d = PoiDefs.get(PoiInstance.defOfKey(key));
        if (d == null || !"night_squads".equals(d.encounter())) return;
        if (sl.random.nextDouble() < d.dblParam("bone_chance", 0.04)) PoiRewards.giveStacks(killer, List.of(PoiRewards.bone(sl.random)));
        PoiRewards.giveStacks(killer, PoiRewards.itemStacks(d, sl.random, CultivationService.get(killer).paths()));
    }

    // ------------------------------------------------------------------ moi truong
    private static void ambient(ServerLevel ow, PoiSavedData data, PoiInstance i, PoiDef d, List<ServerPlayer> players, long now) {
        switch (d.builder()) {
            case "spring" -> {
                for (ServerPlayer p : players) {
                    if (!inSpring(i, p)) continue;
                    PoiBoost.set(p, PoiBoost.SOAK, now);
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false));
                    CultivationService.addExp(p, PoiBoost.trickle(CultivationService.get(p).level(), PoiBoost.SOAK));
                    p.displayClientMessage(Component.translatable("hukhong.poi.spring.soak"), true);
                    ow.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.6, p.getZ(), 1, 0.3, 0.1, 0.3, 0);
                }
                if (!players.isEmpty() && i.dist2(players.get(0).getX(), players.get(0).getZ()) < 40.0 * 40.0)
                    ow.sendParticles(ParticleTypes.CLOUD, i.x + 0.5, i.y + 0.2, i.z + 0.5, 6, 1.5, 0.2, 1.5, 0.01);
            }
            case "waterfall" -> {
                PoiInstance.Act a = i.firstAct("meditate");
                if (a == null) break;
                for (ServerPlayer p : players) {
                    UUID u = p.getUUID();
                    if (!inNook(a, p) || !p.isShiftKeyDown()) { MEDITATE_SECS.remove(u); continue; }
                    PoiBoost.set(p, PoiBoost.MEDITATE, now);
                    CultivationService.addExp(p, PoiBoost.trickle(CultivationService.get(p).level(), PoiBoost.MEDITATE));
                    int secs = MEDITATE_SECS.merge(u, 1, Integer::sum);
                    p.displayClientMessage(Component.translatable("hukhong.poi.waterfall.meditate"), true);
                    ow.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 2, 0.4, 0.5, 0.4, 0.01);
                    if (secs >= 60) { // ngo dao
                        MEDITATE_SECS.put(u, 0);
                        PoiActions.addExpFrac(p, 0.03);
                        p.sendSystemMessage(Component.translatable("hukhong.poi.waterfall.insight"));
                    }
                }
            }
            case "rift" -> {
                if (i.state != PoiInstance.CLEARED) ow.sendParticles(ParticleTypes.PORTAL, i.x + 0.5, i.y + 2.0, i.z + 0.5, 12, 1.2, 1.6, 0.4, 0.4);
            }
            case "inn" -> ensureInnkeeper(ow, i);
            default -> { }
        }
    }

    public static boolean inSpring(PoiInstance i, Player p) {
        double dx = p.getX() - (i.x + 0.5), dz = p.getZ() - (i.z + 0.5);
        return dx * dx + dz * dz <= 12.0 && p.isInWater() && p.getY() >= i.y - 3 && p.getY() <= i.y + 1;
    }

    public static boolean inNook(PoiInstance.Act a, Player p) {
        double dx = p.getX() - (a.x() + 0.5), dz = p.getZ() - (a.z() + 0.5);
        return dx * dx + dz * dz <= 2.6 && Math.abs(p.getY() - a.y()) <= 3;
    }

    // ------------------------------------------------------------------ NPC tieu nhi
    static void ensureInnkeeper(ServerLevel ow, PoiInstance i) {
        PoiInstance.Act a = i.firstAct("innkeeper");
        if (a == null || !ow.hasChunkAt(a.pos())) return;
        AABB box = new AABB(a.pos()).inflate(8, 4, 8);
        if (!ow.getEntitiesOfClass(Villager.class, box, v -> i.key.equals(v.getPersistentData().getString(NPC_TAG))).isEmpty()) return;
        Villager v = EntityType.VILLAGER.create(ow);
        if (v == null) return;
        v.moveTo(a.x() + 0.5, a.y(), a.z() + 0.5, 180f, 0f);
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setPersistenceRequired();
        v.setCustomName(Component.translatable("hukhong.poi.npc.tieu_nhi"));
        v.setCustomNameVisible(true);
        v.getPersistentData().putString(NPC_TAG, i.key);
        ow.addFreshEntity(v);
    }

    /** Vi tri diem dich chuyen cua dia diem (cua khach diem) hoac goc. */
    public static BlockPos travelPos(PoiInstance i) {
        PoiInstance.Act a = i.firstAct("inn_door");
        return a != null ? a.pos() : i.origin();
    }
}
