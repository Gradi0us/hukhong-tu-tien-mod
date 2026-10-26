package com.hukhong.age;

import com.hukhong.core.ModAttachments;
import com.hukhong.network.SyncPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Thọ Nguyên Đan: +years Thọ bổ sung (tổng ≤ 50% Thọ cơ bản, xem AgeMath.lifespan). */
public class ThoNguyenDanItem extends Item {
    private final int years;

    public ThoNguyenDanItem(int years, Properties props) { super(props); this.years = years; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        AgeData a = sp.getData(ModAttachments.AGE);
        int realm = sp.getData(ModAttachments.CULTIVATION).realmIndex();
        int cap = AgeMath.baseLifespan(realm);
        if (cap == AgeMath.UNLIMITED || a.bonusYears() >= cap / 2) {
            sp.sendSystemMessage(Component.translatable("hukhong.age.pill.maxed"));
            return InteractionResultHolder.fail(stack);
        }
        AgeData n = a.withBonusYears(Math.min(cap / 2, a.bonusYears() + years));
        sp.setData(ModAttachments.AGE, n);
        sp.sendSystemMessage(Component.translatable("hukhong.age.pill.used", n.bonusYears() - a.bonusYears(), n.lifespan(realm)));
        SyncPayload.send(sp);
        stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }
}
