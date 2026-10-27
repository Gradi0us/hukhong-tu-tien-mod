package com.hukhong.network;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: một hiệu ứng VFX (client tự sinh hình từ tham số, 1 gói duy nhất). life=0 -> mặc định theo loại. */
public record FxPayload(int fx, double x, double y, double z, float dx, float dy, float dz,
                        float scale, int color, int life) implements CustomPacketPayload {
    public static final Type<FxPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FxPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> {
                b.writeByte(p.fx); b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z);
                b.writeFloat(p.dx); b.writeFloat(p.dy); b.writeFloat(p.dz);
                b.writeFloat(p.scale); b.writeInt(p.color); b.writeVarInt(p.life);
            },
            b -> new FxPayload(b.readByte(), b.readDouble(), b.readDouble(), b.readDouble(),
                    b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readInt(), b.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Tách riêng để server không nạp lớp client. */
    public static void handle(FxPayload pl, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.hukhong.client.fx.FxClient.receive(pl));
    }
}
