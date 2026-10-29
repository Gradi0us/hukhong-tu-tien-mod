package com.hukhong.skillbook;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Truc Nang Cap Ky Nang: chuot phai -> chon 1 ky nang (menu) -> +1 cap (toi da 10). Khong can du cap tu luyen. */
public class SkillUpgradeScrollItem extends Item {
    public SkillUpgradeScrollItem(Properties p) { super(p); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("hukhong.skillbook.upgrade_scroll.tip").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("hukhong.skillbook.upgrade_scroll.tip2").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
        List<String> ids = SkillBookService.scrollCandidates(sp);
        if (ids.isEmpty()) {
            sp.displayClientMessage(Component.translatable("hukhong.skillbook.upgrade_scroll.none"), true);
            return InteractionResultHolder.fail(st);
        }
        PacketDistributor.sendToPlayer(sp, new OpenScrollPayload(ids));
        return InteractionResultHolder.consume(st);
    }
}
