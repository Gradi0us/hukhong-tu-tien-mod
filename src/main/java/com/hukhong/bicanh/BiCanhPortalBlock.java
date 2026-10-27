package com.hukhong.bicanh;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Cổng Bí Cảnh (xuất hiện định kỳ, có hạn). Cấp nằm trong blockstate TIER 0..4. */
public class BiCanhPortalBlock extends Block {
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, 4);
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);

    public BiCanhPortalBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(TIER, 0).setValue(FACING, Direction.NORTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(TIER, FACING); }

    public BlockState stateFor(BiCanhTier t) { return stateFor(t, Direction.NORTH); }
    /** out = hướng nhìn ra miệng hang. */
    public BlockState stateFor(BiCanhTier t, Direction out) { return defaultBlockState().setValue(TIER, t.ordinal()).setValue(FACING, out); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp)
            BiCanhManager.enterFromPortal(sl, pos, BiCanhTier.byIndex(state.getValue(TIER)), sp);
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
        for (int i = 0; i < 2; i++)
            l.addParticle(ParticleTypes.PORTAL, p.getX() + r.nextDouble(), p.getY() + r.nextDouble() * 1.2,
                    p.getZ() + r.nextDouble(), 0, 0.15, 0);
    }
}
