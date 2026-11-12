package com.hukhong.endgame;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.boss.phase.CamPath;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Khung chung 3 cutscene kết (Phong Ấn / Hấp Thụ / Siêu Độ) diễn thật trong thế giới (PLAN-v1 §8b.2): camera
 * {@link BossPhaseDirector#sendCutscene} neo tại chỗ người chơi đứng, người chơi là diễn viên (giữ tại {@code o}),
 * phụ đề = 3 dòng cũ ở mốc 40-200 / 200-360 / 360-520. Chỉ bỏ qua được sau {@link #MIN_SKIP}. Xong (hoặc bỏ qua)
 * mới gửi {@link EndgameNet.PlayCutscene} để client mở credits. Phần dàn dựng từng kết nằm ở
 * {@link EndingOutroSeal}, {@link EndingOutroAbsorb}, {@link EndingOutroTruth}.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class EndingOutro {
    private EndingOutro() {}

    public static final int T = EndgameKit.CUTSCENE_TICKS, MIN_SKIP = T / 2;
    /** Thực thể phụ (Thiên Môn, bóng ma, Dạ Khuyết...) - dọn khi relog/server tắt giữa chừng. */
    public static final String TAG = "hk_outro_actor";

    /** Dàn dựng một kết: camera + bước theo tick. */
    interface Scene {
        List<CamPath.Key> cam();
        void step(Run r, int t);
        /** Gọi một lần khi kết thúc (bình thường hoặc bỏ qua) - đặt dấu ngoại hình cuối cùng. */
        default void finish(Run r) {}
    }

    /** Một lượt outro của một người chơi. */
    static final class Run {
        final ServerLevel sl;
        final UUID pid;
        final int ending;
        final Vec3 o, fwd, right;
        final float yaw;
        /** Hướng mặt người chơi đang bị giữ (đổi được giữa cảnh, vd. quay lưng nhìn bình minh). */
        float face;
        boolean faceDirty;
        final Scene scene;
        final List<Entity> actors = new ArrayList<>();
        int t;
        Run(ServerLevel sl, ServerPlayer p, int ending, Scene scene) {
            this.sl = sl; this.pid = p.getUUID(); this.ending = ending; this.scene = scene;
            this.o = p.position();
            this.yaw = p.getYRot();
            this.face = yaw;
            double yr = Math.toRadians(yaw);
            fwd = new Vec3(-Math.sin(yr), 0, Math.cos(yr));
            right = new Vec3(-Math.cos(yr), 0, -Math.sin(yr));
        }
        /** Điểm tương đối: dx sang phải, dy lên, dz về phía trước người chơi. */
        Vec3 rel(double dx, double dy, double dz) { return o.add(right.scale(dx)).add(fwd.scale(dz)).add(0, dy, 0); }
        void face(float y) { face = y; faceDirty = true; }
        ServerPlayer player() { return sl.getServer().getPlayerList().getPlayer(pid); }
        /** Đăng ký thực thể phụ: noAi, bất tử, im lặng, gắn TAG. Gọi TRƯỚC addFreshEntity. */
        <E extends Entity> E actor(E e) {
            e.addTag(TAG);
            e.setInvulnerable(true);
            e.setSilent(true);
            if (e instanceof Mob m) { m.setNoAi(true); m.setPersistenceRequired(); }
            actors.add(e);
            return e;
        }
        void sound(Vec3 at, SoundEvent ev, float vol, float pitch) {
            sl.playSound(null, at.x, at.y, at.z, ev, SoundSource.AMBIENT, vol, pitch);
        }
    }

    /** Bóng người bằng hạt (đầu, thân, hai chân, một tay với về {@code reach}). */
    static void silhouette(ServerLevel sl, Vec3 feet, Vec3 reach, net.minecraft.core.particles.ParticleOptions pt) {
        sl.sendParticles(pt, feet.x, feet.y + 1.75, feet.z, 2, 0.12, 0.12, 0.12, 0.0);
        for (double y = 0.1; y <= 1.45; y += 0.22) sl.sendParticles(pt, feet.x, feet.y + y, feet.z, 1, 0.1, 0.04, 0.1, 0.0);
        if (reach != null) {
            Vec3 sh = feet.add(0, 1.4, 0), d = reach.subtract(sh);
            for (int i = 1; i <= 4; i++) { Vec3 a = sh.add(d.scale(i / 4.0)); sl.sendParticles(pt, a.x, a.y, a.z, 1, 0.03, 0.03, 0.03, 0.0); }
        }
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();

    public static boolean running(ServerPlayer p) { return RUNS.containsKey(p.getUUID()); }

    static CamPath.Key k(int t, float dx, float dy, float dz, float fov, float lookH, float pitch) {
        return new CamPath.Key(t, dx, dy, dz, 0f, pitch, fov, lookH);
    }

    static CamPath.Key k(int t, float dx, float dy, float dz, float fov, float lookH, float pitch, float yaw) {
        return new CamPath.Key(t, dx, dy, dz, yaw, pitch, fov, lookH);
    }

    private static String key(int e) { return e == EndgameKit.ENDING_SEAL ? "seal" : e == EndgameKit.ENDING_TRUTH ? "truth" : "absorb"; }

    /** Bắt đầu outro cho người chơi; trả về thời lượng (tick). */
    public static int start(ServerPlayer p, int ending) {
        Run old = RUNS.remove(p.getUUID());
        if (old != null) cleanup(old);
        Scene scene = ending == EndgameKit.ENDING_SEAL ? new EndingOutroSeal()
                : ending == EndgameKit.ENDING_TRUTH ? new EndingOutroTruth() : new EndingOutroAbsorb();
        Run r = new Run(p.serverLevel(), p, ending, scene);
        String base = "hukhong.endgame.cutscene." + key(ending) + ".";
        List<SubLine> subs = List.of(
                new SubLine(4, 40, base + "title"),
                new SubLine(40, 200, base + "1"),
                new SubLine(200, 360, base + "2"),
                new SubLine(360, 520, base + "3"));
        BossPhaseDirector.sendCutscene(r.sl, null, r.o, r.yaw, T, scene.cam(), subs, MIN_SKIP);
        RUNS.put(p.getUUID(), r);
        return T;
    }

    /** Client xin bỏ qua (đã qua MIN_SKIP mới nhận). */
    public static boolean skip(ServerPlayer p) {
        Run r = RUNS.get(p.getUUID());
        if (r == null || r.t < MIN_SKIP) return false;
        RUNS.remove(p.getUUID());
        end(r, p);
        return true;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (RUNS.isEmpty()) return;
        MinecraftServer s = e.getServer();
        var it = RUNS.values().iterator();
        List<Run> done = new ArrayList<>();
        while (it.hasNext()) {
            Run r = it.next();
            ServerPlayer p = r.player();
            if (p == null || r.sl.getServer() != s || p.level() != r.sl) { it.remove(); cleanup(r); continue; }
            if (r.t >= T) { it.remove(); done.add(r); continue; }
            hold(p, r);
            try { r.scene.step(r, r.t); } catch (RuntimeException ex) { com.mojang.logging.LogUtils.getLogger().warn("Ending outro step failed", ex); }
            r.t++;
        }
        for (Run r : done) end(r, r.player());
    }

    /** Giữ người chơi đứng yên tại o, mặt hướng về phía trước (diễn viên). */
    private static void hold(ServerPlayer p, Run r) {
        if (r.faceDirty || p.position().distanceToSqr(r.o) > 0.04) p.connection.teleport(r.o.x, r.o.y, r.o.z, r.face, 0f);
        r.faceDirty = false;
        p.setDeltaMovement(Vec3.ZERO);
    }

    private static void end(Run r, ServerPlayer p) {
        try { r.scene.finish(r); } catch (RuntimeException ex) { com.mojang.logging.LogUtils.getLogger().warn("Ending outro finish failed", ex); }
        cleanup(r);
        if (p != null) PacketDistributor.sendToPlayer(p, new EndgameNet.PlayCutscene(r.ending)); // chỉ còn credits
    }

    private static void cleanup(Run r) {
        for (Entity e : r.actors) if (e.isAlive()) e.discard();
        r.actors.clear();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        Run r = RUNS.remove(e.getEntity().getUUID());
        if (r != null) { try { r.scene.finish(r); } catch (RuntimeException ignored) { } cleanup(r); }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Set<UUID> live = new HashSet<>();
        for (Run r : RUNS.values()) for (Entity a : r.actors) live.add(a.getUUID());
        List<Entity> orphans = new ArrayList<>(p.serverLevel().getEntities((Entity) null, new AABB(p.blockPosition()).inflate(160),
                x -> x.getTags().contains(TAG) && !live.contains(x.getUUID())));
        for (Entity x : orphans) x.discard();
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        for (Run r : RUNS.values()) cleanup(r);
        RUNS.clear();
    }
}
