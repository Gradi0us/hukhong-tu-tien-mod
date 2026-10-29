package com.hukhong.world.calendar;

import com.hukhong.bicanh.BiCanhManager;
import com.hukhong.story.world.StoryHooks;
import com.hukhong.travel.TravelRules;
import com.hukhong.travel.TravelService;
import com.hukhong.trial.TrialService;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Nut "Den": dich chuyen mien phi toi diem su kien. Luat: su kien dang dien ra hoac bat dau trong vong 2 phut,
 * 1 lan / lan xuat hien / nguoi, khong khi vua bi danh (10 giay) hoac dang chien dau, khong trong Bi Canh / Tam Thuc.
 */
public final class CalendarService {
    private CalendarService() {}

    public static final int EARLY_TICKS = 2400, HURT_TICKS = 200, ASSEMBLY_OFFSET = 20;

    /** Thuan: rang buoc thoi gian. null = duoc phep. */
    public static String timing(long now, long start, long end) {
        if (now < start - EARLY_TICKS) return "hukhong.cal.err_early";
        if (now >= end) return "hukhong.cal.err_over";
        return null;
    }

    /** Kiem tra day du (khong dich chuyen). null = duoc phep, con lai = key loi. */
    public static String validate(ServerPlayer p, CalendarScheduler.Occ o, CalendarData data) {
        long now = p.serverLevel().getGameTime();
        String err = timing(now, o.start(), o.end());
        if (err != null) return err;
        if (o.def().story() && !StoryHooks.allowed(p, o.def().lore())) return "hukhong.cal.err_locked";
        if (!p.isAlive() || p.isSpectator()) return "hukhong.travel.err_state";
        if (data.isUsed(o.key(), p.getUUID())) return "hukhong.cal.err_used";
        if (TravelRules.inCombat(p) || recentlyHurt(p)) return "hukhong.cal.err_combat";
        if (BiCanhManager.inRun(p)) return "hukhong.travel.err_bicanh";
        if (TrialService.active(p) || p.level().dimension() == TrialService.TAM_THUC) return "hukhong.travel.err_tamthuc";
        if (TravelService.channeling(p)) return "hukhong.travel.err_busy";
        return null;
    }

    private static boolean recentlyHurt(ServerPlayer p) {
        int ts = p.getLastHurtByMobTimestamp();
        return ts != 0 && p.tickCount - ts < HURT_TICKS;
    }

    /** @return null neu da dich chuyen, hoac key loi. */
    public static String go(ServerPlayer p, String key) {
        ServerLevel ow = p.server.overworld();
        CalendarScheduler.Occ o = CalendarScheduler.parse(key);
        if (o == null) return "hukhong.cal.err_unknown";
        CalendarData data = CalendarData.get(ow);
        String err = validate(p, o, data);
        if (err != null) return err;
        CalendarData.Act a = data.active().get(key);
        if (a == null) a = CalendarScheduler.toAct(ow, o); // sap bat dau: toa do tat dinh, giong luc bat dau
        // diem tap ket ngay ngoai vung su kien
        BlockPos land = TravelService.findLanding(ow, a.x() + ASSEMBLY_OFFSET, a.z(), null);
        data.markUsed(key, p.getUUID());
        Fx.play(p.serverLevel(), FxType.LIGHT_PILLAR, p.position(), Vec3.ZERO, 1.2f, Fx.CYAN, 20);
        p.teleportTo(ow, land.getX() + 0.5, land.getY(), land.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        Fx.play(ow, FxType.LIGHT_PILLAR, Vec3.atBottomCenterOf(land), Vec3.ZERO, 1.2f, Fx.CYAN, 30);
        ow.playSound(null, land, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9f, 1.0f);
        p.displayClientMessage(Component.translatable("hukhong.cal.arrived", Component.translatable(o.def().nameKey())), true);
        return null;
    }
}
