package com.hukhong.network;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: thế Trận Tu hiện tại, số tick còn lại của "chọn thế", Kiệt Lực, số tick còn lại của Kiệt Sức. */
public record TranSyncPayload(int stance, int armedTicks, int stacks, int exhaustTicks) implements CustomPacketPayload {
    public static final Type<TranSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "tran_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TranSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TranSyncPayload::stance,
            ByteBufCodecs.VAR_INT, TranSyncPayload::armedTicks,
            ByteBufCodecs.VAR_INT, TranSyncPayload::stacks,
            ByteBufCodecs.VAR_INT, TranSyncPayload::exhaustTicks,
            TranSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
