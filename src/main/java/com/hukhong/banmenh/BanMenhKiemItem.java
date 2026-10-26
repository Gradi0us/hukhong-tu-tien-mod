package com.hukhong.banmenh;

import com.hukhong.core.ModAttachments;
import com.hukhong.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/** Bản Mệnh Kiếm: 1 item, loại/giai đoạn lưu ở data component; model chọn bằng custom_model_data. */
public class BanMenhKiemItem extends SwordItem {
    public BanMenhKiemItem() {
        super(Tiers.IRON, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.RARE)
                .attributes(SwordItem.createAttributes(Tiers.IRON, 2, -2.4F)));
    }

    public static BanMenhType typeOf(ItemStack s) {
        String id = s.get(ModItems.BAN_MENH_TYPE.get());
        return id == null ? null : BanMenhType.byId(id);
    }

    public static int stageOf(ItemStack s) {
        Integer v = s.get(ModItems.BAN_MENH_STAGE.get());
        return v == null ? 0 : Math.max(0, Math.min(6, v));
    }

    public static boolean isOwner(ItemStack s, Entity e) {
        UUID o = s.get(ModItems.BOUND_TO.get());
        return o != null && o.equals(e.getUUID());
    }

    @Override
    public Component getName(ItemStack stack) {
        BanMenhType t = typeOf(stack);
        if (t == null) return super.getName(stack);
        return Component.translatable("hukhong.banmenh.name",
                Component.translatable("hukhong.banmenh.type." + t.id()),
                Component.translatable("hukhong.banmenh.stage." + stageOf(stack)));
    }

    /** Giai đoạn do Hồn Tinh quyết định (attachment); cảnh giới chỉ là trần. Đồng bộ item theo attachment. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer p) || !isOwner(stack, p)) return;
        if (entity.tickCount % 20 != 0) return;
        BanMenhData d = p.getData(BanMenhAttachments.BAN_MENH.get());
        int st = BanMenhMath.effectiveStage(d.stage(), p.getData(ModAttachments.CULTIVATION).level());
        if (stageOf(stack) != st) stack.set(ModItems.BAN_MENH_STAGE.get(), st);
        if (d.stage() != st) p.setData(BanMenhAttachments.BAN_MENH.get(), d.withStage(st));
    }

    /** Tôi Kiếm: cầm kiếm, Hồn Tinh ở tay phụ, chuột phải. */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == net.minecraft.world.InteractionHand.MAIN_HAND && player.getOffhandItem().is(ModItems.HON_TINH.get())) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) BanMenhService.tryEvolve(sp, stack, true);
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, net.minecraft.world.item.TooltipFlag flag) {
        BanMenhType t = typeOf(stack);
        if (t == null) return;
        int st = stageOf(stack);
        tip.add(Component.translatable("hukhong.banmenh.tip.type", Component.translatable("hukhong.banmenh.type." + t.id()))
                .withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("hukhong.banmenh.tip.element", Component.translatable("hukhong.banmenh.element." + t.id()))
                .withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("hukhong.banmenh.tip.stage", Component.translatable("hukhong.banmenh.stage." + st),
                String.format("%.2f", BanMenhService.QUALITY[st])).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("hukhong.banmenh.fx." + t.id() + ".0").withStyle(ChatFormatting.GREEN));
        if (st >= 2) tip.add(Component.translatable("hukhong.banmenh.fx." + t.id() + ".1").withStyle(ChatFormatting.GREEN));
        if (st >= 4) tip.add(Component.translatable("hukhong.banmenh.fx." + t.id() + ".2").withStyle(ChatFormatting.GREEN));
        if (st < BanMenhMath.MAX_STAGE) tip.add(Component.translatableWithFallback("hukhong.banmenh.tip.evolve",
                "Tôi Kiếm: tay phụ cầm Hồn Tinh + chuột phải (hoặc Shift+chuột phải lên đe): cần %s Hồn Tinh, trần theo cảnh giới (cấp %s).",
                BanMenhMath.tinhCost(st + 1), BanMenhMath.STAGE_LEVELS[st + 1]).withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.translatable("hukhong.banmenh.tip.bound").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
