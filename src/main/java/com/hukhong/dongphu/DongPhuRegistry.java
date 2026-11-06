package com.hukhong.dongphu;

import com.hukhong.HuKhong;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Động Phủ (2026-11-06): khối Trận Bàn + item + block entity GeckoLib. */
public final class DongPhuRegistry {
    private DongPhuRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HuKhong.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HuKhong.MODID);

    public static final DeferredBlock<DongPhuTranBanBlock> TRAN_BAN = BLOCKS.register("dong_phu_tran_ban",
            () -> new DongPhuTranBanBlock(BlockBehaviour.Properties.of().strength(3.0f, 1200f).sound(SoundType.AMETHYST)
                    .noOcclusion().lightLevel(s -> 4 + s.getValue(DongPhuTranBanBlock.TIER))));
    public static final DeferredItem<DongPhuTranBanItem> TRAN_BAN_ITEM = ITEMS.register("dong_phu_tran_ban",
            () -> new DongPhuTranBanItem(TRAN_BAN.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DongPhuTranBanBE>> TRAN_BAN_BE = BES.register("dong_phu_tran_ban",
            () -> BlockEntityType.Builder.of(DongPhuTranBanBE::new, TRAN_BAN.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BES.register(bus);
    }
}
