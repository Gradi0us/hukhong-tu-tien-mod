package com.hukhong.phapkhi.hothe;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/**
 * 4 Pháp Khí hộ thể (4 ô giáp). Mọi số cân bằng theo phẩm nằm ở đây (index = PhapKhiTier.ordinal()).
 * Phẩm 5 mức như Pháp Khí: Phàm / Linh / Huyền / Địa / Thiên.
 */
public enum HoTheKind {
    TINH_TAM_QUAN("tinh_tam_quan", "Tịnh Tâm Quan", ArmorItem.Type.HELMET, 0xC9B6FF,
            new int[]{2, 3, 4, 5, 6}, () -> Items.AMETHYST_SHARD),
    HO_TAM_GIAP("ho_tam_giap", "Hộ Tâm Giáp", ArmorItem.Type.CHESTPLATE, 0xE8E0C8,
            new int[]{4, 6, 7, 8, 9}, () -> Items.TURTLE_SCUTE),
    KIM_CANG_HO_THOI("kim_cang_ho_thoi", "Kim Cang Hộ Thối", ArmorItem.Type.LEGGINGS, 0xE0B040,
            new int[]{3, 4, 5, 6, 8}, () -> Items.NETHERITE_SCRAP),
    PHI_VAN_NGOA("phi_van_ngoa", "Phi Vân Ngoa", ArmorItem.Type.BOOTS, 0x8FE6FF,
            new int[]{2, 3, 4, 5, 6}, () -> Items.FEATHER);

    /** Tịnh Tâm Quan: giảm thời gian hiệu ứng xấu 10% .. 40%. */
    public static final double[] TINH_REDUCE = {0.10, 0.175, 0.25, 0.325, 0.40};
    /** Hộ Tâm Giáp: máu tối đa +4 .. +40. */
    public static final double[] HO_HP = {4, 10, 18, 28, 40};
    /** Kim Cang Hộ Thối: độ cứng giáp 0.5 .. 2.5. */
    public static final double[] KIM_TOUGHNESS = {0.5, 1.0, 1.5, 2.0, 2.5};
    /** Kim Cang Hộ Thối: kháng đẩy lùi 0.05 .. 0.25. */
    public static final double[] KIM_KNOCKBACK = {0.05, 0.10, 0.15, 0.20, 0.25};
    /** Phi Vân Ngoa: tốc chạy +3% .. +15%. */
    public static final double[] PHI_SPEED = {0.03, 0.06, 0.09, 0.12, 0.15};
    /** Phi Vân Ngoa: nhân chi phí lách (thể lực) 0.9 .. 0.6 khi mặc. */
    public static final double[] PHI_DODGE_MULT = {0.90, 0.825, 0.75, 0.675, 0.60};

    public final String id, vi;
    public final ArmorItem.Type type;
    public final int rgb;
    /** Giáp (defense) của món này theo phẩm 0..4 — ArmorMaterial của phẩm đó lấy giá trị cột tương ứng. */
    public final int[] defense;
    private final Supplier<Item> catalyst;

    HoTheKind(String id, String vi, ArmorItem.Type type, int rgb, int[] defense, Supplier<Item> catalyst) {
        this.id = id;
        this.vi = vi;
        this.type = type;
        this.rgb = rgb;
        this.defense = defense;
        this.catalyst = catalyst;
    }

    public EquipmentSlot slot() { return type.getSlot(); }

    public Item catalyst() { return catalyst.get(); }

    public String langKey() { return "hukhong.hothe.kind." + id; }

    public String tipKey() { return "hukhong.hothe.tip." + id; }

    public static double tinhReduce(int tier) { return TINH_REDUCE[clamp(tier)]; }

    public static double phiDodgeMult(int tier) { return PHI_DODGE_MULT[clamp(tier)]; }

    static int clamp(int tier) { return Math.max(0, Math.min(4, tier)); }
}
