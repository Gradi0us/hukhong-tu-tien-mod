package com.hukhong.craft;

import com.hukhong.HuKhong;
import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.guild.Bosses;
import com.hukhong.profession.Profession;
import com.hukhong.profession.ProfessionService;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Su kien he che tao: dong bo / migration so cong thuc khi dang nhap va hoi sinh, Chu Sa tu quang redstone,
 * quyen pho tu boss, dong tooltip phat chat.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CraftEvents {
    private CraftEvents() {}

    /** Ty le them 1 Chu Sa khi dap quang redstone. */
    public static final float CHU_SA_CHANCE = 0.30f;
    /** Ty le boss roi 1 quyen pho. */
    public static final double BOSS_SCROLL_CHANCE = 0.04;

    /**
     * Migration nguoi choi cu: lan dau dang nhap sau cap nhat, tu hoc moi cong thuc bac <= cap/4+1 cua nghe.
     * Idempotent, nen chay moi lan dang nhap.
     */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Profession prof = ProfessionService.profession(p);
        if (prof != Profession.NONE) RecipeBook.autoLearnBasics(p, prof);
        RecipeBook.sendTo(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) RecipeBook.sendTo(p);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof ServerLevel lv) || e.getPlayer().isCreative()) return;
        BlockState st = e.getState();
        if (!st.is(Blocks.REDSTONE_ORE) && !st.is(Blocks.DEEPSLATE_REDSTONE_ORE)) return;
        var silk = lv.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        if (EnchantmentHelper.getItemEnchantmentLevel(silk, e.getPlayer().getMainHandItem()) > 0) return;
        if (lv.getRandom().nextFloat() >= CHU_SA_CHANCE) return;
        var p = e.getPos();
        lv.addFreshEntity(new ItemEntity(lv, p.getX() + .5, p.getY() + .5, p.getZ() + .5, new ItemStack(CraftRegistry.CHU_SA.get())));
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        var v = e.getEntity();
        if (v.level().isClientSide || !Bosses.isBoss(v)) return;
        if (v.getRandom().nextDouble() >= BOSS_SCROLL_CHANCE) return;
        int lv = Bosses.levelOf(v);
        int maxTier = lv >= 40 ? 5 : lv >= 20 ? 3 : 2;
        Profession prof = switch (v.getRandom().nextInt(3)) {
            case 0 -> Profession.LUYEN_DAN;
            case 1 -> Profession.PHU_VAN;
            default -> Profession.DOAN_TAO;
        };
        ItemStack s = RecipeBook.randomScroll(v.getRandom(), prof, 2, maxTier, null);
        if (!s.isEmpty()) e.getDrops().add(new ItemEntity(v.level(), v.getX(), v.getY(), v.getZ(), s));
    }

    /** Dong tooltip cho moi stack co phat che tao (craft_quality hoac pill_quality cu). */
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (!s.has(CraftRegistry.CRAFT_QUALITY.get()) && !s.has(AlchemyRegistry.PILL_QUALITY.get())) return;
        e.getToolTip().add(CraftQuality.tooltip(CraftQuality.ordinal(s), s.get(CraftRegistry.CRAFTER.get())));
    }
}
