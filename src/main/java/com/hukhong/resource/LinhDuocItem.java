package com.hukhong.resource;

import com.hukhong.alchemy.AlchemyMath;
import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.alchemy.DanDoc;
import com.hukhong.core.CultivationService;
import com.hukhong.gates.Resources;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Linh dược (2026-11-06): đan tác dụng tức thời / buff, luyện từ linh thảo mới. */
public class LinhDuocItem extends Item {
    public enum Kind { MANA_35, MANA_80, THANH_TAM, TAY_TUY, KIM_CUONG, BAO_HUYET, PHONG_HANH, VAN_NIEN }

    private final Kind kind;
    private final String id;

    public LinhDuocItem(String id, Kind kind, Properties props) { super(props); this.id = id; this.kind = kind; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        boolean cleanser = kind == Kind.THANH_TAM || kind == Kind.TAY_TUY;
        if (!cleanser && !DanDoc.canUse(sp)) return InteractionResultHolder.fail(stack);
        var q = AlchemyRegistry.qualityOf(stack);
        double mult = AlchemyMath.effectMult(DanDoc.get(sp)) * com.hukhong.craft.CraftQuality.mult(q.ordinal());
        int max = CultivationService.get(sp).resourceMax();
        switch (kind) {
            case MANA_35 -> Resources.add(sp, (int) Math.round(max * 0.35 * mult));
            case MANA_80 -> Resources.add(sp, (int) Math.round(max * 0.80 * mult));
            case THANH_TAM -> {
                com.hukhong.pathrules.MaRules.addAcTinh(sp, -30 * com.hukhong.craft.CraftQuality.mult(q.ordinal()));
                DanDoc.set(sp, DanDoc.get(sp) - 20 * com.hukhong.craft.CraftQuality.mult(q.ordinal()));
            }
            case TAY_TUY -> {
                DanDoc.set(sp, 0);
                for (var e : List.copyOf(sp.getActiveEffects()))
                    if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) sp.removeEffect(e.getEffect());
            }
            case KIM_CUONG -> sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, (int) (2400 * mult), 1));
            case BAO_HUYET -> sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, (int) (1800 * mult), 1));
            case PHONG_HANH -> {
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, (int) (2400 * mult), 1));
                sp.addEffect(new MobEffectInstance(MobEffects.JUMP, (int) (2400 * mult), 1));
            }
            case VAN_NIEN -> {
                sp.heal((float) (sp.getMaxHealth() * 0.6 * mult));
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
            }
        }
        if (!cleanser) DanDoc.consume(sp, 0.8, 2, q);
        if (kind == Kind.THANH_TAM)
            sp.displayClientMessage(Component.translatable("hukhong.linh_duoc.ac_tinh",
                    Math.round(com.hukhong.pathrules.MaRules.data(sp).acTinh), (int) DanDoc.get(sp)), true);
        stack.shrink(1);
        player.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("hukhong.linh_duoc." + id).withStyle(ChatFormatting.GRAY));
    }
}
