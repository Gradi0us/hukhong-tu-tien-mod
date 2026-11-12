package com.hukhong.endgame;

import com.hukhong.boss.phase.CamPath;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết Hấp Thụ (PLAN-v1 §8b.4): Hư Không chảy ngược vào ngực người chơi (êm, không nổ) -> Cửa Dạ mở trong lồng ngực,
 * dấu "Nửa Hư Không" hiện dần -> bóng những người quen (Mộ Dao, Dạ Khuyết, Hứa Cô) lần lượt quay lưng và tan ->
 * camera cẩu thẳng lên, người chơi thành chấm nhỏ giữa vùng tối, tắt hết hạt, chỉ còn vết nứt tím phát sáng.
 */
final class EndingOutroAbsorb implements EndingOutro.Scene {
    private static final int VOIDC = 0xA050FF, DEEP = 0x5A1E9A;
    private static final double GATE_DZ = 9;

    private final List<Mob> ghosts = new ArrayList<>();

    @Override
    public List<CamPath.Key> cam() {
        return List.of(
                EndingOutro.k(0, 0, 2.6f, -6.5f, 66, 2f, 0),               // sau lưng, Cửa Dạ phía trước
                EndingOutro.k(40, 0, 2f, -6f, 60, 1.3f, 0),
                EndingOutro.k(90, -5.2f, 2f, -3f, 60, 1.3f, 0),             // vòng 180° chậm
                EndingOutro.k(140, -5.2f, 2f, 3f, 60, 1.3f, 0),
                EndingOutro.k(198, 0, 2f, 6f, 60, 1.3f, 0),
                EndingOutro.k(212, 0.2f, 1.35f, 1.7f, 35, 1.2f, 0),         // cận ngực
                EndingOutro.k(278, -0.2f, 1.4f, 1.5f, 33, 1.25f, 0),
                EndingOutro.k(290, 6f, 1.8f, 2f, 55, 1.4f, 0),              // lướt ngang qua từng bóng
                EndingOutro.k(320, 2f, 1.8f, 6.2f, 55, 1.4f, 0),
                EndingOutro.k(350, -6f, 1.8f, 2f, 55, 1.4f, 0),
                EndingOutro.k(362, -0.02f, 8f, 0.02f, 30, 0f, 0),           // cẩu thẳng lên
                EndingOutro.k(470, -0.02f, 60f, 0.02f, 60, 0f, 0),
                EndingOutro.k(520, -0.02f, 64f, 0.02f, 80, 0f, 0),
                EndingOutro.k(EndingOutro.T, -0.02f, 66f, 0.02f, 80, 0f, 0));
    }

