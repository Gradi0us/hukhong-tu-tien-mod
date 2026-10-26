package com.hukhong.network;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: yêu cầu cast slot 0..3 (Q/E/R/T). Server xác thực toàn bộ. */
public record CastSkillPayload(int slot) implements CustomPacketPayload {
    public static final Type<CastSkillPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "cast_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CastSkillPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, CastSkillPayload::slot, CastSkillPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
