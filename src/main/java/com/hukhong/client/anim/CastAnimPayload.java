package com.hukhong.client.anim;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Server -> client (người đang theo dõi + chính chủ): người chơi entityId thi triển tư thế anim (ordinal CastAnim). */
public record CastAnimPayload(int entityId, int anim) implements CustomPacketPayload {
    public static final Type<CastAnimPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "cast_anim"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CastAnimPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CastAnimPayload::entityId,
            ByteBufCodecs.VAR_INT, CastAnimPayload::anim,
            CastAnimPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(PayloadRegistrar r) {
        // Tham chiếu lớp client nằm trong lambda -> chỉ nạp khi nhận gói (client).
        r.playToClient(TYPE, STREAM_CODEC, (pl, ctx) -> ctx.enqueueWork(() -> CastAnimClient.play(pl.entityId(), pl.anim())));
    }
}
