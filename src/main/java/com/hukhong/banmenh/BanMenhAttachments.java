package com.hukhong.banmenh;

import com.hukhong.HuKhong;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class BanMenhAttachments {
    private BanMenhAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    public static final Supplier<AttachmentType<BanMenhData>> BAN_MENH = ATTACHMENTS.register("ban_menh",
            () -> AttachmentType.builder(() -> BanMenhData.EMPTY)
                    .serialize(BanMenhData.CODEC).copyOnDeath().build());
}
