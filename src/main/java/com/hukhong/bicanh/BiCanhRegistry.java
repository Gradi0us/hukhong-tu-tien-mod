package com.hukhong.bicanh;

import com.hukhong.HuKhong;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registry Bước 21: cổng + khối thoát Bí Cảnh. Gắn bus qua ModItems.register. */
public final class BiCanhRegistry {
    private BiCanhRegistry() {}

    public static final ResourceKey<Level> DIM =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "bicanh"));

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HuKhong.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);

    public static final DeferredBlock<BiCanhPortalBlock> PORTAL = BLOCKS.register("bi_canh_cong",
            () -> new BiCanhPortalBlock(BlockBehaviour.Properties.of().strength(-1f, 3600000f).noLootTable()
                    .sound(SoundType.AMETHYST).lightLevel(s -> 12).noOcclusion()));
    public static final DeferredBlock<BiCanhExitBlock> EXIT = BLOCKS.register("bicanh_exit",
            () -> new BiCanhExitBlock(BlockBehaviour.Properties.of().strength(-1f, 3600000f).noLootTable()
                    .sound(SoundType.AMETHYST).lightLevel(s -> 10)));

    public static final DeferredItem<BlockItem> PORTAL_ITEM = ITEMS.registerSimpleBlockItem("bi_canh_cong", PORTAL);
    public static final DeferredItem<BlockItem> EXIT_ITEM = ITEMS.registerSimpleBlockItem("bicanh_exit", EXIT);

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
                e.accept(PORTAL_ITEM.get());
                e.accept(EXIT_ITEM.get());
            }
        });
    }
}
