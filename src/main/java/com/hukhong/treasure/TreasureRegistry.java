package com.hukhong.treasure;

import com.hukhong.HuKhong;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Bảo Rương (2026-11-06): rương báu có model + hoạt ảnh thay rương thường ở các điểm thưởng. */
public final class TreasureRegistry {
    private TreasureRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HuKhong.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HuKhong.MODID);

    public static final DeferredBlock<BaoRuongBlock> BAO_RUONG = BLOCKS.register("bao_ruong",
            () -> new BaoRuongBlock(BlockBehaviour.Properties.of().strength(2.5f, 1200f).sound(SoundType.WOOD).noOcclusion()
                    .lightLevel(s -> s.getValue(BaoRuongBlock.LOOTED) ? 4 : 7 + 2 * s.getValue(BaoRuongBlock.TIER))));
    public static final DeferredItem<BlockItem> BAO_RUONG_ITEM = ITEMS.register("bao_ruong",
            () -> new BlockItem(BAO_RUONG.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BaoRuongBE>> BAO_RUONG_BE = BES.register("bao_ruong",
            () -> BlockEntityType.Builder.of(BaoRuongBE::new, BAO_RUONG.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BES.register(bus);
    }
}
