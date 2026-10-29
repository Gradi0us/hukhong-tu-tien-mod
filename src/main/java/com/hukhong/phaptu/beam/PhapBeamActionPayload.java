package com.hukhong.phaptu.beam;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: 0 bắt đầu nạp (nhấn T), 1 nhả T, 2 bắt đầu bắn (nhấn chuột phải khi sẵn sàng), 3 nhả chuột phải. Server xác thực toàn bộ. */
public record PhapBeamActionPayload(int action) implements CustomPacketPayload {
    public static final int CHARGE_START = 0, CHARGE_STOP = 1, FIRE_START = 2, FIRE_STOP = 3;
    public static final Type<PhapBeamActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "phap_beam_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhapBeamActionPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, PhapBeamActionPayload::action, PhapBeamActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
