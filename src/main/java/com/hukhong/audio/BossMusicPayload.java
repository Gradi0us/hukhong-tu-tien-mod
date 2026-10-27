package com.hukhong.audio;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: bat dau / dung nhac boss. */
public record BossMusicPayload(String track, boolean start) implements CustomPacketPayload {
    public static final Type<BossMusicPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "boss_music"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BossMusicPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BossMusicPayload::track,
            ByteBufCodecs.BOOL, BossMusicPayload::start,
            BossMusicPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BossMusicPayload pl, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.hukhong.client.audio.ClientBossMusic.handle(pl.track(), pl.start()));
    }
}
