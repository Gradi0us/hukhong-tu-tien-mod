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

    static boolean isCoin(ItemStack s) {
        return s.is(CurrencyRegistry.HA.get()) || s.is(CurrencyRegistry.TRUNG.get()) || s.is(CurrencyRegistry.THUONG.get());
    }

    /** Nhat Tu Tinh duoi dat: khong vao tui, cong thang vao vi + thong bao kem icon. */
    @SubscribeEvent
    public static void onPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre e) {
        if (!(e.getPlayer() instanceof ServerPlayer sp)) return;
        ItemEntity ie = e.getItemEntity();
        ItemStack s = ie.getItem();
        if (ie.hasPickUpDelay()) return;
        if (s.is(com.hukhong.item.ModItems.HON_TINH.get())) {
            e.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
            sp.take(ie, s.getCount());
            long n = s.getCount();
            ie.discard();
            Wallet.grantHonTinh(sp, n);
            return;
        }
        if (!isCoin(s)) return;
        long v = WalletMath.convert(s.is(CurrencyRegistry.HA.get()) ? s.getCount() : 0,
                s.is(CurrencyRegistry.TRUNG.get()) ? s.getCount() : 0, s.is(CurrencyRegistry.THUONG.get()) ? s.getCount() : 0);
        e.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
        sp.take(ie, s.getCount());
        ie.discard();
        Wallet.give(sp, v);
        sp.displayClientMessage(Component.translatable("hukhong.tutinh.gain", TuTinhText.of(v)), true);
    }

    /** Moi giay: Tu Tinh lot vao tui (lay tu ruong, phan thuong nhiem vu...) tu doi vao vi. */
    @SubscribeEvent
    public static void onTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || sp.tickCount % 20 != 7) return;
        var carried = sp.containerMenu.getCarried();
        if (carried != null && (isCoin(carried) || carried.is(com.hukhong.item.ModItems.HON_TINH.get()))) return; // dang cam tren chuot
        if (Wallet.convertHonTinhInventory(sp) > 0) sp.inventoryMenu.broadcastChanges();
        long v = Wallet.convertInventory(sp);
        if (v > 0) {
            sp.inventoryMenu.broadcastChanges();
            sp.displayClientMessage(Component.translatable("hukhong.tutinh.gain", TuTinhText.of(v)), true);
        }
    }

    /** Dang nhap: Tu Tinh dang de trong tui (ban cu / hoc nhap) duoc quy doi vao vi. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        Wallet.convertHonTinhInventory(sp);
        long v = Wallet.convertInventory(sp);
        if (v > 0) sp.displayClientMessage(Component.translatable("hukhong.tutinh.converted", TuTinhText.of(v)), false);
    }
}
