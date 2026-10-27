package com.hukhong.skillbook;

import com.hukhong.HuKhong;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Attachment lưu bền (copy khi chết). Đồng bộ client bằng BookSyncPayload. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class SkillBookAttachments {
    private SkillBookAttachments() {}

    public static final AttachmentType<SkillBookData> BOOK = AttachmentType.builder(() -> SkillBookData.EMPTY)
            .serialize(SkillBookData.CODEC).copyOnDeath().build();

    @SubscribeEvent
    public static void onRegister(RegisterEvent e) {
        e.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,
                h -> h.register(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "skill_book"), BOOK));
    }
}
