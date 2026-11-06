package com.hukhong.memory;

import com.hukhong.auction.Tx;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.memory.MemoryScene.Scene;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Nội dung 8 cảnh hồi ức + cảnh kết. Mỗi cảnh: đường máy quay (K = keyframe theo góc quanh người chơi),
 * phụ đề từng dòng, và dòng thời gian ảo ảnh (Fx.* + hạt + âm vọng) bám theo tick.
 * Ghi chú: góc 180° = máy quay đứng sau lưng người chơi, nhìn về phía trước (nơi các ảo ảnh hiện ra).
 */
final class MemoryVisions {
    private MemoryVisions() {}

    private static final int MOON = 0xC9D6FF, VOID = 0x7A2AB0, VOID_DEEP = 0x4A0F7A, BLOOD = Fx.RED, SOUL = 0x9FD8FF;

    /** Keyframe máy quay: t tick, r bán kính, deg góc quanh người (180 = sau lưng), dy độ cao, fov, lookH độ cao điểm nhìn. */
    static CamPath.Key K(int t, double r, double deg, double dy, double fov, double lookH) {
        double a = Math.toRadians(deg);
        return new CamPath.Key(t, (float) (r * Math.sin(a)), (float) dy, (float) (r * Math.cos(a)), 0f, 0f, (float) fov, (float) lookH);
    }

