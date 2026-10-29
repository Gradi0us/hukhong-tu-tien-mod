package com.hukhong.skillbook;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Server -> client: mo menu chon ky nang de dung Truc Nang Cap (ids = ky nang nang duoc). */
public record OpenScrollPayload(List<String> ids) implements CustomPacketPayload {
    public static final Type<OpenScrollPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "open_scroll"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenScrollPayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(48).apply(ByteBufCodecs.list(64)).map(OpenScrollPayload::new, OpenScrollPayload::ids).mapStream(b -> b);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(OpenScrollPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.hukhong.skillbook.client.SkillScrollScreen.open(p.ids()));
    }
}
