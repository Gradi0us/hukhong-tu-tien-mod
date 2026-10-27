package com.hukhong.guild;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Menu Hiep Hoi (khong co o do). Du lieu hien thi doc tu attachment GuildData da dong bo; nut gui qua clickMenuButton:
 * 0..4 nhan/nop nhiem vu ngay, 10 thu thach (nhan/nop), 20..24 huy nhiem vu.
 */
public class GuildMenu extends AbstractContainerMenu {
    public static final int B_TRIAL = 10, B_ABANDON = 20;

    public final BlockPos pos;

    public GuildMenu(int id, Inventory inv, BlockPos pos) {
        super(GuildRegistry.GUILD_MENU.get(), id);
        this.pos = pos;
    }

    public GuildMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) { this(id, inv, buf.readBlockPos()); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return player instanceof ServerPlayer sp && stillValid(player) && GuildService.handleButton(sp, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) { return player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) < 144; }
}
