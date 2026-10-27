package com.hukhong.entity;

import com.hukhong.HuKhong;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
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

@EventBusSubscriber(modid = HuKhong.MODID)
public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, HuKhong.MODID);
    public static final DeferredRegister.Items EGGS = DeferredRegister.createItems(HuKhong.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<HuThi>> HU_THI = TYPES.register("hu_thi",
            () -> EntityType.Builder.of(HuThi::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8).build("hu_thi"));
    public static final DeferredHolder<EntityType<?>, EntityType<HuCot>> HU_COT = TYPES.register("hu_cot",
            () -> EntityType.Builder.of(HuCot::new, MobCategory.MONSTER).sized(0.6f, 1.99f).clientTrackingRange(8).build("hu_cot"));
    public static final DeferredHolder<EntityType<?>, EntityType<HuChu>> HU_CHU = TYPES.register("hu_chu",
            () -> EntityType.Builder.of(HuChu::new, MobCategory.MONSTER).sized(1.4f, 0.9f).clientTrackingRange(8).build("hu_chu"));

    public static final DeferredHolder<EntityType<?>, EntityType<HuVuong>> HU_VUONG = TYPES.register("hu_vuong",
            () -> EntityType.Builder.of(HuVuong::new, MobCategory.MONSTER).sized(2.4f, 3.6f).fireImmune().clientTrackingRange(10).build("hu_vuong"));

    public static final DeferredHolder<EntityType<?>, EntityType<TamMa>> TAM_MA = TYPES.register("tam_ma",
            () -> EntityType.Builder.of(TamMa::new, MobCategory.MONSTER).sized(0.6f, 1.8f).fireImmune().clientTrackingRange(10).build("tam_ma"));

    public static final DeferredItem<SpawnEggItem> HU_THI_EGG = EGGS.register("hu_thi_spawn_egg",
            () -> new SpawnEggItem(HU_THI.get(), 0x4B2A6B, 0x9B30FF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> HU_COT_EGG = EGGS.register("hu_cot_spawn_egg",
            () -> new SpawnEggItem(HU_COT.get(), 0x3A2A5B, 0xC080FF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> HU_CHU_EGG = EGGS.register("hu_chu_spawn_egg",
            () -> new SpawnEggItem(HU_CHU.get(), 0x2A1A4B, 0x8030D0, new Item.Properties()));

    public static void register(IEventBus bus) {
        com.hukhong.effect.ModEffects.register(bus);
        TYPES.register(bus);
        EGGS.register(bus);
        V3Entities.register(bus);
    }

    @SubscribeEvent
    public static void attrs(EntityAttributeCreationEvent e) {
        e.put(HU_THI.get(), Zombie.createAttributes().build());
        e.put(HU_COT.get(), AbstractSkeleton.createAttributes().build());
        e.put(HU_CHU.get(), Spider.createAttributes().build());
        e.put(HU_VUONG.get(), HuVuong.attrs().build());
        e.put(TAM_MA.get(), TamMa.attrs().build());
    }

    /** Quai Hu Hoa sinh ca ngay lan dem (khong doi toi), tren mat dat, khong o Peaceful. */
    public static boolean huSpawnRules(EntityType<? extends Monster> t, net.minecraft.world.level.ServerLevelAccessor l,
                                       net.minecraft.world.entity.MobSpawnType st, net.minecraft.core.BlockPos p, net.minecraft.util.RandomSource r) {
        return l.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL && Monster.checkMobSpawnRules(t, l, st, p, r);
    }

    @SubscribeEvent
    public static void placements(RegisterSpawnPlacementsEvent e) {
        e.register(HU_THI.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModEntities::huSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        e.register(HU_COT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModEntities::huSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        e.register(HU_CHU.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModEntities::huSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void tab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            e.accept(HU_THI_EGG.get());
            e.accept(HU_COT_EGG.get());
            e.accept(HU_CHU_EGG.get());
        }
    }
}
