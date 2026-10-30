package com.hukhong.dongdao;

import com.hukhong.bicanh.BiCanhManager;
import com.hukhong.endgame.EndgameRegistry;
import com.hukhong.pvp.DuelManager;
import com.hukhong.travel.TravelRules;
import com.hukhong.travel.TravelService;
import com.hukhong.trial.TrialService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** "Dong Dao": dich chuyen mien phi toi nguoi choi khac. Hoi chieu 30s, cam khi chien dau / noi cam / dich dang o noi cam. */
@EventBusSubscriber(modid = com.hukhong.HuKhong.MODID)
public final class DongDao {
    private DongDao() {}

    public static final int COOLDOWN_TICKS = 600, HURT_TICKS = 200;
    private static final Map<UUID, Long> READY = new HashMap<>();

    /** Bi Canh, Tam Thuc, dau truong ket (Sieu Do chay trong chieu nay), tran dau PvP. */
    private static boolean restrictedPlace(ServerPlayer p) {
        return BiCanhManager.inRun(p)
                || TrialService.active(p) || p.level().dimension() == TrialService.TAM_THUC
                || p.level().dimension() == EndgameRegistry.DIM
                || DuelManager.inMatch(p);
    }

    private static boolean recentlyHurt(ServerPlayer p) {
        int ts = p.getLastHurtByMobTimestamp();
        return ts != 0 && p.tickCount - ts < HURT_TICKS;
    }

    /** @return null neu duoc phep, hoac key loi. */
    public static String validate(ServerPlayer p, ServerPlayer t) {
        if (t == null || t == p) return "hukhong.dongdao.err_target";
        if (!p.isAlive() || p.isSpectator()) return "hukhong.travel.err_state";
        if (!t.isAlive() || t.isSpectator()) return "hukhong.dongdao.err_target";
        Long ready = READY.get(p.getUUID());
        if (ready != null && ready > p.server.overworld().getGameTime()) return "hukhong.dongdao.err_cooldown";
        if (TravelRules.inCombat(p) || recentlyHurt(p)) return "hukhong.cal.err_combat";
        if (TravelService.channeling(p)) return "hukhong.travel.err_busy";
        if (restrictedPlace(p)) return "hukhong.dongdao.err_here";
        if (restrictedPlace(t)) return "hukhong.dongdao.err_there";
        return null;
    }

    /** @return null neu da dich chuyen, hoac key loi. */
    public static String go(ServerPlayer p, UUID target) { return go(p, p.server.getPlayerList().getPlayer(target)); }

    public static String go(ServerPlayer p, ServerPlayer t) {
        String err = validate(p, t);
        if (err != null) return err;
        ServerLevel tl = t.serverLevel();
        BlockPos tp = t.blockPosition();
        BlockPos land = TravelService.findLanding(tl, tp.getX(), tp.getZ(), tp.getY() - 1);
        if (Math.abs(land.getY() - tp.getY()) > 8) land = tp;
        READY.put(p.getUUID(), p.server.overworld().getGameTime() + COOLDOWN_TICKS);
        if (p.connection == null || p instanceof net.neoforged.neoforge.common.util.FakePlayer) p.setPos(land.getX() + 0.5, land.getY(), land.getZ() + 0.5); // nguoi choi gia (GameTest)
        else p.teleportTo(tl, land.getX() + 0.5, land.getY(), land.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        t.displayClientMessage(Component.translatable("hukhong.dongdao.notice", p.getDisplayName()), false);
        return null;
    }

    public static void resetCooldown(UUID id) { READY.remove(id); }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { READY.remove(e.getEntity().getUUID()); }
}
