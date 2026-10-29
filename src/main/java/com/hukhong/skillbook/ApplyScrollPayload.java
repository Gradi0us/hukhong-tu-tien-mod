package com.hukhong.skillbook;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: dung Truc Nang Cap len ky nang id (server xac thuc lai moi thu). */
public record ApplyScrollPayload(String id) implements CustomPacketPayload {
    public static final Type<ApplyScrollPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "apply_scroll"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ApplyScrollPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.stringUtf8(48), ApplyScrollPayload::id, ApplyScrollPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
