package com.hukhong.alchemy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Đan Lô: chuột phải mở giao diện luyện đan (minigame lửa). */
public class DanLoBlock extends BaseEntityBlock {
    public static final MapCodec<DanLoBlock> CODEC = simpleCodec(DanLoBlock::new);

    public DanLoBlock(Properties props) { super(props); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DanLoBlockEntity(pos, state); }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, AlchemyRegistry.DAN_LO_BE.get(), DanLoBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof DanLoBlockEntity be) sp.openMenu(be, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DanLoBlockEntity be)
            Containers.dropContents(level, pos, be);
        super.onRemove(state, level, pos, newState, moving);
    }
}
