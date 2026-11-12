package com.hukhong.phapkhi.hothe;

import com.hukhong.HuKhong;
import com.hukhong.guild.Bosses;
import com.hukhong.phapkhi.PhapKhiTier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Random;

/**
 * Luật hộ thể: (1) chỉ pháp khí hộ thể được vào 4 ô giáp; (2) Tịnh Tâm Quan rút ngắn hiệu ứng xấu;
 * (3) rơi nhỏ từ boss. Creative/spectator được miễn ràng buộc ô giáp.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class HoTheEvents {
    private HoTheEvents() {}

    /** Tỉ lệ boss rơi 1 món hộ thể (phẩm 1-2). */
    public static final double BOSS_DROP_CHANCE = 0.05;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Đang tự áp lại hiệu ứng đã rút ngắn: tránh vòng lặp Applicable -> addEffect -> Applicable. */
    private static boolean reapplying;

    @SubscribeEvent
    public static void onEquipChange(LivingEquipmentChangeEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getSlot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) enforce(p, e.getSlot());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) for (EquipmentSlot s : ARMOR) enforce(p, s);
    }

    /** Ô giáp {@code s} có đồ không phải hộ thể đúng ô -> gỡ về túi (rơi xuống đất nếu túi đầy). */
    static void enforce(ServerPlayer p, EquipmentSlot s) {
        if (p.isCreative() || p.isSpectator()) return;
        ItemStack st = p.getItemBySlot(s);
        if (st.isEmpty() || (st.getItem() instanceof HoTheArmorItem h && h.kind.slot() == s)) return;
        p.setItemSlot(s, ItemStack.EMPTY);
        ItemStack back = st.copy();
        p.getInventory().add(back);
        if (!back.isEmpty()) p.drop(back, false);
        p.displayClientMessage(Component.translatable("hukhong.hothe.only"), true);
    }

    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable e) {
        if (reapplying || !(e.getEntity() instanceof ServerPlayer p)) return;
        MobEffectInstance in = e.getEffectInstance();
        if (in.getEffect().value().getCategory() != MobEffectCategory.HARMFUL || in.getDuration() <= 1) return;
        HoTheArmorItem h = HoTheArmorItem.worn(p, HoTheKind.TINH_TAM_QUAN);
        if (h == null) return;
        int dur = (int) Math.round(in.getDuration() * (1 - HoTheKind.tinhReduce(h.tier.ordinal())));
        if (dur <= 0) return;
        // Chặn bản gốc, tự áp bản rút ngắn (Added nằm trước khi lưu hiệu ứng nên không sửa được duration tại chỗ).
        e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        reapplying = true;
        try {
            p.addEffect(new MobEffectInstance(in.getEffect(), dur, in.getAmplifier(), in.isAmbient(), in.isVisible(), in.showIcon()));
        } finally {
            reapplying = false;
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        var v = e.getEntity();
        if (v.level().isClientSide || !Bosses.isBoss(v)) return;
        Random r = new Random(v.getRandom().nextLong());
        if (r.nextDouble() >= BOSS_DROP_CHANCE) return;
        HoTheKind k = HoTheKind.values()[r.nextInt(HoTheKind.values().length)];
        PhapKhiTier t = Bosses.levelOf(v) >= 40 ? PhapKhiTier.LINH : PhapKhiTier.PHAM;
        e.getDrops().add(new ItemEntity(v.level(), v.getX(), v.getY(), v.getZ(), HoTheRegistry.stack(k, t)));
    }
}
