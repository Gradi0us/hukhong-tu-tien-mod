package com.hukhong.scaling;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: Kiếp Số của chính người chơi + hệ số HP quái tại chỗ (cho HUD). */
public record KiepPayload(float kiep, float mult) implements CustomPacketPayload {
    public static final Type<KiepPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "kiep"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KiepPayload> CODEC = StreamCodec.of(
            (b, p) -> { b.writeFloat(p.kiep); b.writeFloat(p.mult); },
            b -> new KiepPayload(b.readFloat(), b.readFloat()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    static void handle(KiepPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> { Difficulty.clientKiep = p.kiep; Difficulty.clientMult = p.mult; });
    }
}
