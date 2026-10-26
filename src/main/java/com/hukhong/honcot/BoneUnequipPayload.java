package com.hukhong.honcot;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: tháo Hồn Cốt ở slot 0..5. Server xác thực. */
public record BoneUnequipPayload(int slot) implements CustomPacketPayload {
    public static final Type<BoneUnequipPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "bone_unequip"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BoneUnequipPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, BoneUnequipPayload::slot, BoneUnequipPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BoneUnequipPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) HonCotService.unequip(sp, p.slot());
        });
    }
}
