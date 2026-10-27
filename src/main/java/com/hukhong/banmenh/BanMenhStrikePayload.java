package com.hukhong.banmenh;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: nhấn đánh khi cầm Bản Mệnh Kiếm, nhắm entity trong tầm phi kiếm. */
public record BanMenhStrikePayload(int targetId) implements CustomPacketPayload {
    public static final Type<BanMenhStrikePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "banmenh_strike"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BanMenhStrikePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, BanMenhStrikePayload::targetId, BanMenhStrikePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
