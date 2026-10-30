package com.hukhong.net;

import com.hukhong.HuKhong;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * Ban dev (./gradlew runClient) khong dang nhap Microsoft nen may chu (mo LAN tu ban dev) tu choi
 * "Phien choi khong hop le". O moi truong dev, tat xac minh tai khoan de nguoi choi khac vao duoc.
 * Ban phat hanh (jar trong launcher that) khong bi anh huong.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class DevOfflineMode {
    private DevOfflineMode() {}

    @SubscribeEvent
    public static void onStart(ServerStartingEvent e) {
        if (!FMLEnvironment.production) e.getServer().setUsesAuthentication(false);
    }
}
