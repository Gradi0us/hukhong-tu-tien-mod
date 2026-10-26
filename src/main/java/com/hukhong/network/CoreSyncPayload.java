package com.hukhong.network;

import com.hukhong.HuKhong;
import com.hukhong.client.screen.profile.ClientCores;
import com.hukhong.cores.CoreAttachments;
import com.hukhong.cores.CoreSlots;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: Lõi đã chọn (không kèm phiên roll đang chờ). */
public record CoreSyncPayload(CoreSlots slots) implements CustomPacketPayload {
    public static final Type<CoreSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "core_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CoreSyncPayload> STREAM_CODEC =
            StreamCodec.composite(CoreSlots.SYNC_CODEC, CoreSyncPayload::slots, CoreSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void send(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new CoreSyncPayload(p.getData(CoreAttachments.CORE_SLOTS)));
    }

    public static void handle(CoreSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientCores.set(payload.slots()));
    }
}
