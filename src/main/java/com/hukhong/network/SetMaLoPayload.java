package com.hukhong.network;

import com.hukhong.HuKhong;
import com.hukhong.pathrules.MaRules;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: Ma tu đổi lộ (true = Hấp Thu, false = Tu Luyện thường). Server xác thực đạo Ma. */
public record SetMaLoPayload(boolean absorb) implements CustomPacketPayload {
    public static final Type<SetMaLoPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "set_ma_lo"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetMaLoPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, SetMaLoPayload::absorb, SetMaLoPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SetMaLoPayload pl, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            if (MaRules.setLo(sp, pl.absorb()))
                sp.displayClientMessage(Component.translatableWithFallback(pl.absorb() ? "hukhong.ma.lo.set_absorb" : "hukhong.ma.lo.set_normal",
                        pl.absorb() ? "Lộ Hấp Thu" : "Lộ Tu Luyện thường"), true);
        });
    }
}
