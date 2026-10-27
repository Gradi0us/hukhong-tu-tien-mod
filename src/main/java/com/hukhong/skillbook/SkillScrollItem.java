package com.hukhong.skillbook;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Bí Tịch (võ kỹ) / Công Pháp (đạo pháp, kỹ năng chung). Chuột phải để học; bản trùng giữ làm vật liệu Cường Hóa. */
public class SkillScrollItem extends Item {
    public SkillScrollItem(Properties p) { super(p); }

    @Override
    public Component getName(ItemStack stack) {
        BookSkill s = SkillBookRegistry.skillOf(stack);
        if (s == null) return super.getName(stack);
        MutableComponent n = Component.translatableWithFallback(s.congPhap() ? "hukhong.skillbook.name.cong_phap" : "hukhong.skillbook.name.bi_tich",
                s.congPhap() ? "Công Pháp: %s" : "Bí Tịch: %s",
                Component.translatableWithFallback(s.langKey(), s.viName));
        return n.withColor(s.grade.argb & 0xFFFFFF);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        BookSkill s = SkillBookRegistry.skillOf(stack);
        if (s == null) return;
        tip.add(Component.translatableWithFallback(s.grade.langKey(), s.grade.viName).withColor(s.grade.argb & 0xFFFFFF)
                .append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY))
                .append(s.path == null ? Component.translatableWithFallback("hukhong.skillbook.universal", "Chung (mọi đạo)")
                        : Component.translatable(com.hukhong.skill.SkillPages.pathLangKey(s.path))));
        tip.add(Component.translatableWithFallback(s.descKey(), s.viDesc).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatableWithFallback("hukhong.skillbook.tip.req", "Yêu cầu cấp tu luyện %s", s.minLevel()).withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.translatableWithFallback("hukhong.skillbook.tip.use", "Chuột phải để học. Bản trùng dùng để Cường Hóa.").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
        BookSkill s = SkillBookRegistry.skillOf(st);
        if (s == null) return InteractionResultHolder.pass(st);
        if (SkillBookService.learn(sp, s)) st.shrink(1);
        return InteractionResultHolder.consume(st);
    }
}
