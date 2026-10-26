package com.hukhong.network;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientShake;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: rung camera nhẹ (ticks, cường độ độ). */
public record BreakthroughPayload(int ticks, float strength) implements CustomPacketPayload {
    public static final Type<BreakthroughPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "breakthrough"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BreakthroughPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BreakthroughPayload::ticks,
            ByteBufCodecs.FLOAT, BreakthroughPayload::strength,
            BreakthroughPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Handler tách riêng để server không nạp lớp client. */
    public static void handle(BreakthroughPayload pl, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientShake.start(pl.ticks(), pl.strength()));
    }
}
