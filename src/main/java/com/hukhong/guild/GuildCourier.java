package com.hukhong.guild;

import com.hukhong.city.TradeDistrict;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Tin su giao hang: NPC di bo toi diem den, nguoi choi phai bao ve. Trang thai chi o bo nho (mat khi tat server -> nhiem vu huy). */
@EventBusSubscriber(modid = com.hukhong.HuKhong.MODID)
public final class GuildCourier {
    private GuildCourier() {}

    public static final String TAG = "hukhong_guild_courier";
    private static final long TIMEOUT_TICKS = 6000;

    private record Run(UUID entity, BlockPos dest, int slot, long deadline) {}

    private static final Map<UUID, Run> RUNS = new HashMap<>();

    public static boolean active(UUID player) { return RUNS.containsKey(player); }

    public static void reset() { RUNS.clear(); WALKERS.clear(); }

    private static final Map<UUID, EscortMover.Walker> WALKERS = new HashMap<>();

    /** Moi tick: tin su di theo tuyen da tinh (xem EscortMover), dung doi khi nguoi choi tut >12 block. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (WALKERS.isEmpty()) return;
        for (var en : WALKERS.entrySet()) {
            Run r = RUNS.get(en.getKey());
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(en.getKey());
            if (r == null || p == null) continue;
            Entity ent = p.serverLevel().getEntity(r.entity());
            if (ent instanceof Villager v && v.isAlive()) en.getValue().tick(v, p);
        }
        WALKERS.keySet().removeIf(k -> !RUNS.containsKey(k));
    }

    /** Sinh tin su canh nguoi choi, diem den cach dist block (huong ngau nhien tat dinh theo thoi gian). */
    public static boolean start(ServerPlayer p, int slot, int dist) {
        cancel(p.getUUID());
        ServerLevel l = p.serverLevel();
        Villager v = EntityType.VILLAGER.create(l);
        if (v == null) return false;
        double ang = new java.util.Random(l.getGameTime() ^ p.getUUID().getLeastSignificantBits()).nextDouble() * Math.PI * 2;
        int dx = (int) Math.round(p.getX() + Math.cos(ang) * dist), dz = (int) Math.round(p.getZ() + Math.sin(ang) * dist);
        int dy = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
        v.moveTo(p.getX() + 1.5, p.getY(), p.getZ(), 0f, 0f);
        v.setPersistenceRequired();
        v.addTag(TAG);
        v.addTag(TradeDistrict.TAG);
        v.setCustomName(Component.translatable("hukhong.guild.courier"));
        v.setCustomNameVisible(true);
        v.setGlowingTag(true);
        if (!l.addFreshEntity(v)) return false;
        RUNS.put(p.getUUID(), new Run(v.getUUID(), new BlockPos(dx, dy, dz), slot, l.getGameTime() + TIMEOUT_TICKS));
        WALKERS.put(p.getUUID(), new EscortMover.Walker(l, v.position(), new BlockPos(dx, dy, dz)));
        return true;
    }

    public static void cancel(UUID player) {
        Run r = RUNS.remove(player);
        if (r == null) return;
        // entity bi go o tick ke tiep neu con (dang o level khac thi bo qua)
        CANCELLED.add(r.entity());
    }

    private static final java.util.Set<UUID> CANCELLED = new java.util.HashSet<>();

    /** Moi 20 tick: ra lenh di chuyen, kiem tra toi noi / chet / het gio. */
    public static void tick(net.minecraft.server.MinecraftServer server) {
        for (ServerLevel l : server.getAllLevels()) {
            if (CANCELLED.isEmpty()) break;
            for (Iterator<UUID> it = CANCELLED.iterator(); it.hasNext(); ) {
                Entity e = l.getEntity(it.next());
                if (e != null) { e.discard(); it.remove(); }
            }
        }
        CANCELLED.clear();
        for (Iterator<Map.Entry<UUID, Run>> it = RUNS.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            Run r = en.getValue();
            if (p == null) { it.remove(); continue; }
            ServerLevel l = p.serverLevel();
            Entity e = l.getEntity(r.entity());
            if (e == null || !e.isAlive()) {
                it.remove(); // chet / mat tich

                GuildService.onCourierLost(p, r.slot());
                continue;
            }
            if (l.getGameTime() > r.deadline()) { e.discard(); it.remove(); GuildService.onCourierLost(p, r.slot()); continue; }
            double dx = e.getX() - (r.dest().getX() + 0.5), dz = e.getZ() - (r.dest().getZ() + 0.5);
            if (dx * dx + dz * dz < 25) {
                e.discard();
                it.remove();
                GuildService.onCourierArrived(p, r.slot());
                continue;
            }
            EscortMover.Walker w = WALKERS.get(en.getKey());
            if (w != null && w.waiting() && l.getGameTime() % 100 < 20) p.displayClientMessage(Component.translatable("hukhong.guild.courier_wait"), true);
        }
    }

}
