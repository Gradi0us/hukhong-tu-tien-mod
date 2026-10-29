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

    public static final String[] KEYS = {"herb", "pill", "weapon", "armor", "soul", "general", "scroll", "evo", "special"};
    /** Loai thuong nhan chi ban hang mod o khu cho (weapon/armor/general cu: khong con hang vanilla -> rong, khong con duoc sinh). */
    public static final int K_EVO = 7, K_SPECIAL = 8;
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
    public static List<Offer> buys(long worldSeed, int kind, CityDef d) { return buys(worldSeed, kind, d, 0L); }

    /** lichDay = ngay Lich hien tai (Quay Dac Biet xoay vong theo ngay nay). */
    public static List<Offer> buys(long worldSeed, int kind, CityDef d, long lichDay) {
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
                add(o, 2500, AlchemyRegistry.TU_VUONG_DAN.get(), 1, 2);
            }
            case 2, 3, 5 -> { /* weapon / armor / general cu: chi co hang vanilla -> khong ban nua (cho chi ban do mod) */ }
            case 4 -> {
                add(o, 12, ModItems.HON_TINH.get(), 1);
                add(o, 30, ModItems.HON_TINH.get(), 3);
                add(o, 2500, ModItems.HON_TINH.get(), 10, 2);
            }
            case K_EVO -> { // vat lieu tien hoa Ban Menh / Linh Bao / Linh Truong = Hon Tinh (tien trong vi) + Pha Canh Dan
                add(o, 1100, ModItems.HON_TINH.get(), 10, 1);
                add(o, 5000, ModItems.HON_TINH.get(), 50, 2);
                add(o, 9500, ModItems.HON_TINH.get(), 100, 3);
                add(o, 800, ModItems.KHAI_MACH_DAN.get(), 1, 1);
                add(o, 2400, ModItems.NGUNG_NGUYEN_DAN.get(), 1, 1);
                add(o, 9000, ModItems.TIEU_KINH_DAN_V.get(), 1, 2);
                add(o, 20000, ModItems.TIEU_BAN_THAN_DAN.get(), 1, 3);
            }
            case K_SPECIAL -> special(o, worldSeed, d, lichDay);
            default -> {
                String id = d == null ? "" : d.id();
                Random r = new Random(worldSeed ^ id.hashCode());
                for (int i = 0; i < 3; i++) {
                    SkillGrade g = tier == Tier.THIEN ? (i == 0 ? SkillGrade.DIA : SkillGrade.HUYEN) : (i == 2 ? SkillGrade.HUYEN : SkillGrade.PHAM);
                    long price = (g == SkillGrade.DIA ? 48L : g == SkillGrade.HUYEN ? 24L : 12L) * 100L; // 12/24/48 Trung pham
                    o.add(new Offer(SkillBookRewards.stack(r, g, null), price, 0));
                }
                Random rr = new Random(worldSeed ^ id.hashCode() ^ 0x7A11L);
                o.add(new Offer(SkillBookRewards.stack(rr, SkillGrade.DIA, null), 48L * 100L * 2, 2));
                // Ton Khach (bac 4): bi tich Thien pham
                Random rt = new Random(worldSeed ^ id.hashCode() ^ 0x7B22L);
                o.add(new Offer(SkillBookRewards.stack(rt, SkillGrade.THIEN, null), 30_000L, 4));
            }
        }
        return o;
    }

    /** Gia Hon Cot theo pham (Tu/Hac/Hong), Ha. */
    public static final long[] BONE_PRICE = {6_000L, 15_000L, 40_000L};
    /** Gia Truc Nang Cap Ky Nang (Ha). */
    public static final long SCROLL_PRICE = 12_000L;

    /**
     * Quay Dac Biet (bac Thuong Khach 3+): Hon Cot xoay vong moi ngay Lich (96000 tick) + Truc Nang Cap Ky Nang.
     * Bac 3: 3 Hon Cot pham Tu/Hac. Bac 4 (Ton Khach): them 2 Hon Cot pham Hac/Hong + 1 Truc nua. Tat dinh theo (seed, thanh, ngay).
     */
    static void special(List<Offer> o, long worldSeed, CityDef d, long lichDay) {
        String id = d == null ? "" : d.id();
        Random r = new Random(worldSeed ^ id.hashCode() * 31L ^ lichDay * 0x9E3779B97F4A7C15L);
        com.hukhong.honcot.Bone[] bones = com.hukhong.honcot.Bone.values();
        for (int i = 0; i < 3; i++) {
            int tierBone = r.nextInt(3) == 0 ? com.hukhong.honcot.HonCotMath.TIER_HAC : com.hukhong.honcot.HonCotMath.TIER_TU;
            o.add(new Offer(com.hukhong.honcot.HonCotRegistry.stack(bones[r.nextInt(bones.length)], tierBone, 45 + r.nextInt(30)),
                    BONE_PRICE[tierBone], com.hukhong.market.MarketRep.SPECIAL_TIER));
        }
        o.add(new Offer(new ItemStack(com.hukhong.skillbook.SkillBookRegistry.UPGRADE_SCROLL.get()), SCROLL_PRICE, com.hukhong.market.MarketRep.SPECIAL_TIER));
        for (int i = 0; i < 2; i++) {
            int tierBone = r.nextInt(3) == 0 ? com.hukhong.honcot.HonCotMath.TIER_HONG : com.hukhong.honcot.HonCotMath.TIER_HAC;
            o.add(new Offer(com.hukhong.honcot.HonCotRegistry.stack(bones[r.nextInt(bones.length)], tierBone, 70 + r.nextInt(30)),
                    BONE_PRICE[tierBone], 4));
        }
        o.add(new Offer(new ItemStack(com.hukhong.skillbook.SkillBookRegistry.UPGRADE_SCROLL.get(), 3), SCROLL_PRICE * 3 - 4_000L, 4));
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
        // Hon Tinh la tien trong vi, khong con thu mua duoc.
        return o;
    }

    public static Component kindName(int kind) { return Component.translatable(NAME_PREFIX + KEYS[Math.max(0, Math.min(KEYS.length - 1, kind))]); }
}
