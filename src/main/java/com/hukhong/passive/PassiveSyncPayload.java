package com.hukhong.passive;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: số tầng nội tại (Kiếm n/4, Đạo n/5, Thể n/15, Ma n/5 trên mục tiêu gần nhất). */
public record PassiveSyncPayload(int kiem, int dao, int the, int ma) implements CustomPacketPayload {
    public static final Type<PassiveSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "passive_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PassiveSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PassiveSyncPayload::kiem,
            ByteBufCodecs.VAR_INT, PassiveSyncPayload::dao,
            ByteBufCodecs.VAR_INT, PassiveSyncPayload::the,
            ByteBufCodecs.VAR_INT, PassiveSyncPayload::ma,
            PassiveSyncPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
