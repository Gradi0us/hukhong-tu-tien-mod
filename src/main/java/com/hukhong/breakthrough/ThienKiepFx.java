package com.hukhong.breakthrough;

import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.boss.phase.CutscenePayload;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.List;

/**
 * Hình ảnh Thiên Kiếp: cảnh mở đầu (máy quay ngước lên lôi vân), lôi vân xoáy, chớp xa, tia điện tụ trước khi đánh,
 * nứt đất + sóng chấn khi sét giáng, tâm ma trỗi dậy, chữ lớn giữa màn hình, mây tan khi kết thúc.
 */
public final class ThienKiepFx {
    private ThienKiepFx() {}

    static final DustParticleOptions STORM = new DustParticleOptions(new Vector3f(0.16f, 0.16f, 0.24f), 4f);
    static final DustParticleOptions BOLT = new DustParticleOptions(new Vector3f(0.75f, 0.88f, 1f), 1.4f);
    static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.86f, 0.35f), 1.8f);

    /** Cảnh mở đầu 5s: máy quay lùi sau lưng rồi ngước lên lôi vân đang tụ; phụ đề + chữ lớn. */
    static void intro(ServerPlayer p, int milestone) {
        List<CamPath.Key> keys = List.of(
                new CamPath.Key(0, 0, 2.2f, 5, 0, 0, 60, 1.6f),
                new CamPath.Key(45, -3, 5, 9, 0, -12, 68, 5f),
                new CamPath.Key(ThienKiep.INTRO, 0, 9, 14, 0, -38, 80, 14f));
        List<SubLine> subs = List.of(
                new SubLine(6, 48, "hukhong.kiep.sub.1"),
                new SubLine(52, ThienKiep.INTRO - 4, "hukhong.kiep.sub.2"));
        try {
            PacketDistributor.sendToPlayer(p, new CutscenePayload(-1, p.getX(), p.getY(), p.getZ(), p.getYRot(), ThienKiep.INTRO, keys, subs));
        } catch (RuntimeException ignored) { } // người chơi giả (test)
        title(p, "hukhong.kiep.title", "hukhong.kiep.title_sub." + Math.min(9, Math.max(1, milestone)), 0x9FC8FF);
    }

    /** Chữ lớn giữa màn hình. Bỏ trống key tiêu đề = chỉ phụ đề. */
    static void title(ServerPlayer p, String titleKey, String subKey, int rgb) {
        if (p.connection == null) return;
        try {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(8, 50, 18));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(subKey).withColor(0xE0E0E0)));
            p.connection.send(new ClientboundSetTitleTextPacket(titleKey.isEmpty() ? Component.empty() : Component.translatable(titleKey).withColor(rgb)));
        } catch (RuntimeException ignored) { }
    }

    /** Lôi vân: 3 nhánh xoắn ốc hội tụ trên đầu, lõi sáng điện ở tâm. {@code k} 0..1 = độ dày (tăng dần trong cảnh mở đầu). */
    static void vortex(ServerLevel lv, Vec3 c, int t, float k) {
        if (t % 2 != 0) return;
        double y = c.y + ThienKiep.CLOUD_H;
        int arms = 3, per = (int) (3 + 5 * k);
        for (int a = 0; a < arms; a++) for (int i = 0; i < per; i++) {
            double r = 2 + i * 1.9, ang = t * 0.06 + a * Math.PI * 2 / arms + r * 0.35;
            double x = c.x + Math.cos(ang) * r, z = c.z + Math.sin(ang) * r;
            lv.sendParticles(STORM, x, y + Math.sin(ang * 2) * 0.6, z, 2, 0.8, 0.3, 0.8, 0);
            if (i % 3 == 0) lv.sendParticles(ParticleTypes.LARGE_SMOKE, x, y - 0.5, z, 1, 0.6, 0.2, 0.6, 0.01);
        }
        if (t % 6 == 0) {
            lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, y - 1, c.z, (int) (6 + 14 * k), 1.6, 0.4, 1.6, 0.25);
            lv.sendParticles(BOLT, c.x, y - 1, c.z, 4, 1.2, 0.3, 1.2, 0);
        }
    }

    /** Chớp xa trong mây (chỉ hình + tiếng), vòng 14-24 khối. */
    static void distantFlash(ServerLevel lv, Vec3 c) {
        double a = lv.random.nextDouble() * Math.PI * 2, r = 14 + lv.random.nextDouble() * 10;
        Vec3 at = new Vec3(c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r);
        int y = lv.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) at.x, (int) at.z);
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(lv);
        if (b != null) { b.moveTo(at.x, y, at.z); b.setVisualOnly(true); lv.addFreshEntity(b); }
    }

    /** Tia điện tụ dần từ mây xuống điểm sắp đánh; {@code k} 0..1 = tiến độ báo trước. */
    static void charge(ServerLevel lv, Vec3 pos, float k, boolean big) {
        if (k < 0 || k > 1) return;
        double top = pos.y + ThienKiep.CLOUD_H, h = top - (top - pos.y) * k;
        double spread = big ? 1.2 : 0.35;
        lv.sendParticles(BOLT, pos.x, h, pos.z, big ? 10 : 4, spread, 0.6, spread, 0);
        lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, h, pos.z, big ? 6 : 2, spread, 0.4, spread, 0.05);
        if (big) lv.sendParticles(GOLD, pos.x, pos.y + 0.2, pos.z, 6, 2.5, 0.05, 2.5, 0);
        else lv.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.1, pos.z, 1, 0.8, 0, 0.8, 0);
    }

    /** Sét giáng: nứt đất, sóng chấn, đất văng, khói cháy, rung theo khoảng cách. */
    static void impact(ServerPlayer p, ServerLevel lv, Vec3 pos, float radius, boolean big) {
        Fx.groundCrack(lv, p, pos, radius * (big ? 1.4f : 1.1f), 0x9FD0FF, big ? 10 : 6, 50);
        Fx.play(lv, FxType.SHOCKWAVE, pos.add(0, 0.1, 0), Vec3.ZERO, radius * (big ? 2.2f : 1.4f), 0xCFE6FF, 16);
        if (big) Fx.play(lv, FxType.LIGHT_PILLAR, pos, Vec3.ZERO, 3.5f, 0xE8F2FF, 30);
        BlockPos below = BlockPos.containing(pos).below();
        lv.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, lv.getBlockState(below)), pos.x, pos.y + 0.3, pos.z, big ? 80 : 30, radius * 0.5, 0.3, radius * 0.5, 0.25);
        lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.5, pos.z, big ? 90 : 35, radius * 0.4, 0.6, radius * 0.4, 0.3);
        lv.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y + 0.2, pos.z, big ? 14 : 5, radius * 0.3, 0.1, radius * 0.3, 0.02);
        lv.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 1, pos.z, 1, 0, 0, 0, 0);
        lv.playSound(null, BlockPos.containing(pos), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, big ? 3f : 1.6f, big ? 0.5f : 0.9f);
        if (big) lv.playSound(null, BlockPos.containing(pos), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.WEATHER, 2f, 0.5f);
        Fx.shake(lv, pos, big ? 1.2f : 0.7f, big ? 24 : 10, big ? 40f : 14f);
    }

    /** Tâm ma trỗi dậy: bóng ma, mực đen phun từ đất. */
    static void demonRise(ServerLevel lv, Entity m) {
        Fx.play(lv, FxType.DEMON_SILHOUETTE, m.position(), Vec3.ZERO, 1.6f, 0x7A2AB0, 40);
        lv.sendParticles(ParticleTypes.SQUID_INK, m.getX(), m.getY() + 0.5, m.getZ(), 40, 0.5, 0.8, 0.5, 0.08);
        lv.sendParticles(ParticleTypes.SOUL, m.getX(), m.getY() + 0.2, m.getZ(), 15, 0.6, 0.2, 0.6, 0.04);
    }

    /** Hào quang tâm ma khi bay. */
    static void demonAura(ServerLevel lv, Entity m) {
        lv.sendParticles(ParticleTypes.SMOKE, m.getX(), m.getY() + 0.4, m.getZ(), 3, 0.2, 0.3, 0.2, 0.01);
        lv.sendParticles(ParticleTypes.WITCH, m.getX(), m.getY() + 0.5, m.getZ(), 1, 0.2, 0.2, 0.2, 0);
    }

    /** Độ kiếp thành công: mây tan tỏa ra, cột kim quang từ trời, chữ lớn. */
    static void success(ServerPlayer p, Vec3 c) {
        ServerLevel lv = p.serverLevel();
        double y = c.y + ThienKiep.CLOUD_H;
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48;
            lv.sendParticles(ParticleTypes.CLOUD, c.x + Math.cos(a) * 4, y, c.z + Math.sin(a) * 4, 0, Math.cos(a), 0.05, Math.sin(a), 0.9);
        }
        Fx.play(lv, FxType.LIGHT_PILLAR, p.position(), Vec3.ZERO, 2.5f, 0xFFE070, 60);
        lv.sendParticles(GOLD, p.getX(), p.getY() + 1, p.getZ(), 80, 1.2, 1.5, 1.2, 0);
        lv.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 60, 0.8, 1, 0.8, 0.5);
        lv.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.2f, 1f);
        lv.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5f, 0.8f);
        title(p, "hukhong.kiep.pass_title", "hukhong.kiep.pass_sub", 0xFFE070);
    }

    /** Thất bại: lôi vân sụp, khói đen, chữ đỏ. */
    static void failure(ServerPlayer p, ServerLevel lv, Vec3 c) {
        lv.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + ThienKiep.CLOUD_H, c.z, 120, 6, 1, 6, 0.05);
        lv.playSound(null, BlockPos.containing(c), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1f, 0.4f);
        title(p, "hukhong.kiep.fail_title", "hukhong.kiep.fail_sub", 0xFF5050);
    }
}
