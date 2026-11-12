package com.hukhong.living;

import com.hukhong.HuKhong;
import com.hukhong.city.ambient.AmbientNpc;
import com.hukhong.guild.EscortMover;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dan lang hop tac cho nhiem vu ESCORT: NPC di bo theo tuyen {@link EscortMover} toi q.target,
 * dung doi khi nguoi choi tut xa, tiep tuc khi nguoi choi lai gan. Walker duoc tinh moi server tick.
 * Khoa theo UUID cua NPC. Entry tu go khi NPC mat, nguoi choi offline, hoac nhiem vu khong con trong danh sach.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class LivingEscort {
    private LivingEscort() {}

    /** Tag danh dau NPC dang hop tac; NpcLife bo qua no de khong bat/tat AI xen ke voi walker. */
    public static final String TAG = "hk_escorting";

    private static final class Entry {
        final UUID player;
        final EscortMover.Walker walker;
        boolean arrived;
        Entry(UUID player, EscortMover.Walker walker) { this.player = player; this.walker = walker; }
    }

    private static final Map<UUID, Entry> ENTRIES = new HashMap<>();

    /** Tao walker moi cho NPC, xuat phat tu vi tri hien tai. */
    public static void start(ServerPlayer p, AmbientNpc n, BlockPos dest) {
        n.addTag(TAG);
        ENTRIES.put(n.getUUID(), new Entry(p.getUUID(), new EscortMover.Walker(p.serverLevel(), n.position(), dest)));
    }

    /** Tao walker neu chua co (sau khi restart server hoac NPC sinh lai). */
    public static void ensure(ServerPlayer p, AmbientNpc n, BlockPos dest) {
        if (!ENTRIES.containsKey(n.getUUID())) start(p, n, dest);
    }

    public static void remove(UUID npc) {
        if (npc != null) ENTRIES.remove(npc);
    }

    /** NPC da toi dich (trong khoang ARRIVE cua walker). */
    public static boolean arrived(UUID npc) {
        Entry e = npc == null ? null : ENTRIES.get(npc);
        return e != null && e.arrived;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (ENTRIES.isEmpty()) return;
        MinecraftServer s = e.getServer();
        for (var it = ENTRIES.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            Entry x = en.getValue();
            ServerPlayer p = s.getPlayerList().getPlayer(x.player);
            Entity ent = p == null ? null : p.serverLevel().getEntity(en.getKey());
            if (!(ent instanceof AmbientNpc n) || !n.isAlive() || !activeFor(p, n.getUUID())) { it.remove(); continue; }
            if (x.walker.tick(n, p)) x.arrived = true;
        }
    }

    private static boolean activeFor(ServerPlayer p, UUID npc) {
        for (Quest q : QuestService.quests(p)) if (npc.equals(q.entity)) return true;
        return false;
    }
}
