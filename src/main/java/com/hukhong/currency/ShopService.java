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

    private static List<ShopMenu.Entry> sells(int kind) {
        List<ShopMenu.Entry> out = new ArrayList<>();
        for (ShopCatalog.Offer o : ShopCatalog.sells(kind)) out.add(new ShopMenu.Entry(o.stack(), o.price(), 0));
        return out;
    }

    public static void open(ServerPlayer sp, Entity trader) {
        int kind = ShopCatalog.kindOf(trader);
        if (kind < 0) return;
        var snap = new ShopMenu.Snapshot(trader.getId(), kind, rank(sp), buys(sp, trader, kind), sells(kind), MarketRep.discountPct(rank(sp)), true);
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
        boolean all = id >= ShopMenu.B_SELLALL;
        int idx = id - (all ? ShopMenu.B_SELLALL : ShopMenu.B_SELL1);
        List<ShopMenu.Entry> l = sells(kind);
        if (idx < 0 || idx >= l.size()) return false;
        ShopMenu.Entry e = l.get(idx);
        int want = all ? Integer.MAX_VALUE : 1, sold = 0;
        for (ItemStack s : sp.getInventory().items) {
            if (sold >= want) break;
            if (!ItemStack.isSameItemSameComponents(s, e.stack())) continue;
            int t = Math.min(want - sold, s.getCount());
            s.shrink(t);
            sold += t;
        }
        if (sold == 0) { sp.displayClientMessage(Component.translatable("hukhong.shop.nothing_to_sell"), true); return false; }
        Wallet.give(sp, e.price() * sold);
        sp.displayClientMessage(Component.translatable("hukhong.shop.sold", sold, e.stack().getHoverName(), TuTinhText.of(e.price() * sold)), true);
        return true;
    }

    public static Component rankName(int rank) { return Component.translatable("hukhong.guild.rank." + GuildMath.clampRank(rank)); }
}
