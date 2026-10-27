package com.hukhong.voidblock;

import com.hukhong.HuKhong;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Khối Hư Không: địa hình bị xâm thực (do VoidCorruptor đặt/hoàn nguyên). */
public final class VoidBlocks {
    private VoidBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HuKhong.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);

    private static BlockBehaviour.Properties base(float hard, SoundType s) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(hard).sound(s);
    }

    public static final DeferredBlock<Block> DAT = BLOCKS.register("hu_khong_dat", () -> new Block(base(0.6f, SoundType.GRAVEL)));
    public static final DeferredBlock<Block> CO = BLOCKS.register("hu_khong_co", () -> new Block(base(0.6f, SoundType.GRASS)));
    public static final DeferredBlock<Block> DA = BLOCKS.register("hu_khong_da", () -> new Block(base(1.5f, SoundType.DEEPSLATE)));
    public static final DeferredBlock<Block> TINH = BLOCKS.register("hu_khong_tinh",
            () -> new Block(base(1.5f, SoundType.AMETHYST_CLUSTER).lightLevel(s -> 7)));
    public static final DeferredBlock<RotatedPillarBlock> GO = BLOCKS.register("hu_khong_go",
            () -> new RotatedPillarBlock(base(2.0f, SoundType.WOOD)));
    public static final DeferredBlock<Block> LA = BLOCKS.register("hu_khong_la", () -> new Block(base(0.2f, SoundType.GRASS)));
    public static final DeferredBlock<VoidRiftBlock> VET_NUT = BLOCKS.register("hu_khong_vet_nut",
            () -> new VoidRiftBlock(base(1.0f, SoundType.AMETHYST).lightLevel(s -> 5)));

    public static final DeferredItem<BlockItem> DAT_ITEM = ITEMS.registerSimpleBlockItem("hu_khong_dat", DAT);
    public static final DeferredItem<BlockItem> CO_ITEM = ITEMS.registerSimpleBlockItem("hu_khong_co", CO);
    public static final DeferredItem<BlockItem> DA_ITEM = ITEMS.registerSimpleBlockItem("hu_khong_da", DA);
    public static final DeferredItem<BlockItem> TINH_ITEM = ITEMS.registerSimpleBlockItem("hu_khong_tinh", TINH);
    public static final DeferredItem<BlockItem> GO_ITEM = ITEMS.registerSimpleBlockItem("hu_khong_go", GO);
    public static final DeferredItem<BlockItem> LA_ITEM = ITEMS.registerSimpleBlockItem("hu_khong_la", LA);
    public static final DeferredItem<BlockItem> VET_NUT_ITEM = ITEMS.registerSimpleBlockItem("hu_khong_vet_nut", VET_NUT);

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
                for (DeferredItem<BlockItem> i : java.util.List.of(DAT_ITEM, CO_ITEM, DA_ITEM, TINH_ITEM, GO_ITEM, LA_ITEM, VET_NUT_ITEM))
                    e.accept(i.get());
            }
        });
    }
}
