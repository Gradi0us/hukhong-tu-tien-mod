package com.hukhong.network;

import com.hukhong.HuKhong;
import com.hukhong.client.screen.core.CoreAltarScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: kết quả chọn (để lật thẻ). stuck = rơi vào Mắc Kẹt. */
public record CoreRevealPayload(int index, boolean stuck, ResourceLocation core, int tier) implements CustomPacketPayload {
    public static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "none");
    public static final Type<CoreRevealPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "core_reveal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CoreRevealPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CoreRevealPayload::index,
            ByteBufCodecs.BOOL, CoreRevealPayload::stuck,
            ResourceLocation.STREAM_CODEC, CoreRevealPayload::core,
            ByteBufCodecs.VAR_INT, CoreRevealPayload::tier,
            CoreRevealPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(CoreRevealPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof CoreAltarScreen s) s.onReveal(payload);
        });
    }
}
