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

/** Ấn Hồn Đài: chuột phải khi bị khóa mốc 10/20/... → chọn Lõi; lúc khác (hoặc ngồi + chuột phải) → mở Đe cường hoá vũ khí. */
public class AnHonDaiBlock extends Block {
    public AnHonDaiBlock(Properties props) { super(props); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (sp.isShiftKeyDown() || !CultivationService.isMilestoneLocked(sp)) {
            openAnvil(sp, pos);
            return InteractionResult.CONSUME;
        }
        int m = CoreService.milestoneOf(CultivationService.get(sp));
        if (CoreService.get(sp).stuckAt() == m) {
            sp.displayClientMessage(Component.translatable("hukhong.altar.stuck_blocked"), true);
            return InteractionResult.SUCCESS;
        }
        // Thiên Kiếp trước khi chọn Lõi: chưa qua mốc này -> bắt đầu kiếp thay vì mở UI.
        if (m < 9 && !com.hukhong.breakthrough.ThienKiep.isPassed(sp, m)) { // từ cấp 90 đã có Khảo Hạch (TrialService), không độ kiếp 2 lần
            if (com.hukhong.breakthrough.ThienKiep.begin(sp, m)) return InteractionResult.CONSUME;
            sp.displayClientMessage(Component.translatable("hukhong.kiep.busy"), true);
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

    /** Đe tích hợp: dùng giao diện đe gốc (sửa, ghép phù phép, đặt tên) nhưng không bao giờ mòn vỡ. */
    public static void openAnvil(ServerPlayer sp, BlockPos pos) {
        sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new net.minecraft.world.inventory.AnvilMenu(id, inv,
                net.minecraft.world.inventory.ContainerLevelAccess.create(sp.level(), pos)) {
            @Override protected boolean isValidBlock(BlockState s) { return s.getBlock() instanceof AnHonDaiBlock; }
        }, Component.translatable("hukhong.altar.anvil")));
    }
}
