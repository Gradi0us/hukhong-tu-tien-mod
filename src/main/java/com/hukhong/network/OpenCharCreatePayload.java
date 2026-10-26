package com.hukhong.network;

import com.hukhong.HuKhong;
import com.hukhong.charcreate.CharRoll;
import com.hukhong.client.screen.charcreate.CharCreateScreen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: mở màn tạo nhân vật với roll đã lưu. */
public record OpenCharCreatePayload(CharRoll roll) implements CustomPacketPayload {
    public static final Type<OpenCharCreatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "open_char_create"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCharCreatePayload> STREAM_CODEC =
            StreamCodec.composite(CharRoll.STREAM_CODEC, OpenCharCreatePayload::roll, OpenCharCreatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(OpenCharCreatePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CharCreateScreen.open(payload.roll()));
    }
}
