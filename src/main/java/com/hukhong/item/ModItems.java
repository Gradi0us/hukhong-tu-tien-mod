package com.hukhong.item;

import com.hukhong.HuKhong;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, HuKhong.MODID);

    /** UUID chủ sở hữu (bind). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> BOUND_TO =
            COMPONENTS.registerComponentType("bound_to",
                    b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    /** Bản Mệnh Kiếm (Bước 10B): loại + giai đoạn ở data component, model theo custom_model_data. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BAN_MENH_TYPE =
            COMPONENTS.registerComponentType("ban_menh_type",
                    b -> b.persistent(com.mojang.serialization.Codec.STRING).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BAN_MENH_STAGE =
            COMPONENTS.registerComponentType("ban_menh_stage",
                    b -> b.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
    public static final DeferredItem<com.hukhong.banmenh.BanMenhKiemItem> BAN_MENH_KIEM =
            ITEMS.register("ban_menh_kiem", com.hukhong.banmenh.BanMenhKiemItem::new);

    public static final DeferredItem<PhaCanhDanItem> KHAI_MACH_DAN = ITEMS.register("khai_mach_dan",
            () -> new PhaCanhDanItem(1, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<PhaCanhDanItem> NGUNG_NGUYEN_DAN = ITEMS.register("ngung_nguyen_dan",
            () -> new PhaCanhDanItem(2, new Item.Properties().stacksTo(16)));
    /** Tiểu Phá Đan (S7 §2.A2): Bình Cảnh nhẹ cấp 94 -> mốc 95 (Tiểu Kình Đan V, phẩm 5) và cấp 98 -> mốc 99 (phẩm 9). */
    public static final DeferredItem<PhaCanhDanItem> TIEU_KINH_DAN_V = ITEMS.register("tieu_kinh_dan_v",
            () -> new PhaCanhDanItem(5, 94, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<PhaCanhDanItem> TIEU_BAN_THAN_DAN = ITEMS.register("tieu_ban_than_dan",
            () -> new PhaCanhDanItem(9, 98, new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> HON_TINH = ITEMS.register("hon_tinh", () -> new Item(new Item.Properties()));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> AN_HON_DAI =
            ITEMS.registerSimpleBlockItem("an_hon_dai", com.hukhong.block.ModBlocks.AN_HON_DAI);

    public static void register(IEventBus bus) {
        // Đăng ký kèm các registry của Bước 6 (không sửa HuKhong.java).
        com.hukhong.block.ModBlocks.BLOCKS.register(bus);
        com.hukhong.menu.ModMenus.MENUS.register(bus);
        com.hukhong.cores.CoreAttachments.ATTACHMENTS.register(bus);
        com.hukhong.banmenh.BanMenhAttachments.ATTACHMENTS.register(bus);
        com.hukhong.artifact.ArtifactAttachments.ATTACHMENTS.register(bus);
        com.hukhong.alchemy.AlchemyRegistry.register(bus);
        com.hukhong.society.SocietyRegistry.register(bus);
        com.hukhong.honcot.HonCotRegistry.register(bus);
        com.hukhong.bicanh.BiCanhRegistry.register(bus);
        com.hukhong.voidblock.VoidBlocks.register(bus);
        com.hukhong.skillbook.SkillBookRegistry.register(bus);
        com.hukhong.guild.GuildRegistry.register(bus);
        ITEMS.register(bus);
        COMPONENTS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) e.accept(AN_HON_DAI.get());
            if (e.getTabKey() == CreativeModeTabs.COMBAT) e.accept(BAN_MENH_KIEM.get());
            if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
                e.accept(KHAI_MACH_DAN.get());
                e.accept(NGUNG_NGUYEN_DAN.get());
                e.accept(TIEU_KINH_DAN_V.get());
                e.accept(TIEU_BAN_THAN_DAN.get());
                e.accept(HON_TINH.get());
            }
        });
    }
}
