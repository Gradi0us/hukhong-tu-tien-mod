package com.hukhong.network;

import com.hukhong.HuKhong;
import com.hukhong.client.hud.ClientMaState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: Ác Tính, đếm ngược Tẩu Hỏa (tick, -1 = tắt), Mất Kiểm Soát, Huyết Tế. */
public record MaSyncPayload(float acTinh, int countdown, boolean lost, int huyetTe) implements CustomPacketPayload {
    public static final Type<MaSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "ma_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MaSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, MaSyncPayload::acTinh,
            ByteBufCodecs.VAR_INT, MaSyncPayload::countdown,
            ByteBufCodecs.BOOL, MaSyncPayload::lost,
            ByteBufCodecs.VAR_INT, MaSyncPayload::huyetTe,
            MaSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(MaSyncPayload pl, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientMaState.update(pl));
    }
}
