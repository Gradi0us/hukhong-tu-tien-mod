package com.hukhong.alchemy;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.core.ModAttachments;
import com.hukhong.craft.CraftQuality;
import com.hukhong.dodge.DodgeService;
import com.hukhong.gates.Resources;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Đan chiến đấu (PLAN-v1-profession-crafting §1.4). Dùng được trong thế tu luyện (tag stance_consumable).
 * Mọi đan chiến đấu chung một hồi chiêu {@link #COOLDOWN} (30 giây). Cường độ = hệ số phẩm × hệ số Đan Độc.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public class CombatPillItem extends Item {
    public enum Kind {
        /** Hồi 30% linh lực tối đa. */
        HOI_KHI(2),
        /** Sức Mạnh I 20 giây, sau đó Suy Yếu 10 giây. */
        CUONG_HUYET(3),
        /** Kháng Cự II 15 giây. */
        KIM_CUONG(4),
        /** Hồi đầy Thể Lực + Tốc Độ I 10 giây. */
        PHONG_HANH(2);

        public final int tier;
        Kind(int tier) { this.tier = tier; }
    }

    public static final int COOLDOWN = 600;
    /** Người chơi -> tick game khi nào thì áp Suy Yếu (sau Cuồng Huyết). */
    private static final Map<UUID, Long> WEAK_AT = new ConcurrentHashMap<>();

    private final Kind kind;

    public CombatPillItem(Kind kind, Properties props) {
        super(props);
        this.kind = kind;
    }

    public Kind kind() { return kind; }

    private static int ticks(int base, double m) { return Math.max(1, (int) Math.round(base * m)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (!DanDoc.canUse(sp)) return InteractionResultHolder.fail(stack);
        var q = AlchemyRegistry.qualityOf(stack);
        double m = CraftQuality.mult(q.ordinal()) * AlchemyMath.effectMult(DanDoc.get(sp));
        long now = sp.level().getGameTime();
        switch (kind) {
            case HOI_KHI -> {
                int max = CultivationService.get(sp).resourceMax();
                Resources.add(sp, (int) Math.round(max * 0.30 * m));
            }
            case CUONG_HUYET -> {
                int dur = ticks(400, m);
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dur, 0));
                WEAK_AT.put(sp.getUUID(), now + dur);
            }
            case KIM_CUONG -> sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks(300, m), 1));
            case PHONG_HANH -> {
                sp.setData(ModAttachments.DODGE.get(), DodgeService.data(sp).withStamina(DodgeService.max(sp), now));
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks(200, m), 0));
            }
        }
        DanDoc.consume(sp, 0.8, kind.tier, q);
        for (var s : AlchemyRegistry.COMBAT_PILLS) sp.getCooldowns().addCooldown(s.get(), COOLDOWN);
        stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || WEAK_AT.isEmpty()) return;
        Long at = WEAK_AT.get(p.getUUID());
        if (at != null && p.level().getGameTime() >= at) {
            WEAK_AT.remove(p.getUUID());
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("hukhong.combat_pill." + kind.name().toLowerCase()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("hukhong.combat_pill.cooldown").withStyle(ChatFormatting.DARK_GRAY));
    }
}
