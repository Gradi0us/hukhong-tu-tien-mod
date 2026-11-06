package com.hukhong.cores;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.network.CoreSyncPayload;
import com.hukhong.story.objective.Objectives;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Nhiệm vụ Phá Chấp (2026-11-06): khi lật trúng ô Mắc Kẹt ở Ấn Hồn Đài, người chơi phải hạ đủ quái
 * (8 + 2 x mốc) để phá Tâm Kiếp, rồi quay lại Ấn Hồn Đài roll lại. Trước đây chỉ gỡ được bằng lệnh admin.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PhaChap {
    private PhaChap() {}

    public static final String OBJ = "pha_chap";

    public static int need(int milestone) { return 8 + 2 * milestone; }

    public static boolean stuck(ServerPlayer p) {
        int m = CoreService.milestoneOf(CultivationService.get(p));
        return CoreService.get(p).stuckAt() == m && m > 0;
    }

    /** Bắt đầu (gọi khi vừa Mắc Kẹt). */
    public static void begin(ServerPlayer p) {
        p.setData(CoreAttachments.PHA_CHAP, 0);
        show(p);
    }

    private static void show(ServerPlayer p) {
        int m = CoreService.get(p).stuckAt();
        Objectives.set(p, OBJ, Component.translatable("hukhong.pha_chap.obj", p.getData(CoreAttachments.PHA_CHAP), need(m)), null, null);
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || !(e.getEntity() instanceof Enemy)) return;
        if (!stuck(p)) return;
        int m = CoreService.get(p).stuckAt();
        int k = p.getData(CoreAttachments.PHA_CHAP) + 1;
        p.setData(CoreAttachments.PHA_CHAP, k);
        if (k < need(m)) { show(p); return; }
        complete(p);
    }

    static void complete(ServerPlayer p) {
        p.setData(CoreAttachments.CORE_SLOTS, CoreService.get(p).withStuckCleared());
        p.setData(CoreAttachments.PHA_CHAP, 0);
        Objectives.complete(p, OBJ);
        CoreSyncPayload.send(p);
        p.sendSystemMessage(Component.translatable("hukhong.pha_chap.done"));
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 80, 0.6, 1.0, 0.6, 0.15);
            sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 0.8f);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && stuck(p)) show(p);
    }
}
