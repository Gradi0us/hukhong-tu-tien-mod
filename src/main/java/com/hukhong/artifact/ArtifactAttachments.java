package com.hukhong.artifact;

import com.hukhong.HuKhong;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ArtifactAttachments {
    private ArtifactAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    /** Giai đoạn pháp khí Đạo/Thể/Ma: lưu, giữ khi chết, đồng bộ cho mọi client thấy người chơi (để vẽ layer). */
    public static final Supplier<AttachmentType<ArtifactData>> ARTIFACT = ATTACHMENTS.register("artifact",
            () -> AttachmentType.builder(() -> ArtifactData.EMPTY)
                    .serialize(ArtifactData.CODEC).copyOnDeath().sync(ArtifactData.STREAM_CODEC).build());
}
