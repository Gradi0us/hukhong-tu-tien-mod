package com.hukhong.currency;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Dinh dang so Tu Tinh: "2 Thuong 5 Trung 30 Ha" (bo phan bang 0; 0 -> "0 Ha"). */
public final class TuTinhText {
    private TuTinhText() {}

    public static MutableComponent of(long v) {
        long[] c = TuTinh.canonical(v);
        MutableComponent out = Component.empty();
        boolean any = false;
        if (c[2] > 0) { out.append(icon(2)).append(" ").append(Component.translatable("hukhong.tutinh.fmt.thuong", c[2])); any = true; }
        if (c[1] > 0) { if (any) out.append(" "); out.append(icon(1)).append(" ").append(Component.translatable("hukhong.tutinh.fmt.trung", c[1])); any = true; }
        if (c[0] > 0 || !any) { if (any) out.append(" "); out.append(icon(0)).append(" ").append(Component.translatable("hukhong.tutinh.fmt.ha", c[0])); }
        return out;
    }

    private static final net.minecraft.resources.ResourceLocation FONT =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hukhong.HuKhong.MODID, "icons");

    /** Icon vien Tu Tinh trong chu (0 = Ha, 1 = Trung, 2 = Thuong) qua font bitmap assets/hukhong/font/icons.json. */
    public static MutableComponent icon(int tier) {
        return Component.literal(String.valueOf((char) (0xE000 + Math.max(0, Math.min(2, tier)))))
                .withStyle(st -> st.withFont(FONT).withColor(0xFFFFFF));
    }
}
