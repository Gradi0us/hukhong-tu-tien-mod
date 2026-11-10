package com.hukhong.boss.ai;

import com.hukhong.HuKhong;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/** Nối sự kiện game -> BossBrain: người chơi đánh boss (né), boss bị trúng (phản đòn), người chơi ăn/uống thuốc (trừng phạt). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class BrainEvents {
    private BrainEvents() {}

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        BossBrain b = BossBrain.get(e.getTarget());
        if (b != null && !e.getEntity().level().isClientSide) b.onThreat(BossBrain.Threat.MELEE);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post e) {
        BossBrain b = BossBrain.get(e.getEntity());
        if (b != null && !e.getEntity().level().isClientSide) b.onHurt(e.getSource().getEntity());
    }

    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof Player p) || !(p.level() instanceof ServerLevel sl) || !isHeal(e.getItem())) return;
        for (BossBrain b : BossBrain.all())
            if (b.mob().level() == sl && b.mob().isAlive() && b.mob().distanceToSqr(p) < 32 * 32) b.onHeal(p);
    }

    static boolean isHeal(ItemStack s) { return s.getItem() instanceof PotionItem || s.has(DataComponents.FOOD); }
}
