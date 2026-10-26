package com.hukhong.society;

import com.hukhong.alchemy.AlchemyRegistry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Thảo dược của Đan Lô (com.hukhong.alchemy) dùng làm vật phẩm nộp nhiệm vụ. */
public final class Herbs {
    private Herbs() {}

    public static boolean isHerb(ItemStack s) {
        return s.is(AlchemyRegistry.LINH_THAO.get()) || s.is(AlchemyRegistry.HUYET_THAM.get())
                || s.is(AlchemyRegistry.HOA_LIEN.get()) || s.is(AlchemyRegistry.THANH_DIEP.get());
    }

    public static int count(Inventory inv) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (isHerb(s)) n += s.getCount();
        }
        return n;
    }

    /** Trừ tối đa n thảo dược, trả số thực sự trừ. */
    public static int take(Inventory inv, int n) {
        int left = n;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!isHerb(s)) continue;
            int t = Math.min(left, s.getCount());
            s.shrink(t);
            left -= t;
        }
        return n - left;
    }
}
