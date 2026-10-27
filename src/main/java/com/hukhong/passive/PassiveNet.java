package com.hukhong.passive;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Đăng ký payload nội tại (gọi từ SkillNet.register dùng chung registrar). */
public final class PassiveNet {
    private PassiveNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToClient(PassiveSyncPayload.TYPE, PassiveSyncPayload.STREAM_CODEC,
                (pl, ctx) -> ctx.enqueueWork(() -> com.hukhong.passive.client.ClientPassiveState.update(pl)));
        r.playToServer(MaBoltPayload.TYPE, MaBoltPayload.STREAM_CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> PassiveService.maBolt(sp));
        });
    }
}
