package com.hukhong.travel;

import com.hukhong.bicanh.BiCanhManager;
import com.hukhong.events.InvasionManager;
import com.hukhong.trial.TrialService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import com.hukhong.HuKhong;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Dieu kien cam dich chuyen: chien dau (5s), boss, Bi Canh, Tam Thuc, xam lan, sai chieu. check() tra key loi hoac null. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class TravelRules {
    private TravelRules() {}

    private static final Map<UUID, Long> LAST_COMBAT = new HashMap<>();

    /** Danh dau nguoi choi vua giao chien (gameTime hien tai). */
    public static void markCombat(ServerPlayer p) { LAST_COMBAT.put(p.getUUID(), p.level().getGameTime()); }

    public static boolean inCombat(ServerPlayer p) {
        Long t = LAST_COMBAT.get(p.getUUID());
        return t != null && p.level().getGameTime() - t < TravelMath.COMBAT_TICKS;
    }

    /** Boss dang nham vao nguoi choi trong 48 block (rong, thu tuc, hoac quai >= 300 mau). */
    public static boolean bossFight(ServerPlayer p) {
        return !p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(48),
                m -> m.isAlive() && m.getTarget() == p
                        && (m instanceof EnderDragon || m instanceof WitherBoss || m.getMaxHealth() >= 300f)).isEmpty();
    }

    /** Rang buoc ngu canh (khong gom hoi chieu / kenh dang chay). null = duoc phep. */
    public static String check(ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator()) return "hukhong.travel.err_state";
        if (inCombat(p)) return "hukhong.travel.err_combat";
        if (BiCanhManager.inRun(p)) return "hukhong.travel.err_bicanh";
        if (TrialService.active(p) || p.level().dimension() == TrialService.TAM_THUC) return "hukhong.travel.err_tamthuc";
        if (p.level().dimension() != Level.OVERWORLD) return "hukhong.travel.err_dim";
        if (InvasionManager.active() || InvasionManager.warning()) return "hukhong.travel.err_invasion";
        if (bossFight(p)) return "hukhong.travel.err_boss";
        return null;
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer v) {
            if (e.getSource().getEntity() != null) markCombat(v);
            TravelService.cancel(v, "hukhong.travel.err_hurt");
        }
        if (e.getSource().getEntity() instanceof ServerPlayer a && a != e.getEntity()) markCombat(a);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { LAST_COMBAT.remove(e.getEntity().getUUID()); }
}