    @Override
    public void step(EndingOutro.Run r, int t) {
        ServerLevel sl = r.sl;
        Vec3 g = r.rel(0, 0, GATE_DZ);
        Vec3 chest = r.o.add(0, 1.25, 0);
        // ---- 0-40: Cửa Dạ trước mặt
        if (t == 0) { Fx.rift(sl, null, g, new Vec3(0, 10, 0), 1.6f, VOIDC, 210); r.sound(g, SoundEvents.PORTAL_AMBIENT, 2f, 0.4f); }
        // ---- 40-200 (dòng 1): Hư Không chảy ngược vào người chơi - chậm, êm, chỉ có âm trầm
        if (t >= 40 && t < 200) {
            if (t % 3 == 0) {
                double a = r.sl.random.nextDouble() * Math.PI * 2, el = (r.sl.random.nextDouble() - 0.3) * 1.2, d = 9 + r.sl.random.nextDouble() * 7;
                Vec3 from = chest.add(Math.cos(a) * Math.cos(el) * d, Math.sin(el) * d, Math.sin(a) * Math.cos(el) * d);
                Fx.ribbonWave(sl, null, from, chest, 1.5, 0.8, 34, 0, 0.12f, t % 2 == 0 ? VOIDC : DEEP);
            }
            if (t % 4 == 0) {
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, chest.x, chest.y, chest.z, 16, 2.5, 1.5, 2.5, 0.25);
                sl.sendParticles(ModParticles.HU_KHONG.get(), g.x, g.y + 5, g.z, 8, 1.2, 4, 1.2, 0.02);
            }
            if (t % 20 == 0) { // Cửa Dạ co lại dần
                float half = (float) (1.6 * (1 - (t - 40) / 170.0));
                Fx.rift(sl, null, g.add(0, (t - 40) * 0.02, 0), new Vec3(0, 10 * (1 - (t - 40) / 200.0), 0), Math.max(0.1f, half), VOIDC, 24);
            }
            if (t % 40 == 0) r.sound(chest, SoundEvents.BEACON_AMBIENT, 1.5f, 0.5f);
        }
        if (t == 40) r.sound(chest, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), 2f, 0.6f);
        // ---- 200-280 (dòng 2): Cửa Dạ mở trong lồng ngực; dấu Nửa Hư Không hiện dần
        if (t == 200) {
            ServerPlayer p = r.player();
            if (p != null) { Fx.shell(sl, null, p, 0.9f, VOIDC, 80); VoidMark.update(p, true); }
            r.sound(chest, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.5f, 0.5f);
        }
        if (t >= 200 && t < 360 && t % 12 == 0)
            Fx.rift(sl, null, chest.add(r.fwd.scale(0.32)).add(0, -0.3, 0), new Vec3(0, 0.6, 0), 0.12f, VOIDC, 14);
        if (t >= 200 && t < 280 && t % 3 == 0)
            sl.sendParticles(ParticleTypes.WITCH, chest.x, chest.y, chest.z, 2, 0.25, 0.3, 0.25, 0.0);
        // ---- 280-360 (dòng 2): bóng những người quen đứng vòng quanh, lần lượt quay lưng rồi tan
        if (t == 280) spawnGhosts(r);
        for (int i = 0; i < ghosts.size(); i++) {
            Mob m = ghosts.get(i);
            if (m == null || !m.isAlive()) continue;
            int turn = 296 + i * 22, vanish = turn + 14;
            if (t % 4 == 0) sl.sendParticles(ParticleTypes.END_ROD, m.getX(), m.getY() + 1, m.getZ(), 1, 0.25, 0.6, 0.25, 0.0);
            if (t == turn) { // quay lưng lại với người chơi
                Vec3 away = m.position().subtract(r.o);
                float y = (float) (Math.atan2(-away.x, away.z) * 180 / Math.PI);
                m.setYRot(y); m.setYHeadRot(y); m.setYBodyRot(y);
            }
            if (t == vanish) {
                sl.sendParticles(ParticleTypes.END_ROD, m.getX(), m.getY() + 1, m.getZ(), 30, 0.3, 0.7, 0.3, 0.03);
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, m.getX(), m.getY() + 1, m.getZ(), 24, 0.3, 0.7, 0.3, 0.01);
                r.sound(m.position(), SoundEvents.SOUL_ESCAPE.value(), 1.5f, 0.8f);
                m.discard();
            }
        }
        // ---- 360-520 (dòng 3): "Ngươi là tất cả. Và chỉ có một mình." - im lặng, chỉ còn vết nứt tím phát sáng
        if (t >= 360 && t % 10 == 0)
            sl.sendParticles(ParticleTypes.WITCH, chest.x, chest.y, chest.z, 1, 0.05, 0.05, 0.05, 0.0);
        if (t == 362) {
            ServerPlayer p = r.player();
            if (p != null) p.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(null, net.minecraft.sounds.SoundSource.MUSIC));
        }
    }

    private void spawnGhosts(EndingOutro.Run r) {
        ServerLevel sl = r.sl;
        String[] ids = {"mo_dao", "hua_co"};
        Vec3[] spots = {r.rel(3.2, 0, 1.5), r.rel(0, 0, 4), r.rel(-3.2, 0, 1.5)};
        // Mộ Dao (phải), Dạ Khuyết (trước), Hứa Cô (trái)
        Mob mo = npc(r, ids[0], spots[0]);
        Mob dk = daKhuyet(r, spots[1]);
        Mob hc = npc(r, ids[1], spots[2]);
        for (Mob m : new Mob[]{mo, dk, hc}) {
            ghosts.add(m);
            if (m == null) continue;
            Vec3 to = r.o.subtract(m.position());
            float y = (float) (Math.atan2(-to.x, to.z) * 180 / Math.PI);
            m.setYRot(y); m.setYHeadRot(y); m.setYBodyRot(y);
            m.setGlowingTag(true);
            sl.sendParticles(ParticleTypes.SOUL, m.getX(), m.getY() + 1, m.getZ(), 12, 0.3, 0.7, 0.3, 0.01);
        }
    }

    private static Mob npc(EndingOutro.Run r, String id, Vec3 at) {
        Mob m = com.hukhong.story.flow.RequiredNpcs.create(r.sl, id, BlockPos.containing(at));
        if (m == null) return null;
        r.actor(m);
        m.moveTo(at.x, at.y, at.z, 0f, 0f);
        return m;
    }

    /** Dạ Khuyết "ảnh" (phân thân, không thanh boss/nhạc), noAi + bất tử. Dùng chung cho Siêu Độ / NttdEnding. */
    static DaKhuyet daKhuyet(EndingOutro.Run r, Vec3 at) { return daKhuyet(r.sl, at, r::actor); }

    static DaKhuyet daKhuyet(ServerLevel sl, Vec3 at, java.util.function.UnaryOperator<net.minecraft.world.entity.Entity> reg) {
        DaKhuyet d = EndgameRegistry.DA_KHUYET.get().create(sl);
        if (d == null) return null;
        d.moveTo(at.x, at.y, at.z, 0f, 0f);
        d.finalizeSpawn(sl, sl.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
        d.markAvatar(); // trước addFreshEntity: không gắn nhạc boss / đấu trường
        reg.apply(d);
        sl.addFreshEntity(d);
        return d;
    }

    @Override
    public void finish(EndingOutro.Run r) {
        ghosts.clear();
        ServerPlayer p = r.player();
        if (p != null) VoidMark.update(p, false);
    }
}
