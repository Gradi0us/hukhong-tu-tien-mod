package com.hukhong.story.freequest;

import com.hukhong.HuKhong;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class FreeQuestAttachments {
    private FreeQuestAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    public static final Supplier<AttachmentType<FreeQuestData>> DATA = ATTACHMENTS.register("freequest",
            () -> AttachmentType.builder(() -> FreeQuestData.EMPTY).serialize(FreeQuestData.CODEC).copyOnDeath().build());
}
