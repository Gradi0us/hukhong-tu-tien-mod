package com.hukhong.auction;

import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.currency.ShopCatalog;
import com.hukhong.honcot.Bone;
import com.hukhong.honcot.HonCotMath;
import com.hukhong.honcot.HonCotRegistry;
import com.hukhong.item.ModItems;
import com.hukhong.skillbook.SkillBookRegistry;
import com.hukhong.skillbook.SkillBookRewards;
import com.hukhong.skillbook.SkillGrade;
import com.hukhong.story.StoryRegistry;
import com.hukhong.story.world.Faction;
import com.hukhong.story.world.Lore;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Sinh 6-10 lo cua Dau Gia Hoi: Hon Cot cao pham, bi tich Thien/Dia, dan quy, lo cot truyen (khoa theo chuong), lo cuoi bao vat bi an. */
public final class LiveLots {
    private LiveLots() {}

    /** stack = vat pham that; mystery = giau cho den khi tung; lore != null -> khoa theo chuong; start = gia khoi diem (Ha). */
    public record Entry(ItemStack stack, boolean mystery, Lore lore, long start, long valuation, String storyKey) {}

    public static final Lore CHG_LORE = new Lore(Faction.CHG, 6, 99, List.of(), List.of("wl.auction_chg_bone"), null, null, null);
    public static final Lore B_DOC_LORE = new Lore(Faction.DA_KHUYET, 4, 99, List.of(), List.of("wl.auction_b_doc"), null, null, null);

    public static List<Entry> generate(Random r, int count) {
        count = Math.max(6, Math.min(10, count));
        List<Entry> body = new ArrayList<>();
        // lo cot truyen
        ItemStack chg = HonCotRegistry.stack(Bone.HU_KHONG_HACH, HonCotMath.TIER_HONG, 96);
        chg.set(DataComponents.CUSTOM_NAME, Tx.t("hukhong.live.lot.chg", "Hồn Cốt Chấp Hành Giả", "Executor's Soul Bone"));
        body.add(new Entry(chg, false, CHG_LORE, ShopCatalog.BONE_PRICE[2] * 2, ShopCatalog.BONE_PRICE[2] * 4, "chg"));
        String[] docs = {"b6", "b9", "b10"};
        ItemStack doc = new ItemStack(StoryRegistry.DOC_ITEMS.get(docs[r.nextInt(docs.length)]).get());
        body.add(new Entry(doc, false, B_DOC_LORE, 6_000, 14_000, "bdoc"));
        while (body.size() < count - 1) body.add(random(r));
        Collections.shuffle(body, r);
        body.add(mystery(r));
        return body;
    }

    private static Entry entry(ItemStack s, long start, double val) { return new Entry(s, false, null, start, Math.round(start * val), null); }

    static Entry random(Random r) {
        double v = 1.5 + r.nextDouble();
        return switch (r.nextInt(8)) {
            case 0, 1 -> {
                int t = r.nextInt(3) == 0 ? HonCotMath.TIER_HONG : HonCotMath.TIER_HAC;
                yield entry(HonCotRegistry.stack(Bone.values()[r.nextInt(Bone.values().length)], t, 60 + r.nextInt(41)), ShopCatalog.BONE_PRICE[t], v);
            }
            case 2 -> entry(SkillBookRewards.stack(r, SkillGrade.THIEN, null), 30_000, v);
            case 3 -> entry(SkillBookRewards.stack(r, SkillGrade.DIA, null), 8_000, v);
            case 4 -> entry(new ItemStack(AlchemyRegistry.TU_VUONG_DAN.get()), 2_500, v);
            case 5 -> entry(new ItemStack(AlchemyRegistry.HOANG_CUC_DAN.get()), 8_000, v);
            case 6 -> entry(new ItemStack(SkillBookRegistry.UPGRADE_SCROLL.get(), 3), ShopCatalog.SCROLL_PRICE * 3 - 4_000, v);
            default -> entry(new ItemStack(ModItems.TIEU_KINH_DAN_V.get()), 9_000, v);
        };
    }

    /** Bao vat bi an: tung roi moi biet la vat gi (co the lai/lo). */
    static Entry mystery(Random r) {
        ItemStack s;
        int k = r.nextInt(100);
        if (k < 30) s = HonCotRegistry.stack(Bone.values()[r.nextInt(Bone.values().length)], HonCotMath.TIER_HONG, 70 + r.nextInt(31));
        else if (k < 60) s = SkillBookRewards.stack(r, SkillGrade.THIEN, null);
        else if (k < 85) s = new ItemStack(AlchemyRegistry.DE_HOAN_DAN.get());
        else s = new ItemStack(AlchemyRegistry.LINH_THAO.get(), 20);
        return new Entry(s, true, null, 5_000, 15_000, null);
    }
}
