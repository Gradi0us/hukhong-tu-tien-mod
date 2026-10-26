package com.hukhong.client.screen.core;

import com.hukhong.HuKhong;
import com.hukhong.menu.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class CoreScreens {
    private CoreScreens() {}

    @SubscribeEvent
    public static void onRegister(RegisterMenuScreensEvent e) {
        e.register(ModMenus.CORE_ALTAR.get(), CoreAltarScreen::new);
    }
}
