package com.hukhong.trungsinh;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** Trùng Sinh Đan: kiểu ăn 32 tick. Dùng lần 2 trong 10s để xác nhận; hoàn tất mới tiêu đan và Trùng Sinh. */
public class TrungSinhDanItem extends Item {
    public static final int USE_TICKS = 32;

    public TrungSinhDanItem(Properties props) { super(props); }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.EAT; }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return USE_TICKS; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean go = level.isClientSide || (player instanceof ServerPlayer sp && TrungSinhService.begin(sp));
        if (!go) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer sp && TrungSinhService.rebirth(sp)) stack.shrink(1);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("hukhong.trung_sinh.tip").withStyle(ChatFormatting.GRAY));
    }
}
