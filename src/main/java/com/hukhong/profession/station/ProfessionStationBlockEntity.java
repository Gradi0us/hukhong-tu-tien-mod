package com.hukhong.profession.station;

import com.hukhong.profession.Profession;
import com.hukhong.profession.ProfessionRecipes;
import com.hukhong.profession.ProfessionRegistry;
import com.hukhong.profession.ProfessionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Slot 0-2 nguyen lieu, slot 3 ket qua. Craft khi bam nut (menu button 0). */
public class ProfessionStationBlockEntity extends RandomizableContainerBlockEntity {
    public static final int SLOTS = 4, IN0 = 0, IN_COUNT = 3, OUT = 3;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public ProfessionStationBlockEntity(BlockPos pos, BlockState state) {
        super(ProfessionRegistry.STATION_BE.get(), pos, state);
    }

    public ProfessionStationBlock.Station station() {
        return getBlockState().getBlock() instanceof ProfessionStationBlock b ? b.station : ProfessionStationBlock.Station.PHU_AN;
    }

    /** Nut menu: chi nhan id 0 (che tao). */
    public void craft(ServerPlayer sp) {
        if (!items.get(OUT).isEmpty()) { say(sp, "hukhong.station.out_full"); return; }
        List<ItemStack> in = List.of(items.get(0), items.get(1), items.get(2));
        ProfessionRecipes.Recipe r = ProfessionRecipes.find(station().recipes(), in);
        if (r == null) { say(sp, "hukhong.station.no_recipe"); return; }
        int lv = ProfessionService.level(sp, r.prof());
        if (lv <= 0) { say(sp, r.prof().needKey()); return; }
        if (lv < r.minLevel()) { sp.displayClientMessage(Component.translatable("hukhong.station.level_low", r.minLevel()), true); return; }
        boolean up = r.prof() == Profession.DOAN_TAO
                && ProfessionService.rollTierBonus(sp, getLevel().getRandom()) > 0;
        ProfessionRecipes.consume(r, in);
        items.set(OUT, r.out().make(lv, up));
        setChanged();
        ProfessionService.addCraftXp(sp, r.prof(), r.xp());
        say(sp, "hukhong.station.crafted");
    }

    private static void say(ServerPlayer p, String key) {
        p.displayClientMessage(Component.translatable(key), true);
    }

    @Override protected Component getDefaultName() { return Component.translatable(station().key); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> list) { items = list; }
    @Override public int getContainerSize() { return SLOTS; }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) { return new StationMenu(id, inv, this, station()); }
}
