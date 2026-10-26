package com.hukhong.core;

import com.hukhong.HuKhong;
import com.hukhong.age.AgeData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    public static final Supplier<AttachmentType<CultivationData>> CULTIVATION = ATTACHMENTS.register("cultivation",
            () -> AttachmentType.builder(() -> CultivationData.DEFAULT)
                    .serialize(CultivationData.CODEC).copyOnDeath().build());

    public static final Supplier<AttachmentType<AgeData>> AGE = ATTACHMENTS.register("age",
            () -> AttachmentType.builder(() -> AgeData.DEFAULT)
                    .serialize(AgeData.CODEC).copyOnDeath().build());
}
