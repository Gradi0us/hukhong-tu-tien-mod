package com.hukhong.phaptu;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: 0 nhấn E (Quang/Lôi/Thổ), 1 nhả E, 2 nhấn T (Thổ: dẫn nổ), 3 nhả T. Server tự phân biệt giữ / bấm nhanh bằng bộ đếm tick. */
public record ThiemBoPayload(int action) implements CustomPacketPayload {
    public static final int PRESS = 0, RELEASE = 1, T_PRESS = 2, T_RELEASE = 3;
    public static final Type<ThiemBoPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "thiem_bo"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ThiemBoPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ThiemBoPayload::action, ThiemBoPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
