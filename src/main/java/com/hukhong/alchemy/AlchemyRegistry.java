package com.hukhong.alchemy;

import com.hukhong.HuKhong;
import com.hukhong.entity.HuMob;
import com.hukhong.gates.RewardGate;
import com.hukhong.item.PhaCanhDanItem;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Registry Bước 12 (Đan Lô). Gắn vào bus qua ModItems.register -> AlchemyRegistry.register. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class AlchemyRegistry {
    private AlchemyRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HuKhong.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BE_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HuKhong.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, HuKhong.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, HuKhong.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    public static final Supplier<AttachmentType<Double>> DAN_DOC = ATTACHMENTS.register("dan_doc",
            () -> AttachmentType.builder(() -> 0.0).serialize(Codec.DOUBLE).build());

    /** Phẩm chất đan (ordinal của AlchemyMath.Quality); thiếu = Trung. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PILL_QUALITY =
            COMPONENTS.registerComponentType("pill_quality",
                    b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredBlock<DanLoBlock> DAN_LO = BLOCKS.register("dan_lo",
            () -> new DanLoBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE).strength(3.5f).sound(SoundType.METAL)
                    .noOcclusion().lightLevel(s -> 8)));
    public static final DeferredItem<BlockItem> DAN_LO_ITEM = ITEMS.registerSimpleBlockItem("dan_lo", DAN_LO);

    public static final Supplier<BlockEntityType<DanLoBlockEntity>> DAN_LO_BE = BE_TYPES.register("dan_lo",
            () -> BlockEntityType.Builder.of(DanLoBlockEntity::new, DAN_LO.get()).build(null));
    public static final Supplier<MenuType<DanLoMenu>> DAN_LO_MENU =
            MENUS.register("dan_lo", () -> IMenuTypeExtension.create(DanLoMenu::new));

    // Thảo dược
    public static final DeferredItem<Item> LINH_THAO = ITEMS.registerSimpleItem("linh_thao");
    public static final DeferredItem<Item> HUYET_THAM = ITEMS.registerSimpleItem("huyet_tham");
    public static final DeferredItem<Item> HOA_LIEN = ITEMS.registerSimpleItem("hoa_lien");
    public static final DeferredItem<Item> THANH_DIEP = ITEMS.registerSimpleItem("thanh_diep");
    /** Dị Hỏa Châu: đặt vào ô Dị Hỏa, mở rộng dải lửa +4 và lửa nguội chậm hơn (dùng hết 1 lần/mẻ). */
    public static final DeferredItem<Item> DI_HOA_CHAU = ITEMS.registerSimpleItem("di_hoa_chau");

    // Phá Cảnh Đan phẩm 3-6
    public static final DeferredItem<PhaCanhDanItem> THONG_HUYEN_DAN = pha("thong_huyen_dan", 3);
    public static final DeferredItem<PhaCanhDanItem> TU_VUONG_DAN = pha("tu_vuong_dan", 4);
    public static final DeferredItem<PhaCanhDanItem> HOANG_CUC_DAN = pha("hoang_cuc_dan", 5);
    public static final DeferredItem<PhaCanhDanItem> DE_HOAN_DAN = pha("de_hoan_dan", 6);

    // Đan tiện ích (S7 §2.B, §2.G)
    public static final DeferredItem<UtilityPillItem> CHI_HUYET_DAN = ITEMS.register("chi_huyet_dan",
            () -> new UtilityPillItem(UtilityPillItem.Kind.HEAL, 1, 0.15, 0.5, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<UtilityPillItem> HOI_XUAN_DAN = ITEMS.register("hoi_xuan_dan",
            () -> new UtilityPillItem(UtilityPillItem.Kind.HEAL, 1, 0.30, 0.5, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<UtilityPillItem> GIAI_DOC_DAN = ITEMS.register("giai_doc_dan",
            () -> new UtilityPillItem(UtilityPillItem.Kind.DETOX, 2, 30, 0, new Item.Properties().stacksTo(16)));

    public static final DeferredItem<com.hukhong.age.ThoNguyenDanItem> THO_NGUYEN_DAN = ITEMS.register("tho_nguyen_dan",
            () -> new com.hukhong.age.ThoNguyenDanItem(5, new Item.Properties().stacksTo(16)));

    private static DeferredItem<PhaCanhDanItem> pha(String id, int tier) {
        return ITEMS.register(id, () -> new PhaCanhDanItem(tier, new Item.Properties().stacksTo(16)));
    }

    public static AlchemyMath.Quality qualityOf(ItemStack s) {
        return AlchemyMath.Quality.of(s.getOrDefault(PILL_QUALITY.get(), AlchemyMath.Quality.TRUNG.ordinal()));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BE_TYPES.register(bus);
        MENUS.register(bus);
        COMPONENTS.register(bus);
        ATTACHMENTS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) e.accept(DAN_LO_ITEM.get());
            if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
                for (var i : new Supplier[]{LINH_THAO, HUYET_THAM, HOA_LIEN, THANH_DIEP, DI_HOA_CHAU, THONG_HUYEN_DAN,
                        TU_VUONG_DAN, HOANG_CUC_DAN, DE_HOAN_DAN, CHI_HUYET_DAN, HOI_XUAN_DAN, GIAI_DOC_DAN, THO_NGUYEN_DAN})
                    e.accept((Item) i.get());
            }
        });
    }

    // ---- Nguồn thảo dược: cỏ (hiếm) + quái Hư Hóa không từ spawner ----
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof net.minecraft.server.level.ServerLevel lv) || e.getPlayer().isCreative()) return;
        var st = e.getState();
        if (!st.is(Blocks.SHORT_GRASS) && !st.is(Blocks.TALL_GRASS) && !st.is(Blocks.FERN)) return;
        var r = lv.getRandom();
        double x = r.nextDouble();
        Item drop = x < 0.03 ? LINH_THAO.get() : x < 0.04 ? THANH_DIEP.get() : x < 0.0425 ? HUYET_THAM.get() : null;
        if (drop == null) return;
        var p = e.getPos();
        lv.addFreshEntity(new ItemEntity(lv, p.getX() + .5, p.getY() + .3, p.getZ() + .5, new ItemStack(drop)));
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        var v = e.getEntity();
        if (v.level().isClientSide || !(v instanceof HuMob) || !RewardGate.canReward(v)) return;
        var r = v.getRandom();
        if (r.nextDouble() < 0.20) add(e, new ItemStack(HUYET_THAM.get()));
        if (r.nextDouble() < 0.10) add(e, new ItemStack(HOA_LIEN.get()));
    }

    private static void add(LivingDropsEvent e, ItemStack s) {
        var v = e.getEntity();
        e.getDrops().add(new ItemEntity(v.level(), v.getX(), v.getY(), v.getZ(), s));
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (!s.has(PILL_QUALITY.get())) return;
        e.getToolTip().add(Component.translatable("hukhong.alchemy.quality." + qualityOf(s).name().toLowerCase())
                .withStyle(ChatFormatting.AQUA));
    }
}
