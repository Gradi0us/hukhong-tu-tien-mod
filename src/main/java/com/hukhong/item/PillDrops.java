package com.hukhong.item;

import com.hukhong.gates.RewardGate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Tạm thời: zombie/skeleton tự nhiên rơi đan với tỉ lệ thấp (spawner không rơi). */
public final class PillDrops {
    private PillDrops() {}

    public static final double KHAI_MACH_CHANCE = 0.02, NGUNG_NGUYEN_CHANCE = 0.01;

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        var v = e.getEntity();
        if (v.level().isClientSide) return;
        // 2026-11-06: zombie/skeleton bị SpawnRules chặn nên trước đây không bao giờ rơi; nay mọi quái thù địch (Hư Hóa, Yêu Thú...) đều rơi
        if (!(v instanceof net.minecraft.world.entity.monster.Enemy)) return;
        if (!RewardGate.canReward(v)) return;
        roll(e, KHAI_MACH_CHANCE, new ItemStack(ModItems.KHAI_MACH_DAN.get()));
        roll(e, NGUNG_NGUYEN_CHANCE, new ItemStack(ModItems.NGUNG_NGUYEN_DAN.get()));
    }

    private static void roll(LivingDropsEvent e, double chance, ItemStack s) {
        var v = e.getEntity();
        if (v.getRandom().nextDouble() < chance)
            e.getDrops().add(new ItemEntity(v.level(), v.getX(), v.getY(), v.getZ(), s));
    }
}
