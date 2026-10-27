package com.hukhong.guild;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Bang nhiem vu Hiep Hoi Mao Hiem Gia: chuot phai mo man hinh Hiep Hoi. */
public class GuildBoardBlock extends Block {
    public GuildBoardBlock(Properties props) { super(props); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) GuildService.open(sp);
        return InteractionResult.CONSUME;
    }
}
