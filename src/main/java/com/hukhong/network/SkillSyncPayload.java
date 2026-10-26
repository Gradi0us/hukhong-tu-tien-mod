package com.hukhong.network;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Server -> client: thế tu luyện + hồi chiêu còn lại / tổng (tick) cho 4 slot. */
public record SkillSyncPayload(boolean stance, List<Integer> remaining, List<Integer> total)
        implements CustomPacketPayload {
    public static final Type<SkillSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "skill_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SkillSyncPayload::stance,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), SkillSyncPayload::remaining,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), SkillSyncPayload::total,
            SkillSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
