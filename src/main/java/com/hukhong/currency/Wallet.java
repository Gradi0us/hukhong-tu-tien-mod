package com.hukhong.currency;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Vi Tu Tinh = cac dong trong tui do (36 o chinh). Tra tien tu dong doi tien thoi. */
public final class Wallet {
    private Wallet() {}

    private static long count(Inventory inv, Item it) {
        long n = 0;
        for (ItemStack s : inv.items) if (s.is(it)) n += s.getCount();
        return n;
    }

    /** Tong gia tri (Ha). */
    public static long count(Player p) {
        Inventory inv = p.getInventory();
        return TuTinh.value(count(inv, CurrencyRegistry.HA.get()), count(inv, CurrencyRegistry.TRUNG.get()), count(inv, CurrencyRegistry.THUONG.get()));
    }

    private static void take(Inventory inv, Item it, long n) {
        for (ItemStack s : inv.items) {
            if (n <= 0) return;
            if (!s.is(it)) continue;
            int t = (int) Math.min(n, s.getCount());
            s.shrink(t);
            n -= t;
        }
    }

    /** Tra price Ha (kem doi tien thoi). false neu khong du. Chi goi tren server. */
    public static boolean pay(Player p, long price) {
        if (price <= 0) return true;
        Inventory inv = p.getInventory();
        TuTinh.Plan plan = TuTinh.pay(count(inv, CurrencyRegistry.HA.get()), count(inv, CurrencyRegistry.TRUNG.get()),
                count(inv, CurrencyRegistry.THUONG.get()), price);
        if (plan == null) return false;
        take(inv, CurrencyRegistry.HA.get(), plan.ha());
        take(inv, CurrencyRegistry.TRUNG.get(), plan.trung());
        take(inv, CurrencyRegistry.THUONG.get(), plan.thuong());
        give(p, plan.change());
        return true;
    }

    /** Nhan v Ha (dang chuan), tui day thi roi xuong dat. */
    public static void give(Player p, long v) {
        for (ItemStack s : stacks(v)) if (!p.getInventory().add(s)) p.drop(s, false);
    }

    /** Doi v Ha ra cac chong tien (dang chuan). */
    public static List<ItemStack> stacks(long v) {
        long[] c = TuTinh.canonical(v);
        List<ItemStack> out = new ArrayList<>();
        addStacks(out, CurrencyRegistry.THUONG.get(), c[2]);
        addStacks(out, CurrencyRegistry.TRUNG.get(), c[1]);
        addStacks(out, CurrencyRegistry.HA.get(), c[0]);
        return out;
    }

    private static void addStacks(List<ItemStack> out, Item it, long n) {
        while (n > 0) { int t = (int) Math.min(n, 99); out.add(new ItemStack(it, t)); n -= t; }
    }
}
