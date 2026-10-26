package com.hukhong.city;

import com.hukhong.society.SocietyRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Menu bảng Thành (không có ô đồ): ảnh chụp chức vị khi mở, nút 0 = nộp thảo dược. */
public class CityMenu extends AbstractContainerMenu {
    public static final int B_DONATE = 0;

    public record Snapshot(BlockPos pos, String cityId, int rank, int points, int level, String lordName,
                           boolean lord, int capLeft, int herbs) {
        public void write(FriendlyByteBuf b) {
            b.writeBlockPos(pos);
            b.writeUtf(cityId);
            b.writeVarInt(rank);
            b.writeVarInt(points);
            b.writeVarInt(level);
            b.writeUtf(lordName);
            b.writeBoolean(lord);
            b.writeVarInt(capLeft);
            b.writeVarInt(herbs);
        }

        public static Snapshot read(FriendlyByteBuf b) {
            return new Snapshot(b.readBlockPos(), b.readUtf(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readUtf(),
                    b.readBoolean(), b.readVarInt(), b.readVarInt());
        }
    }

    public final Snapshot s;

    public CityMenu(int id, Inventory inv, Snapshot s) {
        super(SocietyRegistry.CITY_MENU.get(), id);
        this.s = s;
    }

    public CityMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) { this(id, inv, Snapshot.read(buf)); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp) || !stillValid(player)) return false;
        return CityService.handleButton(sp, s.pos(), id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(s.pos().getX() + .5, s.pos().getY() + .5, s.pos().getZ() + .5) < 64
                && player.level().getBlockState(s.pos()).is(SocietyRegistry.THANH_BANG.get());
    }
}
