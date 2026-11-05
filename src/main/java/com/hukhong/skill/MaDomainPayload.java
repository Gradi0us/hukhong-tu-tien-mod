package com.hukhong.skill;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Server -> người chơi xung quanh: thực thể entityId đang mở Ma Vực thêm ticks tick (0 = tắt) -> client vẽ cánh quỷ + sừng. */
@EventBusSubscriber(modid = HuKhong.MODID)
public record MaDomainPayload(int entityId, int ticks) implements CustomPacketPayload {
    public static final Type<MaDomainPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "ma_domain"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MaDomainPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MaDomainPayload::entityId, ByteBufCodecs.VAR_INT, MaDomainPayload::ticks, MaDomainPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent e) {
        e.registrar("1").playToClient(TYPE, STREAM_CODEC,
                (pl, ctx) -> ctx.enqueueWork(() -> com.hukhong.client.fx.MaDomainClient.set(pl.entityId(), pl.ticks())));
    }
}
