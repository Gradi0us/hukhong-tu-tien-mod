package com.hukhong.storage;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Mở / lưu kho trữ vật. Cần có nhẫn trong túi. Nhẫn không được bỏ vào chính kho. */
public final class TruVatService {
    private TruVatService() {}

    public static boolean hasRing(Player p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(TruVatRegistry.RING.get())) return true;
        return false;
    }

    public static void open(ServerPlayer sp) {
        if (!hasRing(sp)) {
            sp.displayClientMessage(Component.translatable("hukhong.tru_vat.no_ring"), true);
            return;
        }
        NonNullList<ItemStack> store = sp.getData(TruVatRegistry.STORE.get());
        SimpleContainer box = new SimpleContainer(TruVatRegistry.SIZE) {
            @Override public boolean canPlaceItem(int slot, ItemStack s) { return !s.is(TruVatRegistry.RING.get()); }
        };
        for (int i = 0; i < TruVatRegistry.SIZE; i++) box.setItem(i, store.get(i).copy());
        sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, box, 3) {
            @Override public ItemStack quickMoveStack(Player who, int idx) {
                if (getSlot(idx).getItem().is(TruVatRegistry.RING.get())) return ItemStack.EMPTY;
                return super.quickMoveStack(who, idx);
            }
            @Override public void clicked(int idx, int button, net.minecraft.world.inventory.ClickType type, Player who) {
                // chặn đặt nhẫn vào ô kho (kể cả phím số swap)
                if (idx >= 0 && idx < TruVatRegistry.SIZE) {
                    ItemStack carried = getCarried();
                    if (carried.is(TruVatRegistry.RING.get())) return;
                    if (type == net.minecraft.world.inventory.ClickType.SWAP && button >= 0 && button < 9
                            && who.getInventory().getItem(button).is(TruVatRegistry.RING.get())) return;
                }
                super.clicked(idx, button, type, who);
                save(sp, box);
            }
            @Override public void removed(Player who) { super.removed(who); save(sp, box); }
        }, Component.translatable("hukhong.tru_vat.title")));
    }

    static void save(ServerPlayer sp, SimpleContainer box) {
        NonNullList<ItemStack> out = NonNullList.withSize(TruVatRegistry.SIZE, ItemStack.EMPTY);
        for (int i = 0; i < TruVatRegistry.SIZE; i++) out.set(i, box.getItem(i).copy());
        sp.setData(TruVatRegistry.STORE.get(), out);
    }
}
