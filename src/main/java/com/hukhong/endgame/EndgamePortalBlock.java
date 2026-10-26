package com.hukhong.endgame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Cổng Hư Không Bản Nguyên: ở Overworld = vào; trong chiều không gian đó = về lại. */
public class EndgamePortalBlock extends Block {
    public EndgamePortalBlock(Properties props) { super(props); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            if (sl.dimension() == EndgameRegistry.DIM) EndgameService.leave(sp);
            else EndgameService.enter(sp);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
        for (int i = 0; i < 3; i++)
            l.addParticle(ParticleTypes.REVERSE_PORTAL, p.getX() + r.nextDouble(), p.getY() + 1.0 + r.nextDouble() * 1.2,
                    p.getZ() + r.nextDouble(), 0, 0.12, 0);
    }
}
