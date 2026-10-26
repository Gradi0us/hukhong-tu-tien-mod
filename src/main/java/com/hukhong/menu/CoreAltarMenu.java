package com.hukhong.menu;

import com.hukhong.block.ModBlocks;
import com.hukhong.cores.CoreService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu Ấn Hồn Đài. Client chỉ biết 3 ô thường (id + phẩm); ô 4 úp. Lựa chọn gửi bằng clickMenuButton(0..3),
 * server xác thực lại toàn bộ (còn khóa mốc, còn phiên roll, đúng chỉ số).
 */
public class CoreAltarMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    public final int milestone;
    public final int tier;
    public final List<ResourceLocation> ids;

    public CoreAltarMenu(int id, Inventory inv, BlockPos pos, int milestone, int tier, List<ResourceLocation> ids) {
        super(ModMenus.CORE_ALTAR.get(), id);
        this.pos = pos;
        this.milestone = milestone;
        this.tier = tier;
        this.ids = List.copyOf(ids);
    }

    public CoreAltarMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), readIds(buf));
    }

    private static List<ResourceLocation> readIds(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<ResourceLocation> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(buf.readResourceLocation());
        return l;
    }

    public static void write(FriendlyByteBuf buf, BlockPos pos, int milestone, int tier, List<ResourceLocation> ids) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(milestone);
        buf.writeVarInt(tier);
        buf.writeVarInt(ids.size());
        ids.forEach(buf::writeResourceLocation);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp) || !stillValid(player)) return false;
        return CoreService.choose(sp, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(ModBlocks.AN_HON_DAI.get())
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
