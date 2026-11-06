package com.hukhong.currency;

import com.hukhong.resource.ResourceRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 2026-11-06: phi dung luong Linh Thach thay cho cap XP vanilla (mod da bo XP).
 * Tra bang Ha Pham Linh Thach (item linh_thach_ha) trong tui (ca tay phu).
 * Phi tinh o {@link #anvilCost} va {@link #enchantCost}; cac hook Mixin goi {@link #pay} / {@link #canPay}.
 */
public final class SpiritStoneToll {
    private SpiritStoneToll() {}

    /** Ban da (anvil): moi cap phi vanilla = ANVIL_PER_LEVEL vien. */
    public static final int ANVIL_PER_LEVEL = 1;
    /** Phong phap (enchanting table): o thu i (0..2) co cap yeu cau i+1 -> (i+1) * ENCHANT_STEP vien. */
    public static final int ENCHANT_STEP = 4;

    public static final String MATERIAL_ID = "linh_thach_ha";

    /** Pending phi cho lan bam nut phong phap hien tai (chi dung tren server thread). */
    private static int pendingEnchantCost = 0;

    public static int anvilCost(int levels) { return Math.max(0, levels) * ANVIL_PER_LEVEL; }

    /** levels = cap yeu cau cua o phong phap (= id + 1). */
    public static int enchantCost(int levels) { return Math.max(0, levels) * ENCHANT_STEP; }

    public static Item stone() { return ResourceRegistry.MATERIALS.get(MATERIAL_ID).get(); }

    /** So vien Linh Thach trong tui + tay phu. */
    public static int count(Player p) {
        Item it = stone();
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(it)) n += s.getCount();
        for (ItemStack s : p.getInventory().offhand) if (s.is(it)) n += s.getCount();
        return n;
    }

    public static boolean canPay(Player p, int n) { return n <= 0 || count(p) >= n; }

    /** Tru n vien. false (khong doi gi) neu khong du. */
    public static boolean pay(Player p, int n) {
        if (n <= 0) return true;
        if (!canPay(p, n)) return false;
        Item it = stone();
        int left = n;
        for (var list : java.util.List.of(p.getInventory().items, p.getInventory().offhand)) {
            for (ItemStack s : list) {
                if (left <= 0) break;
                if (!s.is(it)) continue;
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        return true;
    }

    /** Thong bao thieu linh thach (action bar). Chi goi tren server. */
    public static void notifyNeed(ServerPlayer p, int n) {
        p.displayClientMessage(Component.translatableWithFallback("hukhong.toll.need_stones",
                "Cần %s Hạ Phẩm Linh Thạch", n), true);
    }

    /** Cong dung cho hook phong phap: dat phi cua lan bam hien tai. */
    public static void beginEnchant(int buttonId) {
        pendingEnchantCost = (buttonId >= 0 && buttonId < 3) ? enchantCost(buttonId + 1) : 0;
    }

    /**
     * Thay the doc XP level cua nguoi choi trong clickMenuButton: tra ve gia tri du lon neu du linh thach,
     * nguoc lai tra ve level thuc (vanilla se tu tu choi vi 0 XP).
     */
    public static int enchantGate(Player p) {
        if (p.getAbilities().instabuild || pendingEnchantCost <= 0) return Integer.MAX_VALUE;
        if (canPay(p, pendingEnchantCost)) return Integer.MAX_VALUE;
        if (p instanceof ServerPlayer sp) notifyNeed(sp, pendingEnchantCost);
        return p.experienceLevel;
    }

    /** Tru phi khi phong phap thanh cong (levels = cap o). */
    public static void chargeEnchant(Player p, int levels) {
        if (p.getAbilities().instabuild) return;
        pay(p, enchantCost(levels));
    }
}
