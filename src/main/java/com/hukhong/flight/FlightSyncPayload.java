package com.hukhong.flight;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client (tracking + self): kiểu bay đang bật của một người chơi. mode = -1: tắt. */
public record FlightSyncPayload(int entityId, int mode, int pairs) implements CustomPacketPayload {
    public static final Type<FlightSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "flight_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlightSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FlightSyncPayload::entityId,
            ByteBufCodecs.VAR_INT, FlightSyncPayload::mode,
            ByteBufCodecs.VAR_INT, FlightSyncPayload::pairs,
            FlightSyncPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
