package com.hukhong.profession;

import com.hukhong.HuKhong;
import com.hukhong.profession.station.ProfessionStationBlock;
import com.hukhong.profession.station.ProfessionStationBlockEntity;
import com.hukhong.profession.station.StationMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Dang ky Nghe phu: attachment, Phu An, Doan Tao Dai, Linh Thiet Phoi, menu, goi mang. */
public final class ProfessionRegistry {
    private ProfessionRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HuKhong.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BE_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HuKhong.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, HuKhong.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    /** Nghe phu cua nguoi choi (copyOnDeath). */
    public static final Supplier<AttachmentType<ProfessionData>> DATA = ATTACHMENTS.register("profession",
            () -> AttachmentType.builder(() -> ProfessionData.DEFAULT)
                    .serialize(ProfessionData.CODEC).copyOnDeath().build());

    public static final DeferredBlock<ProfessionStationBlock> PHU_AN = BLOCKS.register("phu_an",
            () -> new ProfessionStationBlock(ProfessionStationBlock.Station.PHU_AN, stationProps()));
    public static final DeferredItem<BlockItem> PHU_AN_ITEM = ITEMS.registerSimpleBlockItem("phu_an", PHU_AN);

    public static final DeferredBlock<ProfessionStationBlock> DOAN_TAO_DAI = BLOCKS.register("doan_tao_dai",
            () -> new ProfessionStationBlock(ProfessionStationBlock.Station.DOAN_TAO_DAI, stationProps()));
    public static final DeferredItem<BlockItem> DOAN_TAO_DAI_ITEM = ITEMS.registerSimpleBlockItem("doan_tao_dai", DOAN_TAO_DAI);

    /** Nguyen lieu rèn cap 1 (placeholder cho giap pháp khí). */
    public static final DeferredItem<Item> LINH_THIET_PHOI = ITEMS.registerSimpleItem("linh_thiet_phoi");

    public static final Supplier<BlockEntityType<ProfessionStationBlockEntity>> STATION_BE = BE_TYPES.register("profession_station",
            () -> BlockEntityType.Builder.of(ProfessionStationBlockEntity::new, PHU_AN.get(), DOAN_TAO_DAI.get()).build(null));

    public static final Supplier<MenuType<StationMenu>> STATION_MENU =
            MENUS.register("profession_station", () -> IMenuTypeExtension.create(StationMenu::new));

    private static BlockBehaviour.Properties stationProps() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).strength(2.5f).sound(SoundType.WOOD);
    }

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BE_TYPES.register(bus);
        MENUS.register(bus);
        bus.addListener((RegisterPayloadHandlersEvent e) -> ProfessionNet.register(e.registrar("1")));
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
                e.accept(PHU_AN_ITEM.get());
                e.accept(DOAN_TAO_DAI_ITEM.get());
            }
            if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) e.accept(LINH_THIET_PHOI.get());
        });
    }
}
