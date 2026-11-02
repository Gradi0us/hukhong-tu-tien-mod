package com.hukhong.network;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: 0 nhấn Q (bắt đầu tụ lực Sát Trận), 1 nhả Q (thả cột sáng). Server xác thực toàn bộ. */
public record TranChargePayload(int action) implements CustomPacketPayload {
    public static final int PRESS = 0, RELEASE = 1;
    public static final Type<TranChargePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "tran_charge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TranChargePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, TranChargePayload::action, TranChargePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
