package com.hukhong.storage;

import com.hukhong.HuKhong;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: phím B mở kho trữ vật. */
public record TruVatOpenPayload() implements CustomPacketPayload {
    public static final TruVatOpenPayload INSTANCE = new TruVatOpenPayload();
    public static final Type<TruVatOpenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "tru_vat_open"));
    public static final StreamCodec<ByteBuf, TruVatOpenPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(TruVatOpenPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) TruVatService.open(sp); });
    }

    @EventBusSubscriber(modid = HuKhong.MODID)
    public static final class Net {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent e) {
            e.registrar("1").playToServer(TYPE, STREAM_CODEC, TruVatOpenPayload::handle);
        }
    }
}
