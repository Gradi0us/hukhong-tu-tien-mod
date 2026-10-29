package com.hukhong.skill;

import com.hukhong.core.Path;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Vào Thế Tu Luyện (G) / đổi trang: tự đổi tay để đánh thường luôn dùng được (trong thế, phím số và Q bị khoá).
 * Đạo có vũ khí bản mệnh (Kiếm: Bản Mệnh Kiếm, Pháp: Linh Trượng) -> cầm vũ khí đó; đạo khác -> tay không.
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
            default -> false;
        };
    }

    static boolean hasWeapon(Path path) { return path == Path.KIEM || path == Path.PHAP; }

    public static void equip(ServerPlayer p, Path path) {
        Inventory inv = p.getInventory();
        int slot = -1;
        if (hasWeapon(path)) {
            for (int i = 0; i < inv.items.size(); i++) if (isPathWeapon(p, path, inv.items.get(i))) { slot = i; break; }
            if (slot < 0) return; // chưa có/đang tái tạo: giữ nguyên
            if (slot >= HOTBAR) { // nằm trong túi -> đưa lên ô trống trên thanh nhanh, không có thì đổi chỗ với ô đang cầm
                int dst = emptyHotbar(inv);
                if (dst < 0) dst = inv.selected;
                ItemStack w = inv.items.get(slot);
                inv.items.set(slot, inv.items.get(dst));
                inv.items.set(dst, w);
                slot = dst;
            }
        } else {
            if (inv.getSelected().isEmpty()) return;
            slot = emptyHotbar(inv);
            if (slot < 0) { // thanh nhanh đầy -> cất đồ đang cầm vào túi để tay không
                int free = -1;
                for (int i = HOTBAR; i < inv.items.size(); i++) if (inv.items.get(i).isEmpty()) { free = i; break; }
                if (free < 0) {
                    p.displayClientMessage(net.minecraft.network.chat.Component.translatable("hukhong.stance.hands_full"), true);
                    return;
                }
                inv.items.set(free, inv.getSelected());
                inv.items.set(inv.selected, ItemStack.EMPTY);
                slot = inv.selected;
            }
        }
        inv.selected = slot;
        p.connection.send(new ClientboundSetCarriedItemPacket(slot));
        p.inventoryMenu.broadcastChanges();
    }

    static int emptyHotbar(Inventory inv) {
        if (inv.getSelected().isEmpty()) return inv.selected;
        for (int i = 0; i < HOTBAR; i++) if (inv.items.get(i).isEmpty()) return i;
        return -1;
    }
}
