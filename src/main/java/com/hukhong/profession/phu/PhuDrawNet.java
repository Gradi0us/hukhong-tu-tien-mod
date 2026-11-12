package com.hukhong.profession.phu;

import com.hukhong.HuKhong;
import com.hukhong.profession.station.StationMenu;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/** Client -> server: ket qua 1 lan ve phu. trace = (x, y, t_ms) x n, n <= 200. */
public final class PhuDrawNet {
    private PhuDrawNet() {}

    private static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, p); }

    public record Submit(List<Integer> trace, int durationMs) implements CustomPacketPayload {
        public static final Type<Submit> TYPE = new Type<>(id("phu_draw_submit"));
        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Submit> CODEC = StreamCodec.composite(
                ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_INT, PhuPattern.MAX_SAMPLES * 3), Submit::trace,
                ByteBufCodecs.VAR_INT, Submit::durationMs, Submit::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(PayloadRegistrar r) {
        r.playToServer(Submit.TYPE, Submit.CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp)
                ctx.enqueueWork(() -> {
                    if (sp.containerMenu instanceof StationMenu m) m.phuDraw(sp, pl.trace(), pl.durationMs());
                });
        });
    }
}
