package com.hukhong.phaptu.linhtruong;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Client -> server: chuột trái khi cầm Linh Trượng = bắn tia phép. Server tự raycast, kiểm hồi chiêu và chủ sở hữu. */
public record LinhTruongBoltPayload() implements CustomPacketPayload {
    public static final Type<LinhTruongBoltPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "linh_truong_bolt"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LinhTruongBoltPayload> STREAM_CODEC = StreamCodec.unit(new LinhTruongBoltPayload());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(PayloadRegistrar r) {
        r.playToServer(TYPE, STREAM_CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> LinhTruongService.fire(sp));
        });
    }
}
