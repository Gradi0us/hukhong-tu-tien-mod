package com.hukhong.artifact;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: nút tiến hóa pháp khí ở màn Hồ Sơ (K). path = Path.ordinal(). */
public record ArtifactUpgradePayload(int path) implements CustomPacketPayload {
    public static final Type<ArtifactUpgradePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "artifact_upgrade"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArtifactUpgradePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ArtifactUpgradePayload::path, ArtifactUpgradePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
