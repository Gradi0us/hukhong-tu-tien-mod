package com.hukhong.entity;

import com.hukhong.HuKhong;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Dang ky quai/boss moi (v3): Hu Anh, Hu Giap, Hu Trung, Hu Trieu Su, nuoc bot doc, 3 boss the gioi theo bioma. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class V3Entities {
    private V3Entities() {}

    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, HuKhong.MODID);
    public static final DeferredRegister.Items EGGS = DeferredRegister.createItems(HuKhong.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<HuAnh>> HU_ANH = TYPES.register("hu_anh",
            () -> EntityType.Builder.of(HuAnh::new, MobCategory.MONSTER).sized(0.8f, 2.0f).fireImmune().clientTrackingRange(10).build("hu_anh"));
    public static final DeferredHolder<EntityType<?>, EntityType<HuGiap>> HU_GIAP = TYPES.register("hu_giap",
            () -> EntityType.Builder.of(HuGiap::new, MobCategory.MONSTER).sized(1.1f, 2.6f).clientTrackingRange(8).build("hu_giap"));
    public static final DeferredHolder<EntityType<?>, EntityType<HuTrung>> HU_TRUNG = TYPES.register("hu_trung",
            () -> EntityType.Builder.of(HuTrung::new, MobCategory.MONSTER).sized(0.8f, 0.6f).clientTrackingRange(8).build("hu_trung"));
    public static final DeferredHolder<EntityType<?>, EntityType<HuTrieuSu>> HU_TRIEU_SU = TYPES.register("hu_trieu_su",
            () -> EntityType.Builder.of(HuTrieuSu::new, MobCategory.MONSTER).sized(0.7f, 2.0f).clientTrackingRange(8).build("hu_trieu_su"));

    public static final DeferredHolder<EntityType<?>, EntityType<PoisonSpit>> POISON_SPIT = TYPES.register("poison_spit",
            () -> EntityType.Builder.<PoisonSpit>of(PoisonSpit::new, MobCategory.MISC).sized(0.3f, 0.3f).clientTrackingRange(4).updateInterval(2).build("poison_spit"));

    public static final DeferredHolder<EntityType<?>, EntityType<SaHatVuong>> SA_HAT_VUONG = TYPES.register("sa_hat_vuong",
            () -> EntityType.Builder.of(SaHatVuong::new, MobCategory.MONSTER).sized(3.2f, 2.4f).fireImmune().clientTrackingRange(10).build("sa_hat_vuong"));
    public static final DeferredHolder<EntityType<?>, EntityType<BangNhan>> BANG_NHAN = TYPES.register("bang_nhan",
            () -> EntityType.Builder.of(BangNhan::new, MobCategory.MONSTER).sized(2.4f, 5.0f).fireImmune().clientTrackingRange(10).build("bang_nhan"));
    public static final DeferredHolder<EntityType<?>, EntityType<DocMau>> DOC_MAU = TYPES.register("doc_mau",
            () -> EntityType.Builder.of(DocMau::new, MobCategory.MONSTER).sized(2.6f, 3.6f).fireImmune().clientTrackingRange(10).build("doc_mau"));

    public static final DeferredItem<SpawnEggItem> HU_ANH_EGG = EGGS.register("hu_anh_spawn_egg",
            () -> new SpawnEggItem(HU_ANH.get(), 0x2A1A4B, 0xB070FF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> HU_GIAP_EGG = EGGS.register("hu_giap_spawn_egg",
            () -> new SpawnEggItem(HU_GIAP.get(), 0x30283C, 0x9B30FF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> HU_TRUNG_EGG = EGGS.register("hu_trung_spawn_egg",
            () -> new SpawnEggItem(HU_TRUNG.get(), 0x3A2A5B, 0x70E060, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> HU_TRIEU_SU_EGG = EGGS.register("hu_trieu_su_spawn_egg",
            () -> new SpawnEggItem(HU_TRIEU_SU.get(), 0x2A1A4B, 0xE060FF, new Item.Properties()));

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        EGGS.register(bus);
    }

    @SubscribeEvent
    public static void attrs(EntityAttributeCreationEvent e) {
        e.put(HU_ANH.get(), HuAnh.attrs().build());
        e.put(HU_GIAP.get(), HuGiap.attrs().build());
        e.put(HU_TRUNG.get(), HuTrung.attrs().build());
        e.put(HU_TRIEU_SU.get(), HuTrieuSu.attrs().build());
        e.put(SA_HAT_VUONG.get(), WorldBoss.attrs(0.26).build());
        e.put(BANG_NHAN.get(), WorldBoss.attrs(0.24).build());
        e.put(DOC_MAU.get(), WorldBoss.attrs(0.25).build());
    }

    @SubscribeEvent
    public static void placements(RegisterSpawnPlacementsEvent e) {
        for (EntityType<? extends Monster> t : java.util.List.<EntityType<? extends Monster>>of(
                HU_ANH.get(), HU_GIAP.get(), HU_TRUNG.get(), HU_TRIEU_SU.get()))
            e.register(t, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    ModEntities::huSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void tab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            e.accept(HU_ANH_EGG.get());
            e.accept(HU_GIAP_EGG.get());
            e.accept(HU_TRUNG_EGG.get());
            e.accept(HU_TRIEU_SU_EGG.get());
        }
    }
}
