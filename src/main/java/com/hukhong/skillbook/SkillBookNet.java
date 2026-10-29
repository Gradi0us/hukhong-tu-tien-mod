package com.hukhong.skillbook;

import com.hukhong.HuKhong;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Đăng ký payload Công Pháp (mod bus, registrar riêng như HonCotNet). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class SkillBookNet {
    private SkillBookNet() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent e) {
        var r = e.registrar("1");
        r.playToClient(BookSyncPayload.TYPE, BookSyncPayload.STREAM_CODEC, BookSyncPayload::handle);
        r.playToServer(AssignSkillPayload.TYPE, AssignSkillPayload.STREAM_CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> SkillBookService.assign(sp, pl.path(), pl.slot(), pl.id()));
        });
        r.playToClient(OpenScrollPayload.TYPE, OpenScrollPayload.STREAM_CODEC, OpenScrollPayload::handle);
        r.playToServer(ApplyScrollPayload.TYPE, ApplyScrollPayload.STREAM_CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> SkillBookService.applyScroll(sp, pl.id()));
        });
        r.playToServer(UpgradeSkillPayload.TYPE, UpgradeSkillPayload.STREAM_CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> SkillBookService.upgrade(sp, pl.id(), pl.mode()));
        });
    }
}
