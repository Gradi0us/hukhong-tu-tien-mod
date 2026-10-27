package com.hukhong.network;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import com.hukhong.society.SectDef;
import com.hukhong.society.SectEvents;
import com.hukhong.society.SectSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Gửi danh sách thành/tông cho minimap: khi đăng nhập/hồi sinh/đổi chiều + khi danh sách đổi (kiểm 5 giây/lần). Chỉ đọc API công khai. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class MapMarkerSync {
    private MapMarkerSync() {}

    private static int lastHash = 0;
    private static final Set<UUID> SENT = new HashSet<>();

    static List<MapMarkersPayload.Marker> collect(MinecraftServer server) {
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        List<MapMarkersPayload.Marker> out = new ArrayList<>();
        if (ow == null) return out;
        for (CityDef d : CityDef.ALL) {
            BlockPos c = CityService.builtCenter(ow, d);
            if (c != null) out.add(new MapMarkersPayload.Marker(MapMarkersPayload.CITY, d.id(), c.getX(), c.getZ(), true));
            else if (d.index() != 0) { // thành gốc chưa dựng: chooseSite quét biome -> bỏ qua
                BlockPos s = CityService.site(ow, d);
                out.add(new MapMarkersPayload.Marker(MapMarkersPayload.CITY, d.id(), s.getX(), s.getZ(), false));
            }
        }
        SectSavedData sects = SectSavedData.get(ow);
        for (SectDef d : SectDef.ALL) {
            if (d.hidden()) continue; // tông ẩn: không lộ vị trí
            BlockPos c = sects.center(d.id());
            boolean built = c != null;
            BlockPos p = built ? c : SectEvents.site(ow, d);
            out.add(new MapMarkersPayload.Marker(MapMarkersPayload.SECT, d.id(), p.getX(), p.getZ(), built));
        }
        for (var h : com.hukhong.guild.GuildHalls.get(ow).all().entrySet()) // Hiep Hoi Mao Hiem Gia
            out.add(new MapMarkersPayload.Marker(MapMarkersPayload.GUILD, h.getKey(), h.getValue().getX(), h.getValue().getZ(), true));
        return out;
    }

    private static void send(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new MapMarkersPayload(collect(p.server)));
        SENT.add(p.getUUID());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) send(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) send(p);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) send(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        SENT.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 100 != 17) return;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        List<MapMarkersPayload.Marker> now = collect(server);
        int h = now.hashCode();
        boolean changed = h != lastHash;
        lastHash = h;
        MapMarkersPayload pl = null;
        for (ServerPlayer p : players) {
            if (!changed && SENT.contains(p.getUUID())) continue;
            if (pl == null) pl = new MapMarkersPayload(now);
            PacketDistributor.sendToPlayer(p, pl);
            SENT.add(p.getUUID());
        }
    }
}
