package com.hukhong.skillbook;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: gán kỹ năng id ("" = bỏ gán) vào phím slot (0..3) của trang đạo path (ordinal). Server xác thực. */
public record AssignSkillPayload(int path, int slot, String id) implements CustomPacketPayload {
    public static final Type<AssignSkillPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "assign_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssignSkillPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AssignSkillPayload::path,
            ByteBufCodecs.VAR_INT, AssignSkillPayload::slot,
            ByteBufCodecs.stringUtf8(48), AssignSkillPayload::id,
            AssignSkillPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
