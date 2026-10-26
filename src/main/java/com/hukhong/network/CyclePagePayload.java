package com.hukhong.network;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: phím V đổi trang kỹ năng đa đạo. */
public record CyclePagePayload() implements CustomPacketPayload {
    public static final Type<CyclePagePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "cycle_page"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CyclePagePayload> STREAM_CODEC =
            StreamCodec.unit(new CyclePagePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
