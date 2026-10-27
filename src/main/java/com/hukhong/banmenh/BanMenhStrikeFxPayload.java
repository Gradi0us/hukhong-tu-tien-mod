package com.hukhong.banmenh;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client (tracking + self): kiếm sau vai chủ phóng tới mục tiêu rồi bay về (chỉ hình ảnh). */
public record BanMenhStrikeFxPayload(int ownerId, int targetId, int flight, int count) implements CustomPacketPayload {
    public static final Type<BanMenhStrikeFxPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "banmenh_strike_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BanMenhStrikeFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BanMenhStrikeFxPayload::ownerId,
            ByteBufCodecs.VAR_INT, BanMenhStrikeFxPayload::targetId,
            ByteBufCodecs.VAR_INT, BanMenhStrikeFxPayload::flight,
            ByteBufCodecs.VAR_INT, BanMenhStrikeFxPayload::count,
            BanMenhStrikeFxPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
