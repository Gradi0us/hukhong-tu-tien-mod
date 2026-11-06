package com.hukhong.creature;

import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.joml.Vector3f;

/**
 * Kích thước Yêu Thú theo năm tuổi (2026-11-06):
 * - Yêu Thú thường: cao dần theo năm, tối đa 7 khối.
 * - Yêu Thụ (Thụ Yêu, Vạn Niên Thụ Vương): cây khổng lồ, Thụ Yêu tới ~16 khối, Thụ Vương tới 20 khối.
 * Kèm hiệu ứng cho thân khổng lồ: bước chân rung đất, lá rơi, linh quang.
 */
public final class YeuThuSize {
    private YeuThuSize() {}

    public static final double BEAST_MAX_HEIGHT = 7.0;
    /** Hệ số chiều cao theo năm (10 / 100 / 1000 / 10000 / 100000 năm) cho Yêu Thú thường. */
    static final double[] AGE_GROWTH = {1.0, 1.3, 1.7, 2.3, 3.0};
    /** Chiều cao mục tiêu (khối) của Yêu Thụ theo năm. */
    static final double[] THU_YEU_H = {3.5, 5.0, 8.0, 12.0, 16.0};
    static final double[] THU_VUONG_H = {8.0, 10.5, 13.5, 17.0, 20.0};

    public static boolean tree(CreatureKind k) { return k == CreatureKind.THU_YEU || k == CreatureKind.VAN_NIEN_THU_VUONG; }

    /** Hệ số SCALE (nhân với kích thước gốc của loài). */
    public static double scale(CreatureKind k, YeuThuAge a) {
        int i = a.ordinal();
        if (k == CreatureKind.THU_YEU) return THU_YEU_H[i] / k.h;
        if (k == CreatureKind.VAN_NIEN_THU_VUONG) return THU_VUONG_H[i] / k.h;
        double s = CreatureKind.BEAST_SCALE * AGE_GROWTH[i];
        return Math.min(s, Math.max(CreatureKind.BEAST_SCALE, BEAST_MAX_HEIGHT / k.h));
    }

    public static double height(CreatureKind k, YeuThuAge a) { return k.h * scale(k, a); }

    /** Hệ số phóng chiêu theo thân (1 ở cỡ chuẩn ~2,2 lần). */
    public static float skillK(LinhQuai m) { return (float) Math.max(1.0, m.getScale() / 2.2); }

    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), size);
    }

    /** Hiệu ứng thân khổng lồ (gọi mỗi tick phía server). */
    static void tick(LinhQuai m) {
        if (!(m.level() instanceof ServerLevel sl) || !m.isAlive()) return;
        double h = m.getBbHeight();
        if (h < 5) return;
        YeuThuAge a = m.age();
        boolean tree = tree(m.kind());
        // bước chân: bụi đất + rung nhẹ cho người gần (thân càng to càng rung)
        double sp = m.getDeltaMovement().horizontalDistanceSqr();
        if (sp > 1e-4 && m.onGround() && m.tickCount % (h > 12 ? 14 : 10) == 0) {
            var below = sl.getBlockState(m.blockPosition().below());
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), m.getX(), m.getY() + 0.1, m.getZ(),
                    (int) (8 + h * 2), m.getBbWidth() * 0.5, 0.1, m.getBbWidth() * 0.5, 0.15);
            if (h >= 9) {
                Fx.shake(sl, m.position(), (float) Math.min(1.2, h / 20.0), 8, (float) (h * 2));
                sl.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, (float) Math.min(3, h / 6), 0.4f);
            }
        }
        if (m.tickCount % 10 != 0) return;
        double top = m.getY() + h, w = m.getBbWidth();
        if (tree) {
            // tán lá: lá rơi + bào tử phát sáng
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, m.getX(), top - h * 0.2, m.getZ(), (int) (4 + h / 2), w * 0.8, h * 0.1, w * 0.8, 0.0);
            sl.sendParticles(dust(0x58B848, 1.6f), m.getX(), top - h * 0.25, m.getZ(), (int) (3 + h / 3), w * 0.9, h * 0.12, w * 0.9, 0.0);
            if (a != null && a.ordinal() >= YeuThuAge.Y1000.ordinal())
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, m.getX(), top - h * 0.3, m.getZ(), 6, w, h * 0.2, w, 0.01);
            if (a != null && a.ordinal() >= YeuThuAge.Y10000.ordinal()) // linh quang bay lên quanh thân
                sl.sendParticles(ParticleTypes.END_ROD, m.getX(), m.getY() + h * 0.5, m.getZ(), 4, w * 0.7, h * 0.3, w * 0.7, 0.02);
        } else if (a != null && a.ordinal() >= YeuThuAge.Y10000.ordinal()) {
            // thú vạn năm: khí tức bốc lên theo màu năm tuổi
            sl.sendParticles(dust(a.color == 0x2A2A2A ? 0x6A4A8A : a.color, 1.8f), m.getX(), m.getY() + h * 0.5, m.getZ(), 8, w * 0.6, h * 0.35, w * 0.6, 0.02);
        }
    }
}
