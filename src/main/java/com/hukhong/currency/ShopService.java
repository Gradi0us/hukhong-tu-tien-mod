package com.hukhong.currency;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.market.MarketRep;
import com.hukhong.city.CityService;
import com.hukhong.city.TradeDistrict;
import com.hukhong.guild.GuildMath;
import com.hukhong.guild.GuildRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

/** Mo / xu ly cua hang Tu Tinh cua thuong nhan trong thanh (thay giao dich vanilla bang ngoc luc bao). */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class ShopService {
    private ShopService() {}

    /** Chuot phai thuong nhan cho (sau FreeQuest: Shift/da het nhiem vu thi toi day). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getTarget() instanceof WanderingTrader t) || !t.getTags().contains(TradeDistrict.TAG)) return;
        if (ShopCatalog.kindOf(t) < 0) return;
        e.setCanceled(true); // chan giao dich vanilla (ca thuong nhan cu con ngoc luc bao)
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (!e.getLevel().isClientSide() && e.getEntity() instanceof ServerPlayer sp) open(sp, t);
    }

    private static CityDef cityOf(ServerLevel l, Entity e) {
        ServerLevel ow = l.getServer().getLevel(Level.OVERWORLD);
        return ow == null ? null : CityService.nearest(ow, e.getX(), e.getZ(), 80);
    }

    /** Bac Uy Danh Cho (0..4) - thay bac Hiep Hoi: giam gia + khoa hang theo Tu Tinh da tieu o cho. */
    private static int rank(ServerPlayer p) { return MarketRep.tier(p); }

    /** Ngay Lich hien tai (Quay Dac Biet xoay vong). */
    public static long lichDay(ServerLevel l) {
        ServerLevel ow = l.getServer().getLevel(Level.OVERWORLD);
        return com.hukhong.world.calendar.LichMath.day((ow == null ? l : ow).getGameTime());
    }

    private static List<ShopMenu.Entry> buys(ServerPlayer p, Entity t, int kind) {
        List<ShopMenu.Entry> out = new ArrayList<>();
        for (ShopCatalog.Offer o : ShopCatalog.buys(p.serverLevel().getSeed(), kind, cityOf(p.serverLevel(), t), lichDay(p.serverLevel())))
            out.add(new ShopMenu.Entry(o.stack(), com.hukhong.living.AffectionService.adjustPrice(p, MarketRep.discounted(o.price(), rank(p))), o.minRank()));
        return out;
    }

    public static void open(ServerPlayer sp, Entity trader) {
        int kind = ShopCatalog.kindOf(trader);
        if (kind < 0) return;
        var snap = new ShopMenu.Snapshot(trader.getId(), kind, rank(sp), buys(sp, trader, kind), List.of(), MarketRep.discountPct(rank(sp)), true);
        sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ShopMenu(id, inv, snap), ShopCatalog.kindName(kind)), snap::write);
    }

    public static boolean handleButton(ServerPlayer sp, Entity trader, int id) {
        int kind = ShopCatalog.kindOf(trader);
        if (kind < 0) return false;
        if (id < ShopMenu.B_SELL1) {
            List<ShopMenu.Entry> l = buys(sp, trader, kind);
            if (id < 0 || id >= l.size()) return false;
            ShopMenu.Entry e = l.get(id);
            if (rank(sp) < e.minRank()) { sp.displayClientMessage(Component.translatable("hukhong.shop.locked", Component.translatable(MarketRep.tierKey(e.minRank()))), true); return false; }
            if (!Wallet.pay(sp, e.price())) { sp.displayClientMessage(Component.translatable("hukhong.shop.no_money"), true); return false; }
            ItemStack give = e.stack().copy();
            if (give.is(com.hukhong.item.ModItems.HON_TINH.get())) Wallet.grantHonTinh(sp, give.getCount()); // Hon Tinh = tien trong vi
            else if (!sp.getInventory().add(give)) sp.drop(give, false);
            MarketRep.addSpent(sp, e.price());
            sp.displayClientMessage(Component.translatable("hukhong.shop.bought", e.stack().getHoverName(), TuTinhText.of(e.price())), true);
            return true;
        }
        if (id == ShopMenu.B_SELLEVERYTHING) {
            long total = 0;
            int n = 0;
            for (SellPrices.Row r : SellPrices.rows(sp)) {
                int got = SellPrices.take(sp, r.proto(), r.count());
                total += r.unit() * got;
                n += got;
            }
            if (n == 0) { sp.displayClientMessage(Component.translatable("hukhong.shop.nothing_to_sell"), true); return false; }
            long rep = settle(sp, total);
            sp.displayClientMessage(Component.translatable("hukhong.shop.sold_everything", n, TuTinhText.of(total), rep), true);
            return true;
        }
        boolean all = id >= ShopMenu.B_SELLALL;
        int idx = id - (all ? ShopMenu.B_SELLALL : ShopMenu.B_SELL1);
        List<SellPrices.Row> rows = SellPrices.rows(sp);
        if (idx < 0 || idx >= rows.size()) return false;
        SellPrices.Row r = rows.get(idx);
        int sold = SellPrices.take(sp, r.proto(), all ? r.count() : 1);
        if (sold == 0) { sp.displayClientMessage(Component.translatable("hukhong.shop.nothing_to_sell"), true); return false; }
        long total = r.unit() * sold;
        long rep = settle(sp, total);
        sp.displayClientMessage(Component.translatable("hukhong.shop.sold_rep", sold, r.proto().getHoverName(), TuTinhText.of(total), rep), true);
        return true;
    }

    /** Tra tien thu mua + cong Uy Danh = 50% gia tri ban (mua van 100%). Tra so Uy Danh nhan duoc. */
    public static long settle(ServerPlayer sp, long total) {
        Wallet.give(sp, total);
        long rep = total * SellPrices.REP_PCT / 100;
        MarketRep.addSpent(sp, rep);
        return rep;
    }

    public static Component rankName(int rank) { return Component.translatable("hukhong.guild.rank." + GuildMath.clampRank(rank)); }
}
