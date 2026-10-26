package com.hukhong.gates;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid = HuKhong.MODID)
public final class GateEvents {
    private GateEvents() {}

    @SubscribeEvent
    public static void onFinalize(FinalizeSpawnEvent e) {
        MobSpawnType t = e.getSpawnType();
        if (t == MobSpawnType.BREEDING)
            e.getEntity().getPersistentData().putBoolean(com.hukhong.pathrules.MaRules.BRED_TAG, true);
        if (t == MobSpawnType.SPAWNER || t == MobSpawnType.TRIAL_SPAWNER) {
            e.getEntity().setData(SpawnerMarks.MARK, true);
        }
    }

    /** EXP đơn giản hóa P0: máu tối đa nạn nhân x RewardGate. Spawner = 0. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof Player) return;
        if (e.getEntity() instanceof com.hukhong.entity.HuMob) { // quái Hư Hóa tự thưởng trong entity/ (tránh EXP đôi)
            if (e.getSource().getEntity() instanceof ServerPlayer p)
                com.hukhong.pathrules.MaRules.onKill(p, e.getEntity(), RewardGate.canReward(e.getEntity()) ? 1f : 0f);
            return;
        }
        if (e.getSource().getEntity() instanceof ServerPlayer p) {
            float rg = RewardGate.reward(e.getEntity()); // ghi KillRateGate đúng 1 lần
            long exp = Math.round(e.getEntity().getMaxHealth() * rg * com.hukhong.pathrules.MaRules.expMultiplier(p, e.getEntity()));
            if (exp > 0) CultivationService.addExp(p, exp);
            com.hukhong.pathrules.MaRules.onKill(p, e.getEntity(), rg);
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { KillRateGate.clear(); }
}
