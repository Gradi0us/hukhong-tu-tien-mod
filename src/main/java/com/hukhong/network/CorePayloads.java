package com.hukhong.network;

import com.hukhong.HuKhong;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Đăng ký payload Lõi (mod bus). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CorePayloads {
    private CorePayloads() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent e) {
        var r = e.registrar("1");
        r.playToClient(CoreSyncPayload.TYPE, CoreSyncPayload.STREAM_CODEC, CoreSyncPayload::handle);
        r.playToClient(CoreRevealPayload.TYPE, CoreRevealPayload.STREAM_CODEC, CoreRevealPayload::handle);
    }
}
