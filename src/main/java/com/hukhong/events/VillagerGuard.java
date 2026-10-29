package com.hukhong.events;

import com.hukhong.HuKhong;
import com.hukhong.entity.HuMob;
import com.hukhong.world.SafeZones;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;

/** Quai Hu Hoa khong bien dan lang thanh zombie villager vanilla, va khong nham dan lang trong khu dan cu. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class VillagerGuard {
    private VillagerGuard() {}

    /** Thuan: dan lang bi doi thanh zombie villager boi quai mod? (nguoi danh cuoi la HuMob, hoac co HuMob dung sat). */
    public static boolean blocksConversion(LivingEntity victim, EntityType<?> outcome) {
        if (!(victim instanceof Villager) || outcome != EntityType.ZOMBIE_VILLAGER) return false;
        if (victim.getLastHurtByMob() instanceof HuMob) return true;
        return !victim.level().getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(6), e -> e instanceof HuMob).isEmpty();
    }

    @SubscribeEvent
    public static void onConvert(LivingConversionEvent.Pre e) {
        if (blocksConversion(e.getEntity(), e.getOutcome())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent e) {
        if (!(e.getEntity() instanceof HuMob) || !(e.getNewAboutToBeSetTarget() instanceof AbstractVillager v)) return;
        if (e.getEntity().level() instanceof ServerLevel sl && SafeZones.isSafe(sl, v.blockPosition())) e.setCanceled(true);
    }
}
