package com.hukhong.alchemy;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Đan tiện ích: HEAL (Chỉ Huyết/Hồi Xuân: hồi % HP) hoặc DETOX (Giải Độc: trừ Đan Độc). */
public class UtilityPillItem extends Item {
    public enum Kind { HEAL, DETOX }

    private final Kind kind;
    private final int tier;
    private final double base; // HEAL: tỉ lệ HP tối đa; DETOX: điểm độc
    private final double coef;

    public UtilityPillItem(Kind kind, int tier, double base, double coef, Properties props) {
        super(props);
        this.kind = kind;
        this.tier = tier;
        this.base = base;
        this.coef = coef;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (kind == Kind.HEAL && !DanDoc.canUse(sp)) return InteractionResultHolder.fail(stack);
        var q = AlchemyRegistry.qualityOf(stack);
        double mult = AlchemyMath.effectMult(DanDoc.get(sp));
        if (kind == Kind.HEAL) {
            sp.heal((float) (sp.getMaxHealth() * base * q.effect * mult));
            DanDoc.consume(sp, coef, tier, q);
        } else {
            DanDoc.set(sp, DanDoc.get(sp) - base * q.effect);
            sp.sendSystemMessage(Component.translatable("hukhong.dandoc.status", (int) DanDoc.get(sp)));
        }
        stack.shrink(1);
        player.getCooldowns().addCooldown(this, 200);
        return InteractionResultHolder.consume(stack);
    }
}
