package com.hukhong.auction;

import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.currency.CurrencyRegistry;
import com.hukhong.currency.ShopCatalog;
import com.hukhong.honcot.HonCotItem;
import com.hukhong.item.ModItems;
import com.hukhong.skillbook.BookSkill;
import com.hukhong.skillbook.SkillBookRegistry;
import com.hukhong.skillbook.SkillScrollItem;
import com.hukhong.skillbook.SkillUpgradeScrollItem;
import com.hukhong.story.VanKienItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;

import java.util.HashMap;
import java.util.Map;

/** Gia san theo pham, phan loai bo loc, va dieu kien "giao dich duoc" cua Dau Gia Hanh. */
public final class AuctionPricing {
    private AuctionPricing() {}

    public static final int ALL = 0, HONCOT = 1, PILL = 2, BOOK = 3, MATERIAL = 4, GEAR = 5, FILTERS = 6;
    private static final long[] BOOK_PRICE = {1_200L, 2_400L, 4_800L, 30_000L};
    private static final long VAN_KIEN_PRICE = 3_000L;

    private static volatile Map<Item, Long> unit;

    /** Gia ban/mua 1 don vi cua hang thuong nhan (lazily, can registry da dang ky). */
    private static Map<Item, Long> unit() {
        Map<Item, Long> m = unit;
        if (m == null) {
            m = new HashMap<>();
            for (int k : new int[]{0, 1, 5}) for (ShopCatalog.Offer o : ShopCatalog.sells(k)) m.merge(o.stack().getItem(), Math.max(1, o.price() / o.stack().getCount()), Math::max);
            for (int k : new int[]{0, 1, ShopCatalog.K_EVO}) for (ShopCatalog.Offer o : ShopCatalog.buys(0L, k, null))
                m.merge(o.stack().getItem(), Math.max(1, o.price() / 2 / o.stack().getCount()), Math::max);
            unit = m;
        }
        return m;
    }

    /** Hang khong dua len san: tien te, vat chua do (hop, tui) - tranh nhan doi du lieu. */
    public static boolean tradeable(ItemStack s) {
        if (s.isEmpty()) return false;
        Item it = s.getItem();
        if (it == CurrencyRegistry.HA.get() || it == CurrencyRegistry.TRUNG.get() || it == CurrencyRegistry.THUONG.get() || it == ModItems.HON_TINH.get()) return false;
        return !s.has(DataComponents.CONTAINER) && !s.has(DataComponents.BUNDLE_CONTENTS);
    }

    /** Gia san (Ha) cho CA chong. */
    public static long floor(ItemStack s) {
        Item it = s.getItem();
        int n = Math.max(1, s.getCount());
        if (it instanceof HonCotItem) return ShopCatalog.BONE_PRICE[HonCotItem.tierOf(s)];
        if (it instanceof SkillScrollItem) {
            BookSkill b = SkillBookRegistry.skillOf(s);
            return BOOK_PRICE[b == null ? 0 : b.grade.ordinal()];
        }
        if (it instanceof SkillUpgradeScrollItem) return ShopCatalog.SCROLL_PRICE * n;
        if (it instanceof VanKienItem) return VAN_KIEN_PRICE * n;
        return unit().getOrDefault(it, 1L) * n;
    }

    public static int category(ItemStack s) {
        Item it = s.getItem();
        if (it instanceof HonCotItem) return HONCOT;
        if (it instanceof SkillScrollItem || it instanceof SkillUpgradeScrollItem || it instanceof VanKienItem) return BOOK;
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it).getPath();
        if (path.endsWith("_dan") || path.equals("dan")) return PILL;
        if (it instanceof ArmorItem || it instanceof TieredItem || it instanceof ProjectileWeaponItem || it instanceof ShieldItem) return GEAR;
        return MATERIAL;
    }

    /** Dung cho NPC ky gui / test: tien Ha toi thieu mot chong herb (khong dung registry cung). */
    public static boolean isHerb(Item it) {
        return it == AlchemyRegistry.LINH_THAO.get() || it == AlchemyRegistry.THANH_DIEP.get()
                || it == AlchemyRegistry.HUYET_THAM.get() || it == AlchemyRegistry.HOA_LIEN.get();
    }
}
