package com.hukhong.age;

import com.hukhong.core.ModAttachments;
import com.hukhong.network.SyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Tick tuổi. Chưa có Thọ Nguyên (TODO onMonth: kiểm Thiên Nhân Ngũ Suy; R1: không chết vì tuổi). */
public final class AgeService {
    private AgeService() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        if (AfkTracker.check(p)) return;
        AgeData a = p.getData(ModAttachments.AGE);
        long before = a.onlineTicks(), after = before + 20;
        p.setData(ModAttachments.AGE, a.withOnlineTicks(after));
        if (after / AgeData.TICKS_PER_MONTH > before / AgeData.TICKS_PER_MONTH) SyncPayload.send(p);
    }
}
