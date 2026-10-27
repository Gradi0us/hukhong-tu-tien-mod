package com.hukhong.city;

import com.hukhong.HuKhong;
import com.hukhong.society.SectBuilder;
import com.hukhong.society.SectSavedData;
import com.hukhong.story.npc.StoryNpc;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Dân làng / thương nhân lang thang / NPC cốt truyện trong thành (và tông môn) không bị người chơi làm hại:
 * chặn mọi sát thương mà nguồn gốc là người chơi (cận chiến, kỹ năng, tên bắn, phép...).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CityProtection {
    private CityProtection() {}

    /** Bán kính (ô, hệ Chebyshev) quanh tâm thành tính là "trong thành": tường + vùng quanh cổng. */
    public static final int CITY_RADIUS = CityBuilder.HALF + 16;
    public static final int SECT_RADIUS = SectBuilder.HALF + 16;

    /** Thực thể thuộc diện bảo vệ (dân làng, thương nhân, NPC cốt truyện). */
    public static boolean isProtectedKind(Entity e) {
        return e instanceof AbstractVillager || e instanceof StoryNpc;
    }

    /** Vị trí (x,z) nằm trong thành đã dựng hoặc tông môn đã dựng. */
    public static boolean inProtectedZone(ServerLevel any, double x, double z) {
        ServerLevel ow = any.getServer().getLevel(Level.OVERWORLD);
        if (ow == null || any.dimension() != Level.OVERWORLD) return false;
        for (CityDef d : CityDef.ALL) {
            var c = CityService.builtCenter(ow, d);
            if (c != null && Math.abs(c.getX() - x) <= CITY_RADIUS && Math.abs(c.getZ() - z) <= CITY_RADIUS) return true;
        }
        for (var c : SectSavedData.get(ow).all().values())
            if (Math.abs(c.getX() - x) <= SECT_RADIUS && Math.abs(c.getZ() - z) <= SECT_RADIUS) return true;
        return false;
    }

    /** Có nên chặn đòn của {@code attacker} (người chơi) lên {@code victim}? */
    public static boolean shields(Entity victim, Entity attacker) {
        if (!(attacker instanceof Player) || victim.level().isClientSide || !isProtectedKind(victim)) return false;
        if (victim instanceof StoryNpc) return true;
        return victim.level() instanceof ServerLevel sl && inProtectedZone(sl, victim.getX(), victim.getZ());
    }

    private static Player playerOf(DamageSource src) {
        if (src.getEntity() instanceof Player p) return p;
        if (src.getDirectEntity() instanceof Player p) return p;
        return null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity v = e.getEntity();
        Player p = playerOf(e.getSource());
        if (p == null || !shields(v, p)) return;
        e.setCanceled(true);
        if (p instanceof ServerPlayer sp && sp.tickCount % 20 == 0)
            sp.displayClientMessage(Component.translatable("hukhong.city.npc_protected"), true);
    }

    /** Chặn luôn cú đánh (không có hất lùi / âm thanh / quét kiếm). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(AttackEntityEvent e) {
        if (shields(e.getTarget(), e.getEntity())) e.setCanceled(true);
    }
}
