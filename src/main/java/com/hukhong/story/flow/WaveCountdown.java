package com.hukhong.story.flow;

import com.hukhong.HuKhong;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Dem nguoc truoc dot quai tap kich (buoc KILL cua man mo dau): bossbar + title, tieng tick 5s cuoi. Chi o bo nho. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class WaveCountdown {
    private WaveCountdown() {}

    public static final int SECONDS = 30, TICK_SOUND_LAST = 5;

    private static final class State {
        final String stepId; final long endTick; final ServerBossEvent bar;
        State(String stepId, long endTick, ServerBossEvent bar) { this.stepId = stepId; this.endTick = endTick; this.bar = bar; }
    }

    private static final Map<UUID, State> ACTIVE = new HashMap<>();

    public static boolean pending(ServerPlayer p) { return ACTIVE.containsKey(p.getUUID()); }

    /** Giay con lai (lam tron len) cho thoi diem game now va moc end; &lt;=0 = het. Thuan, kiem thu duoc. */
    public static int secondsLeft(long now, long end) { return (int) Math.max(0, (end - now + 19) / 20); }

    /** Co phat tieng tick o giay con lai nay khong (1..TICK_SOUND_LAST). */
    public static boolean tickSound(int secLeft) { return secLeft >= 1 && secLeft <= TICK_SOUND_LAST; }

    public static void start(ServerPlayer p, String stepId) {
        stop(p);
        ServerBossEvent bar = new ServerBossEvent(Component.translatable("hukhong.flow.countdown.bar", SECONDS),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        bar.addPlayer(p);
        ACTIVE.put(p.getUUID(), new State(stepId, p.serverLevel().getGameTime() + SECONDS * 20L, bar));
        StoryFlow.title(p, Component.translatable("hukhong.flow.countdown.title", SECONDS), Component.translatable("hukhong.flow.countdown.sub"));
    }

    public static void stop(ServerPlayer p) {
        State s = ACTIVE.remove(p.getUUID());
        if (s != null) s.bar.removeAllPlayers();
    }

    /** Moi 1s tu StoryFlow.onPlayerTick. Het gio -> goi spawn. Doi buoc / mat Tuyen truyen -> huy. */
    static void tick(ServerPlayer p, FlowSteps.Step cur) {
        State s = ACTIVE.get(p.getUUID());
        if (s == null) return;
        if (cur == null || !cur.id().equals(s.stepId)) { stop(p); return; }
        long now = p.serverLevel().getGameTime();
        int left = secondsLeft(now, s.endTick);
        if (left <= 0) {
            stop(p);
            StoryFlow.spawnHuThi(p, cur.n() + 2);
            p.sendSystemMessage(Component.translatable("hukhong.flow.countdown.go"));
            p.level().playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.8f, 0.8f);
            return;
        }
        s.bar.setProgress(Math.min(1f, Math.max(0f, (s.endTick - now) / (SECONDS * 20f))));
        s.bar.setName(Component.translatable("hukhong.flow.countdown.bar", left));
        if (tickSound(left)) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1.0f, left == 1 ? 1.6f : 1.0f);
            p.displayClientMessage(Component.translatable("hukhong.flow.countdown.sec", left), true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) stop(p);
    }
}
