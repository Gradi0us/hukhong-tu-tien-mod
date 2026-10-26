package com.hukhong.network;

import com.hukhong.client.keybind.ClientSkillState;
import com.hukhong.skill.SkillService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Đăng ký payload kỹ năng (gọi từ SyncPayload.register để dùng chung 1 registrar). */
public final class SkillNet {
    private SkillNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToClient(SkillSyncPayload.TYPE, SkillSyncPayload.STREAM_CODEC,
                (pl, ctx) -> ctx.enqueueWork(() -> ClientSkillState.update(pl)));
        r.playToClient(MaSyncPayload.TYPE, MaSyncPayload.STREAM_CODEC, MaSyncPayload::handle);
        r.playToServer(CastSkillPayload.TYPE, CastSkillPayload.STREAM_CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> SkillService.cast(sp, pl.slot()));
        });
        r.playToServer(ToggleStancePayload.TYPE, ToggleStancePayload.STREAM_CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> SkillService.toggleStance(sp));
        });
    }
}
