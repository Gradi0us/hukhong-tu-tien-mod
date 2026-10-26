package com.hukhong.honcot;

import com.hukhong.HuKhong;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Đăng ký payload Hồn Cốt (mod bus). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class HonCotNet {
    private HonCotNet() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent e) {
        var r = e.registrar("1");
        r.playToClient(BoneSyncPayload.TYPE, BoneSyncPayload.STREAM_CODEC, BoneSyncPayload::handle);
        r.playToServer(BoneUnequipPayload.TYPE, BoneUnequipPayload.STREAM_CODEC, BoneUnequipPayload::handle);
    }
}
