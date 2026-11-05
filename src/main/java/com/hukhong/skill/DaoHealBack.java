package com.hukhong.skill;

import com.hukhong.HuKhong;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gates.DamageRules;
import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Đạo Tu - Phản Sinh: mỗi khi Đạo Tu hồi máu, kẻ địch trong 8 ô mất máu bằng 3 lần lượng vừa hồi
 * (gom mỗi 0,5s để hồi máu nhỏ liên tục không tạo quá nhiều đòn).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class DaoHealBack {
    private DaoHealBack() {}

    public static final double MULT = 3.0, RADIUS = 8.0;
    public static final int FLUSH_TICKS = 10;
    private static final Map<UUID, Float> ACC = new ConcurrentHashMap<>();

    static boolean isDao(ServerPlayer p) { return p.getData(ModAttachments.CULTIVATION).paths().contains(Path.DAO); }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getAmount() <= 0 || !isDao(p)) return;
        float real = Math.min(e.getAmount(), p.getMaxHealth() - p.getHealth()); // chỉ tính phần thực sự hồi
        if (real > 0) ACC.merge(p.getUUID(), real, Float::sum);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && p.tickCount % FLUSH_TICKS == 0) flush(p);
    }

    /** Gây sát thương phản sinh đã gom (test gọi tay). Trả tổng sát thương mỗi mục tiêu. */
    public static float flush(ServerPlayer p) {
        Float acc = ACC.remove(p.getUUID());
        if (acc == null || acc <= 0 || !(p.level() instanceof ServerLevel lv)) return 0;
        float dmg = (float) (acc * MULT);
        for (LivingEntity t : lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(RADIUS, 4, RADIUS),
                x -> x.isAlive() && x != p && !(x instanceof Player) && !(x instanceof net.minecraft.world.entity.npc.Npc)
                        && (x instanceof net.minecraft.world.entity.monster.Enemy || (x instanceof net.minecraft.world.entity.Mob m && m.getTarget() != null))
                        && x.distanceToSqr(p) <= RADIUS * RADIUS)) {
            t.invulnerableTime = 0;
            DamageRules.hurtBySkill(p, t, dmg);
            Fx.ribbon(lv, p, p.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), 0.4, 5, 0, 0.10f, 0xFFE9A0);
            lv.sendParticles(ParticleTypes.END_ROD, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 6, 0.25, 0.35, 0.25, 0.04);
        }
        return dmg;
    }
}
