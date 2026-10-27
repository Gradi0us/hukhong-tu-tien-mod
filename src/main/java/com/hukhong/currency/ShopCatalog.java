package com.hukhong.currency;

import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.city.CityDef;
import com.hukhong.city.Tier;
import com.hukhong.item.ModItems;
import com.hukhong.skillbook.SkillBookRewards;
import com.hukhong.skillbook.SkillGrade;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Hang hoa cua thuong nhan trong thanh (gia bang Ha pham Tu Tinh; 1 ngoc cu = 10 Ha). Loai thuong nhan lay tu ten
 * ("hukhong.trade.<key>") nen thuong nhan sinh tu ban cu van chay. minRank = bac Hiep Hoi mo hang hiem.
 */
public final class ShopCatalog {
    private ShopCatalog() {}

    public static final String[] KEYS = {"herb", "pill", "weapon", "armor", "soul", "general", "scroll"};
    public static final String NAME_PREFIX = "hukhong.trade.";

    public record Offer(ItemStack stack, long price, int minRank) {}

    /** Loai thuong nhan (0..6) hoac -1 neu khong phai thuong nhan cho. */
    public static int kindOf(Entity e) {
        if (e.getCustomName() == null || !(e.getCustomName().getContents() instanceof TranslatableContents tc)) return -1;
        String k = tc.getKey();
        if (!k.startsWith(NAME_PREFIX)) return -1;
        String s = k.substring(NAME_PREFIX.length());
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equals(s)) return i;
        return -1;
    }

    private static void add(List<Offer> l, int emeralds, Item item, int count) { add(l, TuTinh.fromEmerald(emeralds), item, count, 0); }

    private static void add(List<Offer> l, long ha, Item item, int count, int minRank) {
        l.add(new Offer(new ItemStack(item, count), ha, minRank));
    }

    /** Hang ban (gia goc, chua giam gia). d co the null (mac dinh cap Nhan). */
    public static List<Offer> buys(long worldSeed, int kind, CityDef d) {
        Tier tier = d == null ? Tier.NHAN : d.tier();
        int mul = tier.ordinal() + 1;
        List<Offer> o = new ArrayList<>();
        switch (kind) {
            case 0 -> {
                add(o, 1, AlchemyRegistry.LINH_THAO.get(), 3);
                add(o, 2, AlchemyRegistry.THANH_DIEP.get(), 2);
                add(o, 3, AlchemyRegistry.HUYET_THAM.get(), 1);
                add(o, 3, AlchemyRegistry.HOA_LIEN.get(), 1);
            }
            case 1 -> {
                add(o, 3, AlchemyRegistry.CHI_HUYET_DAN.get(), 1);
                add(o, 4, AlchemyRegistry.HOI_XUAN_DAN.get(), 1);
                add(o, 3, AlchemyRegistry.GIAI_DOC_DAN.get(), 1);
                add(o, 8 * mul, AlchemyRegistry.THO_NGUYEN_DAN.get(), 1);
                add(o, 600, AlchemyRegistry.THONG_HUYEN_DAN.get(), 1, 2);
                add(o, 2500, AlchemyRegistry.TU_VUONG_DAN.get(), 1, 3);
            }
            case 2 -> {
                add(o, 6, Items.IRON_SWORD, 1);
                add(o, 8, Items.BOW, 1);
                add(o, 2, Items.ARROW, 16);
                add(o, 14 * mul, Items.DIAMOND_SWORD, 1);
                add(o, 8000, Items.NETHERITE_SWORD, 1, 4);
            }
            case 3 -> {
                add(o, 8, Items.IRON_CHESTPLATE, 1);
                add(o, 5, Items.IRON_HELMET, 1);
                add(o, 4, Items.SHIELD, 1);
                add(o, 18 * mul, Items.DIAMOND_CHESTPLATE, 1);
                add(o, 10000, Items.NETHERITE_CHESTPLATE, 1, 4);
            }
            case 4 -> {
                add(o, 12, ModItems.HON_TINH.get(), 1);
                add(o, 30, ModItems.HON_TINH.get(), 3);
                add(o, 20, Items.ENDER_PEARL, 1);
                add(o, 2500, ModItems.HON_TINH.get(), 10, 2);
            }
            case 5 -> {
                add(o, 1, Items.BREAD, 6);
                add(o, 1, Items.TORCH, 16);
                add(o, 3, Items.COOKED_BEEF, 8);
                add(o, 5, Items.BUCKET, 1);
                add(o, 2, Items.LEAD, 1);
            }
            default -> {
                String id = d == null ? "" : d.id();
                Random r = new Random(worldSeed ^ id.hashCode());
                for (int i = 0; i < 3; i++) {
                    SkillGrade g = tier == Tier.THIEN ? (i == 0 ? SkillGrade.DIA : SkillGrade.HUYEN) : (i == 2 ? SkillGrade.HUYEN : SkillGrade.PHAM);
                    long price = (g == SkillGrade.DIA ? 48L : g == SkillGrade.HUYEN ? 24L : 12L) * 100L; // 12/24/48 Trung pham
                    o.add(new Offer(SkillBookRewards.stack(r, g, null), price, 0));
                }
                Random rr = new Random(worldSeed ^ id.hashCode() ^ 0x7A11L);
                o.add(new Offer(SkillBookRewards.stack(rr, SkillGrade.DIA, null), 48L * 100L * 2, 3));
            }
        }
        return o;
    }

    /** Hang thu mua (thao duoc, Hon Tinh roi tu Hu Hoa). */
    public static List<Offer> sells(int kind) {
        List<Offer> o = new ArrayList<>();
        if (kind == 0 || kind == 1 || kind == 5) {
            add(o, 2L, AlchemyRegistry.LINH_THAO.get(), 1, 0);
            add(o, 3L, AlchemyRegistry.THANH_DIEP.get(), 1, 0);
            add(o, 8L, AlchemyRegistry.HUYET_THAM.get(), 1, 0);
            add(o, 10L, AlchemyRegistry.HOA_LIEN.get(), 1, 0);
        }
        if (kind == 4 || kind == 5) add(o, 30L, ModItems.HON_TINH.get(), 1, 0);
        return o;
    }

    public static Component kindName(int kind) { return Component.translatable(NAME_PREFIX + KEYS[Math.max(0, Math.min(KEYS.length - 1, kind))]); }
}
