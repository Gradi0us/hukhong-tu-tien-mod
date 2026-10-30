package com.hukhong.phaptu;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> chủ nhân: 0 hết xuyên tường, 1 bắt đầu xuyên tường (ticks), 2 mở cửa sổ dẫn nổ T (ticks), 3 đóng cửa sổ dẫn nổ. */
public record ThoStatePayload(int kind, int ticks) implements CustomPacketPayload {
    public static final int PHASE_OFF = 0, PHASE_ON = 1, BLAST_OPEN = 2, BLAST_CLOSED = 3;
    public static final Type<ThoStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "tho_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ThoStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ThoStatePayload::kind, ByteBufCodecs.VAR_INT, ThoStatePayload::ticks, ThoStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
