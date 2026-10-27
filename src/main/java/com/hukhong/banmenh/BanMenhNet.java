package com.hukhong.banmenh;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Đăng ký payload phi kiếm (gọi từ SkillNet.register để dùng chung registrar). */
public final class BanMenhNet {
    private BanMenhNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToServer(BanMenhStrikePayload.TYPE, BanMenhStrikePayload.STREAM_CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> BanMenhStrike.request(sp, pl.targetId()));
        });
        r.playToClient(BanMenhStrikeFxPayload.TYPE, BanMenhStrikeFxPayload.STREAM_CODEC,
                (pl, ctx) -> ctx.enqueueWork(() ->
                        com.hukhong.client.render.orbit.OrbitState.onFx(pl.ownerId(), pl.targetId(), pl.flight(), pl.count())));
    }
}
