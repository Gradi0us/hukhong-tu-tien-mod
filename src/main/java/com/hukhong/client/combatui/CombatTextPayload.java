package com.hukhong.client.combatui;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Server -> client: một con số sát thương/hồi máu nổi tại vị trí (client không biết sát thương chính xác). */
public record CombatTextPayload(int entityId, float x, float y, float z, float amount, byte kind, int color)
        implements CustomPacketPayload {
    public static final byte NORMAL = 0, CRIT = 1, SKILL = 2, HEAL = 3;

    public static final Type<CombatTextPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "combat_text"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CombatTextPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> {
                b.writeVarInt(p.entityId); b.writeFloat(p.x); b.writeFloat(p.y); b.writeFloat(p.z);
                b.writeFloat(p.amount); b.writeByte(p.kind); b.writeInt(p.color);
            },
            b -> new CombatTextPayload(b.readVarInt(), b.readFloat(), b.readFloat(), b.readFloat(),
                    b.readFloat(), b.readByte(), b.readInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(PayloadRegistrar r) {
        r.playToClient(TYPE, STREAM_CODEC, CombatTextPayload::handle);
    }

    /** Tách riêng để server không nạp lớp client. */
    private static void handle(CombatTextPayload pl, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CombatUiClient.receive(pl));
    }
}
