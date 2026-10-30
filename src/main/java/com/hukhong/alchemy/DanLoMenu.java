package com.hukhong.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/** Menu Đan Lô. Nút: 0 bắt đầu, 1 tăng lửa, 2 giảm lửa (server xác thực trong BE). */
public class DanLoMenu extends AbstractContainerMenu {
    public static final int[][] POS = {{8, 18}, {26, 18}, {44, 18}, {8, 44}, {44, 44}, {138, 20}};

    private final Container box;
    public final ContainerData data;

    public DanLoMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, new SimpleContainer(DanLoBlockEntity.SLOTS), new SimpleContainerData(DanLoBlockEntity.DATA_SIZE));
        buf.readBlockPos();
    }

    public DanLoMenu(int id, Inventory inv, Container box, ContainerData data) {
        super(AlchemyRegistry.DAN_LO_MENU.get(), id);
        this.box = box;
        this.data = data;
        for (int i = 0; i < 3; i++) addSlot(new Slot(box, i, POS[i][0], POS[i][1]) {
            @Override public boolean mayPlace(ItemStack s) { return isHerb(s); }
        });
        addSlot(new Slot(box, DanLoBlockEntity.FUEL, POS[3][0], POS[3][1]) {
            @Override public boolean mayPlace(ItemStack s) { return s.getBurnTime(RecipeType.SMELTING) > 0; }
        });
        addSlot(new Slot(box, DanLoBlockEntity.DI_HOA, POS[4][0], POS[4][1]) {
            @Override public boolean mayPlace(ItemStack s) { return s.is(AlchemyRegistry.DI_HOA_CHAU.get()); }
        });
        addSlot(new Slot(box, DanLoBlockEntity.OUT, POS[5][0], POS[5][1]) {
            @Override public boolean mayPlace(ItemStack s) { return false; }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlots(data);
    }

    static boolean isHerb(ItemStack s) {
        return s.is(AlchemyRegistry.LINH_THAO.get()) || s.is(AlchemyRegistry.HUYET_THAM.get())
                || s.is(AlchemyRegistry.HOA_LIEN.get()) || s.is(AlchemyRegistry.THANH_DIEP.get())
                || com.hukhong.resource.ResourceRegistry.isAlchemyIngredient(s);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id > 2 || !stillValid(player)) return false;
        if (box instanceof DanLoBlockEntity be) be.button(id, player);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < DanLoBlockEntity.SLOTS) {
            if (!moveItemStackTo(stack, DanLoBlockEntity.SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (isHerb(stack)) {
            if (!moveItemStackTo(stack, 0, 3, false)) return ItemStack.EMPTY;
        } else if (stack.is(AlchemyRegistry.DI_HOA_CHAU.get())) {
            if (!moveItemStackTo(stack, DanLoBlockEntity.DI_HOA, DanLoBlockEntity.DI_HOA + 1, false)) return ItemStack.EMPTY;
        } else if (stack.getBurnTime(RecipeType.SMELTING) > 0) {
            if (!moveItemStackTo(stack, DanLoBlockEntity.FUEL, DanLoBlockEntity.FUEL + 1, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) { return box.stillValid(player); }
}
