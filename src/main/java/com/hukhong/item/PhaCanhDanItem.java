package com.hukhong.item;

import com.hukhong.core.CultivationService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/** Phá Cảnh Đan phẩm tier: qua Bình Cảnh đúng mốc (1 -> cấp 10, 2 -> cấp 20). Bind người nhận đầu. */
public class PhaCanhDanItem extends Item {
    private final int tier;
    /** Tiểu Phá Đan: cấp Bình Cảnh nhẹ (94/98) đan này phá; 0 = Phá Cảnh Đan chính. */
    private final int lightLevel;

    public PhaCanhDanItem(int tier, Properties props) { this(tier, 0, props); }

    public PhaCanhDanItem(int tier, int lightLevel, Properties props) {
        super(props);
        this.tier = tier;
        this.lightLevel = lightLevel;
    }

    public int lightLevel() { return lightLevel; }

    public int tier() { return tier; }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof Player p && !stack.has(ModItems.BOUND_TO.get())) {
            stack.set(ModItems.BOUND_TO.get(), p.getUUID());
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        UUID owner = stack.get(ModItems.BOUND_TO.get());
        if (owner == null) {
            stack.set(ModItems.BOUND_TO.get(), sp.getUUID());
        } else if (!owner.equals(sp.getUUID())) {
            sp.displayClientMessage(Component.translatable("hukhong.pill.not_owner"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (lightLevel > 0 ? CultivationService.tryLightBreakthrough(sp, lightLevel)
                : CultivationService.tryBreakthrough(sp, tier)) {
            stack.shrink(1);
            sp.displayClientMessage(Component.translatable("hukhong.pill.success"), true);
            return InteractionResultHolder.consume(stack);
        }
        sp.displayClientMessage(Component.translatable("hukhong.pill.useless"), true);
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("hukhong.pill.tier." + tier).withStyle(ChatFormatting.GOLD));
        tip.add((lightLevel > 0 ? Component.translatable("hukhong.pill.light_effect", lightLevel, lightLevel + 1)
                : Component.translatable("hukhong.pill.effect", tier * 10)).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable(stack.has(ModItems.BOUND_TO.get()) ? "hukhong.pill.bound" : "hukhong.pill.unbound")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
