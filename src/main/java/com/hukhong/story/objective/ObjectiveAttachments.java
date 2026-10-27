package com.hukhong.story.objective;

import com.hukhong.HuKhong;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ObjectiveAttachments {
    private ObjectiveAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    public static final Supplier<AttachmentType<ObjectiveData>> OBJECTIVE = ATTACHMENTS.register("objective",
            () -> AttachmentType.builder(() -> ObjectiveData.EMPTY).serialize(ObjectiveData.CODEC).copyOnDeath().build());
}
