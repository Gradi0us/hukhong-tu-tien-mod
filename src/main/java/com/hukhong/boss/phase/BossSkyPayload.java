package com.hukhong.boss.phase;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> client: tô màu trời. level 0..1 giữ {@code hold} tick rồi tàn dần trong {@code fade} tick;
 * warMix 0 = tím Hư Hoá, 1 = đỏ máu (chiến sự); rain 0 = không, 1 = mưa máu, 2 = mưa Hư Không, 3 = mưa độc. Server làm tươi định kỳ khi trận còn diễn ra.
 */
public record BossSkyPayload(float level, float warMix, int rain, int hold, int fade) implements CustomPacketPayload {
    public static final Type<BossSkyPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "boss_sky"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BossSkyPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeFloat(p.level); b.writeFloat(p.warMix); b.writeVarInt(p.rain); b.writeVarInt(p.hold); b.writeVarInt(p.fade); },
            b -> new BossSkyPayload(b.readFloat(), b.readFloat(), b.readVarInt(), b.readVarInt(), b.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BossSkyPayload pl, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.hukhong.client.boss.BossSkyFx.set(pl.level(), pl.warMix(), pl.rain(), pl.hold(), pl.fade()));
    }
}
