package com.hukhong.story;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Văn Kiện: chuột phải để đọc, ghi vào Nhật ký truyện (phím J) và tiêu hao. */
public class VanKienItem extends Item {
    private final String docId;

    public VanKienItem(String docId) {
        super(new Item.Properties().stacksTo(16));
        this.docId = docId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
        if (StoryService.collect(sp, docId)) st.shrink(1);
        return InteractionResultHolder.consume(st);
    }
}
