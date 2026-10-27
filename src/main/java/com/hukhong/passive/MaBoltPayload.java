package com.hukhong.passive;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: Ma Tu tay không đánh thường = bắn tia ma. Server tự raycast và xác thực. */
public record MaBoltPayload() implements CustomPacketPayload {
    public static final Type<MaBoltPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "ma_bolt"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MaBoltPayload> STREAM_CODEC = StreamCodec.unit(new MaBoltPayload());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
