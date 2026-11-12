package com.hukhong.craft;

import com.hukhong.HuKhong;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Dang ky chung cua he che tao: data component (pham, nguoi che, id cong thuc tren quyen pho),
 * attachment KnownRecipes, nguyen lieu moi (0.3), 3 quyen pho (0.2).
 */
public final class CraftRegistry {
    private CraftRegistry() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, HuKhong.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    /** Pham che tao 0..3 (AlchemyMath.Quality: Ha, Trung, Thuong, Cuc). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CRAFT_QUALITY =
            COMPONENTS.registerComponentType("craft_quality", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** Ten nguoi che. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CRAFTER =
            COMPONENTS.registerComponentType("crafter", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    /** Id cong thuc ghi tren quyen pho (vd dan:khai_mach). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> RECIPE_ID =
            COMPONENTS.registerComponentType("recipe_id", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final Supplier<AttachmentType<KnownRecipes>> KNOWN = ATTACHMENTS.register("known_recipes",
            () -> AttachmentType.builder(() -> KnownRecipes.EMPTY).serialize(KnownRecipes.CODEC).copyOnDeath().build());

    // ---- 0.3 nguyen lieu moi ----
    public static final DeferredItem<Item> CHU_SA = ITEMS.registerSimpleItem("chu_sa");
    /** Phu Chi: pham gan san qua craft_quality (Ha=0, Trung=1, Thuong=2) boi cong thuc ban thuong. */
    public static final DeferredItem<Item> PHU_CHI_HA = ITEMS.registerSimpleItem("phu_chi_ha");
    public static final DeferredItem<Item> PHU_CHI_TRUNG = ITEMS.registerSimpleItem("phu_chi_trung");
    public static final DeferredItem<Item> PHU_CHI_THUONG = ITEMS.registerSimpleItem("phu_chi_thuong");
    public static final DeferredItem<Item> LINH_MAC = ITEMS.registerSimpleItem("linh_mac");
    /** Toi Dich (Linh Tuyen Thuy): pham Trung gan san boi cong thuc. */
    public static final DeferredItem<Item> TOI_DICH = ITEMS.registerSimpleItem("toi_dich");
    public static final DeferredItem<Item> LINH_THAN = ITEMS.registerSimpleItem("linh_than");

    // ---- 0.2 quyen pho ----
    public static final DeferredItem<RecipeScrollItem> DAN_PHUONG = ITEMS.register("dan_phuong",
            () -> new RecipeScrollItem(RecipeScrollItem.Kind.DAN, new Item.Properties()));
    public static final DeferredItem<RecipeScrollItem> PHU_PHO = ITEMS.register("phu_pho",
            () -> new RecipeScrollItem(RecipeScrollItem.Kind.PHU, new Item.Properties()));
    public static final DeferredItem<RecipeScrollItem> KHI_PHO = ITEMS.register("khi_pho",
            () -> new RecipeScrollItem(RecipeScrollItem.Kind.KHI, new Item.Properties()));

    /** Quyen pho loai {@code k} ghi cong thuc {@code recipeId}. */
    public static ItemStack scroll(RecipeScrollItem.Kind k, String recipeId) {
        ItemStack s = new ItemStack(switch (k) {
            case DAN -> DAN_PHUONG.get();
            case PHU -> PHU_PHO.get();
            case KHI -> KHI_PHO.get();
        });
        s.set(RECIPE_ID.get(), recipeId);
        return s;
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
        ITEMS.register(bus);
        ATTACHMENTS.register(bus);
        bus.addListener((RegisterPayloadHandlersEvent e) -> RecipeBookNet.register(e.registrar("1")));
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
                for (DeferredItem<?> i : new DeferredItem<?>[]{CHU_SA, PHU_CHI_HA, PHU_CHI_TRUNG, PHU_CHI_THUONG,
                        LINH_MAC, TOI_DICH, LINH_THAN, DAN_PHUONG, PHU_PHO, KHI_PHO})
                    e.accept(i.get());
            }
        });
    }
}
