package com.hukhong.dongphu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/** Item đặt Động Phủ: kiểm tra "mỗi người một trận bàn" trước khi đặt. Lập trận thật sự ở Block.setPlacedBy. */
public class DongPhuTranBanItem extends BlockItem {
    public DongPhuTranBanItem(Block block, Properties props) { super(block, props); }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        if (ctx.getLevel() instanceof ServerLevel && ctx.getPlayer() instanceof ServerPlayer sp) {
            Component err = DongPhuService.validatePlace(sp);
            if (err != null) {
                sp.displayClientMessage(err, false);
                return InteractionResult.FAIL;
            }
        }
        return super.place(ctx);
    }
}
