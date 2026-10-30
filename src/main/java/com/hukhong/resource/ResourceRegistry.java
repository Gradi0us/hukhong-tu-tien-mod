package com.hukhong.resource;

import com.hukhong.HuKhong;
import com.hukhong.item.PhaCanhDanItem;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** F6 tai nguyen: 12 linh thao (khoi cay), 5 quang mach, vat pham khoang san, Tu Linh Dan + dan tier 7-9. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class ResourceRegistry {
    private ResourceRegistry() {}

    /** 12 linh thao. 4 loai dau da co vat pham trong AlchemyRegistry (khong tao lai). */
    public static final List<String> HERBS = HerbDef.IDS;
    private static final List<String> EXISTING = List.of("linh_thao", "thanh_diep", "hoa_lien", "huyet_tham");
    /** 5 quang mach -> vat pham rot. */
    public static final Map<String, String> ORES = new LinkedHashMap<>();
    static {
        ORES.put("linh_thach_ha_ore", "linh_thach_ha");
        ORES.put("linh_thach_trung_ore", "linh_thach_trung");
        ORES.put("linh_thach_thuong_ore", "linh_thach_thuong");
        ORES.put("huyen_thiet_ore", "huyen_thiet");
        ORES.put("tinh_than_sa_ore", "tinh_than_sa");
    }

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HuKhong.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, HuKhong.MODID);

    /** Pham chat linh thao 0..3 (cung bang ten voi pham chat dan: Ha/Trung/Thuong/Cuc). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> HERB_QUALITY =
            COMPONENTS.registerComponentType("herb_quality", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final Map<String, DeferredBlock<HerbBlock>> HERB_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> HERB_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<DropExperienceBlock>> ORE_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> ORE_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> MATERIALS = new LinkedHashMap<>();

    public static final DeferredItem<TuLinhDanItem> TU_LINH_DAN = ITEMS.register("tu_linh_dan",
            () -> new TuLinhDanItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<PhaCanhDanItem> HUYEN_TON_DAN = pill("huyen_ton_dan", 7);
    public static final DeferredItem<PhaCanhDanItem> THANH_CO_DAN = pill("thanh_co_dan", 8);
    public static final DeferredItem<PhaCanhDanItem> THAN_DAN = pill("than_dan", 9);

    private static DeferredItem<PhaCanhDanItem> pill(String id, int tier) {
        return ITEMS.register(id, () -> new PhaCanhDanItem(tier, new Item.Properties().stacksTo(16)));
    }

    static {
        for (String h : HERBS) {
            HERB_BLOCKS.put(h, BLOCKS.register("herb_" + h, () -> new HerbBlock(h, BlockBehaviour.Properties.of()
                    .noCollission().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY)
                    .offsetType(BlockBehaviour.OffsetType.XZ).noLootTable())));
            if (!EXISTING.contains(h)) HERB_ITEMS.put(h, ITEMS.registerSimpleItem(h));
        }
        for (var e : ORES.entrySet()) {
            String ore = e.getKey();
            boolean low = ore.startsWith("linh_thach_ha") || ore.startsWith("linh_thach_trung");
            DeferredBlock<DropExperienceBlock> b = BLOCKS.register(ore, () -> new DropExperienceBlock(UniformInt.of(1, low ? 3 : 5),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).strength(low ? 3.0f : 4.5f, 3.0f).requiresCorrectToolForDrops()));
            ORE_BLOCKS.put(ore, b);
            ORE_ITEMS.put(ore, ITEMS.registerSimpleBlockItem(ore, b));
            MATERIALS.put(e.getValue(), ITEMS.registerSimpleItem(e.getValue()));
        }
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        COMPONENTS.register(bus);
        bus.addListener((RegisterGameTestsEvent e) -> e.register(ResourceGameTests.class));
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
                for (var i : HERB_ITEMS.values()) e.accept(i.get());
                for (var i : MATERIALS.values()) e.accept(i.get());
                e.accept(TU_LINH_DAN.get());
                e.accept(HUYEN_TON_DAN.get());
                e.accept(THANH_CO_DAN.get());
                e.accept(THAN_DAN.get());
            }
            if (e.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) for (var i : ORE_ITEMS.values()) e.accept(i.get());
        });
    }

    // ---- tra cuu ----
    /** Id linh thao cua vat pham (null neu khong phai linh thao trong 12 loai). */
    public static String herbIdOf(ItemStack s) {
        if (s.isEmpty()) return null;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(s.getItem());
        return HuKhong.MODID.equals(key.getNamespace()) && HERBS.contains(key.getPath()) ? key.getPath() : null;
    }

    public static boolean isHerb(ItemStack s) { return herbIdOf(s) != null; }

    /** Nguyen lieu Dan Lo: 12 linh thao + Tinh Than Sa (bot). */
    public static boolean isAlchemyIngredient(ItemStack s) {
        return isHerb(s) || s.is(MATERIALS.get("tinh_than_sa").get());
    }

    /** Vat pham linh thao theo id (loai cu lay tu registry theo ten). */
    public static Supplier<Item> herbItem(String id) {
        if (HERB_ITEMS.containsKey(id)) return () -> HERB_ITEMS.get(id).get();
        return () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, id));
    }

    // ---- su kien ----
    /** Chuot phai thao duoc len dat: trong lai (mam tuoi 0, la cay da cham soc). */
    @SubscribeEvent
    public static void onPlant(PlayerInteractEvent.RightClickBlock e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || e.getFace() != Direction.UP || !(e.getLevel() instanceof ServerLevel lv)) return;
        String id = herbIdOf(e.getItemStack());
        if (id == null || e.getEntity().isSecondaryUseActive()) return;
        BlockPos at = e.getPos().above();
        BlockState st = HERB_BLOCKS.get(id).get().defaultBlockState().setValue(HerbBlock.CULTIVATED, true);
        if (!lv.isEmptyBlock(at) || !st.canSurvive(lv, at)) return;
        lv.setBlock(at, st, 3);
        if (!e.getEntity().isCreative()) e.getItemStack().shrink(1);
        e.setCancellationResult(InteractionResult.SUCCESS);
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (!s.has(HERB_QUALITY.get())) return;
        int q = s.getOrDefault(HERB_QUALITY.get(), 0);
        e.getToolTip().add(Component.translatable("hukhong.alchemy.quality." + com.hukhong.alchemy.AlchemyMath.Quality.of(q).name().toLowerCase())
                .withStyle(ChatFormatting.GREEN));
    }
}
