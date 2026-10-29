package com.hukhong.client.ambient;

import com.hukhong.HuKhong;
import com.hukhong.bicanh.BiCanhManager;
import com.hukhong.city.CitySavedData;
import com.hukhong.events.CorruptionData;
import com.hukhong.events.InvasionManager;
import com.hukhong.events.VoidRaid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Gửi {@link AmbientSyncPayload} (chỉ đọc dữ liệu có sẵn): mỗi 2s nếu đổi, ép gửi lại mỗi 10s. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class AmbientSync {
    private AmbientSync() {}

    private static final int CHUNK_R = 14;
    private static final Map<UUID, AmbientSyncPayload> LAST = new HashMap<>();
    private static final Map<UUID, Integer> AGE = new HashMap<>();

    static AmbientSyncPayload build(ServerPlayer p) {
        List<Long> chunks = new ArrayList<>();
        List<BlockPos> hearts = new ArrayList<>();
        int flags = 0, cx = 0, cz = 0;
        if (p.level().dimension() == Level.OVERWORLD && p.level() instanceof ServerLevel ow) {
            CorruptionData cd = CorruptionData.get(ow);
            int pcx = p.blockPosition().getX() >> 4, pcz = p.blockPosition().getZ() >> 4;
            for (long l : cd.chunks()) {
                int x = ChunkPos.getX(l), z = ChunkPos.getZ(l);
                if (Math.abs(x - pcx) <= CHUNK_R && Math.abs(z - pcz) <= CHUNK_R) chunks.add(l);
                if (chunks.size() >= 1100) break;
            }
            com.hukhong.zone.ZoneData.addNearbyChunks(pcx, pcz, CHUNK_R, chunks, 1100);
            hearts = nearestHearts(cd.hearts(), p.blockPosition());
            if (InvasionManager.active() || InvasionManager.warning()) {
                CitySavedData d = CitySavedData.get(ow);
                if (d.isBuilt()) { flags |= AmbientSyncPayload.F_INVASION; cx = d.center().getX(); cz = d.center().getZ(); }
            }
            if (VoidRaid.active(p.getUUID())) { flags |= AmbientSyncPayload.F_RAID; cx = p.getBlockX(); cz = p.getBlockZ(); }
        }
        return new AmbientSyncPayload(chunks, hearts, flags, cx, cz, BiCanhManager.tierOrdinalOf(p));
    }

    private static List<BlockPos> nearestHearts(List<BlockPos> all, BlockPos at) {
        List<BlockPos> out = new ArrayList<>(all);
        out.sort((a, b) -> Double.compare(a.distSqr(at), b.distSqr(at)));
        return out.size() > 16 ? new ArrayList<>(out.subList(0, 16)) : out;
    }

    private static void send(ServerPlayer p, boolean force) {
        AmbientSyncPayload pl = build(p);
        int age = AGE.merge(p.getUUID(), 1, Integer::sum);
        if (!force && pl.equals(LAST.get(p.getUUID())) && age < 5) return;
        LAST.put(p.getUUID(), pl);
        AGE.put(p.getUUID(), 0);
        PacketDistributor.sendToPlayer(p, pl);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 40 != 11) return;
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) send(p, false);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) send(p, true);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) send(p, true);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        LAST.remove(e.getEntity().getUUID());
        AGE.remove(e.getEntity().getUUID());
    }
}
