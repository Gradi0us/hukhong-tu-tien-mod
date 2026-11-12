package com.hukhong.alchemy;

import com.hukhong.item.ModItems;
import com.hukhong.resource.ResourceRegistry;
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
                    AlchemyRegistry.THO_NGUYEN_DAN, 1),
            // ---- Dot 7b (G10): dan tier 7-9, Tu Linh Dan; nguyen lieu tu linh thao tu nhien (resource.ResourceRegistry) ----
            new Recipe("tu_linh", 3, List.of(n(AlchemyRegistry.LINH_THAO, 2), n(ResourceRegistry.herbItem("kim_tinh_thao"), 1), n(AlchemyRegistry.THANH_DIEP, 1)),
                    ResourceRegistry.TU_LINH_DAN, 1),
            new Recipe("huyen_ton", 7, List.of(n(ResourceRegistry.herbItem("hoa_linh_chi"), 3), n(ResourceRegistry.herbItem("bang_lien"), 2),
                    n(ResourceRegistry.herbItem("kim_tinh_thao"), 1)), ResourceRegistry.HUYEN_TON_DAN, 1),
            new Recipe("thanh_co", 8, List.of(n(ResourceRegistry.herbItem("tu_linh_thao"), 3), n(ResourceRegistry.herbItem("ngung_hon_hoa"), 2),
                    n(ResourceRegistry.herbItem("kim_tinh_thao"), 2)), ResourceRegistry.THANH_CO_DAN, 1),
            new Recipe("than_dan", 9, List.of(n(ResourceRegistry.herbItem("ngung_hon_hoa"), 3), n(ResourceRegistry.herbItem("tu_linh_thao"), 3),
                    n(() -> ResourceRegistry.MATERIALS.get("tinh_than_sa").get(), 1)), ResourceRegistry.THAN_DAN, 1),
            // ---- 2026-11-06: linh duoc tu linh thao moi ----
            new Recipe("hoi_linh", 2, List.of(n(ResourceRegistry.herbItem("hoang_tinh"), 2), n(ResourceRegistry.herbItem("thuy_tam_thao"), 1)),
                    ResourceRegistry.linhDuoc("hoi_linh_dan"), 2),
            new Recipe("dai_hoi_linh", 5, List.of(n(ResourceRegistry.herbItem("bich_hai_thao"), 2), n(ResourceRegistry.herbItem("tinh_than_hoa"), 1),
                    n(ResourceRegistry.herbItem("ngoc_tuy_chi"), 1)), ResourceRegistry.linhDuoc("dai_hoi_linh_dan"), 1),
            new Recipe("thanh_tam", 4, List.of(n(ResourceRegistry.herbItem("am_duong_thao"), 2), n(ResourceRegistry.herbItem("u_minh_hoa"), 1)),
                    ResourceRegistry.linhDuoc("thanh_tam_dan"), 1),
            new Recipe("tay_tuy", 6, List.of(n(ResourceRegistry.herbItem("thien_son_tuyet_lien"), 1), n(ResourceRegistry.herbItem("ha_thu_o"), 2),
                    n(ResourceRegistry.herbItem("ngoc_tuy_chi"), 1)), ResourceRegistry.linhDuoc("tay_tuy_dan"), 1),
            new Recipe("kim_cuong", 3, List.of(n(ResourceRegistry.herbItem("dia_long_can"), 2), n(ResourceRegistry.herbItem("ha_thu_o"), 1)),
                    ResourceRegistry.linhDuoc("kim_cuong_dan"), 1),
            new Recipe("bao_huyet", 4, List.of(n(ResourceRegistry.herbItem("long_huyet_thao"), 2), n(ResourceRegistry.herbItem("xich_duong_chi"), 1)),
                    ResourceRegistry.linhDuoc("bao_huyet_dan"), 1),
            new Recipe("phong_hanh", 3, List.of(n(ResourceRegistry.herbItem("phong_linh_diep"), 2), n(ResourceRegistry.herbItem("loi_kich_moc"), 1)),
                    ResourceRegistry.linhDuoc("phong_hanh_dan"), 1),
            new Recipe("van_nien", 7, List.of(n(ResourceRegistry.herbItem("van_nien_linh_chi"), 1), n(AlchemyRegistry.HUYET_THAM, 2),
                    n(ResourceRegistry.herbItem("tinh_than_hoa"), 1)), ResourceRegistry.linhDuoc("van_nien_linh_dich"), 1),
            // ---- Dan chien dau (PLAN-v1-profession-crafting §1.4): dung trong the tu luyen, chung hoi chieu 30s ----
            new Recipe("hoi_khi", 2, List.of(n(AlchemyRegistry.LINH_THAO, 3), n(AlchemyRegistry.THANH_DIEP, 1)),
                    AlchemyRegistry.HOI_KHI_DAN, 2),
            new Recipe("phong_hanh_chien", 2, List.of(n(AlchemyRegistry.LINH_THAO, 2), n(AlchemyRegistry.THANH_DIEP, 1), n(AlchemyRegistry.HOA_LIEN, 1)),
                    AlchemyRegistry.PHONG_HANH_CHIEN_DAN, 2),
            new Recipe("cuong_huyet", 3, List.of(n(AlchemyRegistry.HUYET_THAM, 2), n(AlchemyRegistry.HOA_LIEN, 2)),
                    AlchemyRegistry.CUONG_HUYET_DAN, 1),
            new Recipe("kim_cuong_chien", 4, List.of(n(AlchemyRegistry.HOA_LIEN, 3), n(AlchemyRegistry.THANH_DIEP, 1)),
                    AlchemyRegistry.KIM_CUONG_CHIEN_DAN, 1));

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
