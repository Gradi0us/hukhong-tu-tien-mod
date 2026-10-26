package com.hukhong.flight;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Đăng ký payload bay (gọi từ SkillNet.register để dùng chung registrar). */
public final class FlightNet {
    private FlightNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToClient(FlightSyncPayload.TYPE, FlightSyncPayload.STREAM_CODEC,
                (pl, ctx) -> ctx.enqueueWork(() -> FlightClientState.set(pl.entityId(), pl.mode(), pl.pairs())));
        r.playToServer(FlightTogglePayload.TYPE, FlightTogglePayload.STREAM_CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> FlightService.toggle(sp));
        });
        r.playToServer(FlightJumpPayload.TYPE, FlightJumpPayload.STREAM_CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> FlightService.airJump(sp));
        });
    }
}
