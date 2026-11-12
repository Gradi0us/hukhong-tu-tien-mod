package com.hukhong.phapkhi.hothe;

import com.hukhong.HuKhong;
import com.hukhong.phapkhi.PhapKhiTier;
import com.hukhong.profession.Profession;
import com.hukhong.profession.ProfessionRecipes;
import com.hukhong.profession.ProfessionRegistry;
import com.hukhong.resource.ResourceRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 20 Pháp Khí hộ thể (4 loại x 5 phẩm) + 5 ArmorMaterial (một cho mỗi phẩm, dùng cho lớp render giáp).
 * Công thức rèn Đoán Tạo Đài: 4 loại x 5 phẩm, cấp nghề tối thiểu = 4 * phẩm + 1, bậc công thức = phẩm + 1 (phẩm 1 cần Lv 1, phẩm 5 cần Lv 17).
 */
public final class HoTheRegistry {
    private HoTheRegistry() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister<ArmorMaterial> MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, HuKhong.MODID);

    /** Giáp theo phẩm: giáp (defense) theo ô, độ cứng/kháng đẩy để 0 (riêng món thêm qua HoTheArmorItem). */
    private static final Map<PhapKhiTier, DeferredHolder<ArmorMaterial, ArmorMaterial>> MAT = new EnumMap<>(PhapKhiTier.class);
    private static final Map<HoTheKind, DeferredItem<HoTheArmorItem>[]> BY_KIND = new EnumMap<>(HoTheKind.class);

    static {
        for (PhapKhiTier t : PhapKhiTier.values()) {
            int i = t.ordinal();
            Map<ArmorItem.Type, Integer> def = new EnumMap<>(ArmorItem.Type.class);
            for (HoTheKind k : HoTheKind.values()) def.put(k.type, k.defense[i]);
            List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(rl("hothe_" + t.id)));
            MAT.put(t, MATERIALS.register("hothe_" + t.id, () -> new ArmorMaterial(def, 15,
                    SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(ProfessionRegistry.LINH_THIET_PHOI.get()),
                    layers, 0f, 0f)));
        }
        for (HoTheKind k : HoTheKind.values()) {
            @SuppressWarnings("unchecked")
            DeferredItem<HoTheArmorItem>[] arr = new DeferredItem[PhapKhiTier.values().length];
            for (PhapKhiTier t : PhapKhiTier.values()) {
                arr[t.ordinal()] = ITEMS.register(id(k, t), () -> new HoTheArmorItem(k, t, MAT.get(t), new Item.Properties()));
            }
            BY_KIND.put(k, arr);
        }
    }

    public static String id(HoTheKind k, PhapKhiTier t) { return k.id + "_" + t.id; }

    public static HoTheArmorItem item(HoTheKind k, PhapKhiTier t) { return BY_KIND.get(k)[t.ordinal()].get(); }

    public static ItemStack stack(HoTheKind k, PhapKhiTier t) { return new ItemStack(item(k, t)); }

    static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, path); }

    public static void register(IEventBus bus) {
        MATERIALS.register(bus);
        ITEMS.register(bus);
        for (HoTheKind k : HoTheKind.values()) addRecipes(k);
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() != CreativeModeTabs.COMBAT) return;
            for (HoTheKind k : HoTheKind.values())
                for (PhapKhiTier t : PhapKhiTier.values()) e.accept(item(k, t));
        });
    }

    /**
     * Phẩm i (0..4): nguyên liệu khung (Linh Thiết Phôi / Linh Thạch / Huyền Thiết) + nguyên liệu vanilla + xúc tác riêng loại.
     * Output: phẩm = i, +1 nếu Kim bonus trúng (trần Thiên).
     */
    private static void addRecipes(HoTheKind k) {
        for (PhapKhiTier t : PhapKhiTier.values()) {
            int i = t.ordinal();
            Supplier<Item> frame;
            int frameN;
            Supplier<Item> main;
            int mainN;
            switch (i) {
                case 0 -> { frame = () -> ProfessionRegistry.LINH_THIET_PHOI.get(); frameN = 1; main = () -> Items.LEATHER; mainN = 4; }
                case 1 -> { frame = () -> ProfessionRegistry.LINH_THIET_PHOI.get(); frameN = 2; main = () -> Items.IRON_INGOT; mainN = 4; }
                case 2 -> { frame = () -> ResourceRegistry.MATERIALS.get("linh_thach_trung").get(); frameN = 2; main = () -> Items.GOLD_INGOT; mainN = 4; }
                case 3 -> { frame = () -> ResourceRegistry.MATERIALS.get("linh_thach_thuong").get(); frameN = 2; main = () -> Items.DIAMOND; mainN = 2; }
                default -> { frame = () -> ResourceRegistry.MATERIALS.get("huyen_thiet").get(); frameN = 2; main = () -> Items.NETHERITE_INGOT; mainN = 1; }
            }
            int tier = i;
            ProfessionRecipes.registerForge(new ProfessionRecipes.Recipe(
                    "hothe_" + id(k, t), Profession.DOAN_TAO,
                    List.of(new ProfessionRecipes.Need(frame, frameN), new ProfessionRecipes.Need(main, mainN),
                            new ProfessionRecipes.Need(k::catalyst, 1)),
                    20 + 15 * i,
                    (lv, up) -> stack(k, PhapKhiTier.of(Math.min(4, tier + (up ? 1 : 0)))),
                    4 * i + 1, i + 1));
        }
    }
}
