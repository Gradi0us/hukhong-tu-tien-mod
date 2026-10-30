package com.hukhong.living;

import com.hukhong.story.world.Faction;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Thien cam (-100..100) cua nguoi choi voi tung NPC (toi da 64 khoa gan nhat) va voi tung phe. */
public final class AffectionService {
    private AffectionService() {}

    public static String factionKey(Faction f) { return "f:" + f.name(); }

    public static String npcKey(UUID npc) { return "n:" + npc; }

    public static int get(ServerPlayer p, String key) {
        return LivingData.get(p.server).of(p.getUUID()).aff.getOrDefault(key, 0);
    }

    public static int faction(ServerPlayer p, Faction f) { return get(p, factionKey(f)); }

    /** Doi thien cam, kep [-100,100]; cat khoa NPC cu khi qua 64. Tra gia tri moi. */
    public static int change(ServerPlayer p, String key, int delta) {
        LivingData d = LivingData.get(p.server);
        Map<String, Integer> m = d.of(p.getUUID()).aff;
        int v = AffectionMath.clamp(m.getOrDefault(key, 0) + delta);
        m.remove(key); // chen lai o cuoi (moi nhat)
        m.put(key, v);
        int npcKeys = 0;
        for (String k : m.keySet()) if (k.startsWith("n:")) npcKeys++;
        for (Iterator<String> it = m.keySet().iterator(); npcKeys > LivingData.MAX_NPC_KEYS && it.hasNext(); ) {
            String k = it.next();
            if (k.startsWith("n:")) { it.remove(); npcKeys--; }
        }
        d.setDirty();
        return v;
    }

    /** Doi ca thien cam voi NPC va phe cua NPC (phe nhan 1/2 do doi, toi thieu 1 neu delta != 0). */
    public static void change(ServerPlayer p, UUID npc, Faction f, int delta) {
        change(p, npcKey(npc), delta);
        int fd = delta / 2;
        if (fd == 0 && delta != 0) fd = delta > 0 ? 1 : -1;
        change(p, factionKey(f), fd);
    }

    /** Gia sau giam gia/phu thu theo thien cam voi phe Thanh (thuong nhan khu cho). */
    public static long adjustPrice(ServerPlayer p, long price) {
        return AffectionMath.adjust(price, faction(p, Faction.THANH));
    }

    public static boolean onCooldown(ServerPlayer p, String key, long now, long ticks) {
        Long last = LivingData.get(p.server).of(p.getUUID()).cd.get(key);
        return last != null && now - last < ticks;
    }

    public static void stamp(ServerPlayer p, String key, long now) {
        LivingData d = LivingData.get(p.server);
        var cd = d.of(p.getUUID()).cd;
        cd.remove(key);
        cd.put(key, now);
        while (cd.size() > 96) { var it = cd.keySet().iterator(); it.next(); it.remove(); }
        d.setDirty();
    }
}
