package com.hukhong.world.calendar;

import com.hukhong.HuKhong;
import com.hukhong.entity.WorldBoss;
import com.hukhong.events.WorldBossManager;
import com.hukhong.travel.TravelService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Yeu Vuong xuat son. Boss KHONG con sinh mot lan luc bat dau (khi do thuong khong co ai gan -> chunk chua tai / boss bi bo quen).
 * Thay vao do, moi giay handler kiem tra: su kien dang dien ra + co nguoi choi gan diem su kien (chunk dang tick) + chua co boss gan day
 * (tag theo key) -> sinh boss. Thanh mau hien cho moi nguoi trong 160 block. Boss chet -> su kien ket thuc; het gio -> boss bien mat.
 * Khong giu trang thai trong bo nho nen song sot qua restart (Act duoc luu trong CalendarData).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class YeuVuongHandler {
    private YeuVuongHandler() {}

    public static final String TAG_PREFIX = "hk_cal_ev:";
    public static final int SPAWN_RANGE = 120, BAR_RANGE = 160, SEARCH_RADIUS = 160;
    private static final long RESPAWN_GAP = 40;
    private static final Map<String, Long> LAST_SPAWN = new HashMap<>();

    public static String tag(String key) { return TAG_PREFIX + key; }

    public static void begin(ServerLevel ow, CalendarData.Act a) { ensure(ow, a); }

    public static void end(ServerLevel ow, CalendarData.Act a) {
        LAST_SPAWN.remove(a.key());
        for (WorldBoss b : find(ow, a)) { b.hideAll(); b.discard(); }
    }

    static java.util.List<WorldBoss> find(ServerLevel ow, CalendarData.Act a) {
        String tag = tag(a.key());
        return ow.getEntitiesOfClass(WorldBoss.class, new AABB(a.x() - SEARCH_RADIUS, ow.getMinBuildHeight(), a.z() - SEARCH_RADIUS,
                a.x() + SEARCH_RADIUS, ow.getMaxBuildHeight(), a.z() + SEARCH_RADIUS), b -> b.isAlive() && b.getTags().contains(tag));
    }

    private static boolean playerNear(ServerLevel ow, CalendarData.Act a, int range) {
        for (ServerPlayer p : ow.players())
            if (!p.isSpectator() && Math.abs(p.getX() - a.x()) < range && Math.abs(p.getZ() - a.z()) < range) return true;
        return false;
    }

    /** Dam bao co boss tai diem su kien (neu chunk dang tick). @return boss hien co / vua sinh, hoac null. */
    static WorldBoss ensure(ServerLevel ow, CalendarData.Act a) { return ensure(ow, a, true); }

    static WorldBoss ensure(ServerLevel ow, CalendarData.Act a, boolean needPlayer) {
        var cur = find(ow, a);
        if (!cur.isEmpty()) return cur.get(0);
        if ((needPlayer && !playerNear(ow, a, SPAWN_RANGE)) || !ow.isPositionEntityTicking(new BlockPos(a.x(), 64, a.z()))) return null;
        long now = ow.getGameTime();
        Long last = LAST_SPAWN.get(a.key());
        if (last != null && now - last < RESPAWN_GAP) return null; // cho entity cua chunk vua tai kip nap, tranh sinh trung
        BlockPos land = TravelService.findLanding(ow, a.x(), a.z(), null);
        int kind = WorldBossManager.kindOf(ow.getBiome(land).unwrapKey().map(k -> k.location().getPath()).orElse(""));
        if (kind < 0) kind = (int) (LichMath.unit(a.defId(), a.start()) * 3);
        WorldBoss b = WorldBossManager.spawn(ow, kind, land);
        if (b == null) return null;
        b.addTag(tag(a.key()));
        b.setPersistenceRequired();
        LAST_SPAWN.put(a.key(), now);
        return b;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        ServerLevel ow = e.getServer().overworld();
        if (ow == null || ow.getGameTime() % 20 != 7) return;
        for (CalendarData.Act a : new java.util.ArrayList<>(CalendarData.get(ow).active().values())) {
            if (!"yeu_vuong".equals(a.defId())) continue;
            WorldBoss b = ensure(ow, a);
            if (b == null) continue;
            for (ServerPlayer p : ow.players()) {
                double dx = p.getX() - b.getX(), dz = p.getZ() - b.getZ();
                if (!p.isSpectator() && dx * dx + dz * dz < (double) BAR_RANGE * BAR_RANGE) b.showBar(p);
                else if (dx * dx + dz * dz > (double) (BAR_RANGE + 32) * (BAR_RANGE + 32)) b.hideBar(p);
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof WorldBoss b) || !(b.level() instanceof ServerLevel sl)) return;
        for (String t : b.getTags())
            if (t.startsWith(TAG_PREFIX)) CalendarScheduler.finishNow(sl.getServer().overworld(), t.substring(TAG_PREFIX.length()));
    }
}
