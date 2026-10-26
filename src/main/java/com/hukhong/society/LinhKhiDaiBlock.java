package com.hukhong.society;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Linh Khí Đài (R22): đứng lên và giữ Shift (ngồi thiền) để Bế Quan, EXP rất chậm. Chuột phải = xem hướng dẫn. */
public class LinhKhiDaiBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    public LinhKhiDaiBlock(Properties props) { super(props); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) player.displayClientMessage(Component.translatable("hukhong.lkd.hint"), true);
        return InteractionResult.SUCCESS;
    }
}
