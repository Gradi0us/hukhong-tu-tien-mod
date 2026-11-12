package com.hukhong.endgame;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Cutscene kết sau khi hạ Nguyên Thủy Tịch Diệt (~50s, nhiều cảnh): thân xác sụp đổ -> Hư Không khép lại -> Mộ Dao ->
 * ký ức Dạ Khuyết -> trời hửng sáng -> lời nhắn cuối. Camera neo tĩnh tại chỗ boss ngã; VFX phía server chạy theo nhịp.
 * Chỉ được bỏ qua sau nửa thời lượng (MIN_SKIP, client + server đều kiểm).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class NttdEnding {
    private NttdEnding() {}

    public static final int T = 1000, MIN_SKIP = T / 2;
    private static final int GOLD = 0xFFD866, VOIDC = 0xA050FF, MOON = 0x8FA8FF;

    private static final class Run {
        final ServerLevel sl;
        final Vec3 o, fwd, right;
        final boolean dk;
        int t;
        Run(ServerLevel sl, Vec3 o, float yaw, boolean dk) {
            this.sl = sl; this.o = o; this.dk = dk;
            double yr = Math.toRadians(yaw);
            fwd = new Vec3(-Math.sin(yr), 0, Math.cos(yr));
            right = new Vec3(-Math.cos(yr), 0, -Math.sin(yr));
        }
        Vec3 rel(double dx, double dz) { return o.add(right.scale(dx)).add(fwd.scale(dz)); }
    }

    private static final List<Run> RUNS = new ArrayList<>();

    private static CamPath.Key k(int t, float dx, float dy, float dz, float fov, float lookH, float pitch) {
        return new CamPath.Key(t, dx, dy, dz, 0f, pitch, fov, lookH);
    }

    /** Phát cutscene cho người chơi quanh đó và bắt đầu VFX; trả về thời lượng (tick). */
    public static int start(ServerLevel sl, NguyenThuyTichDiet boss, boolean dkMemory) {
        float yaw = boss.yBodyRot;
        Vec3 o = boss.position();
        float s = (float) Math.max(2.5, boss.getBbHeight() * 0.5);
        List<CamPath.Key> cam = List.of(
                k(0, 0, 0.6f * s, 2.4f * s, 55, 0.7f * s, 0),          // 1. sụp đổ: cận
                k(60, 1.6f * s, 0.4f * s, 2.0f * s, 60, 0.6f * s, -4),
                k(130, -1.8f * s, 0.9f * s, 2.6f * s, 62, 0.5f * s, 0),
                k(210, 0, 10, 16, 78, 3, -6),                             // 2. Hư Không khép: cẩu lên toàn cảnh
                k(300, 7, 18, 12, 82, 0, -10),
                k(360, 4.5f, 2.2f, 9.5f, 60, 1.6f, 0),                     // 3. Mộ Dao
                k(450, 3.6f, 1.8f, 7.8f, 50, 1.6f, 0),
                k(500, 0, 2.0f, 11, 62, 1.6f, 0),                          // cả hai
                k(560, -4.5f, 2.2f, 9.5f, 60, 1.6f, 0),                    // 4. ký ức Dạ Khuyết
                k(650, -3.6f, 1.7f, 7.4f, 50, 1.8f, 0),
                k(720, 0, 2.0f, 9, 60, 1.6f, 0),
                k(800, 0, 8, 16, 72, 2, -4),                               // 5. trời hửng sáng
                k(900, 0, 22, 30, 82, 0, -8),
                k(T, 0, 30, 40, 85, 0, -10));                              // 6. kéo xa
        List<SubLine> subs = new ArrayList<>();
        subs.add(new SubLine(4, 60, "hukhong.ending.nttd.1"));
        subs.add(new SubLine(60, 130, "hukhong.ending.nttd.2"));
        subs.add(new SubLine(130, 210, "hukhong.ending.nttd.3"));
        subs.add(new SubLine(210, 290, "hukhong.ending.nttd.4"));
        subs.add(new SubLine(290, 360, "hukhong.ending.nttd.5"));
        subs.add(new SubLine(360, 430, "hukhong.ending.mo.1"));
        subs.add(new SubLine(430, 500, "hukhong.ending.mo.2"));
        String m = dkMemory ? "hukhong.ending.dk." : "hukhong.ending.nodk.";
        subs.add(new SubLine(500, 570, m + "1"));
        subs.add(new SubLine(570, 650, m + "2"));
        subs.add(new SubLine(650, 720, m + "3"));
        subs.add(new SubLine(720, 800, "hukhong.ending.nttd.6"));
        subs.add(new SubLine(800, 880, "hukhong.ending.nttd.7"));
        subs.add(new SubLine(880, T - 4, "hukhong.ending.nttd.8"));
        BossPhaseDirector.sendCutscene(sl, null, o, yaw, T, cam, subs, MIN_SKIP);
        RUNS.add(new Run(sl, o, yaw, dkMemory));
        return T;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (RUNS.isEmpty()) return;
        var it = RUNS.iterator();
        while (it.hasNext()) {
            Run r = it.next();
            if (r.t > T + 10 || r.sl.getServer() != e.getServer()) { it.remove(); continue; }
            step(r, r.t++);
        }
    }

    private static void step(Run r, int t) {
        ServerLevel sl = r.sl;
        Vec3 o = r.o;
        // 1. thân xác sụp đổ
        if (t == 0) { Fx.groundCrack(sl, null, o, 14f, VOIDC, 16, 140); Fx.shake(sl, o, 3f, 30, 80f); }
        if (t < 130 && t % 8 == 0) {
            sl.sendParticles(ModParticles.HU_KHONG.get(), o.x, o.y + 3, o.z, 40, 3, 3, 3, 0.15);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, o.x, o.y + 2, o.z, 30, 2, 3, 2, 0.4);
        }
        if (t == 40 || t == 90) { Fx.elem(sl, null, Fx.E_NUKE, o, Vec3.ZERO, 5f, VOIDC, 0); sound(sl, o, SoundEvents.GENERIC_EXPLODE.value(), 0.5f); }
        if (t == 120) { sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, o.x, o.y + 2, o.z, 3, 2, 1, 2, 0); Fx.shake(sl, o, 4f, 20, 80f); }
        // 2. Hư Không khép lại: vòng vàng co dần, ấn phong trên trời
        if (t == 140) { Fx.rune(sl, null, o.add(0, 25, 0), 20f, GOLD, 320, 9); sound(sl, o, SoundEvents.BEACON_ACTIVATE, 0.6f); }
        if (t >= 150 && t <= 300 && t % 15 == 0) Fx.groundRing(sl, null, o, (float) (30 - (t - 150) * 0.17), GOLD, 20);
        if (t > 140 && t < 320 && t % 6 == 0) sl.sendParticles(ParticleTypes.END_ROD, o.x, o.y + 1, o.z, 20, 10, 8, 10, 0.05);
        if (t == 300) { Fx.elem(sl, null, Fx.E_NUKE, o, Vec3.ZERO, 6f, GOLD, 0); Fx.shake(sl, o, 2f, 20, 80f); sound(sl, o, SoundEvents.BEACON_POWER_SELECT, 0.5f); }
        // 3. Mộ Dao
        if (t == 320) NttdAllies.spawnMo(sl, ground(sl, r.rel(2.5, 5)), r.rel(-2.5, 5));
        // 4. ký ức Dạ Khuyết: bóng trăng khuyết từ cánh hoa
        Vec3 mem = ground(sl, r.rel(-2.5, 5));
        if (r.dk && t >= 500 && t < 700 && t % 4 == 0) {
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, mem.x, mem.y + 1.5, mem.z, 8, 0.5, 1.0, 0.5, 0.02);
            sl.sendParticles(ParticleTypes.END_ROD, mem.x, mem.y + 1.0, mem.z, 4, 0.3, 0.9, 0.3, 0.01);
        }
        if (r.dk && t == 520) {
            Fx.groundRing(sl, null, mem, 2.5f, MOON, 180);
            Fx.elem(sl, null, Fx.E_HALOS, mem.add(0, 1.5, 0), Vec3.ZERO, 1.6f, MOON, 160);
            for (int side = -1; side <= 1; side += 2)
                Fx.ribbon(sl, null, mem.add(0, 1.4, 0), mem.add(r.right.scale(side * 3)).add(0, 2.5, 0), 1.5, 14, 0, 0.3f, MOON);
            sound(sl, mem, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f);
        }
        if (r.dk && t == 700) {
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, mem.x, mem.y + 1.5, mem.z, 160, 1, 1.5, 1, 0.1);
            Fx.ribbon(sl, null, mem.add(0, 1.5, 0), mem.add(0, 20, 0), 0, 30, 0, 0.4f, MOON);
        }
        if (!r.dk && t >= 500 && t < 720 && t % 10 == 0) sl.sendParticles(ParticleTypes.SOUL, mem.x, mem.y + 0.5, mem.z, 3, 3, 0.2, 3, 0.02);
        // 5. trời hửng sáng
        if (t >= 720 && t < 980 && t % 5 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD, o.x, o.y + 18, o.z, 30, 24, 4, 24, 0.02);
            sl.sendParticles(ParticleTypes.WAX_OFF, o.x, o.y + 6, o.z, 20, 20, 6, 20, 0.02);
        }
        if (t == 760) { Fx.groundRing(sl, null, o, 34f, GOLD, 120); sound(sl, o, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f); }
        // kết: Mộ Dao rời đi
        if (t == T + 10) NttdAllies.dismissMo(sl, o);
    }

    private static Vec3 ground(ServerLevel sl, Vec3 v) {
        var g = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.core.BlockPos.containing(v));
        return new Vec3(v.x, Math.min(g.getY(), v.y + 4), v.z);
    }

    private static void sound(ServerLevel sl, Vec3 at, net.minecraft.sounds.SoundEvent ev, float pitch) {
        sl.playSound(null, at.x, at.y, at.z, ev, SoundSource.HOSTILE, 4f, pitch);
    }
}
