package com.hukhong.skill;

import com.hukhong.HuKhong;
import com.hukhong.gates.SpawnerMarks;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Đăng ký attachment mới (SkillState, SpawnerMarks) qua RegisterEvent, không đụng ModAttachments. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class SkillAttachments {
    private SkillAttachments() {}

    public static final AttachmentType<SkillState> STATE = AttachmentType.builder(SkillState::new).build();

    @SubscribeEvent
    public static void onRegister(RegisterEvent e) {
        e.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, h -> {
            h.register(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "skill_state"), STATE);
            h.register(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "spawner_mark"), SpawnerMarks.MARK);
        });
    }
}
