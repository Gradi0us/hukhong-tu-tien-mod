package com.hukhong.network;

import com.hukhong.HuKhong;
import com.hukhong.age.AfkTracker;
import com.hukhong.age.AgeData;
import com.hukhong.client.ClientCache;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Server -> client: CultivationData + AgeData. */
public record SyncPayload(CultivationData cultivation, AgeData age) implements CustomPacketPayload {
    public static final Type<SyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> STREAM_CODEC = StreamCodec.composite(
            CultivationData.STREAM_CODEC, SyncPayload::cultivation,
            AgeData.STREAM_CODEC, SyncPayload::age,
            SyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void send(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new SyncPayload(
                p.getData(ModAttachments.CULTIVATION), p.getData(ModAttachments.AGE)));
    }

    public static void register(RegisterPayloadHandlersEvent e) {
        var r = e.registrar("1").playToClient(TYPE, STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientCache.update(payload.cultivation(), payload.age())));
        SkillNet.register(r);
        r.playToClient(BreakthroughPayload.TYPE, BreakthroughPayload.STREAM_CODEC, BreakthroughPayload::handle);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) send(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) send(p);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) send(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) AfkTracker.remove(p);
    }
}
