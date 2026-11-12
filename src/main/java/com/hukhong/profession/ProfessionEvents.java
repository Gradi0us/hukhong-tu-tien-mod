package com.hukhong.profession;

import com.hukhong.HuKhong;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Dong bo nghe phu khi dang nhap / hoi sinh (attachment copyOnDeath giu du lieu qua chet). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class ProfessionEvents {
    private ProfessionEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) ProfessionNet.send(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) ProfessionNet.send(p);
    }
}
