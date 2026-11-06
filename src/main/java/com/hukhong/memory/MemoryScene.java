package com.hukhong.memory;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.boss.phase.CutscenePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Cảnh hồi ức: máy quay điện ảnh (CutscenePayload) + phụ đề, hiệu ứng ảo ảnh dựng từ Fx, âm vọng.
 * Mỗi cảnh là một dòng thời gian tick; người chơi bị giữ trong cảnh cho tới khi xong (hoặc chết / đổi thế giới thì huỷ).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class MemoryScene {
    private MemoryScene() {}

    public static final int MEMORY_TICKS = 300, FINALE_TICKS = 360;
    private static final Map<UUID, Scene> ACTIVE = new HashMap<>();

    /** Một cảnh đang chạy: mốc tick -> việc cần làm, đường máy quay, phụ đề, lời kết khi xong. */
    static final class Scene {
        final ServerPlayer p;
        final ServerLevel lv;
        final Vec3 c, fwd, right;
        final Runnable onEnd;
        final TreeMap<Integer, List<Consumer<Scene>>> beats = new TreeMap<>();
        final List<CamPath.Key> keys = new ArrayList<>();
        final List<SubLine> subs = new ArrayList<>();
        Runnable endCard = () -> {};
        int total = MEMORY_TICKS, tick;

        Scene(ServerPlayer p, Runnable onEnd) {
            this.p = p;
            this.lv = p.serverLevel();
            this.onEnd = onEnd;
            this.c = p.position();
            double yr = Math.toRadians(p.getYRot()); // cùng quy ước với CutsceneClient
            this.fwd = new Vec3(-Math.sin(yr), 0, Math.cos(yr));
            this.right = new Vec3(-Math.cos(yr), 0, -Math.sin(yr));
        }

        /** Điểm so với chân người chơi: r sang phải, up lên, f ra trước (theo hướng nhìn lúc bắt đầu). */
        Vec3 pos(double r, double up, double f) { return c.add(right.scale(r)).add(0, up, 0).add(fwd.scale(f)); }

        void beat(int t, Consumer<Scene> fn) { beats.computeIfAbsent(t, k -> new ArrayList<>()).add(fn); }

        void every(int from, int to, int step, Consumer<Scene> fn) { for (int t = from; t <= to; t += step) beat(t, fn); }

        void sub(int from, int to, String key) { subs.add(new SubLine(from, to, key)); }

        void cam(CamPath.Key... ks) { keys.addAll(List.of(ks)); }
    }

    public static boolean active(ServerPlayer p) { return ACTIVE.containsKey(p.getUUID()); }

    public static void cancel(ServerPlayer p) { ACTIVE.remove(p.getUUID()); }

    /** Bắt đầu cảnh: id 1..8 = hồi ức, 0 = cảnh kết. onEnd chạy khi cảnh xong (không chạy nếu bị huỷ). */
    public static void start(ServerPlayer p, int id, Runnable onEnd) {
        Scene s = new Scene(p, onEnd);
        if (id == 0) MemoryVisions.finale(s);
        else MemoryVisions.memory(s, id);
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 14, 0, false, false));
        // không thêm DARKNESS: cảnh ký ức phải nhìn rõ
        try {
            PacketDistributor.sendToPlayer(p, new CutscenePayload(-1, s.c.x, s.c.y, s.c.z, p.getYRot(), s.total, s.keys, s.subs));
        } catch (RuntimeException ignored) { } // người chơi giả (test)
        ACTIVE.put(p.getUUID(), s);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (ACTIVE.isEmpty()) return;
        List<Scene> done = new ArrayList<>();
        Iterator<Scene> it = ACTIVE.values().iterator();
        while (it.hasNext()) {
            Scene s = it.next();
            if (!s.p.isAlive() || s.p.isRemoved() || s.p.serverLevel() != s.lv) { it.remove(); continue; }
            s.tick++;
            while (!s.beats.isEmpty() && s.beats.firstKey() <= s.tick)
                for (Consumer<Scene> fn : s.beats.pollFirstEntry().getValue()) fn.accept(s);
            if (s.tick >= s.total) { it.remove(); done.add(s); }
        }
        for (Scene s : done) {  // ngoài vòng lặp: onEnd có thể mở cảnh mới
            s.endCard.run();
            if (s.onEnd != null) s.onEnd.run();
        }
    }

    /** Chữ lớn giữa màn hình + phụ đề (dưới chữ lớn). */
    static void title(ServerPlayer p, Component title, Component sub) {
        if (p.connection == null) return;
        try {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 30));
            p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
            p.connection.send(new ClientboundSetTitleTextPacket(title));
        } catch (RuntimeException ignored) { }
    }
}
