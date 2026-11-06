package com.hukhong.currency;

import com.hukhong.HuKhong;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** 2026-11-06: ban va phong phap (0 XP) tra bang Ha Pham Linh Thach. Kiem tra qua Slot/Menu API (di qua mixin). */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class SpiritStoneTollGameTests {
    private SpiritStoneTollGameTests() {}

    private static ServerPlayer fresh(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "hk-toll"));
        p.getInventory().clearContent();
        return p;
    }

    @GameTest(template = "empty")
    public static void tollCostsAndPayment(GameTestHelper h) {
        ServerPlayer p = fresh(h);
        if (SpiritStoneToll.anvilCost(5) != 5) { h.fail("phi ban 5 cap != 5"); return; }
        if (SpiritStoneToll.enchantCost(3) != 12) { h.fail("phi phong phap o 3 != 12"); return; }
        Item stone = SpiritStoneToll.stone();
        p.getInventory().add(new ItemStack(stone, 10));
        p.getInventory().offhand.set(0, new ItemStack(stone, 5));
        if (SpiritStoneToll.count(p) != 15) { h.fail("dem linh thach sai: " + SpiritStoneToll.count(p)); return; }
        if (SpiritStoneToll.canPay(p, 16) || !SpiritStoneToll.canPay(p, 15)) { h.fail("canPay sai"); return; }
        if (SpiritStoneToll.pay(p, 16) || SpiritStoneToll.count(p) != 15) { h.fail("tru khi thieu van tru"); return; }
        if (!SpiritStoneToll.pay(p, 12) || SpiritStoneToll.count(p) != 3) { h.fail("tru 12 sai"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void anvilChargesStonesNotXp(GameTestHelper h) {
        ServerPlayer p = fresh(h);
        AnvilMenu menu = new AnvilMenu(0, p.getInventory(), ContainerLevelAccess.NULL);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.setDamageValue(50);
        menu.getSlot(AnvilMenu.INPUT_SLOT).set(sword);
        menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.IRON_INGOT));
        menu.createResult();
        int cost = menu.getCost();
        Slot out = menu.getSlot(AnvilMenu.RESULT_SLOT);
        if (cost <= 0 || out.getItem().isEmpty()) { h.fail("khong co ket qua ban: cost=" + cost); return; }
        if (out.mayPickup(p)) { h.fail("cho nhac do khi chua co linh thach"); return; }
        p.getInventory().add(new ItemStack(SpiritStoneToll.stone(), cost));
        if (!out.mayPickup(p)) { h.fail("du linh thach ma van khong nhac duoc"); return; }
        out.onTake(p, out.getItem().copy());
        if (SpiritStoneToll.count(p) != 0) { h.fail("ban khong tru linh thach: con " + SpiritStoneToll.count(p)); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void enchantChargesStonesNotXp(GameTestHelper h) {
        ServerPlayer p = fresh(h);
        EnchantmentMenu menu = new EnchantmentMenu(0, p.getInventory(),
                ContainerLevelAccess.create(h.getLevel(), h.absolutePos(BlockPos.ZERO)));
        menu.getSlot(0).set(new ItemStack(Items.IRON_SWORD));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 3));
        if (menu.costs[0] <= 0) { h.fail("khong co phi enchant de test"); return; }
        if (menu.clickMenuButton(p, 0)) { h.fail("phong phap chay khi chua co linh thach"); return; }
        if (menu.getSlot(1).getItem().getCount() != 3) { h.fail("lapis bi tru khi that bai"); return; }
        p.getInventory().add(new ItemStack(SpiritStoneToll.stone(), SpiritStoneToll.enchantCost(1)));
        if (!menu.clickMenuButton(p, 0)) { h.fail("du linh thach ma phong phap khong chay"); return; }
        if (SpiritStoneToll.count(p) != 0) { h.fail("phong phap khong tru linh thach"); return; }
        if (menu.getSlot(1).getItem().getCount() != 2) { h.fail("lapis khong tru 1"); return; }
        h.succeed();
    }
}
