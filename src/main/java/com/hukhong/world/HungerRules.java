package com.hukhong.world;

import com.hukhong.HuKhong;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 2026-11-06: bỏ hệ đói. Thanh thức ăn luôn đầy (chạy nhanh thoải mái, không chết đói); chỗ thanh thức ăn trên HUD
 * được thay bằng thanh Linh Khí (client/hud/LinhKhiHud). Hồi máu vẫn theo HealRules (không phụ thuộc thức ăn).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class HungerRules {
    private HungerRules() {}

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        Player p = e.getEntity();
        if (p.level().isClientSide) return;
        FoodData f = p.getFoodData();
        if (f.getFoodLevel() < 20) f.setFoodLevel(20);
        f.setExhaustion(0f);
    }
}
