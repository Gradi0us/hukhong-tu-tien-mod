package com.hukhong.story.objective;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: toast "Hoàn thành: ...". */
public record ObjectiveCompletePayload(Component message) implements CustomPacketPayload {
    public static final Type<ObjectiveCompletePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "objective_complete"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ObjectiveCompletePayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, ObjectiveCompletePayload::message, ObjectiveCompletePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ObjectiveCompletePayload pl, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.hukhong.client.objective.ClientObjective.toast(pl.message()));
    }
}
