package com.hukhong.guild;

import com.hukhong.HuKhong;
import com.hukhong.currency.CurrencyRegistry;
import com.hukhong.currency.ShopMenu;
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

/** Registry Hiep Hoi Mao Hiem Gia + cua hang Tu Tinh (menu). Gan bus qua ModItems.register. */
public final class GuildRegistry {
    private GuildRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HuKhong.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, HuKhong.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    public static final DeferredBlock<GuildBoardBlock> BOARD = BLOCKS.register("mao_hiem_bang",
            () -> new GuildBoardBlock(BlockBehaviour.Properties.of().strength(3f, 1200f).sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> BOARD_ITEM = ITEMS.registerSimpleBlockItem("mao_hiem_bang", BOARD);

    public static final Supplier<MenuType<GuildMenu>> GUILD_MENU =
            MENUS.register("guild", () -> IMenuTypeExtension.create(GuildMenu::new));
    public static final Supplier<MenuType<ShopMenu>> SHOP_MENU =
            MENUS.register("shop", () -> IMenuTypeExtension.create(ShopMenu::new));

    public static final Supplier<AttachmentType<GuildData>> DATA = ATTACHMENTS.register("guild",
            () -> AttachmentType.builder(() -> GuildData.EMPTY).serialize(GuildData.CODEC).copyOnDeath()
                    .sync(GuildData.STREAM_CODEC).build());

    public static void register(IEventBus bus) {
        CurrencyRegistry.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        MENUS.register(bus);
        ATTACHMENTS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) e.accept(BOARD_ITEM.get());
        });
    }
}
