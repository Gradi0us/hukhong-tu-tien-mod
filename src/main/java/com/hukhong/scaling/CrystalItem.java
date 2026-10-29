package com.hukhong.scaling;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Tâm Tinh (+máu tối đa vĩnh viễn) / Linh Tinh (+sát thương vĩnh viễn), có trần. */
public class CrystalItem extends Item {
    private final boolean heart;

    public CrystalItem(boolean heart, Properties p) { super(p); this.heart = heart; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
        double per = heart ? ScalingConfig.HEART_PER_USE.get() : ScalingConfig.POWER_PER_USE.get();
        double cap = heart ? ScalingConfig.HEART_CAP.get() : ScalingConfig.POWER_CAP.get();
        String key = heart ? Difficulty.HEART : Difficulty.POWER;
        double cur = sp.getPersistentData().getDouble(key);
        if (cur + 1e-6 >= cap) {
            sp.displayClientMessage(Component.translatable("hukhong.scaling.crystal.full"), true);
            return InteractionResultHolder.fail(st);
        }
        cur = Math.min(cap, cur + per);
        sp.getPersistentData().putDouble(key, cur);
        ScalingEvents.applyCrystals(sp);
        ScalingEvents.sync(sp);
        if (heart) sp.heal((float) per);
        if (!sp.getAbilities().instabuild) st.shrink(1);
        level.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, heart ? 0.8f : 1.3f);
        sp.displayClientMessage(Component.translatable(heart ? "hukhong.scaling.heart.used" : "hukhong.scaling.power.used",
                String.format("%.1f", cur), String.format("%.0f", cap)), true);
        return InteractionResultHolder.sidedSuccess(st, false);
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext c, List<Component> t, TooltipFlag f) {
        t.add(Component.translatable(heart ? "hukhong.scaling.heart.tip" : "hukhong.scaling.power.tip").withStyle(ChatFormatting.GRAY));
    }
}
