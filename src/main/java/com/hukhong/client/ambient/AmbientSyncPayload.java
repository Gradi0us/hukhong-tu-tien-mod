package com.hukhong.client.ambient;

import com.hukhong.HuKhong;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

/**
 * Server -> client: dữ liệu khí quyển quanh người chơi. chunks = chunk Hư Hóa lân cận (ChunkPos.asLong), hearts = Tâm Hư Hóa,
 * flags bit0 = xâm lăng (hoặc báo trước), bit1 = đột kích Hư Không của người chơi này; (cx,cz) = tâm sự kiện;
 * bicanhTier = ordinal BiCanhTier hoặc -1. Lớp này không chạm lớp client-only.
 */
public record AmbientSyncPayload(List<Long> chunks, List<BlockPos> hearts, int flags, int cx, int cz, int bicanhTier)
        implements CustomPacketPayload {
    public static final int F_INVASION = 1, F_RAID = 2;

    public static final Type<AmbientSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "ambient_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AmbientSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(1200)), AmbientSyncPayload::chunks,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(16)), AmbientSyncPayload::hearts,
            ByteBufCodecs.VAR_INT, AmbientSyncPayload::flags,
            ByteBufCodecs.VAR_INT, AmbientSyncPayload::cx,
            ByteBufCodecs.VAR_INT, AmbientSyncPayload::cz,
            ByteBufCodecs.VAR_INT, AmbientSyncPayload::bicanhTier,
            AmbientSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(PayloadRegistrar r) {
        r.playToClient(TYPE, STREAM_CODEC, (pl, ctx) -> ctx.enqueueWork(() -> AmbientState.apply(pl)));
    }
}
