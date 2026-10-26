package com.hukhong.endgame;

import com.hukhong.HuKhong;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Đăng ký Bước 25-26: cổng Hư Không Bản Nguyên, boss cuối, Dạ Khuyết, payload. */
public final class EndgameRegistry {
    private EndgameRegistry() {}

    public static final ResourceKey<Level> DIM =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "hu_khong_ban_nguyen"));

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HuKhong.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, HuKhong.MODID);

    public static final DeferredBlock<EndgamePortalBlock> PORTAL = BLOCKS.register("endgame_portal",
            () -> new EndgamePortalBlock(BlockBehaviour.Properties.of().strength(-1f, 3600000f).noLootTable()
                    .sound(SoundType.AMETHYST).lightLevel(s -> 14)));
    public static final DeferredItem<BlockItem> PORTAL_ITEM = ITEMS.registerSimpleBlockItem("endgame_portal", PORTAL);

    public static final DeferredHolder<EntityType<?>, EntityType<NguyenThuyTichDiet>> BOSS = TYPES.register("nguyen_thuy_tich_diet",
            () -> EntityType.Builder.of(NguyenThuyTichDiet::new, MobCategory.MONSTER)
                    .sized(3.0f, 6.0f).fireImmune().clientTrackingRange(16).build("nguyen_thuy_tich_diet"));
    public static final DeferredHolder<EntityType<?>, EntityType<DaKhuyet>> DA_KHUYET = TYPES.register("da_khuyet",
            () -> EntityType.Builder.of(DaKhuyet::new, MobCategory.MONSTER)
                    .sized(1.1f, 3.5f).fireImmune().clientTrackingRange(12).build("da_khuyet"));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TYPES.register(bus);
        bus.addListener(EndgameNet::register);
        bus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(BOSS.get(), NguyenThuyTichDiet.attrs().build());
            e.put(DA_KHUYET.get(), DaKhuyet.attrs().build());
        });
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) e.accept(PORTAL_ITEM.get());
        });
    }
}
