package com.hukhong.currency;

import com.hukhong.HuKhong;
import com.hukhong.entity.HuMob;
import com.hukhong.gates.RewardGate;
import com.hukhong.guild.Bosses;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Random;

/**
 * Quai Hu Hoa roi Tu Tinh nho (theo cap); boss / Chap Hanh Gia roi nhieu hon. Quai tu spawner (RewardGate) = 0.
 * Nguoi giet: tien vao thang vi truc tiep. Khong co nguoi giet: roi vat pham nhu cu.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CurrencyEvents {
    private CurrencyEvents() {}

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide) return;
        boolean boss = Bosses.isBoss(v);
        if (!boss && !(v instanceof HuMob)) return;
        if (!RewardGate.canReward(v)) return; // spawner / thi luyen = 0
        Random r = new Random(v.getRandom().nextLong());
        long amount;
        if (boss) amount = TuTinh.bossDrop(Bosses.levelOf(v), Bosses.coinMult(v), r);
        else if (r.nextDouble() < TuTinh.mobDropChance()) amount = TuTinh.mobDrop(Bosses.levelOf(v), r);
        else return;
        if (e.getSource().getEntity() instanceof ServerPlayer sp) {
            Wallet.give(sp, amount);
            sp.displayClientMessage(Component.translatable("hukhong.tutinh.gain", TuTinhText.of(amount)), true);
            return;
        }
        for (ItemStack s : Wallet.stacks(amount)) e.getDrops().add(new ItemEntity(v.level(), v.getX(), v.getY(), v.getZ(), s));
    }

    /** Dang nhap: Tu Tinh dang de trong tui (ban cu / hoc nhap) duoc quy doi vao vi. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        long v = Wallet.convertInventory(sp);
        if (v > 0) sp.displayClientMessage(Component.translatable("hukhong.tutinh.converted", TuTinhText.of(v)), false);
    }
}
