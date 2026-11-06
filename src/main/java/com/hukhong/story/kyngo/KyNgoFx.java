package com.hukhong.story.kyngo;

import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.boss.phase.CutscenePayload;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.List;

/**
 * Hình ảnh Kỳ Ngộ: cột sáng dẫn đường từ xa (màu theo loại), cảnh máy quay giới thiệu khi lại gần,
 * không khí riêng từng loại, hiệu ứng cho từng kết cục.
 */
public final class KyNgoFx {
    private KyNgoFx() {}

    public static final int REVEAL_TICKS = 70, APPROACH = 10;

    static int color(KyNgoType t) {
        return switch (t) {
            case LAO_NHAN -> 0xFFD27A;
            case DONG_PHU -> 0x8FB8FF;
            case THI_THE -> 0xB8C8D8;
            case LINH_THU -> 0x9CFFB0;
        };
    }

    static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), size);
    }

    static Vec3 c(KyNgoInstance i) { return Vec3.atBottomCenterOf(i.origin); }

    /** Lúc xuất hiện: sương mù dâng lên che chỗ kỳ ngộ, tiếng chuông xa. */
    static void appear(KyNgoInstance i) {
        Vec3 c = c(i);
        i.level.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.5, c.z, 60, 2.5, 0.4, 2.5, 0.02);
        i.level.sendParticles(dust(color(i.type), 1.6f), c.x, c.y + 1, c.z, 40, 1.5, 1, 1.5, 0);
        i.level.playSound(null, i.origin, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 1.5f, 0.6f);
    }

    /** Cột sáng dẫn đường (chưa tới gần) + không khí riêng mỗi loại. */
    static void ambient(KyNgoInstance i, int t) {
        ServerLevel lv = i.level;
        Vec3 c = c(i);
        if (!i.approached && t % 3 == 0) { // cột sáng dẫn đường: gửi thẳng cho người chơi, thấy từ xa (vượt giới hạn 32 khối)
            DustParticleOptions d = dust(color(i.type), 3.0f);
            for (int k = 0; k < 10; k++) {
                double y = c.y + 1 + ((t / 3 + k * 3) % 30);
                lv.sendParticles(i.owner, d, true, c.x, y, c.z, 2, 0.12, 0.4, 0.12, 0);
            }
            lv.sendParticles(i.owner, ParticleTypes.END_ROD, true, c.x, c.y + 1 + (t % 30), c.z, 3, 0.15, 1.5, 0.15, 0.01);
            lv.sendParticles(i.owner, ParticleTypes.END_ROD, true, c.x, c.y + 0.3, c.z, 2, 0.6, 0.05, 0.6, 0.01);
        }
        if (t % 8 != 0) return;
        Entity m = i.main == null ? null : lv.getEntity(i.main);
        switch (i.type) {
            case LAO_NHAN -> {
                if (m != null) {
                    lv.sendParticles(dust(0x8A0F0F, 1.1f), m.getX(), m.getY() + 0.9, m.getZ(), 2, 0.2, 0.3, 0.2, 0); // vết máu
                    if (t % 80 == 0) lv.playSound(null, m.blockPosition(), SoundEvents.VILLAGER_HURT, SoundSource.NEUTRAL, 0.6f, 0.6f); // ho khan
                }
            }
            case DONG_PHU -> {
                if (!i.rewarded) {
                    if (t % 40 == 0) Fx.rune(lv, i.owner, c.add(0, 0.05, 0), 1.6f, 0x8FB8FF, 42, 6); // phong ấn
                    lv.sendParticles(ParticleTypes.ENCHANT, c.x, c.y + 1.2, c.z, 6, 1.2, 0.6, 1.2, 0.4);
                }
                lv.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x + (lv.random.nextBoolean() ? 2 : -2), c.y + 2.6, c.z + (lv.random.nextBoolean() ? 2 : -2), 1, 0.05, 0.05, 0.05, 0.005);
            }
            case THI_THE -> {
                if (m != null) {
                    lv.sendParticles(ParticleTypes.SOUL, m.getX(), m.getY() + 0.6, m.getZ(), 1, 0.3, 0.2, 0.3, 0.01);
                    lv.sendParticles(dust(0x6A0A0A, 1.3f), m.getX(), m.getY() + 0.05, m.getZ(), 2, 0.6, 0, 0.6, 0);
                }
            }
            case LINH_THU -> {
                if (m != null) {
                    lv.sendParticles(ParticleTypes.GLOW, m.getX(), m.getY() + 0.6, m.getZ(), 2, 0.4, 0.3, 0.4, 0.01);
                    if (t % 60 == 0) lv.playSound(null, m.blockPosition(), SoundEvents.FOX_AMBIENT, SoundSource.NEUTRAL, 0.8f, 1.5f); // rên khẽ
                }
            }
        }
    }

    /** Lại gần: máy quay lượn quanh cảnh, phụ đề lời dẫn. */
    static void reveal(KyNgoInstance i) {
        Vec3 c = c(i);
        String k = "hukhong.kyngo.sub." + i.type.name().toLowerCase();
        List<CamPath.Key> keys = List.of(
                new CamPath.Key(0, 0, 3, 7, 0, 8, 58, 1f),
                new CamPath.Key(REVEAL_TICKS / 2, -5, 2.4f, 5, 0, 6, 54, 0.9f),
                new CamPath.Key(REVEAL_TICKS, -6, 2f, 1, 0, 4, 50, 0.9f));
        List<SubLine> subs = List.of(new SubLine(4, REVEAL_TICKS - 4, k));
        float yaw = (float) Math.toDegrees(Math.atan2(-(c.x - i.owner.getX()), c.z - i.owner.getZ()));
        try {
            PacketDistributor.sendToPlayer(i.owner, new CutscenePayload(-1, c.x, c.y, c.z, yaw, REVEAL_TICKS, keys, subs));
        } catch (RuntimeException ignored) { }
        i.level.sendParticles(dust(color(i.type), 1.4f), c.x, c.y + 1, c.z, 50, 2, 1, 2, 0);
        i.level.playSound(null, i.origin, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.AMBIENT, 1.2f, 0.7f);
    }

    // ---- kết cục ----

    /** Biến mất vào sương (lão nhân đi, linh hồn siêu thoát...). */
    static void vanish(ServerLevel lv, Entity e, int rgb) {
        if (e == null) return;
        lv.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 1, e.getZ(), 40, 0.5, 0.9, 0.5, 0.03);
        lv.sendParticles(dust(rgb, 1.5f), e.getX(), e.getY() + 1, e.getZ(), 30, 0.4, 0.9, 0.4, 0);
        lv.playSound(null, e.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.NEUTRAL, 1f, 1.2f);
    }

    /** Ban thưởng: kim quang bao quanh người chơi. */
    static void blessing(ServerPlayer p) {
        ServerLevel lv = p.serverLevel();
        Fx.play(lv, FxType.LIGHT_PILLAR, p.position(), Vec3.ZERO, 1.2f, 0xFFE070, 40);
        lv.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 0.9, 0.6, 0.35);
        lv.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    /** Yêu ma lộ nguyên hình. */
    static void demonReveal(ServerLevel lv, Vec3 at) {
        Fx.play(lv, FxType.DEMON_SILHOUETTE, at, Vec3.ZERO, 2f, 0x8A1020, 40);
        lv.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y + 1, at.z, 60, 0.6, 1, 0.6, 0.1);
        lv.sendParticles(ParticleTypes.CRIMSON_SPORE, at.x, at.y + 1, at.z, 40, 1, 1, 1, 0.05);
        lv.playSound(null, BlockPos.containing(at), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.2f, 0.7f);
        Fx.shake(lv, at, 0.6f, 14, 16f);
    }

    /** Oán khí bám thân khi cướp đồ. */
    static void curse(ServerPlayer p) {
        ServerLevel lv = p.serverLevel();
        lv.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.9, 0.5, 0.03);
        lv.sendParticles(ParticleTypes.WITCH, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.9, 0.5, 0.1);
        lv.playSound(null, p.blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 1f, 0.7f);
    }

    /** Phong ấn vỡ: chớp sáng, chữ phù tan, rương hiện ra. */
    static void sealBreak(KyNgoInstance i) {
        Vec3 c = c(i);
        ServerLevel lv = i.level;
        lv.sendParticles(ParticleTypes.FLASH, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
        lv.sendParticles(ParticleTypes.ENCHANT, c.x, c.y + 1, c.z, 120, 1.5, 1, 1.5, 1.2);
        lv.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y + 0.8, c.z, 40, 0.4, 0.4, 0.4, 0.5);
        Fx.play(lv, FxType.SHOCKWAVE, c.add(0, 0.1, 0), Vec3.ZERO, 4f, 0x8FB8FF, 16);
        lv.playSound(null, i.origin, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.5f, 1.4f);
        lv.playSound(null, i.origin, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1f, 0.8f);
        Fx.shake(lv, c, 0.5f, 10, 16f);
    }

    /** Xác chết trỗi dậy: đất văng, hồn đen. */
    static void riseFromGround(ServerLevel lv, Entity z) {
        BlockPos b = z.blockPosition().below();
        lv.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, lv.getBlockState(b)), z.getX(), z.getY() + 0.2, z.getZ(), 40, 0.4, 0.2, 0.4, 0.15);
        lv.sendParticles(ParticleTypes.SCULK_SOUL, z.getX(), z.getY() + 0.5, z.getZ(), 10, 0.3, 0.5, 0.3, 0.03);
        lv.playSound(null, z.blockPosition(), SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 1.2f, 0.6f);
    }

    /** Thả linh thú: lồng vỡ, tim bay, dải sáng nối chủ - thú. */
    static void bond(ServerPlayer p, Entity beast, List<BlockPos> cage) {
        ServerLevel lv = p.serverLevel();
        for (BlockPos b : cage)
            lv.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_FENCE.defaultBlockState()), b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5, 15, 0.3, 0.4, 0.3, 0.1);
        lv.sendParticles(ParticleTypes.HEART, beast.getX(), beast.getY() + 1, beast.getZ(), 10, 0.5, 0.4, 0.5, 0.1);
        Fx.ribbon(lv, p, beast.position().add(0, 0.6, 0), p.position().add(0, 1, 0), 0.6, 18, 0, 0.25f, 0x9CFFB0);
        lv.playSound(null, beast.blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1f, 1f);
        lv.playSound(null, beast.blockPosition(), SoundEvents.FOX_EAT, SoundSource.NEUTRAL, 1f, 1.4f);
    }
}
