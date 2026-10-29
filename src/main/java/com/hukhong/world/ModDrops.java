package com.hukhong.world;

import com.hukhong.HuKhong;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Quái (sinh vật của mod + mọi quái thù địch) chỉ rơi vật phẩm của mod: bỏ mọi vật phẩm "minecraft:" (kiếm/giáp sắt,
 * thịt thối, xương...). Động vật vanilla (bò, cừu...) giữ nguyên đồ rơi. Chạy sau cùng để lọc cả đồ do mod khác thêm.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class ModDrops {
    private ModDrops() {}

    static boolean filtered(LivingEntity v) {
        if (v instanceof Player) return false;
        String ns = BuiltInRegistries.ENTITY_TYPE.getKey(v.getType()).getNamespace();
        return HuKhong.MODID.equals(ns) || v instanceof Enemy;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrops(LivingDropsEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide || !filtered(v)) return;
        e.getDrops().removeIf(ie -> "minecraft".equals(BuiltInRegistries.ITEM.getKey(ie.getItem().getItem()).getNamespace()));
    }
}
