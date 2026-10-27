package com.hukhong.story.objective;

import com.hukhong.HuKhong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: mục tiêu chính hiện tại (id rỗng = xóa). */
public record ObjectiveSyncPayload(ObjectiveData data) implements CustomPacketPayload {
    public static final Type<ObjectiveSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "objective_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ObjectiveSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, p -> p.data().id(),
            ComponentSerialization.STREAM_CODEC, p -> p.data().text(),
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), p -> p.data().target(),
            ByteBufCodecs.optional(ResourceKey.streamCodec(Registries.DIMENSION)), p -> p.data().dim(),
            (id, t, pos, dim) -> new ObjectiveSyncPayload(new ObjectiveData(id, t, pos, dim)));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ObjectiveSyncPayload pl, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.hukhong.client.objective.ClientObjective.set(pl.data()));
    }
}
