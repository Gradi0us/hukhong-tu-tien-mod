package com.hukhong.client.minimap;

import com.hukhong.HuKhong;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Phím minimap: M đổi cỡ (giữ M = bản đồ lớn), N bản đồ lớn, B ping thành/tông gần nhất (Shift+B xóa điểm). */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class MapKeys {
    private MapKeys() {}

    private static final String CAT = "key.categories.hukhong";
    public static final KeyMapping SIZE = new KeyMapping("key.hukhong.map_size", InputConstants.KEY_M, CAT);
    public static final KeyMapping FULL = new KeyMapping("key.hukhong.map_full", InputConstants.KEY_N, CAT);
    public static final KeyMapping PING = new KeyMapping("key.hukhong.map_ping", InputConstants.KEY_B, CAT);

    @SubscribeEvent
    public static void onRegister(RegisterKeyMappingsEvent e) {
        e.register(SIZE);
        e.register(FULL);
        e.register(PING);
    }
}
