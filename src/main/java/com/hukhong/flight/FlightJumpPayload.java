package com.hukhong.flight;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: nhấn nhảy giữa không trung (Thể tu đạp không). */
public record FlightJumpPayload() implements CustomPacketPayload {
    public static final Type<FlightJumpPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "flight_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlightJumpPayload> STREAM_CODEC =
            StreamCodec.unit(new FlightJumpPayload());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
