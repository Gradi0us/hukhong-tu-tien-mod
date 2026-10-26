package com.hukhong.gates;

import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.network.SyncPayload;
import net.minecraft.server.level.ServerPlayer;

/** Cổng tài nguyên duy nhất: mọi chi phí/hồi tài nguyên đi qua đây. */
public final class Resources {
    private Resources() {}

    /** Hệ số chi phí đa đạo: 1 đạo x1, 2 đạo x2, 3+ đạo x3. */
    public static int pathMultiplier(CultivationData d) {
        return Math.max(1, Math.min(3, d.paths().size()));
    }

    public static int costFor(CultivationData d, int baseCost) { return baseCost * pathMultiplier(d); }

    public static boolean tryConsume(ServerPlayer p, int baseCost) {
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        int c = costFor(d, baseCost);
        if (d.resource() < c) return false;
        set(p, d, d.resource() - c);
        return true;
    }

    /** Cộng (hoặc trừ nếu âm) có kẹp [0,max]; chỉ sync khi đổi. */
    public static void add(ServerPlayer p, int amount) {
        CultivationData d = p.getData(ModAttachments.CULTIVATION);
        int v = Math.max(0, Math.min(d.resourceMax(), d.resource() + amount));
        if (v != d.resource()) set(p, d, v);
    }

    private static void set(ServerPlayer p, CultivationData d, int v) {
        p.setData(ModAttachments.CULTIVATION, new CultivationData(d.level(), d.exp(), v, d.resourceMax(),
                d.paths(), d.aptitude(), d.bottleneck()));
        SyncPayload.send(p);
    }
}
