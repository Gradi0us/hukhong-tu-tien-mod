package com.hukhong.phaptu.beam;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Đăng ký payload của Tinh Hà Quang Trụ (SkillNet.register gọi). */
public final class PhapBeamNet {
    private PhapBeamNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToServer(PhapBeamActionPayload.TYPE, PhapBeamActionPayload.STREAM_CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> PhapBeamService.action(sp, pl.action()));
        });
        r.playToClient(PhapBeamStatePayload.TYPE, PhapBeamStatePayload.STREAM_CODEC,
                (pl, ctx) -> ctx.enqueueWork(() -> com.hukhong.client.fx.PhapBeamClient.update(pl)));
    }
}
