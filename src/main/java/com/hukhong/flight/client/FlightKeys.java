package com.hukhong.flight.client;

import com.hukhong.HuKhong;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Phím Z (đổi được) bật/tắt bay. R25 đã dùng Q/E/R/T/G/V/K. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class FlightKeys {
    private FlightKeys() {}

    public static final KeyMapping TOGGLE =
            new KeyMapping("key.hukhong.flight_toggle", InputConstants.KEY_Z, "key.categories.hukhong");

    @SubscribeEvent
    public static void onRegister(RegisterKeyMappingsEvent e) { e.register(TOGGLE); }
}
