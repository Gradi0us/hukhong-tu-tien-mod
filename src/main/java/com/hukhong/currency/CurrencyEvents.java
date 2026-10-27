package com.hukhong.currency;

import com.hukhong.HuKhong;
import com.hukhong.entity.HuMob;
import com.hukhong.gates.RewardGate;
import com.hukhong.guild.Bosses;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.Random;

/** Quai Hu Hoa roi Ha pham Tu Tinh nho (theo cap); boss / Chap Hanh Gia roi Trung pham. Quai tu spawner (RewardGate) = 0. */
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
        for (ItemStack s : Wallet.stacks(amount)) e.getDrops().add(new ItemEntity(v.level(), v.getX(), v.getY(), v.getZ(), s));
    }
}
