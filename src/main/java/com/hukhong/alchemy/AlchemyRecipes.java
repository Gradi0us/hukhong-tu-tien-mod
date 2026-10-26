package com.hukhong.alchemy;

import com.hukhong.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/** Bảng đan phương (code): nguyên liệu (tổng các ô thảo dược) -> đan, phẩm, số viên. */
public final class AlchemyRecipes {
    private AlchemyRecipes() {}

    public record Need(Supplier<? extends Item> item, int count) {}
    public record Recipe(String id, int tier, List<Need> needs, Supplier<? extends Item> result, int yield) {}

    private static Need n(Supplier<? extends Item> i, int c) { return new Need(i, c); }

    public static final List<Recipe> ALL = List.of(
            new Recipe("khai_mach", 1, List.of(n(AlchemyRegistry.LINH_THAO, 2), n(AlchemyRegistry.THANH_DIEP, 1)),
                    ModItems.KHAI_MACH_DAN, 1),
            new Recipe("ngung_nguyen", 2, List.of(n(AlchemyRegistry.LINH_THAO, 3), n(AlchemyRegistry.HUYET_THAM, 1)),
                    ModItems.NGUNG_NGUYEN_DAN, 1),
            new Recipe("thong_huyen", 3, List.of(n(AlchemyRegistry.LINH_THAO, 3), n(AlchemyRegistry.HUYET_THAM, 2)),
                    AlchemyRegistry.THONG_HUYEN_DAN, 1),
            new Recipe("tu_vuong", 4, List.of(n(AlchemyRegistry.HUYET_THAM, 3), n(AlchemyRegistry.HOA_LIEN, 1)),
                    AlchemyRegistry.TU_VUONG_DAN, 1),
            new Recipe("hoang_cuc", 5, List.of(n(AlchemyRegistry.HUYET_THAM, 3), n(AlchemyRegistry.HOA_LIEN, 2)),
                    AlchemyRegistry.HOANG_CUC_DAN, 1),
            new Recipe("de_hoan", 6, List.of(n(AlchemyRegistry.HUYET_THAM, 4), n(AlchemyRegistry.HOA_LIEN, 3)),
                    AlchemyRegistry.DE_HOAN_DAN, 1),
            new Recipe("chi_huyet", 1, List.of(n(AlchemyRegistry.LINH_THAO, 1), n(AlchemyRegistry.THANH_DIEP, 1)),
                    AlchemyRegistry.CHI_HUYET_DAN, 2),
            new Recipe("hoi_xuan", 1, List.of(n(AlchemyRegistry.LINH_THAO, 2), n(AlchemyRegistry.THANH_DIEP, 2)),
                    AlchemyRegistry.HOI_XUAN_DAN, 2),
            new Recipe("giai_doc", 2, List.of(n(AlchemyRegistry.THANH_DIEP, 3), n(AlchemyRegistry.HOA_LIEN, 1)),
                    AlchemyRegistry.GIAI_DOC_DAN, 2),
            new Recipe("tho_nguyen", 3, List.of(n(AlchemyRegistry.LINH_THAO, 2), n(AlchemyRegistry.HUYET_THAM, 2), n(AlchemyRegistry.HOA_LIEN, 1)),
                    AlchemyRegistry.THO_NGUYEN_DAN, 1));

    private static int total(List<ItemStack> in, Item item) {
        int c = 0;
        for (ItemStack s : in) if (s.is(item)) c += s.getCount();
        return c;
    }

    /** Phương khớp (đủ số lượng, không có thảo dược lạ). Ưu tiên phương nhiều nguyên liệu hơn. */
    public static Recipe match(List<ItemStack> herbs) {
        Recipe best = null;
        int bestSize = -1;
        for (Recipe r : ALL) {
            boolean ok = true;
            int size = 0;
            for (Need nd : r.needs()) {
                if (total(herbs, nd.item().get()) < nd.count()) { ok = false; break; }
                size += nd.count();
            }
            if (ok) for (ItemStack s : herbs) {
                if (s.isEmpty()) continue;
                boolean listed = false;
                for (Need nd : r.needs()) if (s.is(nd.item().get())) listed = true;
                if (!listed) { ok = false; break; }
            }
            if (ok && size > bestSize) { best = r; bestSize = size; }
        }
        return best;
    }
}
