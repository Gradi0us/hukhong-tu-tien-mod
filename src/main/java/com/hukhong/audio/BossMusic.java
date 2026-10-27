package com.hukhong.audio;

import com.hukhong.HuKhong;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nhac boss. API cong khai: {@link #start}/{@link #stop} (ep phat/dung cho 1 nguoi choi) va {@link #watch}
 * (boss tu quan: nguoi choi trong ~48 block khi boss co muc tieu thi nghe nhac; roi xa / boss chet / despawn thi dung).
 * Chi chay tren server thread.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class BossMusic {
    private BossMusic() {}

    public static final double RANGE = 48, LEAVE_RANGE = 56;

    /** Trang thai nhac cua 1 nguoi choi: cac boss dang giu nhac (thu tu them) + track dang phat. */
    private static final class Active {
        final LinkedHashMap<UUID, String> bosses = new LinkedHashMap<>();
        String playing;
    }

    private static final Map<UUID, Active> ACTIVE = new HashMap<>();
    private static final List<Watch> WATCHED = new ArrayList<>();
    private static final UUID FORCED = new UUID(0, 0);

    private record Watch(Mob boss, String track) {}

    // ------------------------------------------------------------------ public API
    public static void start(ServerPlayer p, String trackId) {
        Active a = ACTIVE.computeIfAbsent(p.getUUID(), k -> new Active());
        a.bosses.put(FORCED, trackId);
        refresh(p, a);
    }

    public static void stop(ServerPlayer p) {
        Active a = ACTIVE.remove(p.getUUID());
        if (a != null && a.playing != null) send(p, a.playing, false);
    }

    /** Dang ky boss de tu quan nhac. Goi 1 lan khi boss vao level (onAddedToLevel). */
    public static void watch(Mob boss, String track) {
        if (boss.level().isClientSide) return;
        for (Watch w : WATCHED) if (w.boss == boss) return;
        WATCHED.add(new Watch(boss, track));
    }

    // ------------------------------------------------------------------ internals
    private static void send(ServerPlayer p, String track, boolean start) {
        PacketDistributor.sendToPlayer(p, new BossMusicPayload(track, start));
    }

    private static void refresh(ServerPlayer p, Active a) {
        String want = null;
        for (String t : a.bosses.values()) want = t;  // boss them sau cung thang
        if (want == null) {
            if (a.playing != null) send(p, a.playing, false);
            ACTIVE.remove(p.getUUID());
            return;
        }
        if (!want.equals(a.playing)) {
            a.playing = want;
            send(p, want, true);
        }
    }

    private static void release(ServerPlayer p, UUID boss) {
        Active a = ACTIVE.get(p.getUUID());
        if (a == null || a.bosses.remove(boss) == null) return;
        refresh(p, a);
    }

    private static void tickWatch(Watch w) {
        Mob b = w.boss;
        if (!(b.level() instanceof ServerLevel sl)) return;
        UUID bid = b.getUUID();
        if (b.getTarget() != null) {
            for (ServerPlayer p : sl.players()) {
                if (p.isSpectator() || !p.isAlive()) continue;
                if (p.distanceToSqr(b) > RANGE * RANGE) continue;
                Active a = ACTIVE.computeIfAbsent(p.getUUID(), k -> new Active());
                if (!w.track.equals(a.bosses.put(bid, w.track))) refresh(p, a);
            }
        }
        // nguoi choi dang giu boss nay ma da di xa / chet / khac chieu
        for (Map.Entry<UUID, Active> e : new ArrayList<>(ACTIVE.entrySet())) {
            if (!e.getValue().bosses.containsKey(bid)) continue;
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null) { ACTIVE.remove(e.getKey()); continue; }
            if (p.level() != b.level() || !p.isAlive() || p.distanceToSqr(b) > LEAVE_RANGE * LEAVE_RANGE)
                release(p, bid);
        }
    }

    private static void endWatch(Watch w, ServerLevel any) {
        UUID bid = w.boss.getUUID();
        for (Map.Entry<UUID, Active> e : new ArrayList<>(ACTIVE.entrySet())) {
            if (!e.getValue().bosses.containsKey(bid)) continue;
            ServerPlayer p = any.getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null) { ACTIVE.remove(e.getKey()); continue; }
            release(p, bid);
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (WATCHED.isEmpty() || e.getServer().getTickCount() % 20 != 0) return;
        ServerLevel any = e.getServer().overworld();
        for (Iterator<Watch> it = WATCHED.iterator(); it.hasNext(); ) {
            Watch w = it.next();
            LivingEntity b = w.boss;
            if (b.isRemoved() || !b.isAlive()) {
                endWatch(w, any);
                it.remove();
            } else tickWatch(w);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        ACTIVE.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) stop(p);  // chet: tat nhac; hoi sinh trong tam se nghe lai
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        ACTIVE.clear();
        WATCHED.clear();
    }
}
