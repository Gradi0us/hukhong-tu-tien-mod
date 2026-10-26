package com.hukhong.bicanh;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Khối thoát trong Bí Cảnh: chuột phải để về điểm vào. */
public class BiCanhExitBlock extends Block {
    public BiCanhExitBlock(Properties props) { super(props); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) BiCanhManager.leave(sp, "hukhong.bicanh.left");
        return InteractionResult.CONSUME;
    }
}
