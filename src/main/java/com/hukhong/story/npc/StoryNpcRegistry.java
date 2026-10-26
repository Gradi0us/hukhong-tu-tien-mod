package com.hukhong.story.npc;

import com.hukhong.HuKhong;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Đăng ký Bước 22: entity StoryNpc, attachment QuestData, payload. */
public final class StoryNpcRegistry {
    private StoryNpcRegistry() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);
    public static final Supplier<AttachmentType<QuestData>> QUEST = ATTACHMENTS.register("quest",
            () -> AttachmentType.builder(() -> QuestData.DEFAULT).serialize(QuestData.CODEC).copyOnDeath().build());

    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, HuKhong.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<StoryNpc>> STORY_NPC = TYPES.register("story_npc",
            () -> EntityType.Builder.of(StoryNpc::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10).build("story_npc"));

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
        TYPES.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> e.put(STORY_NPC.get(), StoryNpc.attrs().build()));
        bus.addListener(NpcNet::register);
    }
}
