package com.hukhong.endgame;

import com.hukhong.boss.phase.CamPath;
import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Kết Phong Ấn (PLAN-v1 §8b.3): Cửa Dạ (vết rách dọc) trước mặt -> 4 Mảnh bay ra từ người chơi, xoay quanh rồi lên
 * 4 góc -> nối thành khung Thiên Môn vàng -> Cửa Dạ bị ép khép, bóng mờ bên trong với tay ra rồi bị nhốt lại ->
 * chớp trắng -> người chơi quay lưng nhìn bình minh. Hậu quả: dấu ấn vàng trên mu bàn tay ({@link VoidMark#SEAL}).
 */
final class EndingOutroSeal implements EndingOutro.Scene {
    private static final int GOLD = 0xFFD866, VOIDC = 0xA050FF;
    /** Màu vệt của 4 Mảnh: vàng / xanh / đỏ / trắng. */
    private static final int[] SHARD_RGB = {0xFFD040, 0x60B8FF, 0xFF4050, 0xF4F4FF};
    private static final double GATE_DZ = 10, GATE_H = 12, HALF_W = 2.6;

    private final Display.ItemDisplay[] shards = new Display.ItemDisplay[4];

    @Override
    public List<CamPath.Key> cam() {
        return List.of(
                EndingOutro.k(0, 0, 3f, -8f, 70, 3f, 0),                 // rộng sau lưng: người chơi trước Cửa Dạ
                EndingOutro.k(40, 0, 2.4f, -5.5f, 66, 1.4f, 0),
                EndingOutro.k(80, -3.9f, 2.4f, -3.9f, 64, 1.4f, 0),       // vòng quanh 90°
                EndingOutro.k(118, -5.5f, 2.2f, 0.2f, 62, 1.6f, 0),
                EndingOutro.k(130, 0.6f, 0.5f, 4f, 62, 2f, 0, 180),       // cận khung từ dưới nhìn lên
                EndingOutro.k(198, -0.6f, 0.6f, 4.6f, 66, 2f, 0, 180),
                EndingOutro.k(210, 13f, 3.5f, 5f, 78, 2f, 0, -18),        // ngang, rộng: cổng ép vào
                EndingOutro.k(278, 12f, 3f, 6f, 76, 2f, 0, -22),
                EndingOutro.k(290, 1f, 2.6f, 5.2f, 40, 3f, 0, 180),       // cận khe nứt: khoảnh khắc buồn
                EndingOutro.k(358, 0.4f, 2.8f, 6f, 36, 3f, 0, 180),
                EndingOutro.k(372, 0, 4f, 6f, 70, 1.6f, 0),               // quay lại nhìn người chơi (đã quay lưng)
                EndingOutro.k(470, 0, 14f, 15f, 80, 1.2f, -8),            // cẩu lên cao lùi xa
                EndingOutro.k(520, 0, 20f, 20f, 82, 1f, -10),
                EndingOutro.k(EndingOutro.T, 0, 22f, 22f, 82, 1f, -10));
    }

    private static Vec3 gate(EndingOutro.Run r) { return r.rel(0, 0, GATE_DZ); }

    /** Góc khung i (0 = dưới trái, 1 = dưới phải, 2 = trên phải, 3 = trên trái), squeeze 0..1 ép vào giữa. */
    private static Vec3 corner(EndingOutro.Run r, int i, double squeeze) {
        double w = HALF_W * (1 - 0.8 * squeeze);
        double dx = (i == 0 || i == 3) ? -w : w, dy = (i < 2) ? 0.8 : GATE_H - 0.6;
        return r.rel(dx, dy, GATE_DZ);
    }

    @Override
    public void step(EndingOutro.Run r, int t) {
        ServerLevel sl = r.sl;
        Vec3 g = gate(r);
        Vec3 chest = r.o.add(0, 1.2, 0);
        // ---- 0-40: tiêu đề - Cửa Dạ sừng sững trước mặt, gió hút
        if (t == 0) {
            Fx.rift(sl, null, g, new Vec3(0, GATE_H, 0), 1.8f, VOIDC, 230);
            r.sound(g, SoundEvents.PORTAL_AMBIENT, 2f, 0.5f);
        }
        if (t < 200 && t % 4 == 0)
            sl.sendParticles(ParticleTypes.PORTAL, g.x, g.y + GATE_H / 2, g.z, 14, 1.2, GATE_H / 2.5, 1.2, 0.6);
        // ---- 40-120 (dòng 1): 4 Mảnh bay ra, xoay quanh người chơi rồi lên 4 góc
        if (t == 40) {
            for (int i = 0; i < 4; i++) shards[i] = spawnShard(r, chest);
            r.sound(chest, SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1.2f);
            sl.sendParticles(ParticleTypes.END_ROD, chest.x, chest.y, chest.z, 30, 0.3, 0.3, 0.3, 0.12);
        }
        if (t >= 40 && t < 120) {
            for (int i = 0; i < 4; i++) {
                Display.ItemDisplay s = shards[i];
                if (s == null) continue;
                Vec3 at;
                if (t < 82) { // xoay quanh, bốc lên dần
                    double a = (t - 40) * 0.22 + i * Math.PI / 2, rad = 0.6 + (t - 40) * 0.03;
                    at = chest.add(r.right.scale(Math.cos(a) * rad)).add(r.fwd.scale(Math.sin(a) * rad)).add(0, (t - 40) * 0.025, 0);
                } else { // bay lên góc khung
                    double u = smooth((t - 82) / 37.0);
                    Vec3 from = shardOrbitEnd(r, chest, i);
                    at = from.lerp(corner(r, i, 0), u).add(0, Math.sin(u * Math.PI) * 2.5, 0);
                }
                move(s, at, t * 9f + i * 90);
                if (t % 2 == 0) sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
                if (t == 82) Fx.ribbon(sl, null, at, corner(r, i, 0), 2.5, 36, 0, 0.18f, SHARD_RGB[i]);
            }
            if (t == 82) r.sound(chest, SoundEvents.TRIDENT_RIPTIDE_2.value(), 1.5f, 1.4f);
        }
        // ---- 120-200 (dòng 1): nối 4 góc bằng tia -> khung Thiên Môn vàng
        if (t >= 120 && t < 360 && (t - 120) % 20 == 0) {
            double sq = t < 200 ? 0 : Math.min(1, (t - 200) / 160.0);
            for (int i = 0; i < 4; i++) Fx.ribbon(sl, null, corner(r, i, sq), corner(r, (i + 1) % 4, sq), 0, 22, 0, 0.32f, GOLD);
        }
        if (t == 120) {
            r.sound(g, SoundEvents.BEACON_ACTIVATE, 3f, 0.8f);
            Fx.rune(sl, null, g.add(0, GATE_H + 0.5, 0), 4f, GOLD, 240, 10);
            Fx.groundRing(sl, null, g, 4f, GOLD, 200);
        }
        if (t > 120 && t < 200 && t % 5 == 0)
            for (int i = 0; i < 4; i++) { Vec3 c = corner(r, i, 0); sl.sendParticles(ParticleTypes.WAX_OFF, c.x, c.y, c.z, 3, 0.2, 0.2, 0.2, 0.02); }
        // ---- 200-280 (dòng 2): Cửa Dạ co hẹp, cổng vàng ép vào
        if (t >= 200 && t < 360) {
            double sq = Math.min(1, (t - 200) / 160.0);
            for (int i = 0; i < 4; i++) if (shards[i] != null) move(shards[i], corner(r, i, sq), t * 4f + i * 90);
            if (t % 10 == 0) {
                float half = (float) Math.max(0.12, 1.8 * (1 - sq));
                Fx.rift(sl, null, g.add(0, GATE_H * sq * 0.15, 0), new Vec3(0, GATE_H * (1 - sq * 0.3), 0), half, VOIDC, 14);
            }
        }
        if (t == 200) { Fx.groundCrack(sl, null, g, 6f, GOLD, 10, 160); Fx.shake(sl, r.o, 2f, 30, 60f); r.sound(g, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 2f, 0.6f); }
        if (t > 200 && t < 280 && t % 20 == 0) r.sound(g, SoundEvents.IRON_DOOR_CLOSE, 1.5f, 0.5f);
        // ---- 280-360 (dòng 2): bóng mờ bên trong với tay ra rồi bị khép lại
        if (t >= 280 && t < 360 && t % 3 == 0) {
            double fade = 1 - (t - 280) / 80.0;
            for (int i = -1; i <= 1; i++) {
                Vec3 feet = r.rel(i * 0.8, 0.2 + Math.abs(i) * 0.6, GATE_DZ + 1.2);
                Vec3 reach = r.rel(i * 0.3, 1.5 + Math.abs(i) * 0.5, GATE_DZ - 0.6 + fade * 0.4);
                if (fade > 0.15 || i == 0) EndingOutro.silhouette(sl, feet, reach, ParticleTypes.SOUL);
            }
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, g.x, g.y + GATE_H / 2, g.z, 10, 1.5, GATE_H / 3, 1.5, 0.05);
        }
        if (t == 300 || t == 330) r.sound(g, SoundEvents.SOUL_ESCAPE.value(), 2f, 0.6f);
        // ---- 360: khe khép hẳn - chớp trắng
        if (t == 360) {
            Fx.elem(sl, null, Fx.E_NUKE, g.add(0, GATE_H / 2, 0), Vec3.ZERO, 6f, GOLD, 0);
            Fx.shake(sl, r.o, 3f, 24, 80f);
            r.sound(g, SoundEvents.BEACON_DEACTIVATE, 3f, 0.6f);
            r.sound(g, SoundEvents.GENERIC_EXPLODE.value(), 1f, 1.4f);
            for (int i = 0; i < 4; i++) if (shards[i] != null) {
                Vec3 c = shards[i].position();
                sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 30, 0.3, 0.3, 0.3, 0.2);
                shards[i].discard();
                shards[i] = null;
            }
        }
        // ---- 360-520 (dòng 3): người chơi quay lưng, bình minh đầu tiên sau 300 năm
        if (t == 366) r.face(r.yaw + 180f);
        if (t >= 366 && t < EndingOutro.T) {
            Vec3 dawn = r.rel(0, 6, -70);
            double rise = Math.min(1, (t - 366) / 150.0);
            if (t % 3 == 0) {
                // dải sáng ngang chân trời + "mặt trời" nhô lên
                sl.sendParticles(ParticleTypes.END_ROD, dawn.x, dawn.y + rise * 6, dawn.z, 18, 22, 0.6, 3, 0.0);
                sl.sendParticles(ParticleTypes.FLAME, dawn.x, dawn.y + rise * 10, dawn.z, 6, 2, 2, 1, 0.0);
                sl.sendParticles(ParticleTypes.WAX_OFF, r.o.x, r.o.y + 8, r.o.z, 10, 10, 3, 10, 0.0);
            }
            if (t % 8 == 0) sl.sendParticles(ParticleTypes.FALLING_HONEY, r.o.x, r.o.y + 6, r.o.z, 4, 6, 1, 6, 0.0);
            if (t == 380) { Fx.groundRing(sl, null, r.o, 6f, GOLD, 160); r.sound(r.o, SoundEvents.AMETHYST_BLOCK_CHIME, 2f, 1.2f); }
            if (t == 440) r.sound(r.o, SoundEvents.NOTE_BLOCK_CHIME.value(), 1.5f, 1.0f);
        }
    }

    @Override
    public void finish(EndingOutro.Run r) {
        for (int i = 0; i < 4; i++) shards[i] = null; // discard bởi cleanup()
        ServerPlayer p = r.player();
        if (p != null) VoidMark.update(p, false);
    }

    private static Vec3 shardOrbitEnd(EndingOutro.Run r, Vec3 chest, int i) {
        double a = 42 * 0.22 + i * Math.PI / 2, rad = 0.6 + 42 * 0.03;
        return chest.add(r.right.scale(Math.cos(a) * rad)).add(r.fwd.scale(Math.sin(a) * rad)).add(0, 42 * 0.025, 0);
    }

    private static double smooth(double x) { x = Math.max(0, Math.min(1, x)); return x * x * (3 - 2 * x); }

    private static Display.ItemDisplay spawnShard(EndingOutro.Run r, Vec3 at) {
        Display.ItemDisplay d = EntityType.ITEM_DISPLAY.create(r.sl);
        if (d == null) return null;
        d.getSlot(0).set(new ItemStack(com.hukhong.memory.MemoryRegistry.SHARD.get()));
        d.moveTo(at.x, at.y, at.z, 0f, 0f);
        d.setGlowingTag(true);
        r.actor(d);
        r.sl.addFreshEntity(d);
        return d;
    }

    private static void move(Display.ItemDisplay d, Vec3 at, float spin) {
        if (d == null || !d.isAlive()) return;
        d.moveTo(at.x, at.y, at.z, spin % 360f, 0f);
    }
}
