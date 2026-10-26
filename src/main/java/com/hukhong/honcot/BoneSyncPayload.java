package com.hukhong.honcot;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: 6 ô Hồn Cốt. */
public record BoneSyncPayload(BoneSlots slots) implements CustomPacketPayload {
    public static final Type<BoneSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "bone_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BoneSyncPayload> STREAM_CODEC =
            StreamCodec.composite(BoneSlots.STREAM_CODEC, BoneSyncPayload::slots, BoneSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BoneSyncPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientBones.set(p.slots()));
    }
}
