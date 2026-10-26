package com.hukhong.flight;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: phím Z bật/tắt bay. */
public record FlightTogglePayload() implements CustomPacketPayload {
    public static final Type<FlightTogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "flight_toggle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlightTogglePayload> STREAM_CODEC =
            StreamCodec.unit(new FlightTogglePayload());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
