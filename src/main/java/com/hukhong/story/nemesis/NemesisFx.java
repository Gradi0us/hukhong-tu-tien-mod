package com.hukhong.story.nemesis;

import com.hukhong.boss.phase.CamPath;
import com.hukhong.boss.phase.CutscenePayload;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.List;

/**
 * Hình ảnh Phản Thần Dạ Khuyết: bóng ma, cột tím-đen, trăng khuyết trên trời, vòng nhiễm Hư Không, hút ánh sáng,
 * tan biến nổ tung. Mọi hiệu ứng đi qua {@link Fx} (client tự dựng) hoặc packet vanilla; phụ đề dùng subtitle packet.
 */
final class NemesisFx {
    private NemesisFx() {}

    static final int VOID_RGB = 0x7A2AB0, MOON_RGB = 0x9FB4FF, INK_RGB = 0x2A1040, RIFT_RGB = 0x8A1020;
    static final DustParticleOptions VOID_DUST = dust(VOID_RGB, 1.6f), INK_DUST = dust(INK_RGB, 1.4f), MOON_DUST = dust(MOON_RGB, 1.1f);

    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), size);
    }

    // ------------------------------------------------------------------ hình học mặt đất
    /** Mặt đất (bề mặt chặn) tại toạ độ x,z. */
    static Vec3 ground(ServerLevel lv, double x, double z) {
        BlockPos top = lv.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0, z));
        return Vec3.atBottomCenterOf(top);
    }

    /** Điểm cách người chơi {@code dist} block theo hướng {@code yawOffsetDeg} so với hướng nhìn (0 = phía trước). */
    static Vec3 around(ServerLevel lv, ServerPlayer p, double dist, double yawOffsetDeg) {
        double yr = Math.toRadians(p.getYRot() + yawOffsetDeg);
        return ground(lv, p.getX() - Math.sin(yr) * dist, p.getZ() + Math.cos(yr) * dist);
    }

    /** Góc yaw (độ, quy ước Minecraft) hướng từ {@code from} tới {@code to}. */
    static float yawFromTo(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    // ------------------------------------------------------------------ lời thoại
    /** Một dòng lời: phụ đề giữa màn hình + dòng chat (tên màu tím). Lỗi kết nối (người chơi giả) bị nuốt. */
    static void say(ServerPlayer p, Component line) {
        // phụ đề nằm trong cutscene (SubLine); chat giữ lại lời để đọc lại
        p.sendSystemMessage(Component.literal("[Dạ Khuyết] ").withStyle(ChatFormatting.DARK_PURPLE)
                .append(line.copy().withStyle(ChatFormatting.GRAY)));
    }

    /** Dòng chat có tên người nói (lưu lại lời thoại cảnh). */
    static void chat(ServerPlayer p, String who, ChatFormatting color, Component line) {
        p.sendSystemMessage(Component.literal("[" + who + "] ").withStyle(color).append(line.copy().withStyle(ChatFormatting.GRAY)));
    }

    // ------------------------------------------------------------------ máy quay
    /** Cutscene camera neo tĩnh tại anchor; refYaw = hướng "trước" của khung (dz > 0 = về phía trước). */
    static void cam(ServerPlayer p, Vec3 anchor, float refYaw, int ticks, List<CamPath.Key> keys, List<com.hukhong.boss.phase.BossPhaseConfig.SubLine> subs) {
        try {
            PacketDistributor.sendToPlayer(p, new CutscenePayload(-1, anchor.x, anchor.y, anchor.z, refYaw, ticks, keys, subs));
        } catch (RuntimeException ignored) { }
    }

    // ------------------------------------------------------------------ hình ảnh
    /** Cột tím-đen dâng từ mặt đất. */
    static void pillar(ServerLevel lv, Vec3 c, int rgb, int life) {
        Fx.play(lv, FxType.LIGHT_PILLAR, c, Vec3.ZERO, 2.0f, rgb, life);
    }

    /** Trăng khuyết (pháp trận 3 cánh) treo trên trời phía trên điểm c. */
    static void moonRune(ServerLevel lv, ServerPlayer p, Vec3 c, float radius, int life) {
        Fx.rune(lv, p, c.add(0, 28, 0), radius, MOON_RGB, life, 3);
        lv.sendParticles(MOON_DUST, c.x, c.y + 28, c.z, 24, radius * 0.5, 0.3, radius * 0.5, 0.02);
    }

    /** Bóng ma Dạ Khuyết đứng tại c (client dựng bóng đen). */
    static void silhouette(ServerLevel lv, ServerPlayer p, Vec3 c, float scale, int life) {
        // thân thật là model Dạ Khuyết (NemesisService.spawnPhantom); ở đây chỉ còn mực đen + hồn bay
        lv.sendParticles(ParticleTypes.SOUL, c.x, c.y + 1.2, c.z, 12, 0.7, 1.2, 0.7, 0.02);
        lv.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y + 1, c.z, 30, 0.6, 1.0, 0.6, 0.05);
    }

    /** Vòng nhiễm Hư Không nở trên đất. */
    static void voidRing(ServerLevel lv, ServerPlayer p, Vec3 c, float radius, int life) {
        Fx.groundRing(lv, p, c.add(0, 0.05, 0), radius, VOID_RGB, life);
        lv.sendParticles(VOID_DUST, c.x, c.y + 0.3, c.z, 18, radius * 0.5, 0.2, radius * 0.5, 0.02);
    }

    /** Hào quang đen-tím quanh thân (gọi mỗi vài tick). */
    static void aura(ServerLevel lv, Vec3 c, int t) {
        if (t % 3 != 0) return;
        lv.sendParticles(VOID_DUST, c.x, c.y + 1.1, c.z, 4, 0.5, 0.9, 0.5, 0);
        lv.sendParticles(ParticleTypes.SOUL, c.x, c.y + 0.2, c.z, 1, 0.4, 0.1, 0.4, 0.01);
        if (t % 12 == 0) lv.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y + 1, c.z, 3, 0.4, 0.8, 0.4, 0.02);
    }

    /** Hư Không co lại: các hạt dạt trên vòng tròn co dần về tâm (gọi mỗi tick trong lúc tan). {@code r} = bán kính hiện tại. */
    static void converge(ServerLevel lv, Vec3 c, double r, int n) {
        if (r <= 0.05) return;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n + lv.random.nextDouble() * 0.3;
            lv.sendParticles(VOID_DUST, c.x + Math.cos(a) * r, c.y + 1.2 + Math.sin(a * 2) * 0.4, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    /** Hút ánh sáng: hạt bay từ đầu người chơi về phía bóng. */
    static void drain(ServerLevel lv, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from).normalize();
        lv.sendParticles(ParticleTypes.END_ROD, from.x, from.y, from.z, 0, d.x, d.y, d.z, 0.35);
        lv.sendParticles(ParticleTypes.SMOKE, from.x, from.y, from.z, 0, d.x, d.y, d.z, 0.2);
    }

    /** Tan biến: sóng xung, chớp, mực đen và hồn bay ra, rung màn hình, âm thanh rít của Wither. */
    static void implode(ServerPlayer p, ServerLevel lv, Vec3 c) {
        Fx.play(lv, FxType.SHOCKWAVE, c.add(0, 0.1, 0), Vec3.ZERO, 3.5f, VOID_RGB, 18);
        Fx.groundRing(lv, p, c.add(0, 0.05, 0), 4.5f, VOID_RGB, 40);
        Fx.shake(lv, c, 0.9f, 20, 24f);
        lv.sendParticles(ParticleTypes.FLASH, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
        lv.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y + 1, c.z, 70, 1.2, 1.2, 1.2, 0.2);
        lv.sendParticles(ParticleTypes.SOUL, c.x, c.y + 1, c.z, 40, 1.0, 1.2, 1.0, 0.08);
        lv.sendParticles(VOID_DUST, c.x, c.y + 1, c.z, 60, 1.5, 1.2, 1.5, 0.2);
        lv.playSound(null, BlockPos.containing(c), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.4f, 0.6f);
        lv.playSound(null, BlockPos.containing(c), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 1.2f, 0.5f);
    }

    /** Tiếng rên Wither trầm (âm lượng nhỏ để không át lời thoại). */
    static void growl(ServerLevel lv, Vec3 c) {
        lv.playSound(null, BlockPos.containing(c), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    /** Đen kịt: bóng tối nhẹ trong vài giây (không gây hại). */
    static void darkness(ServerPlayer p, int ticks) {
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, ticks, 0, false, false));
    }

    /** Mù 3 giây: "ánh sáng" bị hút đi. */
    static void blindness(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
    }
}
