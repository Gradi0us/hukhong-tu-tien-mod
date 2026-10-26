package com.hukhong.cores;

import com.hukhong.HuKhong;
import com.hukhong.network.CoreSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Áp lại bị động Lõi (modifier transient) khi login/respawn. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CoreEvents {
    private CoreEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) refresh(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) refresh(p);
    }

    private static void refresh(ServerPlayer p) {
        CoreService.applyAll(p);
        CoreSyncPayload.send(p);
    }
}
