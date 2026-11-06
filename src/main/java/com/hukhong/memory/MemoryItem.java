package com.hukhong.memory;

import com.hukhong.auction.Tx;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** Mảnh Ký Ức: chuột phải để nhập ký ức (diễn cảnh hồi ức). Tên ký ức hiện ở tooltip; lấp lánh như đồ hiếm. */
public final class MemoryItem extends Item {
    public MemoryItem(Properties props) { super(props); }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(st);
        MemoryService.Result r = MemoryService.use(sp, st);
        return new InteractionResultHolder<>(r == MemoryService.Result.OK ? InteractionResult.SUCCESS : InteractionResult.FAIL, st);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        int id = MemoryRegistry.idOf(stack);
        MemoryLore.Memory m = MemoryLore.byId(id);
        if (m == null) {
            tip.add(Tx.t("hukhong.memory.invalid", "Mảnh ký ức bị vỡ, không đọc được.", "This shard is corrupted and unreadable."));
            return;
        }
        tip.add(Tx.t("hukhong.memory.tip.title", "Ký Ức %s: %s", "Memory %s: %s",
                MemoryLore.roman(id), m.name().tr()).withStyle(net.minecraft.ChatFormatting.GOLD));
        tip.add(Tx.t("hukhong.memory.tip.hint", "Chuột phải để nhập ký ức.", "Right-click to absorb this memory.")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
