package com.hukhong.skill;

import com.hukhong.core.Path;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Nhận diện vũ khí bản mệnh của đạo (Kiếm: Bản Mệnh Kiếm, Pháp: Linh Trượng).
 * Bố cục thanh nhanh trong Thế Tu Luyện nằm ở {@link StanceHotbar}.
 */
public final class StanceHands {
    private StanceHands() {}

    public static final int HOTBAR = 9;

    /** Vũ khí bản mệnh của đạo (không phụ thuộc biên dịch vào item Pháp: nhận theo id). */
    static boolean isPathWeapon(ServerPlayer p, Path path, ItemStack s) {
        if (s.isEmpty() || path == null) return false;
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
        return switch (path) {
            case KIEM -> s.is(com.hukhong.item.ModItems.BAN_MENH_KIEM.get()) && com.hukhong.banmenh.BanMenhKiemItem.isOwner(s, p);
            case PHAP -> id.equals("linh_truong");
            case TRAN, DAN -> false;
            default -> false;
        };
    }
}
