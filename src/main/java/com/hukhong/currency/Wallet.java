package com.hukhong.currency;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Vi Tu Tinh = attachment {@link WalletAttachment#TU_TINH} (Ha). Khong con dung dong do trong tui.
 * Dong do Tu Tinh cu trong tui duoc quy doi vao vi khi dang nhap ({@link #convertInventory}).
 */
public final class Wallet {
    private Wallet() {}

    /** So du (Ha). Dung duoc ca phia client (attachment da dong bo). */
    public static long count(Player p) { return p.getData(WalletAttachment.TU_TINH.get()); }

    public static void set(Player p, long v) { p.setData(WalletAttachment.TU_TINH.get(), Math.max(0, v)); }

    /** Tra price Ha tu vi. false neu khong du. Chi goi tren server. */
    public static boolean pay(Player p, long price) {
        if (!WalletMath.canPay(count(p), price)) return false;
        set(p, WalletMath.debit(count(p), price));
        return true;
    }

    /** Nhan v Ha vao vi. */
    public static void give(Player p, long v) { set(p, WalletMath.credit(count(p), v)); }

    // ---- Hon Tinh (vi rieng, don vi = 1 vien) ----

    public static long honTinh(Player p) { return p.getData(WalletAttachment.HON_TINH.get()); }

    public static void setHonTinh(Player p, long v) { p.setData(WalletAttachment.HON_TINH.get(), Math.max(0, v)); }

    /** Tru n Hon Tinh tu vi. false neu khong du. Chi goi tren server. */
    public static boolean payHonTinh(Player p, long n) {
        if (!WalletMath.canPay(honTinh(p), n)) return false;
        setHonTinh(p, WalletMath.debit(honTinh(p), n));
        return true;
    }

    /** Cong n Hon Tinh vao vi + popup icon (action bar). */
    public static void grantHonTinh(net.minecraft.server.level.ServerPlayer p, long n) {
        if (n <= 0) return;
        setHonTinh(p, WalletMath.credit(honTinh(p), n));
        p.displayClientMessage(net.minecraft.network.chat.Component.translatableWithFallback("hukhong.hontinh.gain", "+%s Hồn Tinh",
                TuTinhText.honTinh(n)), true);
    }

    public static final double SHARE_RADIUS = 32;

    /** Boss chet: nguoi giet (neu la nguoi choi) + moi nguoi trong 32 block deu nhan n; khong ai -> roi vat pham (nhat len se tu doi vao vi). */
    public static void grantHonTinhBoss(net.minecraft.server.level.ServerLevel sl, net.minecraft.world.phys.Vec3 at,
                                        net.minecraft.world.damagesource.DamageSource src, long n) {
        java.util.Set<java.util.UUID> done = new java.util.HashSet<>();
        if (src != null && src.getEntity() instanceof net.minecraft.server.level.ServerPlayer k) { grantHonTinh(k, n); done.add(k.getUUID()); }
        for (net.minecraft.server.level.ServerPlayer p : sl.players()) {
            if (p.isSpectator() || done.contains(p.getUUID()) || p.position().distanceToSqr(at) > SHARE_RADIUS * SHARE_RADIUS) continue;
            grantHonTinh(p, n);
            done.add(p.getUUID());
        }
        if (done.isEmpty()) sl.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(sl, at.x, at.y + 0.5, at.z,
                new ItemStack(com.hukhong.item.ModItems.HON_TINH.get(), (int) Math.min(n, 99))));
    }

    /** Doi moi vat pham Hon Tinh trong tui (ca tay phu) vao vi. Tra ve so vien da chuyen. */
    public static long convertHonTinhInventory(Player p) {
        Inventory inv = p.getInventory();
        long n = 0;
        Item it = com.hukhong.item.ModItems.HON_TINH.get();
        List<ItemStack> all = new ArrayList<>(inv.items);
        all.addAll(inv.offhand);
        for (ItemStack s : all) if (s.is(it)) { n += s.getCount(); s.setCount(0); }
        if (n > 0 && p instanceof net.minecraft.server.level.ServerPlayer sp) grantHonTinh(sp, n);
        else if (n > 0) setHonTinh(p, WalletMath.credit(honTinh(p), n));
        return n;
    }

    /** Doi moi dong Tu Tinh trong tui (Ha/Trung/Thuong, ca tay phu) thanh gia tri vi. Tra ve so Ha da chuyen. */
    public static long convertInventory(Player p) {
        Inventory inv = p.getInventory();
        long[] n = new long[3];
        Item[] items = {CurrencyRegistry.HA.get(), CurrencyRegistry.TRUNG.get(), CurrencyRegistry.THUONG.get()};
        List<ItemStack> all = new ArrayList<>(inv.items);
        all.addAll(inv.offhand);
        for (ItemStack s : all) {
            for (int i = 0; i < 3; i++) {
                if (s.is(items[i])) { n[i] += s.getCount(); s.setCount(0); }
            }
        }
        long v = WalletMath.convert(n[0], n[1], n[2]);
        if (v > 0) give(p, v);
        return v;
    }

    /** Doi v Ha ra cac chong tien (dang chuan), dung cho quai roi khi khong co nguoi nhat (ra dat). */
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
