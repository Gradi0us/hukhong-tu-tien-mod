package com.hukhong.skillbook;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: Cường Hóa kỹ năng id lên 1 cấp bằng cách trả phí mode (SkillBookMath.Mode ordinal). */
public record UpgradeSkillPayload(String id, int mode) implements CustomPacketPayload {
    public static final Type<UpgradeSkillPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "upgrade_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeSkillPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(48), UpgradeSkillPayload::id,
            ByteBufCodecs.VAR_INT, UpgradeSkillPayload::mode,
            UpgradeSkillPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
