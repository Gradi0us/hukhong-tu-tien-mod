package com.hukhong.block;

import com.hukhong.cores.CoreService;
import com.hukhong.cores.CoreSlots;
import com.hukhong.core.CultivationService;
import com.hukhong.menu.CoreAltarMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Ấn Hồn Đài: chuột phải khi bị khóa mốc 10/20/... → chọn Lõi. */
public class AnHonDaiBlock extends Block {
    public AnHonDaiBlock(Properties props) { super(props); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (!CultivationService.isMilestoneLocked(sp)) {
            sp.displayClientMessage(Component.translatable("hukhong.altar.not_locked"), true);
            return InteractionResult.SUCCESS;
        }
        int m = CoreService.milestoneOf(CultivationService.get(sp));
        if (CoreService.get(sp).stuckAt() == m) {
            sp.displayClientMessage(Component.translatable("hukhong.altar.stuck_blocked"), true);
            return InteractionResult.SUCCESS;
        }
        CoreSlots.Pending pend = CoreService.ensurePending(sp); // roll + lưu TRƯỚC khi mở UI
        if (pend == null) {
            sp.displayClientMessage(Component.translatable("hukhong.altar.no_cores"), true);
            return InteractionResult.SUCCESS;
        }
        sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new CoreAltarMenu(id, inv, pos, m, pend.tier(), pend.ids()),
                Component.translatable("hukhong.altar.title")), buf -> CoreAltarMenu.write(buf, pos, m, pend.tier(), pend.ids()));
        return InteractionResult.CONSUME;
    }
}
