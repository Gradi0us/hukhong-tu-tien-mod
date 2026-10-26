package com.hukhong.network;

import com.hukhong.HuKhong;
import com.hukhong.charcreate.CharCreateService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: bitmask các đạo đã chọn (bit = Path.ordinal). Server xác thực lại toàn bộ. */
public record ConfirmCharCreatePayload(int mask) implements CustomPacketPayload {
    public static final Type<ConfirmCharCreatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "confirm_char_create"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfirmCharCreatePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ConfirmCharCreatePayload::mask, ConfirmCharCreatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ConfirmCharCreatePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) CharCreateService.confirm(sp, payload.mask());
        });
    }
}
