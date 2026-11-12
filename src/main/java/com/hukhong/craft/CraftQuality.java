package com.hukhong.craft;

import com.hukhong.alchemy.AlchemyMath;
import com.hukhong.alchemy.AlchemyRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Phẩm chất sản phẩm chế tạo (dùng chung Đan / Phù / Giáp). Thang = AlchemyMath.Quality (ordinal 0..3 = Hạ/Trung/Thượng/Cực).
 * Data component {@code hukhong:craft_quality} (int) + {@code hukhong:crafter} (tên người chế).
 * Đan cũ chỉ có {@code pill_quality}: vẫn được đọc khi thiếu craft_quality.
 */
public final class CraftQuality {
    private CraftQuality() {}

    public static final int HA = 0, TRUNG = 1, THUONG = 2, CUC = 3;

    /** Hệ số hiệu lực theo phẩm: Hạ 0.85, Trung 1.00, Thượng 1.15, Cực 1.30. */
    public static double mult(int q) {
        return switch (Math.max(HA, Math.min(CUC, q))) {
            case HA -> 0.85;
            case THUONG -> 1.15;
            case CUC -> 1.30;
            default -> 1.0;
        };
    }

    /** Phẩm của stack; null nếu không có component nào. */
    public static AlchemyMath.Quality quality(ItemStack s) {
        if (s.has(CraftRegistry.CRAFT_QUALITY.get())) return AlchemyMath.Quality.of(s.get(CraftRegistry.CRAFT_QUALITY.get()));
        if (s.has(AlchemyRegistry.PILL_QUALITY.get())) return AlchemyMath.Quality.of(s.get(AlchemyRegistry.PILL_QUALITY.get()));
        return null;
    }

    /** Phẩm của stack, mặc định Trung nếu không có component. */
    public static int ordinal(ItemStack s) {
        AlchemyMath.Quality q = quality(s);
        return q == null ? TRUNG : q.ordinal();
    }

    public static ChatFormatting color(int q) {
        return switch (Math.max(HA, Math.min(CUC, q))) {
            case HA -> ChatFormatting.GRAY;
            case THUONG -> ChatFormatting.AQUA;
            case CUC -> ChatFormatting.GOLD;
            default -> ChatFormatting.WHITE;
        };
    }

    public static String name(int q) { return AlchemyMath.Quality.of(q).name().toLowerCase(); }

    /** "Thượng phẩm · do <tên> chế" (hoặc chỉ tên phẩm nếu không có người chế). */
    public static Component tooltip(int q, String crafter) {
        Component qn = Component.translatable("hukhong.craft.quality." + name(q)).withStyle(color(q));
        if (crafter == null || crafter.isEmpty()) return qn;
        return Component.translatable("hukhong.craft.tooltip", qn, crafter).withStyle(ChatFormatting.GRAY);
    }

    /** Gắn phẩm + người chế lên stack (crafter null/rỗng = giữ nguyên). Trả về chính stack. */
    public static ItemStack stamp(ItemStack s, int q, String crafter) {
        s.set(CraftRegistry.CRAFT_QUALITY.get(), Math.max(HA, Math.min(CUC, q)));
        if (crafter != null && !crafter.isEmpty()) s.set(CraftRegistry.CRAFTER.get(), crafter);
        return s;
    }
}
