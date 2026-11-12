package com.hukhong.profession.station;

import com.hukhong.profession.ProfessionRegistry;
import com.hukhong.profession.phu.PhuDraw;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Menu tram nghe phu. Nut 0 = che tao (server xac thuc trong BE). */
public class StationMenu extends AbstractContainerMenu {
    /** Vi tri o (tuong doi goc GUI): 3 nguyen lieu, 1 ket qua. */
    public static final int[][] POS = {{30, 20}, {48, 20}, {66, 20}, {122, 20}};

    private final Container box;
    public final ProfessionStationBlock.Station station;

    /** Client: doc vi tri block de biet tram nao. */
    public StationMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, new SimpleContainer(ProfessionStationBlockEntity.SLOTS), stationAt(inv, buf.readBlockPos()));
    }

    public StationMenu(int id, Inventory inv, Container box, ProfessionStationBlock.Station station) {
        super(ProfessionRegistry.STATION_MENU.get(), id);
        this.box = box;
        this.station = station;
        for (int i = 0; i < ProfessionStationBlockEntity.IN_COUNT; i++) addSlot(new Slot(box, i, POS[i][0], POS[i][1]));
        addSlot(new Slot(box, ProfessionStationBlockEntity.OUT, POS[3][0], POS[3][1]) {
            @Override public boolean mayPlace(ItemStack s) { return false; }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
    }

    private static ProfessionStationBlock.Station stationAt(Inventory inv, BlockPos pos) {
        return inv.player.level().getBlockState(pos).getBlock() instanceof ProfessionStationBlock b
                ? b.station : ProfessionStationBlock.Station.PHU_AN;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 1 && player instanceof ServerPlayer sp) { quickDraw(sp); return true; }
        if (id != 0 || !stillValid(player)) return false;
        if (box instanceof ProfessionStationBlockEntity be && player instanceof ServerPlayer sp) be.craft(sp);
        return true;
    }

    /** Phu An: ve phu bang minigame (client gui ket qua qua PhuDrawNet.Submit). */
    public void phuDraw(ServerPlayer sp, java.util.List<Integer> trace, int durationMs) {
        if (station == ProfessionStationBlock.Station.PHU_AN && stillValid(sp)) PhuDraw.submit(sp, box, trace, durationMs);
    }

    /** Ve nhanh (menu button 1, chi Phu An). */
    public void quickDraw(ServerPlayer sp) {
        if (station == ProfessionStationBlock.Station.PHU_AN && stillValid(sp)) PhuDraw.quick(sp, box);
    }

    /** Nut "Xep nguyen lieu" tu so cong thuc (server). */
    public void arrange(ServerPlayer sp, String recipeId) {
        if (stillValid(sp) && box instanceof ProfessionStationBlockEntity be) be.arrange(sp, recipeId);
    }

    /** Thao tac Doan Tao Dai (Nung/Ron/Toi/Tinh luyen/Sua/Phan ra), chi tram DOAN_TAO_DAI. */
    public void forge(ServerPlayer sp, int op, long tick) {
        if (station == ProfessionStationBlock.Station.DOAN_TAO_DAI && stillValid(sp))
            com.hukhong.profession.khi.ForgeService.handle(sp, box, op, tick);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < ProfessionStationBlockEntity.SLOTS) {
            if (!moveItemStackTo(stack, ProfessionStationBlockEntity.SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, ProfessionStationBlockEntity.IN_COUNT, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) { return box.stillValid(player); }
}
