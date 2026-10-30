package com.hukhong.dongdao;

import com.hukhong.bicanh.BiCanhManager;
import com.hukhong.endgame.EndgameRegistry;
import com.hukhong.pvp.DuelManager;
import com.hukhong.travel.TravelRules;
import com.hukhong.travel.TravelService;
import com.hukhong.trial.TrialService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** "Ve cho chet": dich chuyen mien phi toi cho chet cuoi (1 lan / lan chet), roi "Ba The" mien sat thuong 4 giay. */
@EventBusSubscriber(modid = com.hukhong.HuKhong.MODID)
public final class VeChoChet {
    private VeChoChet() {}

    public static final int IMMUNE_TICKS = 80;
    /** Thoi diem chet (gameTime) va co "chet o noi cam". Chi ghi nho trong bo nho. */
    private static final Map<UUID, Long> DIED_AT = new HashMap<>();
    private static final Map<UUID, Boolean> DIED_RESTRICTED = new HashMap<>();
    private static final Map<UUID, Long> IMMUNE_UNTIL = new HashMap<>();

    private static boolean restricted(ServerPlayer p) {
        return BiCanhManager.inRun(p) || TrialService.active(p) || p.level().dimension() == TrialService.TAM_THUC
                || p.level().dimension() == EndgameRegistry.DIM || DuelManager.inMatch(p);
    }

    private static long now(ServerPlayer p) { return p.server.overworld().getGameTime(); }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            DIED_AT.put(p.getUUID(), now(p));
            DIED_RESTRICTED.put(p.getUUID(), restricted(p));
            IMMUNE_UNTIL.remove(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        DIED_AT.remove(e.getEntity().getUUID());
        DIED_RESTRICTED.remove(e.getEntity().getUUID());
        IMMUNE_UNTIL.remove(e.getEntity().getUUID());
    }

    public static boolean immune(ServerPlayer p) {
        Long u = IMMUNE_UNTIL.get(p.getUUID());
        return u != null && u > now(p);
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && immune(p)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !IMMUNE_UNTIL.containsKey(p.getUUID())) return;
        if (!immune(p)) { IMMUNE_UNTIL.remove(p.getUUID()); return; }
        if (p.tickCount % 4 == 0) p.serverLevel().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 3, 0.4, 0.8, 0.4, 0.02);
        if (p.tickCount % 10 == 0) p.displayClientMessage(Component.translatable("hukhong.chochet.immune", (IMMUNE_UNTIL.get(p.getUUID()) - now(p) + 19) / 20), true);
    }

    /** Vi tri chet con hop le (chua dung, khong o noi cam, chieu con ton tai), hoac empty. */
    public static Optional<GlobalPos> valid(ServerPlayer p) {
        Optional<GlobalPos> d = p.getLastDeathLocation();
        if (d.isEmpty()) return d;
        if (Boolean.TRUE.equals(DIED_RESTRICTED.get(p.getUUID()))) return Optional.empty();
        ServerLevel l = p.server.getLevel(d.get().dimension());
        if (l == null || d.get().dimension() == TrialService.TAM_THUC || d.get().dimension() == EndgameRegistry.DIM) return Optional.empty();
        return d;
    }

    /** Ticks tu luc chet, -1 neu khong ro. */
    public static long ageTicks(ServerPlayer p) { Long t = DIED_AT.get(p.getUUID()); return t == null ? -1 : now(p) - t; }

    /** @return null neu da dich chuyen, hoac key loi. */
    public static String go(ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator()) return "hukhong.travel.err_state";
        Optional<GlobalPos> d = valid(p);
        if (d.isEmpty()) return "hukhong.chochet.err_none";
        if (TravelRules.inCombat(p)) return "hukhong.cal.err_combat";
        if (TravelService.channeling(p)) return "hukhong.travel.err_busy";
        if (restricted(p)) return "hukhong.dongdao.err_here";
        ServerLevel tl = p.server.getLevel(d.get().dimension());
        BlockPos tp = d.get().pos();
        BlockPos land = TravelService.findLanding(tl, tp.getX(), tp.getZ(), tp.getY() - 1);
        if (Math.abs(land.getY() - tp.getY()) > 8) land = tp;
        p.setLastDeathLocation(Optional.empty());
        DIED_AT.remove(p.getUUID());
        DIED_RESTRICTED.remove(p.getUUID());
        if (p.connection == null || p instanceof net.neoforged.neoforge.common.util.FakePlayer) p.setPos(land.getX() + 0.5, land.getY(), land.getZ() + 0.5);
        else p.teleportTo(tl, land.getX() + 0.5, land.getY(), land.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        IMMUNE_UNTIL.put(p.getUUID(), now(p) + IMMUNE_TICKS);
        p.displayClientMessage(Component.translatable("hukhong.chochet.immune", IMMUNE_TICKS / 20), true);
        return null;
    }
}
