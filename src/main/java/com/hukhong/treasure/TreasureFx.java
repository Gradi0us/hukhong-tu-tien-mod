package com.hukhong.treasure;

import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Hiệu ứng mở Bảo Rương lần đầu + tiện ích đặt rương thay Blocks.CHEST. */
public final class TreasureFx {
    private TreasureFx() {}

    /** Đặt Bảo Rương cấp {@code tier}, quay mặt {@code face}. Trả Container để nạp đồ (null nếu thất bại). */
    public static Container place(ServerLevel lv, BlockPos pos, int tier, Direction face) {
        lv.setBlock(pos, TreasureRegistry.BAO_RUONG.get().defaultBlockState()
                .setValue(BaoRuongBlock.TIER, Math.max(0, Math.min(3, tier))).setValue(BaoRuongBlock.FACING, face), 3);
        return lv.getBlockEntity(pos) instanceof Container c ? c : null;
    }

    static DustParticleOptions dust(int rgb, float s) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), s);
    }

    /** Mở lần đầu: cột sáng, vòng sóng, vàng/ngọc bắn tung, chớp; càng cấp cao càng lớn. */
    static void firstOpen(ServerLevel lv, BlockPos pos, int tier, ServerPlayer p) {
        Vec3 c = Vec3.atBottomCenterOf(pos);
        int rgb = BaoRuongBlock.COLOR[tier];
        Fx.play(lv, FxType.LIGHT_PILLAR, c, Vec3.ZERO, 0.8f + 0.5f * tier, rgb, 30 + 10 * tier);
        Fx.play(lv, FxType.SHOCKWAVE, c.add(0, 0.1, 0), Vec3.ZERO, 2.5f + tier, rgb, 14);
        Fx.rune(lv, p, c.add(0, 0.05, 0), 1.6f + 0.4f * tier, rgb, 30, 6 + tier);
        lv.sendParticles(dust(rgb, 1.6f), c.x, c.y + 1.1, c.z, 40 + 20 * tier, 0.4, 0.3, 0.4, 0.25);
        lv.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y + 1, c.z, 20 + 15 * tier, 0.3, 0.4, 0.3, 0.45);
        lv.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1, c.z, 15 + 8 * tier, 0.2, 0.6, 0.2, 0.12);
        lv.sendParticles(ParticleTypes.FLASH, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
        if (tier == 3) lv.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1, c.z, 80, 0.5, 0.6, 0.5, 0.4);
        lv.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5f, 0.8f + 0.15f * tier);
        lv.playSound(null, pos, tier >= 2 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, tier >= 2 ? 0.8f : 0.6f, 1.2f);
        if (tier >= 1) lv.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.4f);
        if (tier >= 2) Fx.shake(lv, c, 0.35f, 8, 10f);
    }
}
