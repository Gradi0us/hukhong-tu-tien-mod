package com.hukhong.world;

import com.hukhong.HuKhong;
import net.minecraft.world.entity.ExperienceOrb;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * 2026-11-06: bỏ hệ kinh nghiệm vanilla (thanh XP xanh). Sức mạnh chỉ đến từ tu luyện của mod.
 * Không rơi quả cầu XP (quái, khối, lò nung, câu cá, nhân giống...), người chơi không nhận / mất cấp XP vanilla.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class NoVanillaXp {
    private NoVanillaXp() {}

    @SubscribeEvent
    public static void onMobXp(LivingExperienceDropEvent e) { e.setDroppedExperience(0); }

    @SubscribeEvent
    public static void onBlockXp(BlockDropsEvent e) { e.setDroppedExperience(0); }

    /** Chốt chặn cuối: mọi quả cầu XP khác (lò nung, câu cá, chai XP...) không được sinh ra. */
    @SubscribeEvent
    public static void onOrb(EntityJoinLevelEvent e) { if (e.getEntity() instanceof ExperienceOrb) e.setCanceled(true); }

    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange e) { if (e.getAmount() > 0) e.setCanceled(true); }

    @SubscribeEvent
    public static void onLevelChange(PlayerXpEvent.LevelChange e) { if (e.getLevels() > 0) e.setCanceled(true); }
}
