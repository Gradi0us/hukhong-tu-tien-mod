package com.hukhong.society;

import com.hukhong.HuKhong;
import com.hukhong.city.CityBoardBlock;
import com.hukhong.city.CityMenu;
import com.hukhong.city.CityStanding;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Registry Bước 14: Linh Khí Đài, bảng Tông Môn, bảng Thành, menu, attachment. Gắn bus qua ModItems.register. */
public final class SocietyRegistry {
    private SocietyRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HuKhong.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, HuKhong.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    public static final DeferredBlock<LinhKhiDaiBlock> LINH_KHI_DAI = BLOCKS.register("linh_khi_dai",
            () -> new LinhKhiDaiBlock(BlockBehaviour.Properties.of().strength(3.5f, 1200f).sound(SoundType.AMETHYST)
                    .lightLevel(s -> 9).noOcclusion()));
    public static final DeferredBlock<SectBoardBlock> TONG_MON_BANG = BLOCKS.register("tong_mon_bang",
            () -> new SectBoardBlock(BlockBehaviour.Properties.of().strength(3f, 1200f).sound(SoundType.WOOD)));
    public static final DeferredBlock<CityBoardBlock> THANH_BANG = BLOCKS.register("thanh_bang",
            () -> new CityBoardBlock(BlockBehaviour.Properties.of().strength(3f, 1200f).sound(SoundType.WOOD)));

    public static final DeferredItem<BlockItem> LINH_KHI_DAI_ITEM = ITEMS.registerSimpleBlockItem("linh_khi_dai", LINH_KHI_DAI);
    public static final DeferredItem<BlockItem> TONG_MON_BANG_ITEM = ITEMS.registerSimpleBlockItem("tong_mon_bang", TONG_MON_BANG);
    public static final DeferredItem<BlockItem> THANH_BANG_ITEM = ITEMS.registerSimpleBlockItem("thanh_bang", THANH_BANG);

    public static final Supplier<MenuType<SectMenu>> SECT_MENU =
            MENUS.register("sect", () -> IMenuTypeExtension.create(SectMenu::new));
    public static final Supplier<MenuType<CityMenu>> CITY_MENU =
            MENUS.register("city", () -> IMenuTypeExtension.create(CityMenu::new));

    public static final Supplier<AttachmentType<SectData>> SECT = ATTACHMENTS.register("sect",
            () -> AttachmentType.builder(() -> SectData.DEFAULT).serialize(SectData.CODEC).copyOnDeath().build());
    public static final Supplier<AttachmentType<CityStanding>> CITY_STANDING = ATTACHMENTS.register("city_standing",
            () -> AttachmentType.builder(() -> CityStanding.DEFAULT).serialize(CityStanding.CODEC).copyOnDeath().build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        MENUS.register(bus);
        ATTACHMENTS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
                e.accept(LINH_KHI_DAI_ITEM.get());
                e.accept(TONG_MON_BANG_ITEM.get());
                e.accept(THANH_BANG_ITEM.get());
            }
        });
    }
}
