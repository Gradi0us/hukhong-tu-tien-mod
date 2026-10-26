package com.hukhong.network;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: bật/tắt Thế Tu Luyện (phím G). */
public record ToggleStancePayload() implements CustomPacketPayload {
    public static final Type<ToggleStancePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "toggle_stance"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleStancePayload> STREAM_CODEC =
            StreamCodec.unit(new ToggleStancePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
