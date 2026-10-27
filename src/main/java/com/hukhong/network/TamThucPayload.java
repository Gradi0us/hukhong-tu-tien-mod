package com.hukhong.network;

import com.hukhong.HuKhong;
import com.hukhong.client.TamThucClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: mode 0 = toi dan trong ticks (giu den); mode 1 = bat dau tu den, sang dan trong ticks. */
public record TamThucPayload(int mode, int ticks) implements CustomPacketPayload {
    public static final Type<TamThucPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "tam_thuc_fade"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TamThucPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TamThucPayload::mode,
            ByteBufCodecs.VAR_INT, TamThucPayload::ticks,
            TamThucPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(TamThucPayload pl, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> TamThucClient.fade(pl.mode(), pl.ticks()));
    }
}
