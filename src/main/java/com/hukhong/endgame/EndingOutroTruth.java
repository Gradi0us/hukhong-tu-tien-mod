package com.hukhong.endgame;

import com.hukhong.boss.phase.CamPath;
import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Kết Siêu Độ (PLAN-v1 §8b.5): vòm Hư Không nứt, rơi thành mưa linh quang -> Dạ Khuyết đứng trong mưa, ngẩng lên,
 * quay về người chơi, cánh trăng mở nhẹ -> làn sóng sáng lan ra xa -> camera quay ngoắt về chân trời: một con mắt đỏ
 * lóe trong mây (Yêu Tộc thức giấc). Hậu quả: vầng sáng vàng nhạt sau lưng ({@link VoidMark#HALO}).
 */
final class EndingOutroTruth implements EndingOutro.Scene {
    private static final int GOLD = 0xFFE9A0, WHITE = 0xF4F8FF, VOIDC = 0xA050FF, MOON = 0x8FA8FF, RED = 0xE01828;
    private DaKhuyet dk;

    @Override
    public List<CamPath.Key> cam() {
        return List.of(
                EndingOutro.k(0, 0, 2.6f, -6f, 68, 2f, 0),
                EndingOutro.k(40, 0.6f, 0.35f, -1.2f, 72, 30f, 0),          // ngửa nhìn trời từ dưới chân
                EndingOutro.k(198, -0.6f, 0.35f, -1.0f, 76, 30f, 0),
                EndingOutro.k(212, 0.4f, 2.3f, 4.6f, 35, 1.9f, 0),          // cận mặt Dạ Khuyết
                EndingOutro.k(280, -0.3f, 2.2f, 4.4f, 35, 1.9f, 0),
                EndingOutro.k(300, 4.5f, 2f, 1.5f, 58, 1.5f, 0),            // hai người cùng khung
                EndingOutro.k(358, 4.2f, 2.2f, 2.5f, 58, 1.5f, 0),
                EndingOutro.k(372, 0, 12f, -10f, 74, 0f, 0),                // cẩu cao: sóng sáng lan ra
                EndingOutro.k(450, 0, 30f, -24f, 82, 0f, 0),
                EndingOutro.k(462, 0, 2f, 3f, 60, 1.8f, 0),                 // quay ngoắt 180°
                EndingOutro.k(474, 0, 1.9f, -1.6f, 30, 1.9f, -10),          // nhìn xa về chân trời, zoom
                EndingOutro.k(520, 0, 1.9f, -1.6f, 28, 1.9f, -10),
                EndingOutro.k(EndingOutro.T, 0, 2.2f, -2.4f, 34, 1.9f, -8));
    }

    @Override
    public void step(EndingOutro.Run r, int t) {
        ServerLevel sl = r.sl;
        Vec3 sky = r.o.add(0, 24, 0);
        Vec3 dkAt = r.rel(0, 0, 3);
        // ---- 0-40: vòm Hư Không tím phủ trên đầu
        if (t == 0) {
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                Vec3 p0 = sky.add(Math.cos(a) * 6, 0, Math.sin(a) * 6);
                Fx.rift(sl, null, p0, new Vec3(Math.cos(a) * 10, -2, Math.sin(a) * 10), 1.2f, VOIDC, 60);
            }
            r.sound(r.o, SoundEvents.PORTAL_AMBIENT, 2f, 0.6f);
        }
        // ---- 40-200 (dòng 1): Hư Không vỡ thành mưa linh quang
        if (t == 40) { Fx.elem(sl, null, Fx.E_NUKE, sky, Vec3.ZERO, 5f, WHITE, 0); r.sound(sky, SoundEvents.GLASS_BREAK, 3f, 0.5f); r.sound(sky, SoundEvents.AMETHYST_CLUSTER_BREAK, 3f, 0.6f); }
        if (t >= 40 && t < 360) {
            if (t % 2 == 0) sl.sendParticles(ParticleTypes.END_ROD, r.o.x, r.o.y + 16, r.o.z, 12, 12, 3, 12, 0.0);
            if (t % 5 == 0) Fx.elem(sl, null, Fx.E_NEEDLES, sky.add(sl.random.nextGaussian() * 6, 0, sl.random.nextGaussian() * 6), new Vec3(0, -1, 0), 1.2f, t % 10 == 0 ? GOLD : WHITE, 30);
            if (t % 7 == 0) sl.sendParticles(ParticleTypes.WAX_OFF, r.o.x, r.o.y + 6, r.o.z, 8, 8, 4, 8, 0.0);
        }
        if (t >= 40 && t < 200 && t % 30 == 0) r.sound(r.o, SoundEvents.AMETHYST_BLOCK_CHIME, 2f, 0.8f + sl.random.nextFloat() * 0.6f);
        // ---- 200-360 (dòng 2): Dạ Khuyết đứng trong mưa, ngẩng lên rồi quay về người chơi, mỉm cười
        if (t == 196) {
            dk = EndingOutroAbsorb.daKhuyet(r, dkAt);
            if (dk != null) {
                face(dk, r.o.add(r.fwd.scale(20)));         // ban đầu nhìn ra xa (lưng về phía người chơi)
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, dkAt.x, dkAt.y + 1.5, dkAt.z, 60, 0.8, 1.2, 0.8, 0.05);
            }
        }
        if (dk != null && dk.isAlive()) {
            if (t == 210) { dk.triggerAnim(DaKhuyet.CINE, "look_up"); dk.setXRot(-30); Fx.elem(sl, null, Fx.E_HALOS, dkAt.add(0, 2.2, 0), Vec3.ZERO, 1.4f, MOON, 140); }
            if (t >= 240 && t <= 270) { // quay dần về người chơi
                Vec3 away = r.o.add(r.fwd.scale(20)), toward = r.o;
                Vec3 look = away.lerp(toward, (t - 240) / 30.0);
                face(dk, look);
                dk.setXRot(t < 260 ? -15 : 6); // đầu hơi nghiêng xuống
            }
            if (t == 272) r.sound(dkAt, SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1.3f);
            if (t == 300) { dk.triggerAnim("skill", "rise"); DaKhuyetMoves.wingsFx(sl, dk, MOON, 60); }
            if (t >= 200 && t < 360 && t % 4 == 0)
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, dkAt.x, dkAt.y + 2, dkAt.z, 3, 1, 1, 1, 0.02);
            if (t == 356) {
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, dkAt.x, dkAt.y + 1.5, dkAt.z, 120, 0.8, 1.5, 0.8, 0.08);
                Fx.ribbon(sl, null, dkAt.add(0, 1.5, 0), dkAt.add(0, 22, 0), 0, 30, 0, 0.35f, MOON);
                dk.discard();
                dk = null;
            }
        }
        // ---- 360-460 (dòng 3): thế gian thở ra - sóng sáng lan từ chỗ đứng ra xa
        if (t >= 360 && t <= 450 && t % 10 == 0) Fx.groundRing(sl, null, r.o, 5f + (t - 360) * 0.6f, GOLD, 50);
        if (t == 360) { r.sound(r.o, SoundEvents.BEACON_POWER_SELECT, 3f, 0.7f); Fx.shake(sl, r.o, 1f, 20, 60f); }
        // ---- 460-520 (dòng 3): phương xa có thứ thức giấc - con mắt đỏ lóe trong mây
        Vec3 eye = r.rel(0, 26, -80);
        if (t == 466) {
            Fx.elem(sl, null, Fx.E_HALOS, eye, Vec3.ZERO, 4f, RED, 90);
            sl.playSound(null, r.o.x, r.o.y, r.o.z, SoundEvents.ENDER_DRAGON_GROWL, net.minecraft.sounds.SoundSource.AMBIENT, 0.25f, 0.5f);
        }
        if (t >= 466 && t < 560 && t % 3 == 0) {
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, eye.x, eye.y, eye.z, 4, 6, 1.5, 3, 0.0);
            sl.sendParticles(net.minecraft.core.particles.DustParticleOptions.REDSTONE, eye.x, eye.y, eye.z, 6, 0.6, 0.3, 0.6, 0.0);
        }
    }

    private static void face(DaKhuyet d, Vec3 at) {
        Vec3 to = at.subtract(d.position());
        float y = (float) (Math.atan2(-to.x, to.z) * 180 / Math.PI);
        d.setYRot(y); d.setYHeadRot(y); d.setYBodyRot(y);
    }

    @Override
    public void finish(EndingOutro.Run r) {
        dk = null;
        ServerPlayer p = r.player();
        if (p != null) VoidMark.update(p, false);
    }
}
