package com.hukhong.phapkhi;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Một Pháp Khí (loại x phẩm). Click phải: dùng; Shift+click phải (phù Công): đổi Toàn Lực / Kỹ Năng. Hết lần thì vỡ. */
public final class PhapKhiItem extends Item {
    public final PhapKhiKind kind;
    public final PhapKhiTier tier;

    public PhapKhiItem(PhapKhiKind kind, PhapKhiTier tier, Properties props) {
        super(props);
        this.kind = kind;
        this.tier = tier;
    }

    public int maxUses() { return kind.maxUses(tier); }

    public static int usesLeft(ItemStack s) {
        int max = s.getItem() instanceof PhapKhiItem i ? i.maxUses() : 0;
        return Math.min(max, s.getOrDefault(PhapKhiRegistry.USES.get(), max));
    }

    public static boolean skillMode(ItemStack s) { return s.getOrDefault(PhapKhiRegistry.SKILL_MODE.get(), false); }

    @Override
    public Component getName(ItemStack stack) {
        MutableComponent n = Component.translatableWithFallback(kind.langKey(), kind.vi).withColor(tier.rgb);
        return n.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY)).append(tierName());
    }

    public MutableComponent tierName() {
        return Component.translatableWithFallback("hukhong.phapkhi.tier." + tier.id, tier.vi + " Phẩm").withColor(tier.rgb);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("hukhong.phapkhi.tip.tier", "Phẩm: %s", tierName()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatableWithFallback("hukhong.phapkhi.tip.uses", "Còn %s lần", usesLeft(stack) + "/" + maxUses())
                .withStyle(ChatFormatting.YELLOW));
        tip.add(Component.translatableWithFallback("hukhong.phapkhi.tip." + kind.id, PhapKhiText.tip(kind)).withStyle(ChatFormatting.AQUA));
        if (kind.cat == PhapKhiKind.Cat.ATTACK) {
            boolean sk = skillMode(stack);
            tip.add(Component.translatableWithFallback(sk ? "hukhong.phapkhi.mode.skill" : "hukhong.phapkhi.mode.full",
                    sk ? "Chế độ: Kỹ Năng" : "Chế độ: Toàn Lực").withColor(kind.rgb));
            tip.add(Component.translatableWithFallback("hukhong.phapkhi.tip.toggle", "Shift + Chuột phải: đổi chế độ")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tip.add(Component.translatableWithFallback("hukhong.phapkhi.tip.anyone", "Mọi đạo đều dùng được").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack s) { return usesLeft(s) < maxUses(); }

    @Override
    public int getBarWidth(ItemStack s) { return Math.round(13f * usesLeft(s) / Math.max(1, maxUses())); }

    @Override
    public int getBarColor(ItemStack s) { return tier.rgb; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(st);
        if (kind.cat == PhapKhiKind.Cat.ATTACK && sp.isShiftKeyDown()) {
            boolean now = !skillMode(st);
            st.set(PhapKhiRegistry.SKILL_MODE.get(), now);
            sp.displayClientMessage(Component.translatableWithFallback(now ? "hukhong.phapkhi.mode.skill" : "hukhong.phapkhi.mode.full",
                    now ? "Chế độ: Kỹ Năng" : "Chế độ: Toàn Lực").withColor(kind.rgb), true);
            return InteractionResultHolder.sidedSuccess(st, false);
        }
        if (!activate(sp, st)) return InteractionResultHolder.fail(st);
        return InteractionResultHolder.sidedSuccess(st, false);
    }

    /** Thi triển + trừ 1 lần + hồi chiêu cả loại. false = không thi triển được (không trừ lần). Dùng cho cả GameTest. */
    public static boolean activate(ServerPlayer sp, ItemStack st) {
        if (!(st.getItem() instanceof PhapKhiItem it) || usesLeft(st) <= 0) return false;
        boolean ok = switch (it.kind.cat) {
            case ATTACK -> PhapKhiAttack.fire(sp, it.kind, it.tier, skillMode(st));
            case DEFENSE -> PhapKhiDefense.use(sp, it.kind, it.tier);
            case ZONE -> PhapKhiZones.place(sp, it.kind, it.tier);
        };
        if (!ok) return false;
        int left = usesLeft(st) - 1;
        if (left <= 0) {
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.9f, 1.1f);
            sp.displayClientMessage(Component.translatableWithFallback("hukhong.phapkhi.broke", "%s đã vỡ vụn.", st.getHoverName()), true);
            st.shrink(1);
        } else {
            st.set(PhapKhiRegistry.USES.get(), left);
        }
        for (PhapKhiTier t : PhapKhiTier.values()) sp.getCooldowns().addCooldown(PhapKhiRegistry.item(it.kind, t), it.kind.cooldownTicks());
        return true;
    }
}
