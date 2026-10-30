package com.hukhong.resource;

import com.hukhong.alchemy.AlchemyMath;
import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.alchemy.DanDoc;
import com.hukhong.core.CultivationService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Tu Linh Dan: cho EXP bang ~12% thanh EXP cap hien tai (nhan pham chat + Dan Doc), co cooldown. */
public class TuLinhDanItem extends Item {
    public static final double FRACTION = 0.12;
    public static final int COOLDOWN = 600, TIER = 3;

    public TuLinhDanItem(Properties p) { super(p); }

    public static long expFor(int level, AlchemyMath.Quality q, double toxMult) {
        return Math.max(1, Math.round(CultivationService.expToNext(level) * FRACTION * q.effect * toxMult));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (!DanDoc.canUse(sp)) return InteractionResultHolder.fail(stack);
        var q = AlchemyRegistry.qualityOf(stack);
        double mult = AlchemyMath.effectMult(DanDoc.get(sp));
        long exp = expFor(CultivationService.get(sp).level(), q, mult);
        CultivationService.addExp(sp, exp);
        DanDoc.consume(sp, 1.0, TIER, q);
        sp.displayClientMessage(Component.translatable("hukhong.pill.tu_linh", exp), true);
        stack.shrink(1);
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.consume(stack);
    }
}
