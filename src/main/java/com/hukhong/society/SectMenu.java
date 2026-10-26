package com.hukhong.society;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Menu bảng Tông Môn (không có ô đồ). Client nhận ảnh chụp trạng thái khi mở; nút gửi bằng clickMenuButton,
 * server xác thực lại mọi thứ rồi mở lại menu với ảnh chụp mới.
 */
public class SectMenu extends AbstractContainerMenu {
    public static final int B_APPLY = 0, B_ACCEPT = 1, B_LEAVE = 2, B_KILL = 3, B_HERB = 4, B_SUBMIT = 5, B_CLAIM = 6, B_ABANDON = 7;

    /** boardSect: tông sở hữu bảng này; memberSect: tông của người chơi ("" nếu chưa vào). */
    public record Snapshot(BlockPos pos, String boardSect, String memberSect, int rank, int totalCh, String quest,
                           int progress, int need, String invite, int applyCode, int capLeft, int level) {
        public void write(FriendlyByteBuf b) {
            b.writeBlockPos(pos);
            b.writeUtf(boardSect);
            b.writeUtf(memberSect);
            b.writeVarInt(rank);
            b.writeVarInt(totalCh);
            b.writeUtf(quest);
            b.writeVarInt(progress);
            b.writeVarInt(need);
            b.writeUtf(invite);
            b.writeVarInt(applyCode);
            b.writeVarInt(capLeft);
            b.writeVarInt(level);
        }

        public static Snapshot read(FriendlyByteBuf b) {
            return new Snapshot(b.readBlockPos(), b.readUtf(), b.readUtf(), b.readVarInt(), b.readVarInt(), b.readUtf(),
                    b.readVarInt(), b.readVarInt(), b.readUtf(), b.readVarInt(), b.readVarInt(), b.readVarInt());
        }
    }

    public final Snapshot s;

    public SectMenu(int id, Inventory inv, Snapshot s) {
        super(SocietyRegistry.SECT_MENU.get(), id);
        this.s = s;
    }

    public SectMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) { this(id, inv, Snapshot.read(buf)); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp) || !stillValid(player)) return false;
        return SectService.handleButton(sp, s.pos(), id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(s.pos().getX() + .5, s.pos().getY() + .5, s.pos().getZ() + .5) < 64
                && player.level().getBlockState(s.pos()).is(SocietyRegistry.TONG_MON_BANG.get());
    }
}