    static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), size);
    }

    /** Âm vọng: cùng một âm lặp n lần, mỗi lần nhỏ hơn và trầm hơn. */
    static void echo(Scene s, int t, SoundEvent snd, float vol, float pitch, int n, int gap) {
        for (int k = 0; k < n; k++) {
            float v = vol * (float) Math.pow(0.7, k), pt = pitch - 0.07f * k;
            s.beat(t + gap * k, sc -> sc.lv.playSound(null, BlockPos.containing(sc.c), snd, SoundSource.AMBIENT, v, pt));
        }
    }

    /** Vầng trăng khuyết: cung ngoài + cung bóng bên trong, vẽ trên mặt phẳng đứng hướng về người chơi. */
    static void crescent(Scene s, Vec3 center, double r, int rgb, float size) {
        for (int i = 0; i < 44; i++) {
            double th = i * Math.PI * 2 / 44;
            double ox = r * Math.cos(th), oy = r * Math.sin(th);
            if ((ox - 0.45 * r) * (ox - 0.45 * r) + oy * oy >= 0.81 * r * r) emit(s, center, ox, oy, rgb, size);
            double ix = 0.45 * r + 0.9 * r * Math.cos(th), iy = 0.9 * r * Math.sin(th);
            if (ix * ix + iy * iy <= r * r) emit(s, center, ix, iy, rgb, size);
        }
    }

    private static void emit(Scene s, Vec3 center, double x, double y, int rgb, float size) {
        Vec3 q = center.add(s.right.scale(x)).add(0, y, 0);
        s.lv.sendParticles(dust(rgb, size), q.x, q.y, q.z, 1, 0, 0, 0, 0);
    }

    /** Khung cổng: hai trụ + vòm, vẽ bằng hạt vàng. base = giữa cổng dưới đất. */
    static void gateFrame(Scene s, Vec3 base, double w, double h, int rgb) {
        for (double y = 0; y <= h; y += 1.5) {
            emit(s, base, -w, y, rgb, 1.6f);
            emit(s, base, w, y, rgb, 1.6f);
        }
        for (double th = 0; th <= Math.PI; th += 0.16) emit(s, base, w * Math.cos(th), h + 0.35 * w * Math.sin(th), rgb, 1.6f);
    }

    /** Ảo ảnh bóng người (Fx DEMON_SILHOUETTE) đứng tại pos, màu rgb. */
    static void phantom(Scene s, Vec3 pos, float scale, int rgb, int life) {
        Fx.play(s.lv, FxType.DEMON_SILHOUETTE, pos, Vec3.ZERO, scale, rgb, life);
    }

    /** Một kiếm rơi thẳng từ trên trời xuống điểm tgt (kiếm ảo). */
    static void swordFall(Scene s, Vec3 tgt, int delay, float size) {
        Fx.swordStab(s.lv, tgt.add(0, 26, 0), tgt, 12, delay, size);
    }

    static void memory(Scene s, int id) {
        MemoryLore.Memory m = MemoryLore.byId(id);
        for (int k = 0; k < m.lines().size(); k++) s.sub(30 + 52 * k, 80 + 52 * k, m.lines().get(k).key());
        s.endCard = () -> endCard(s, m);
        switch (id) {
            case 1 -> gate(s);
            case 2 -> moon(s);
            case 3 -> whisper(s);
            case 4 -> riftNight(s);
            case 5 -> battlefield(s);
            case 6 -> swordMound(s);
            case 7 -> executors(s);
            case 8 -> sealed(s);
            default -> { }
        }
    }

    /** Lời kết một ký ức: chữ lớn "Ký Ức I/VIII", phụ đề tên, rồi các dòng văn vào khung chat. */
    private static void endCard(Scene s, MemoryLore.Memory m) {
        ServerPlayer p = s.p;
        String roman = MemoryLore.roman(m.id());
        MemoryScene.title(p,
                Tx.t("hukhong.memory.title", "Ký Ức %s/VIII", "Memory %s/VIII", roman).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                m.name().tr().withStyle(ChatFormatting.GRAY));
        p.sendSystemMessage(Tx.t("hukhong.memory.chat.head", "— Ký Ức %s: %s —", "— Memory %s: %s —", roman, m.name().tr())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        for (MemoryLore.L line : m.lines()) p.sendSystemMessage(line.tr().withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
    }

    // ---------------------------------------------------------------- 1. Thiên Môn còn nguyên
    private static void gate(Scene s) {
        s.cam(K(0, 6, 180, 2.5, 70, 2), K(150, 9, 196, 5, 62, 16), K(300, 12, 170, 10, 58, 24));
        Vec3 g = s.pos(0, 0, 16);
        s.beat(0, sc -> Fx.play(sc.lv, FxType.LIGHT_PILLAR, sc.c, Vec3.ZERO, 2f, Fx.GOLD, 280));
        s.beat(0, sc -> Fx.play(sc.lv, FxType.SHOCKWAVE, sc.c.add(0, 0.1, 0), Vec3.ZERO, 3f, Fx.GOLD, 16));
        s.every(0, 290, 3, sc -> gateFrame(sc, g, 7, 24, Fx.GOLD));
        s.beat(12, sc -> Fx.rune(sc.lv, sc.p, sc.pos(0, 40, 16), 12f, Fx.GOLD, 240, 12));
        s.every(20, 240, 4, sc -> { // chư thần: cột sáng dựng dọc hai trụ cổng
            for (double side : new double[]{-7, 7}) {
                Vec3 b = g.add(sc.right.scale(side));
                sc.lv.sendParticles(ParticleTypes.END_ROD, b.x, b.y + 1 + sc.lv.random.nextDouble() * 22, b.z, 2, 0.3, 0.5, 0.3, 0.02);
            }
        });
        s.every(0, 280, 2, sc -> { // thiên quang rơi xuống qua khung cổng
            double x = (sc.lv.random.nextDouble() - 0.5) * 16, y = 6 + sc.lv.random.nextDouble() * 28;
            Vec3 q = g.add(sc.right.scale(x)).add(0, y, 0);
            sc.lv.sendParticles(dust(Fx.GOLD, 1.2f), q.x, q.y, q.z, 1, 0, -0.08, 0, 0.02);
        });
        echo(s, 10, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 0.9f, 3, 40);
        s.beat(140, sc -> sc.lv.playSound(null, BlockPos.containing(sc.c), SoundEvents.BEACON_AMBIENT, SoundSource.AMBIENT, 1.2f, 0.7f));
    }

    // ---------------------------------------------------------------- 2. Dạ Khuyết, thần trăng khuyết
    private static void moon(Scene s) {
        s.cam(K(0, 4, 200, 1.6, 75, 3), K(150, 7, 165, 4, 62, 20), K(300, 6, 210, 2, 66, 6));
        Vec3 moon = s.pos(0, 46, 26);
        s.every(0, 290, 3, sc -> crescent(sc, moon, 7, MOON, 1.5f));
        s.beat(0, sc -> Fx.rune(sc.lv, sc.p, sc.pos(0, 0.05, 0), 6f, MOON, 240, 8));
        s.beat(10, sc -> phantom(sc, sc.pos(0, 0, 12), 2.2f, MOON, 240));
        s.every(20, 240, 4, sc -> sc.lv.sendParticles(ParticleTypes.ENCHANT, sc.c.x, sc.c.y + 2.6, sc.c.z, 4, 1.2, 0.8, 1.2, 0.4));
        s.beat(150, sc -> Fx.ribbon(sc.lv, sc.p, sc.pos(0, 1.5, 12), moon, 3, 80, 0, 0.22f, MOON)); // khát vọng vươn tới trăng
        s.beat(160, sc -> Fx.groundRing(sc.lv, sc.p, sc.pos(0, 0.05, 0), 8f, MOON, 100));
        echo(s, 20, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.7f, 3, 45);
    }

    // ---------------------------------------------------------------- 3. Lời thì thầm
    private static void whisper(Scene s) {
        s.cam(K(0, 3, 0, 1.8, 72, 1.5), K(100, 4, 90, 2, 66, 1.5), K(200, 4, 180, 1.8, 66, 1.2), K(300, 5, 270, 2, 60, 1.6));
        s.every(0, 280, 2, sc -> { // xoáy portal thu hẹp quanh người, hồn bay lên
            double a = sc.tick * 0.22, r = 4.2 - sc.tick / 110.0;
            Vec3 q = sc.pos(Math.cos(a) * r, 1 + (sc.tick % 60) / 30.0, Math.sin(a) * r);
            sc.lv.sendParticles(ParticleTypes.PORTAL, q.x, q.y, q.z, 2, 0.05, 0.05, 0.05, 0.4);
            sc.lv.sendParticles(ParticleTypes.SOUL, q.x, q.y, q.z, 1, 0, 0.1, 0, 0.01);
        });
        s.beat(20, sc -> Fx.ribbonHelix(sc.lv, sc.p, sc.pos(0, 1, 18), sc.pos(0, 1, 2), 1.5, 0, 60, 0, 0.2f, Fx.PURPLE));
        s.beat(120, sc -> Fx.ribbonHelix(sc.lv, sc.p, sc.pos(-16, 1, 6), sc.pos(0, 1, 1), 1.2, 2.1, 60, 0, 0.18f, Fx.PURPLE));
        s.beat(200, sc -> Fx.ribbonHelix(sc.lv, sc.p, sc.pos(14, 1, 9), sc.pos(0, 1, 1), 1.2, 4.2, 60, 0, 0.18f, Fx.PURPLE));
        s.beat(90, sc -> Fx.groundRing(sc.lv, sc.p, sc.pos(0, 0.05, 0), 5f, Fx.PURPLE, 200));
        echo(s, 8, SoundEvents.AMETHYST_BLOCK_CHIME, 0.55f, 0.5f, 6, 28);
        echo(s, 150, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.9f, 0.55f, 3, 40);
    }

    // ---------------------------------------------------------------- 4. Đêm trăng khuyết
    private static void riftNight(Scene s) {
        s.cam(K(0, 6, 180, 2, 70, 3), K(150, 9, 200, 4, 62, 14), K(300, 7, 160, 3, 60, 12));
        s.beat(4, sc -> Fx.groundCrack(sc.lv, sc.p, sc.pos(0, 0, 6), 6f, Fx.PURPLE, 6, 150));
        s.beat(20, sc -> Fx.rift(sc.lv, sc.p, sc.pos(-6, 24, 20), sc.right.scale(12), 5f, Fx.PURPLE, 240));
        s.beat(40, sc -> phantom(sc, sc.pos(0, 0, 14), 2.6f, VOID, 200));
        s.every(60, 240, 2, sc -> { // Hư Không đổ từ khe xuống mặt đất
            double x = (sc.lv.random.nextDouble() - 0.5) * 12;
            Vec3 top = sc.pos(x, 24, 20), bot = sc.pos(x * 0.9, 0.2, 10 + sc.lv.random.nextDouble() * 4);
            for (int k = 0; k < 4; k++) {
                Vec3 q = top.lerp(bot, sc.lv.random.nextDouble());
                sc.lv.sendParticles(ParticleTypes.PORTAL, q.x, q.y, q.z, 2, 0.15, 0.1, 0.15, 0.3);
                sc.lv.sendParticles(dust(VOID_DEEP, 1.4f), q.x, q.y, q.z, 1, 0.1, 0.1, 0.1, 0);
            }
        });
        s.beat(150, sc -> {
            Fx.play(sc.lv, FxType.SHOCKWAVE, sc.pos(0, 0.1, 6), Vec3.ZERO, 5f, Fx.PURPLE, 18);
            Fx.shake(sc.lv, sc.pos(0, 0, 6), 0.6f, 14, 16f);
        });
        echo(s, 12, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2f, 0.5f, 3, 35);
    }

    // ---------------------------------------------------------------- 5. Cổ chiến trường
    private static void battlefield(Scene s) {
        s.cam(K(0, 12, 150, 6, 80, 2), K(150, 10, 190, 3, 68, 3), K(300, 14, 220, 5, 64, 4));
        s.beat(0, sc -> {
            Fx.pool(sc.lv, sc.p, sc.pos(-6, 0, 8), 5f, BLOOD, 260);
            Fx.pool(sc.lv, sc.p, sc.pos(7, 0, 10), 5f, BLOOD, 260);
            Fx.pool(sc.lv, sc.p, sc.pos(0, 0, 14), 5f, BLOOD, 260);
            Fx.pool(sc.lv, sc.p, sc.pos(-10, 0, 5), 4f, BLOOD, 260);
        });
        s.every(20, 240, 3, sc -> { // lửa hồn vòng quanh chiến địa
            for (int k = 0; k < 10; k++) {
                double a = k * Math.PI / 5 + sc.tick * 0.05;
                Vec3 q = sc.pos(Math.cos(a) * 9, 0.2, Math.sin(a) * 9);
                sc.lv.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, q.x, q.y, q.z, 1, 0, 0.25, 0, 0.02);
            }
        });
        for (int k = 0; k < 8; k++) { // kiếm ma rơi xuống vòng đất chiến
            double a = Math.toRadians(k * 45);
            Vec3 tgt = s.pos(Math.sin(a) * 7, 0, 10 + Math.cos(a) * 5);
            s.beat(40 + k * 18, sc -> swordFall(sc, tgt, 0, 1.1f));
        }
        s.beat(120, sc -> phantom(sc, sc.pos(-4, 0, 12), 1.2f, SOUL, 160));
        s.beat(150, sc -> phantom(sc, sc.pos(5, 0, 14), 1.2f, SOUL, 160));
        s.beat(180, sc -> phantom(sc, sc.pos(0, 0, 18), 1.2f, SOUL, 160));
        s.beat(60, sc -> Fx.shake(sc.lv, sc.c, 0.3f, 8, 12f));
        echo(s, 30, SoundEvents.WITHER_AMBIENT, 0.8f, 0.5f, 3, 40);
    }

    // ---------------------------------------------------------------- 6. Kiếm Trủng
    private static void swordMound(Scene s) {
        s.cam(K(0, 9, 180, 2, 72, 4), K(40, 6, 150, 3, 64, 10), K(120, 6, 210, 4, 60, 8), K(300, 10, 190, 3, 62, 6));
        for (int k = 0; k < 9; k++) { // rừng kiếm cắm quanh gò
            double a = Math.toRadians(k * 40);
            Vec3 tgt = s.pos(Math.sin(a) * 12, 0, 8 + Math.cos(a) * 12);
            s.beat(10 + k * 6, sc -> Fx.swordStab(sc.lv, tgt.add(0, 20, 0), tgt, 10, 0, 1.0f));
        }
        Vec3 tip = s.pos(0, 0, 8);
        s.beat(150, sc -> Fx.giantSword(sc.lv, sc.p, tip, 2.5f, Fx.CYAN, 30, 80)); // cự kiếm giáng xuống, chạm đất sau 30 tick
        s.beat(180, sc -> {
            Fx.play(sc.lv, FxType.SHOCKWAVE, tip.add(0, 0.1, 0), Vec3.ZERO, 6f, Fx.CYAN, 16);
            Fx.groundCrack(sc.lv, sc.p, tip, 8f, Fx.CYAN, 8, 100);
            Fx.play(sc.lv, FxType.LIGHT_PILLAR, tip, Vec3.ZERO, 3f, Fx.GOLD, 90);
            Fx.rune(sc.lv, sc.p, tip.add(0, 0.05, 0), 7f, Fx.CYAN, 100, 6);
            Fx.shake(sc.lv, tip, 1.0f, 20, 30f);
            sc.lv.playSound(null, BlockPos.containing(tip), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5f, 0.6f);
        });
        s.every(190, 280, 6, sc -> Fx.groundRing(sc.lv, sc.p, tip, 4f + (sc.tick - 190) * 0.05f, Fx.CYAN, 40));
        echo(s, 185, SoundEvents.BEACON_ACTIVATE, 1.2f, 0.6f, 3, 40);
    }

    // ---------------------------------------------------------------- 7. Bốn Chấp Hành Giả
    private static void executors(Scene s) {
        s.cam(K(0, 14, 180, 5, 78, 3), K(150, 12, 120, 6, 70, 3), K(300, 14, 200, 6, 66, 3));
        Vec3 core = s.pos(0, 0, 10);
        Vec3[] ex = {s.pos(0, 0, 22), s.pos(14, 0, 10), s.pos(0, 0, -2), s.pos(-14, 0, 10)};
        int[] col = {Fx.GOLD, Fx.CYAN, Fx.PURPLE, Fx.ORANGE};
        for (int i = 0; i < 4; i++) {
            Vec3 e = ex[i];
            int c = col[i];
            s.beat(20 + i * 8, sc -> Fx.play(sc.lv, FxType.TALISMAN_CIRCLE, e.add(0, 0.05, 0), Vec3.ZERO, 1.3f, c, 200));
            s.beat(60 + i * 20, sc -> Fx.ribbon(sc.lv, sc.p, e.add(0, 1.5, 0), core.add(0, 1.5, 0), 1.0, 60, 0, 0.22f, c));
            s.every(20, 220, 6, sc -> sc.lv.sendParticles(ParticleTypes.END_ROD, e.x, e.y + 1 + sc.lv.random.nextDouble() * 2.5, e.z, 2, 0.4, 0.5, 0.4, 0.02));
        }
        s.beat(140, sc -> {
            Fx.play(sc.lv, FxType.LIGHT_PILLAR, core, Vec3.ZERO, 2.2f, Fx.WHITE, 90);
            Fx.play(sc.lv, FxType.SHOCKWAVE, core.add(0, 0.1, 0), Vec3.ZERO, 4f, Fx.WHITE, 16);
            sc.lv.sendParticles(ParticleTypes.ENCHANT, core.x, core.y + 1, core.z, 80, 1.2, 1, 1.2, 1.0);
        });
        echo(s, 20, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.2f, 4, 40);
    }

    // ---------------------------------------------------------------- 8. Nửa thân phong ấn
    private static void sealed(Scene s) {
        s.cam(K(0, 8, 180, 3, 70, 4), K(150, 7, 160, 3, 64, 8), K(300, 10, 200, 5, 66, 6));
        Vec3 body = s.pos(0, 0, 12), moon = s.pos(0, 46, 26);
        s.beat(0, sc -> Fx.groundCrack(sc.lv, sc.p, body, 8f, VOID, 4, 260));
        s.beat(10, sc -> Fx.rune(sc.lv, sc.p, body.add(0, 0.05, 0), 7f, MOON, 260, 8));
        s.beat(20, sc -> phantom(sc, body, 3.0f, VOID, 260));
        s.every(0, 280, 3, sc -> crescent(sc, moon, 7, VOID, 1.4f)); // trăng bị che khuất
        Vec3[] anchors = {s.pos(-6, 0.5, 6), s.pos(6, 0.5, 6), s.pos(-6, 0.5, 18), s.pos(6, 0.5, 18)};
        for (int k = 0; k < anchors.length; k++) {
            Vec3 a = anchors[k];
            s.beat(60 + k * 12, sc -> Fx.ribbonWave(sc.lv, sc.p, a, body.add(0, 3, 0), 0.6, 0.8, 120, 0, 0.16f, MOON)); // xiềng xích
        }
        s.beat(250, sc -> Fx.shake(sc.lv, body, 0.6f, 14, 14f));
        s.beat(280, sc -> Fx.groundRing(sc.lv, sc.p, body, 8f, VOID, 100));
        echo(s, 60, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.7f, 0.5f, 4, 50);
    }

    // ---------------------------------------------------------------- Cảnh kết: Người Giữ Ký Ức
    static void finale(Scene s) {
        s.total = MemoryScene.FINALE_TICKS;
        s.cam(K(0, 12, 180, 4, 76, 4), K(180, 10, 240, 8, 66, 12), K(360, 14, 150, 6, 60, 6));
        int[] cols = {Fx.GOLD, MOON, Fx.CYAN, Fx.ORANGE, Fx.PURPLE, BLOOD, SOUL, Fx.WHITE};
        for (int i = 0; i < 8; i++) { // tám cột sáng, mỗi cột một sắc ký ức
            Vec3 at = s.pos(Math.cos(i * Math.PI / 4) * 7, 0, 8 + Math.sin(i * Math.PI / 4) * 7);
            int c = cols[i];
            s.beat(i * 4, sc -> Fx.play(sc.lv, FxType.LIGHT_PILLAR, at, Vec3.ZERO, 1.6f, c, 300));
        }
        s.beat(10, sc -> Fx.rune(sc.lv, sc.p, sc.pos(0, 0.05, 0), 10f, Fx.GOLD, 340, 12));
        s.beat(20, sc -> Fx.rune(sc.lv, sc.p, sc.pos(0, 0.05, 0), 8f, Fx.CYAN, 320, 6));
        s.every(0, 340, 3, sc -> { // vầng sáng vàng vòng quanh
            double a = sc.tick * 0.08;
            Vec3 q = sc.pos(Math.cos(a) * 10, 1 + Math.sin(sc.tick * 0.05), Math.sin(a) * 10);
            sc.lv.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0, 0, 0, 0.01);
        });
        Vec3 tip = s.pos(0, 0, 10);
        s.beat(140, sc -> Fx.giantSword(sc.lv, sc.p, tip, 2.2f, Fx.GOLD, 30, 80));
        s.beat(170, sc -> {
            Fx.play(sc.lv, FxType.SHOCKWAVE, tip.add(0, 0.1, 0), Vec3.ZERO, 5f, Fx.GOLD, 16);
            Fx.play(sc.lv, FxType.LIGHT_PILLAR, tip, Vec3.ZERO, 3f, Fx.WHITE, 90);
            Fx.shake(sc.lv, tip, 0.9f, 18, 26f);
        });
        s.beat(230, sc -> phantom(sc, sc.pos(0, 0, 30), 2.4f, VOID, 110)); // Dạ Khuyết vẫn đợi
        s.beat(300, sc -> {
            Fx.play(sc.lv, FxType.LIGHT_PILLAR, sc.c, Vec3.ZERO, 3.5f, Fx.GOLD, 60);
            sc.lv.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, sc.c.x, sc.c.y + 1, sc.c.z, 60, 0.8, 1, 0.8, 0.5);
            sc.lv.playSound(null, BlockPos.containing(sc.c), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.2f, 0.8f);
            sc.lv.playSound(null, BlockPos.containing(sc.c), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
        });
        s.sub(10, 110, MemoryLore.FINALE.get(0).key());
        s.sub(120, 220, MemoryLore.FINALE.get(1).key());
        s.sub(230, 340, MemoryLore.FINALE.get(2).key());
        s.endCard = () -> {
            MemoryScene.title(s.p,
                    Tx.t("hukhong.memory.final.title", "Người Giữ Ký Ức", "Keeper of Memories").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Tx.t("hukhong.memory.final.sub", "Tám ký ức đã về tay ngươi.", "All eight memories are yours.").withStyle(ChatFormatting.GRAY));
        };
    }
}
