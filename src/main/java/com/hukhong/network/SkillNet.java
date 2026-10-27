package com.hukhong.network;

import com.hukhong.client.keybind.ClientSkillState;
import com.hukhong.skill.SkillService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Đăng ký payload kỹ năng (gọi từ SyncPayload.register để dùng chung 1 registrar). */
public final class SkillNet {
    private SkillNet() {}

    public static void register(PayloadRegistrar r) {
        com.hukhong.flight.FlightNet.register(r);
        com.hukhong.audio.AudioNet.register(r);
        com.hukhong.banmenh.BanMenhNet.register(r);
        com.hukhong.artifact.ArtifactNet.register(r);
        r.playToClient(SkillSyncPayload.TYPE, SkillSyncPayload.STREAM_CODEC,
                (pl, ctx) -> ctx.enqueueWork(() -> ClientSkillState.update(pl)));
        r.playToClient(FxPayload.TYPE, FxPayload.STREAM_CODEC, FxPayload::handle);
        r.playToClient(MaSyncPayload.TYPE, MaSyncPayload.STREAM_CODEC, MaSyncPayload::handle);
        r.playToClient(MapMarkersPayload.TYPE, MapMarkersPayload.STREAM_CODEC, MapMarkersPayload::handle);
        r.playToClient(com.hukhong.story.objective.ObjectiveSyncPayload.TYPE, com.hukhong.story.objective.ObjectiveSyncPayload.STREAM_CODEC, com.hukhong.story.objective.ObjectiveSyncPayload::handle);
        r.playToClient(com.hukhong.story.objective.ObjectiveCompletePayload.TYPE, com.hukhong.story.objective.ObjectiveCompletePayload.STREAM_CODEC, com.hukhong.story.objective.ObjectiveCompletePayload::handle);
        r.playToServer(SetMaLoPayload.TYPE, SetMaLoPayload.STREAM_CODEC, SetMaLoPayload::handle);
        r.playToServer(CastSkillPayload.TYPE, CastSkillPayload.STREAM_CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> SkillService.cast(sp, pl.slot()));
        });
        r.playToServer(CyclePagePayload.TYPE, CyclePagePayload.STREAM_CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> SkillService.cyclePage(sp));
        });
        r.playToServer(ToggleStancePayload.TYPE, ToggleStancePayload.STREAM_CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> SkillService.toggleStance(sp));
        });
    }
}
