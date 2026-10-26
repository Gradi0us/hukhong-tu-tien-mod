package com.hukhong.honcot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

/** Hồn Cốt: chuột phải để mang vào slot tương ứng (đổi ra nếu đã có). Phẩm/roll ở data component. */
public class HonCotItem extends Item {
    public final Bone bone;

    public HonCotItem(Bone bone) {
        super(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));
        this.bone = bone;
    }

    public static int tierOf(ItemStack s) {
        Integer v = s.get(HonCotRegistry.BONE_TIER.get());
        return HonCotMath.clampTier(v == null ? HonCotMath.TIER_HAC : v);
    }

    public static int rollOf(ItemStack s) {
        Integer v = s.get(HonCotRegistry.BONE_ROLL.get());
        return v == null ? 50 : Math.max(0, Math.min(100, v));
    }

    public static String tierKey(int tier) { return "hukhong.honcot.tier." + tier; }

    public static ChatFormatting tierColor(int tier) {
        return switch (HonCotMath.clampTier(tier)) {
            case 0 -> ChatFormatting.LIGHT_PURPLE;
            case 1 -> ChatFormatting.DARK_GRAY;
            default -> ChatFormatting.RED;
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("hukhong.honcot.name", Component.translatable(bone.nameKey()),
                Component.translatable(tierKey(tierOf(stack)))).withStyle(tierColor(tierOf(stack)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (HonCotService.equip(sp, st)) return InteractionResultHolder.sidedSuccess(ItemStack.EMPTY, false);
            return InteractionResultHolder.fail(st);
        }
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        int tier = tierOf(stack), roll = rollOf(stack);
        tip.add(Component.translatable("hukhong.honcot.tip.slot", Component.translatable(bone.slot.langKey())).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("hukhong.honcot.tip.quality", roll).withStyle(ChatFormatting.AQUA));
        for (Bone.Stat st : bone.stats) tip.add(statLine(st, tier, roll));
        tip.add(Component.translatable(bone.fxKey()).withStyle(ChatFormatting.GREEN));
        tip.add(Component.translatable("hukhong.honcot.tip.req", HonCotMath.requiredLevel(tier)).withStyle(ChatFormatting.DARK_GRAY));
    }

    /** "+8.5% Bạo Kích" — dùng chung cho tooltip và màn Hồn Cốt. */
    public static Component statLine(Bone.Stat st, int tier, int roll) {
        double v = HonCotMath.statValue(st.base(), tier, roll);
        String name = st.attr().substring(st.attr().lastIndexOf('.') + 1).replace("vanilla:", "").toLowerCase(Locale.ROOT);
        boolean pct = st.mult() || st.attr().equals("crit_chance") || st.attr().equals("lifesteal")
                || st.attr().equals("crit_damage") || name.equals("knockback_resistance");
        String num = String.format(Locale.ROOT, pct ? "+%.1f%%" : "+%.2f", pct ? v * 100 : v);
        return Component.literal(num + " ").append(Component.translatable("hukhong.honcot.stat." + name)).withStyle(ChatFormatting.GREEN);
    }
}
