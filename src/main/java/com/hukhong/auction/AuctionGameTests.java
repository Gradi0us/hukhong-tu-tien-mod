package com.hukhong.auction;

import com.hukhong.HuKhong;
import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.currency.Wallet;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Dot 7c: escrow / hoan tien / dot phi san Dau Gia Hanh, NPC mua lai, gioi han lo, tu mua, thu giao khi dang nhap. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class AuctionGameTests {
    private AuctionGameTests() {}

    private static final long NOW = 5_000_000_000_000L;

    private static ServerPlayer player(GameTestHelper h, UUID id, long wallet) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(id, "auc-" + id.toString().substring(0, 5)));
        Wallet.set(p, wallet);
        p.getInventory().clearContent();
        return p;
    }

    private static Lot lotOf(AuctionData d, UUID seller) {
        Lot r = null;
        for (Lot l : d.lots.values()) if (seller.equals(l.seller)) r = l;
        return r;
    }

    @GameTest(template = "empty")
    public static void escrowRefundAndFeeBurn(GameTestHelper h) {
        MinecraftServer s = h.getLevel().getServer();
        AuctionData d = AuctionData.get(s);
        ServerPlayer a = player(h, UUID.randomUUID(), 1000), b = player(h, UUID.randomUUID(), 1000), c = player(h, UUID.randomUUID(), 1000);
        long burned0 = d.burned;
        Component e = AuctionService.list(a, new ItemStack(AlchemyRegistry.LINH_THAO.get(), 8), 100, 0, 0, NOW);
        if (e != null) h.fail("list failed: " + e.getString());
        Lot lot = lotOf(d, a.getUUID());
        if (lot == null) h.fail("lot not stored");
        if (Wallet.count(a) != 999) h.fail("listing fee 1% (min 1) expected, wallet=" + Wallet.count(a));
        if (AuctionService.bid(a, lot.id, 200, NOW + 1) == null) h.fail("self-buy allowed");
        if (AuctionService.bid(b, lot.id, 99, NOW + 1) == null) h.fail("bid below start accepted");
        if (AuctionService.bid(b, lot.id, 100, NOW + 2) != null) h.fail("first bid rejected");
        if (Wallet.count(b) != 900) h.fail("escrow not held from wallet: " + Wallet.count(b));
        if (AuctionService.bid(c, lot.id, 101, NOW + 3) == null) h.fail("bid below +5% step accepted");
        if (AuctionService.bid(c, lot.id, 105, NOW + 3) != null) h.fail("outbid rejected");
        if (Wallet.count(c) != 895) h.fail("outbidder wallet=" + Wallet.count(c));
        if (MailData.get(s).pendingCount(b.getUUID()) != 1) h.fail("refund mail not queued for outbid player");
        MailService.deliver(b);
        if (Wallet.count(b) != 1000) h.fail("outbid refund not credited: " + Wallet.count(b));
        if (!AuctionService.settle(s, d, lot, lot.endMs)) h.fail("expired lot not settled");
        if (d.burned - burned0 != 1 + 5) h.fail("burn expected 1 (listing) + 5 (5% of 105), got " + (d.burned - burned0));
        MailService.deliver(a);
        if (Wallet.count(a) != 999 + 100) h.fail("seller net expected 1099, got " + Wallet.count(a));
        MailService.deliver(c);
        if (c.getInventory().countItem(AlchemyRegistry.LINH_THAO.get()) != 8) h.fail("winner did not receive the goods");
        if (d.lots.containsKey(lot.id)) h.fail("settled lot still listed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void buyNowFloorAndCap(GameTestHelper h) {
        MinecraftServer s = h.getLevel().getServer();
        AuctionData d = AuctionData.get(s);
        ServerPlayer a = player(h, UUID.randomUUID(), 100_000), b = player(h, UUID.randomUUID(), 10_000);
        if (AuctionService.list(a, new ItemStack(AlchemyRegistry.LINH_THAO.get(), 8), 5, 0, 0, NOW) == null) h.fail("price below the grade floor accepted");
        if (AuctionService.list(a, new ItemStack(AlchemyRegistry.LINH_THAO.get(), 8), 100, 50, 0, NOW) == null) h.fail("buy-now below start accepted");
        if (AuctionService.list(a, ItemStack.EMPTY, 100, 0, 0, NOW) == null) h.fail("empty stack accepted");
        long burned0 = d.burned;
        if (AuctionService.list(a, new ItemStack(AlchemyRegistry.THANH_DIEP.get(), 6), 200, 500, 1, NOW) != null) h.fail("valid listing rejected");
        Lot lot = lotOf(d, a.getUUID());
        if (AuctionService.bid(b, lot.id, 150, NOW + 1) == null) h.fail("bid below start accepted");
        if (AuctionService.buy(b, lot.id, NOW + 1) != null) h.fail("buy-now rejected");
        if (Wallet.count(b) != 9_500) h.fail("buyer paid wrong amount: " + Wallet.count(b));
        if (d.lots.containsKey(lot.id)) h.fail("sold lot still listed");
        if (d.burned - burned0 != 2 + 25) h.fail("burn expected 2 (listing 1% of 200) + 25 (5% of 500), got " + (d.burned - burned0));
        MailService.deliver(b);
        if (b.getInventory().countItem(AlchemyRegistry.THANH_DIEP.get()) != 6) h.fail("buyer item missing");
        // gioi han lo moi nguoi (cau hinh tam: 2)
        AuctionConfig old = AuctionConfig.get();
        AuctionConfig tmp = new AuctionConfig();
        tmp.maxLotsPerPlayer = 2;
        AuctionConfig.set(tmp);
        try {
            ServerPlayer c = player(h, UUID.randomUUID(), 100_000);
            for (int i = 0; i < 2; i++) if (AuctionService.list(c, new ItemStack(AlchemyRegistry.LINH_THAO.get(), 8), 100, 0, 0, NOW) != null) h.fail("listing " + i + " rejected under cap");
            if (AuctionService.list(c, new ItemStack(AlchemyRegistry.LINH_THAO.get(), 8), 100, 0, 0, NOW) == null) h.fail("lot cap not enforced");
        } finally {
            AuctionConfig.set(old);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void npcBuybackAndWithdraw(GameTestHelper h) {
        MinecraftServer s = h.getLevel().getServer();
        AuctionData d = AuctionData.get(s);
        ServerPlayer a = player(h, UUID.randomUUID(), 1000);
        long floor = AuctionPricing.floor(new ItemStack(AlchemyRegistry.LINH_THAO.get(), 8));
        if (AuctionService.list(a, new ItemStack(AlchemyRegistry.LINH_THAO.get(), 8), floor, 0, 0, NOW) != null) h.fail("floor-priced listing rejected");
        Lot lot = lotOf(d, a.getUUID());
        long w0 = Wallet.count(a);
        d.npcBudgetDay = NOW / 86_400_000L;
        d.npcBudgetLeft = 10_000;
        if (!AuctionService.settle(s, d, lot, lot.endMs)) h.fail("not settled");
        MailService.deliver(a);
        long fee = AuctionMath.saleFee(floor, 5);
        if (Wallet.count(a) != w0 + floor - fee) h.fail("NPC buyback payout wrong: " + (Wallet.count(a) - w0));
        if (a.getInventory().countItem(AlchemyRegistry.LINH_THAO.get()) != 0) h.fail("bought-back goods returned to seller");
        // khong ai tra gia + gia cao hon san -> tra lai vat pham
        if (AuctionService.list(a, new ItemStack(AlchemyRegistry.LINH_THAO.get(), 8), floor + 500, 0, 0, NOW) != null) h.fail("listing rejected");
        Lot l2 = lotOf(d, a.getUUID());
        AuctionService.settle(s, d, l2, l2.endMs);
        MailService.deliver(a);
        if (a.getInventory().countItem(AlchemyRegistry.LINH_THAO.get()) != 8) h.fail("unsold item not returned");
        // rut lo khi chua co ai tra gia
        if (AuctionService.list(a, new ItemStack(AlchemyRegistry.HUYET_THAM.get(), 3), 500, 0, 0, NOW) != null) h.fail("listing rejected");
        Lot l3 = lotOf(d, a.getUUID());
        ServerPlayer b = player(h, UUID.randomUUID(), 1000);
        if (AuctionService.cancel(b, l3.id) == null) h.fail("stranger withdrew a lot");
        if (AuctionService.cancel(a, l3.id) != null) h.fail("seller could not withdraw");
        MailService.deliver(a);
        if (a.getInventory().countItem(AlchemyRegistry.HUYET_THAM.get()) != 3) h.fail("withdrawn item not returned");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mailDeliveredOnLogin(GameTestHelper h) {
        MinecraftServer s = h.getLevel().getServer();
        UUID id = UUID.randomUUID();
        MailService.send(s, id, "hukhong.mail.sub.sold", "x", 321, new ItemStack(AlchemyRegistry.HOA_LIEN.get(), 2));
        if (MailData.get(s).pendingCount(id) != 1) h.fail("offline mail should be queued");
        ServerPlayer p = player(h, id, 0);
        MailService.onLogin(new PlayerEvent.PlayerLoggedInEvent(p));
        if (Wallet.count(p) != 321) h.fail("currency not delivered on login: " + Wallet.count(p));
        if (p.getInventory().countItem(AlchemyRegistry.HOA_LIEN.get()) != 2) h.fail("item not delivered on login");
        if (MailData.get(s).pendingCount(id) != 0) h.fail("mail still pending after delivery");
        // tui day: vat pham o lai hom thu, tien van vao vi
        UUID id2 = UUID.randomUUID();
        ServerPlayer q = player(h, id2, 0);
        for (int i = 0; i < q.getInventory().items.size(); i++) q.getInventory().items.set(i, new ItemStack(net.minecraft.world.item.Items.DIRT, 64));
        for (int i = 0; i < q.getInventory().offhand.size(); i++) q.getInventory().offhand.set(i, new ItemStack(net.minecraft.world.item.Items.DIRT, 64));
        MailService.send(s, id2, "hukhong.mail.sub.won", "y", 50, new ItemStack(AlchemyRegistry.HOA_LIEN.get(), 1));
        MailService.deliver(q);
        if (Wallet.count(q) != 50) h.fail("currency must arrive even when the inventory is full");
        if (MailData.get(s).pendingCount(id2) != 1) h.fail("item should stay in the mailbox while the inventory is full");
        q.getInventory().clearContent();
        MailService.deliver(q);
        if (MailData.get(s).pendingCount(id2) != 0 || q.getInventory().countItem(AlchemyRegistry.HOA_LIEN.get()) != 1) h.fail("leftover not claimable later");
        h.succeed();
    }
}
