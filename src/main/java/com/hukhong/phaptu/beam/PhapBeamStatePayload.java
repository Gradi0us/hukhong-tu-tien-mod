package com.hukhong.phaptu.beam;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client (người chơi và người đang theo dõi): trạng thái tia của một người chơi. phase = {@link PhapBeamService.Phase} ordinal. */
public record PhapBeamStatePayload(int entityId, int phase, int element, int ticks) implements CustomPacketPayload {
    public static final Type<PhapBeamStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "phap_beam_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhapBeamStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PhapBeamStatePayload::entityId,
            ByteBufCodecs.VAR_INT, PhapBeamStatePayload::phase,
            ByteBufCodecs.VAR_INT, PhapBeamStatePayload::element,
            ByteBufCodecs.VAR_INT, PhapBeamStatePayload::ticks,
            PhapBeamStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
