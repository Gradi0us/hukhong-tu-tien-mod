package com.hukhong.cores;

import com.hukhong.HuKhong;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class CoreAttachments {
    private CoreAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    public static final Supplier<AttachmentType<CoreSlots>> CORE_SLOTS = ATTACHMENTS.register("core_slots",
            () -> AttachmentType.builder(() -> CoreSlots.EMPTY)
                    .serialize(CoreSlots.CODEC).copyOnDeath().build());

    /** Tiến độ nhiệm vụ Phá Chấp (số quái đã hạ khi đang Mắc Kẹt). */
    public static final Supplier<AttachmentType<Integer>> PHA_CHAP = ATTACHMENTS.register("pha_chap",
            () -> AttachmentType.builder(() -> 0).serialize(com.mojang.serialization.Codec.INT).copyOnDeath().build());
}
