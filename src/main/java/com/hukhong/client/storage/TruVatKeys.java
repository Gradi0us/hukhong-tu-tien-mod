package com.hukhong.client.storage;

import com.hukhong.HuKhong;
import com.hukhong.storage.TruVatOpenPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Phím B mở kho Nhẫn Trữ Vật. */
public final class TruVatKeys {
    private TruVatKeys() {}

    public static final KeyMapping OPEN =
            new KeyMapping("key.hukhong.tru_vat", InputConstants.KEY_B, "key.categories.hukhong");

    @EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
    public static final class Reg {
        @SubscribeEvent
        public static void onRegister(RegisterKeyMappingsEvent e) { e.register(OPEN); }

        @SubscribeEvent
        public static void onTick(ClientTickEvent.Pre e) {
            Minecraft mc = Minecraft.getInstance();
            while (OPEN.consumeClick()) {
                if (mc.player != null && mc.screen == null) PacketDistributor.sendToServer(TruVatOpenPayload.INSTANCE);
            }
        }
    }
}
