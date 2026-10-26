package com.hukhong.story;

import com.hukhong.HuKhong;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Đăng ký Bước 19: attachment StoryData, 3 biến thể Sứ Đồ, 12 Văn Kiện, payload. */
public final class StoryRegistry {
    private StoryRegistry() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);
    public static final Supplier<AttachmentType<StoryData>> STORY = ATTACHMENTS.register("story",
            () -> AttachmentType.builder(() -> StoryData.DEFAULT).serialize(StoryData.CODEC).copyOnDeath().build());

    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, HuKhong.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SuDo>> SU_DO_KIEM = TYPES.register("su_do_kiem",
            () -> EntityType.Builder.of(SuDo::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(10).build("su_do_kiem"));
    public static final DeferredHolder<EntityType<?>, EntityType<SuDo>> SU_DO_CHU = TYPES.register("su_do_chu",
            () -> EntityType.Builder.of(SuDo::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(10).build("su_do_chu"));
    public static final DeferredHolder<EntityType<?>, EntityType<SuDo>> SU_DO_TE = TYPES.register("su_do_te",
            () -> EntityType.Builder.of(SuDo::new, MobCategory.MONSTER).sized(0.7f, 2.0f).clientTrackingRange(10).build("su_do_te"));

    public static EntityType<SuDo> variant(int v) { return (v == 0 ? SU_DO_KIEM : v == 1 ? SU_DO_CHU : SU_DO_TE).get(); }

    public static final Map<String, DeferredItem<VanKienItem>> DOC_ITEMS = new LinkedHashMap<>();
    static {
        for (StoryDocs.Doc d : StoryDocs.ALL)
            DOC_ITEMS.put(d.id(), ITEMS.register(d.itemId(), () -> new VanKienItem(d.id())));
    }
    public static final DeferredItem<SpawnEggItem> SU_DO_KIEM_EGG = ITEMS.register("su_do_kiem_spawn_egg",
            () -> new SpawnEggItem(SU_DO_KIEM.get(), 0x1A1030, 0xE8E0F0, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> SU_DO_CHU_EGG = ITEMS.register("su_do_chu_spawn_egg",
            () -> new SpawnEggItem(SU_DO_CHU.get(), 0x2A1A4B, 0xB070FF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> SU_DO_TE_EGG = ITEMS.register("su_do_te_spawn_egg",
            () -> new SpawnEggItem(SU_DO_TE.get(), 0x3A0A1A, 0xD04060, new Item.Properties()));

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
        TYPES.register(bus);
        ITEMS.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> {
            for (int i = 0; i < 3; i++) e.put(variant(i), SuDo.attrs().build());
        });
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
                e.accept(SU_DO_KIEM_EGG.get());
                e.accept(SU_DO_CHU_EGG.get());
                e.accept(SU_DO_TE_EGG.get());
            }
            if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) DOC_ITEMS.values().forEach(i -> e.accept(i.get()));
        });
        bus.addListener(StoryNet::register);
        com.hukhong.story.npc.StoryNpcRegistry.register(bus);
    }
}
