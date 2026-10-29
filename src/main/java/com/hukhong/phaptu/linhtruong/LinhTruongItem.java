package com.hukhong.phaptu.linhtruong;

import com.hukhong.artifact.ArtifactService;
import com.hukhong.banmenh.BanMenhMath;
import com.hukhong.core.Path;
import com.hukhong.item.ModItems;
import com.hukhong.phaptu.PhapElement;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/** Linh Trượng: Bản Mệnh Trượng của Pháp Tu. Nguyên tố ở BAN_MENH_TYPE, giai đoạn ở BAN_MENH_STAGE, chủ ở BOUND_TO. */
public class LinhTruongItem extends Item {
    public LinhTruongItem() {
        super(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.RARE)
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false)));
    }

    @Override public int getMaxDamage(ItemStack stack) { return 0; }
    @Override public boolean isBarVisible(ItemStack stack) { return false; }
    @Override public boolean isDamageable(ItemStack stack) { return false; }

    /** Trượng không dùng để đào / phá khối (đòn chuột trái là tia phép). */
    @Override
    public float getDestroySpeed(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) { return 0f; }

    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state, Level level,
                                  net.minecraft.core.BlockPos pos, Player player) { return false; }

    public static PhapElement elementOf(ItemStack s) {
        return PhapElement.byId(s.get(ModItems.BAN_MENH_TYPE.get()));
    }

    public static int stageOf(ItemStack s) {
        Integer v = s.get(ModItems.BAN_MENH_STAGE.get());
        return v == null ? 0 : LinhTruongMath.clampStage(v);
    }

    public static boolean isOwner(ItemStack s, Entity e) {
        UUID o = s.get(ModItems.BOUND_TO.get());
        return o != null && o.equals(e.getUUID());
    }

    @Override
    public Component getName(ItemStack stack) {
        PhapElement el = elementOf(stack);
        if (el == null) return super.getName(stack);
        return Component.translatable("hukhong.linhtruong.name", Component.translatable(el.langKey()),
                Component.translatable("hukhong.banmenh.stage." + stageOf(stack)));
    }

    /** Đồng bộ giai đoạn / nguyên tố của item theo người chủ (giai đoạn = ArtifactData.phap, trần theo cảnh giới). */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer p) || !isOwner(stack, p) || p.tickCount % 20 != 0) return;
        if (stack.has(DataComponents.DAMAGE)) stack.remove(DataComponents.DAMAGE);
        LinhTruongService.syncStack(p, stack);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        if (!player.isAlive()) return true;
        if (!player.level().isClientSide && player instanceof ServerPlayer sp) {
            sp.containerMenu.sendAllDataToRemote();
            sp.inventoryMenu.sendAllDataToRemote();
            player.displayClientMessage(Component.translatableWithFallback("hukhong.linhtruong.msg.no_drop",
                    "Linh Trượng gắn liền với chủ nhân, không thể vứt bỏ hay cất vào vật chứa."), true);
        }
        return false;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (player.isCreative()) return false;
        if (slot.container != player.getInventory()) return true;
        ItemStack in = slot.getItem();
        return in.has(DataComponents.BUNDLE_CONTENTS) || in.has(DataComponents.CONTAINER);
    }

    /** Cường hóa: cầm trượng + Hồn Tinh ở tay phụ + chuột phải (cùng luồng Pháp Khí ở màn K / Shift+chuột phải). */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && (player.isShiftKeyDown() || player.getOffhandItem().is(ModItems.HON_TINH.get()))) {
            if (!level.isClientSide && player instanceof ServerPlayer sp && isOwner(stack, sp)) {
                if (ArtifactService.tryUpgrade(sp, Path.PHAP)) LinhTruongService.syncStack(sp, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, net.minecraft.world.item.TooltipFlag flag) {
        PhapElement el = elementOf(stack);
        if (el == null) return;
        int st = stageOf(stack);
        tip.add(Component.translatable("hukhong.linhtruong.tip.element", Component.translatable(el.langKey())).withColor(el.rgb));
        tip.add(Component.translatable("hukhong.banmenh.tip.stage", Component.translatable("hukhong.banmenh.stage." + st),
                String.format("%.2f", LinhTruongMath.quality(st))).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatableWithFallback("hukhong.linhtruong.tip.bolt",
                "Chuột trái: bắn tia phép (%s block, hồi %ss) gây %s Phép lực.",
                (int) LinhTruongMath.RANGE, String.format("%.1f", LinhTruongMath.COOLDOWN_TICKS / 20.0),
                String.format("%.0f%%", LinhTruongMath.coef(st) * LinhTruongMath.quality(st) * 100)).withStyle(ChatFormatting.GREEN));
        tip.add(Component.translatableWithFallback("hukhong.linhtruong.tip.skills",
                "Kỹ năng Pháp Tu Q/E/R/T phát ra từ đầu trượng.").withStyle(ChatFormatting.GREEN));
        if (st < BanMenhMath.MAX_STAGE) tip.add(Component.translatableWithFallback("hukhong.linhtruong.tip.evolve",
                "Cường hóa: tay phụ cầm Hồn Tinh + chuột phải (cần %s Hồn Tinh, trần theo cảnh giới cấp %s).",
                BanMenhMath.tinhCost(st + 1), BanMenhMath.STAGE_LEVELS[st + 1]).withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.translatable("hukhong.linhtruong.tip.bound").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
