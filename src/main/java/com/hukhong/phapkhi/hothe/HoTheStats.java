package com.hukhong.phapkhi.hothe;

import com.hukhong.craft.CraftQuality;
import com.hukhong.craft.CraftRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Chi so thuc te cua Phap Khi ho the theo tung mon: nhan voi CraftQuality.mult(phẩm) x (1 + 4% x tinh luyen).
 * Ghi lai ATTRIBUTE_MODIFIERS tren stack (ban mac dinh cua loai giu nguyen, khong doi).
 */
public final class HoTheStats {
    private HoTheStats() {}

    public static final int REFINE_MAX = 5;
    public static final double REFINE_STEP = 0.04;

    public static int refine(ItemStack s) { return s.getOrDefault(CraftRegistry.REFINE.get(), 0); }

    public static void setRefine(ItemStack s, int r) {
        s.set(CraftRegistry.REFINE.get(), Math.max(0, Math.min(REFINE_MAX, r)));
    }

    /** He so nhan chi so cua mon: phẩm x tinh luyện. Mon khong co phẩm = Trung (x1.0). */
    public static double factor(ItemStack s) {
        return CraftQuality.mult(CraftQuality.ordinal(s)) * (1 + REFINE_STEP * refine(s));
    }

    /** Tinh lai chi so cua stack theo factor(stack). Khong phai hộ thể thi bo qua. */
    public static void refresh(ItemStack s) {
        if (!(s.getItem() instanceof HoTheArmorItem h)) return;
        double f = factor(s);
        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry e : h.getDefaultAttributeModifiers().modifiers()) {
            AttributeModifier m = e.modifier();
            b.add(e.attribute(), new AttributeModifier(m.id(), m.amount() * f, m.operation()), e.slot());
        }
        s.set(DataComponents.ATTRIBUTE_MODIFIERS, b.build());
    }
}
