package com.hukhong.profession;

import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.phapkhi.PhapKhiKind;
import com.hukhong.phapkhi.PhapKhiRegistry;
import com.hukhong.phapkhi.PhapKhiTier;
import com.hukhong.resource.ResourceRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Bang cong thuc Tram che nghe phu (code, khong datapack).
 * Moi cong thuc dung dung 3 o nguyen lieu (hoac it hon) -> 1 ket qua.
 * PHU: Phu Van Su (Phu An). FORGE: Doan Tao Su (Doan Tao Dai) - mo rong bang registerForge().
 */
public final class ProfessionRecipes {
    private ProfessionRecipes() {}

    /** Ket qua. level = cap nghe hien tai; tierUp = co +1 bac pham (Kim Phap Tu, chi Doan Tao Su). */
    @FunctionalInterface
    public interface Output { ItemStack make(int level, boolean tierUp); }

    public record Need(Supplier<? extends Item> item, int count) {}

    public record Recipe(String id, Profession prof, List<Need> needs, int xp, Output out) {}

    /** Phu Van Su: moi loai Phap Khi phu/tran (15 loai) = giay + nguyen to + linh thao. Bac pham = min(4, cap/4). */
    public static final List<Recipe> PHU = buildPhu();

    /** Placeholder cho Doan Tao Su: Linh Thiet Phoi (4 sat thoi + linh thach ha). Nguyen lieu cho giap sau nay. */
    public static final Recipe LINH_THIET_PHOI_RECIPE = new Recipe("linh_thiet_phoi", Profession.DOAN_TAO,
            List.of(new Need(() -> Items.IRON_INGOT, 4), new Need(() -> ResourceRegistry.MATERIALS.get("linh_thach_ha").get(), 1)),
            15, (lv, up) -> new ItemStack(ProfessionRegistry.LINH_THIET_PHOI.get()));

    /** Cong thuc Doan Tao Dai. Dung registerForge() de them (pham chat giap se dung API nay). */
    public static final List<Recipe> FORGE = new CopyOnWriteArrayList<>(List.of(LINH_THIET_PHOI_RECIPE));

    /** Them cong thuc ran (goi luc khoi dong mod, truoc khi nguoi choi craft). */
    public static void registerForge(Recipe r) { FORGE.add(Objects.requireNonNull(r)); }

    private static List<Recipe> buildPhu() {
        List<Recipe> list = new ArrayList<>();
        for (PhapKhiKind k : PhapKhiKind.values()) {
            list.add(new Recipe("phu_" + k.id, Profession.PHU_VAN,
                    List.of(new Need(() -> Items.PAPER, 1), new Need(element(k), 1),
                            new Need(() -> AlchemyRegistry.LINH_THAO.get(), 1)),
                    10, (lv, up) -> PhapKhiRegistry.stack(k, PhapKhiTier.of(Math.min(4, lv / 4 + (up ? 1 : 0))))));
        }
        return List.copyOf(list);
    }

    private static Supplier<Item> element(PhapKhiKind k) {
        return switch (k) {
            case KIEM -> () -> Items.IRON_NUGGET;
            case HOA -> () -> Items.BLAZE_POWDER;
            case LOI -> () -> Items.LAPIS_LAZULI;
            case BANG -> () -> Items.SNOWBALL;
            case THO -> () -> Items.CLAY_BALL;
            case MA -> () -> Items.WITHER_ROSE;
            case QUANG -> () -> Items.GLOWSTONE_DUST;
            case HO_THAN -> () -> Items.GOLD_NUGGET;
            case KIM_CANG -> () -> Items.IRON_INGOT;
            case DON_KHONG -> () -> Items.ENDER_PEARL;
            case HOI_XUAN -> () -> Items.GLISTERING_MELON_SLICE;
            case TU_LINH -> () -> Items.EMERALD;
            case KHON -> () -> Items.SOUL_SAND;
            case SAT -> () -> Items.MAGMA_CREAM;
            case HO_TRAN -> () -> Items.DIAMOND;
        };
    }

    /** Tim cong thuc khop 3 o nguyen lieu (dung so luong, khong thua o nao). null neu khong khop. */
    public static Recipe find(List<Recipe> list, List<ItemStack> in) {
        for (Recipe r : list) if (match(r, in) != null) return r;
        return null;
    }

    /** Tru nguyen lieu theo cong thuc (goi sau khi find() != null). */
    public static void consume(Recipe r, List<ItemStack> in) {
        int[] idx = match(r, in);
        if (idx == null) return;
        for (int i = 0; i < idx.length; i++) in.get(idx[i]).shrink(r.needs().get(i).count());
    }

    /** Chi so o nguyen lieu cho tung Need (theo thu tu needs), hoac null. */
    private static int[] match(Recipe r, List<ItemStack> in) {
        int filled = 0;
        for (ItemStack s : in) if (!s.isEmpty()) filled++;
        if (r.needs().size() != filled) return null;
        int[] idx = new int[r.needs().size()];
        boolean[] used = new boolean[in.size()];
        for (int n = 0; n < idx.length; n++) {
            Need need = r.needs().get(n);
            int hit = -1;
            for (int i = 0; i < in.size() && hit < 0; i++) {
                ItemStack s = in.get(i);
                if (!used[i] && !s.isEmpty() && s.is(need.item().get()) && s.getCount() >= need.count()) hit = i;
            }
            if (hit < 0) return null;
            used[hit] = true;
            idx[n] = hit;
        }
        return idx;
    }
}
