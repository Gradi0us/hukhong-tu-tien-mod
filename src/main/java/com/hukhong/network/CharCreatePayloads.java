package com.hukhong.network;

import com.hukhong.HuKhong;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Đăng ký payload tạo nhân vật (mod bus). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CharCreatePayloads {
    private CharCreatePayloads() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent e) {
        var r = e.registrar("1");
        r.playToClient(OpenCharCreatePayload.TYPE, OpenCharCreatePayload.STREAM_CODEC, OpenCharCreatePayload::handle);
        r.playToServer(ConfirmCharCreatePayload.TYPE, ConfirmCharCreatePayload.STREAM_CODEC, ConfirmCharCreatePayload::handle);
    }
}
