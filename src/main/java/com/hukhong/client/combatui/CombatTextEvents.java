package com.hukhong.client.combatui;

import com.hukhong.HuKhong;
import com.hukhong.core.ModAttachments;
import com.hukhong.gates.DamageRules;
import com.hukhong.skill.SkillPages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Phía server: báo sát thương người chơi gây ra / hồi máu cho client hiển thị số nổi. Chỉ gửi, không đổi gameplay. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CombatTextEvents {
    private CombatTextEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent.Post e) {
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide || t instanceof ArmorStand) return;
        float amt = e.getNewDamage();
        if (amt <= 0f) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || t == p) return;
        boolean skill = DamageRules.isSkill();
        boolean crit = DamageRules.lastCrit;
        DamageRules.lastCrit = false;
        int color = 0;
        if (skill) {
            var path = SkillPages.active(p.getData(ModAttachments.CULTIVATION).paths(), 0);
            if (path != null) color = path.argb;
        }
        byte kind = skill ? CombatTextPayload.SKILL : crit ? CombatTextPayload.CRIT : CombatTextPayload.NORMAL;
        send(t, amt, kind, color);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHeal(LivingHealEvent e) {
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide || !(t instanceof ServerPlayer p) || e.getAmount() < 1f) return;
        send(t, e.getAmount(), CombatTextPayload.HEAL, 0);
    }

    private static void send(LivingEntity t, float amt, byte kind, int color) {
        var rnd = t.getRandom();
        var pl = new CombatTextPayload(t.getId(), (float) t.getX() + (rnd.nextFloat() - 0.5f) * 0.6f,
                (float) (t.getY() + t.getBbHeight() * 0.9), (float) t.getZ() + (rnd.nextFloat() - 0.5f) * 0.6f,
                amt, kind, color);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(t, pl);
    }
}
