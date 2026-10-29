package com.hukhong.market;

import com.hukhong.HuKhong;
import com.hukhong.city.AltarShrine;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityMarket;
import com.hukhong.city.MarketDistrict;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.currency.ShopCatalog;
import com.hukhong.currency.ShopService;
import com.hukhong.currency.Wallet;
import com.hukhong.currency.WalletAttachment;
import com.hukhong.skillbook.BookSkill;
import com.hukhong.skillbook.SkillBookAttachments;
import com.hukhong.skillbook.SkillBookData;
import com.hukhong.skillbook.SkillBookMath;
import com.hukhong.skillbook.SkillBookRegistry;
import com.hukhong.skillbook.SkillBookService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Dot 2 T1: Uy Danh Cho (giam gia, Quay Dac Biet khoa bac 4), Truc Nang Cap Ky Nang, Khu Cho tach khoi An Hon Dai. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class MarketGameTests {
    private MarketGameTests() {}

    private static ServerPlayer player(GameTestHelper h, long spent, long wallet) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-market"));
        p.setData(ModAttachments.CULTIVATION, new CultivationData(30, 0, 4000, 4000, Set.of(Path.KIEM), Map.<Path, Aptitude>of(), false));
        p.setData(WalletAttachment.MARKET_SPENT.get(), spent);
        Wallet.set(p, wallet);
        return p;
    }

    private static WanderingTrader trader(GameTestHelper h, String key) {
        WanderingTrader t = h.spawn(EntityType.WANDERING_TRADER, 1, 1, 1);
        t.addTag(CityMarket.TAG);
        t.setCustomName(Component.translatable("hukhong.trade." + key));
        t.setNoAi(true);
        return t;
    }

    /** Giam gia theo bac: mua mon dau cua thuong nhan thao duoc, so Ha tru dung = gia goc giam 0/5/10/10/10%. */
    @GameTest(template = "empty")
    public static void discountByTier(GameTestHelper h) {
        WanderingTrader t = trader(h, "herb");
        long base = ShopCatalog.buys(h.getLevel().getSeed(), 0, null, 0).get(0).price();
        for (int tier = 0; tier < MarketRep.TIERS; tier++) {
            ServerPlayer p = player(h, MarketRep.THRESHOLDS[tier], 1_000_000L);
            if (MarketRep.tier(p) != tier) { h.fail("tier " + MarketRep.tier(p) + " != " + tier); return; }
            long before = Wallet.count(p);
            if (!ShopService.handleButton(p, t, 0)) { h.fail("buy failed at tier " + tier); return; }
            long paid = before - Wallet.count(p);
            long want = Math.max(1, base * (100 - MarketRep.discountPct(tier)) / 100);
            if (paid != want) { h.fail("tier " + tier + ": paid " + paid + " != " + want + " (base " + base + ")"); return; }
            if (MarketRep.spent(p) < MarketRep.THRESHOLDS[tier] + paid) { h.fail("spent not tracked at tier " + tier); return; }
        }
        if (MarketRep.discountPct(1) != 5 || MarketRep.discountPct(2) != 10) { h.fail("discount table wrong"); return; }
        h.succeed();
    }

    /** Quay Dac Biet khoa duoi Thuong Khach (bac 4 = chi so 3); mo o bac do. */
    @GameTest(template = "empty")
    public static void specialCounterLockedBelowTier4(GameTestHelper h) {
        WanderingTrader t = trader(h, "special");
        ServerPlayer low = player(h, MarketRep.THRESHOLDS[2], 1_000_000L); // Khach Quy (bac 3)
        long before = Wallet.count(low);
        if (ShopService.handleButton(low, t, 0)) { h.fail("special counter sold to tier-3 player"); return; }
        if (Wallet.count(low) != before) { h.fail("wallet changed on refused purchase"); return; }
        ServerPlayer high = player(h, MarketRep.THRESHOLDS[3], 1_000_000L); // Thuong Khach (bac 4)
        long b2 = Wallet.count(high);
        if (!ShopService.handleButton(high, t, 0)) { h.fail("special counter refused tier-4 player"); return; }
        if (Wallet.count(high) >= b2) { h.fail("tier-4 purchase did not charge"); return; }
        if (high.getInventory().isEmpty()) { h.fail("no Hon Cot delivered"); return; }
        h.succeed();
    }

    /** Hang Quay Dac Biet xoay vong theo ngay Lich (96000 tick) va on dinh trong cung ngay. */
    @GameTest(template = "empty")
    public static void specialStockRotatesPerLichDay(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        var d0 = ShopCatalog.buys(seed, ShopCatalog.K_SPECIAL, null, 0);
        var d0b = ShopCatalog.buys(seed, ShopCatalog.K_SPECIAL, null, 0);
        boolean rotated = false;
        for (int day = 1; day <= 6 && !rotated; day++) {
            var dn = ShopCatalog.buys(seed, ShopCatalog.K_SPECIAL, null, day);
            for (int i = 0; i < 3; i++) if (!ItemStack.isSameItemSameComponents(d0.get(i).stack(), dn.get(i).stack())) rotated = true;
        }
        for (int i = 0; i < d0.size(); i++)
            if (!ItemStack.isSameItemSameComponents(d0.get(i).stack(), d0b.get(i).stack())) { h.fail("same day must be stable"); return; }
        if (!rotated) { h.fail("special stock never rotated over 6 days"); return; }
        if (MarketRep.lichDay(96_000L * 3 + 5) != 3) { h.fail("lichDay wrong"); return; }
        h.succeed();
    }

    /** Cho chi ban do mod: moi mon o moi loai thuong nhan, moi bac, moi thanh deu thuoc namespace hukhong. */
    @GameTest(template = "empty")
    public static void marketSellsOnlyModItems(GameTestHelper h) {
        for (CityDef d : CityDef.ALL) for (int kind = 0; kind < ShopCatalog.KEYS.length; kind++) {
            for (var o : ShopCatalog.buys(h.getLevel().getSeed(), kind, d, 2)) {
                String ns = BuiltInRegistries.ITEM.getKey(o.stack().getItem()).getNamespace();
                if (!ns.equals(HuKhong.MODID)) { h.fail("non-mod item " + o.stack().getItem() + " in kind " + kind + " of " + d.id()); return; }
            }
        }
        if (ShopCatalog.buys(0, ShopCatalog.K_EVO, null, 0).isEmpty()) { h.fail("evolution stall empty"); return; }
        h.succeed();
    }

    /** Dung Truc Nang Cap: +1 cap (bi tich da hoc), tieu 1 truc; khong co truc/da toi da thi tu choi. */
    @GameTest(template = "empty")
    public static void scrollRaisesSkillLevel(GameTestHelper h) {
        ServerPlayer p = player(h, 0, 0);
        BookSkill b = BookSkill.PHA_KHONG_TRAM;
        p.setData(SkillBookAttachments.BOOK, SkillBookData.EMPTY.withLevel(b.id, 1));
        if (SkillBookService.applyScroll(p, b.id)) { h.fail("applied without a scroll"); return; }
        p.getInventory().add(new ItemStack(SkillBookRegistry.UPGRADE_SCROLL.get(), 2));
        if (!SkillBookService.scrollCandidates(p).contains(b.id)) { h.fail("learned skill missing from candidates"); return; }
        if (!SkillBookService.applyScroll(p, b.id)) { h.fail("apply failed"); return; }
        if (SkillBookService.data(p).level(b.id) != 2) { h.fail("level " + SkillBookService.data(p).level(b.id) + " != 2"); return; }
        int left = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(SkillBookRegistry.UPGRADE_SCROLL.get())) left += s.getCount();
        if (left != 1) { h.fail("scroll not consumed exactly once, left " + left); return; }
        p.setData(SkillBookAttachments.BOOK, SkillBookData.EMPTY.withLevel(b.id, SkillBookMath.MAX_LEVEL));
        if (SkillBookService.applyScroll(p, b.id)) { h.fail("applied beyond max level"); return; }
        if (SkillBookService.applyScroll(p, "khong_ton_tai")) { h.fail("applied to unknown skill"); return; }
        h.succeed();
    }

    /** Rơi hiếm ở Bí Cảnh cấp cao: xác suất 0 ở cấp thấp, > 0 ở Hắc/Hồng. */
    @GameTest(template = "empty")
    public static void scrollDropsOnlyInHighBiCanh(GameTestHelper h) {
        var low = com.hukhong.bicanh.BiCanhTier.BACH;
        if (com.hukhong.bicanh.BiCanhLoot.upgradeScrollChance(low) != 0) { h.fail("low tier must not drop"); return; }
        if (com.hukhong.bicanh.BiCanhLoot.upgradeScrollChance(com.hukhong.bicanh.BiCanhTier.HAC) <= 0
                || com.hukhong.bicanh.BiCanhLoot.upgradeScrollChance(com.hukhong.bicanh.BiCanhTier.HONG) <= 0) { h.fail("high tiers must drop"); return; }
        int got = 0;
        java.util.Random r = new java.util.Random(7);
        for (int i = 0; i < 2000; i++)
            for (ItemStack s : com.hukhong.bicanh.BiCanhLoot.stacks(com.hukhong.bicanh.BiCanhTier.HONG, r)) if (s.is(SkillBookRegistry.UPGRADE_SCROLL.get())) got++;
        if (got == 0) { h.fail("HONG tier never dropped the scroll in 2000 rolls"); return; }
        h.succeed();
    }

    /**
     * Quang truong An Hon lon (R=18) + khu cho: dien dan co 8 tru lua hon va ho nuoc; thuong nhan cu quanh An Hon Dai bi don;
     * khu cho cach altar 40-70 block co du thuong nhan; khong co thuong nhan nao trong 32 block cua altar.
     */
    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void noTraderNearAltarAfterBuild(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        BlockPos altar = new BlockPos(o.getX() + 300, 100, o.getZ() + 300);
        int t = AltarShrine.build(l, altar, AltarShrine.MAX_R, true);
        if (t != AltarShrine.MAX_R + 3) { h.fail("plaza T " + t + " != " + (AltarShrine.MAX_R + 3)); return; }
        int fire = 0, water = 0;
        for (int dx = -t; dx <= t; dx++) for (int dz = -t; dz <= t; dz++) for (int y = altar.getY() - 6; y <= altar.getY() + 12; y++) {
            var s = l.getBlockState(new BlockPos(altar.getX() + dx, y, altar.getZ() + dz));
            if (s.is(Blocks.SOUL_FIRE)) fire++;
            if (s.is(Blocks.WATER)) water++;
        }
        if (fire != 8) { h.fail("soul-fire pillars " + fire + " != 8"); return; }
        if (water < 40) { h.fail("moat too small: " + water); return; }
        // thuong nhan cu (the gioi cu) quanh An Hon Dai
        for (int i = 0; i < 3; i++) {
            WanderingTrader w = EntityType.WANDERING_TRADER.create(l);
            w.moveTo(altar.getX() + 4 + i, altar.getY(), altar.getZ() + 5, 0, 0);
            w.addTag(CityMarket.TAG);
            l.addFreshEntity(w);
        }
        BlockPos cityCenter = altar.below();
        if (MarketDistrict.cleanupAltar(l, cityCenter) != 3) { h.fail("legacy merchants not removed"); return; }
        // vi tri khu cho: moi ung vien deu 40-70 block
        CityDef d = CityDef.ALL.get(1);
        for (int k = 0; k < MarketDistrict.MAX_CANDIDATES; k++) {
            int[] c = MarketDistrict.candidate(l.getSeed(), d.id(), cityCenter.getX(), cityCenter.getZ(), k);
            double dist = Math.sqrt((double) (c[0] - cityCenter.getX()) * (c[0] - cityCenter.getX()) + (double) (c[1] - cityCenter.getZ()) * (c[1] - cityCenter.getZ()));
            if (dist < MarketDistrict.MIN_DIST || dist > MarketDistrict.MAX_DIST) { h.fail("candidate " + k + " at distance " + dist); return; }
        }
        int[] c0 = MarketDistrict.candidate(l.getSeed(), d.id(), cityCenter.getX(), cityCenter.getZ(), 0);
        BlockPos market = MarketDistrict.build(l, d, new BlockPos(c0[0], 100, c0[1]), cityCenter);
        int near = l.getEntitiesOfClass(WanderingTrader.class, new AABB(altar).inflate(MarketDistrict.ALTAR_CLEAR, 40, MarketDistrict.ALTAR_CLEAR),
                w -> w.getTags().contains(CityMarket.TAG)).size();
        if (near != 0) { h.fail(near + " traders within 32 blocks of the altar"); return; }
        List<WanderingTrader> inMarket = l.getEntitiesOfClass(WanderingTrader.class, new AABB(market).inflate(MarketDistrict.HALF, 12, MarketDistrict.HALF),
                w -> w.getTags().contains(CityMarket.TAG));
        if (inMarket.size() < 6) { h.fail("only " + inMarket.size() + " traders in the market"); return; }
        boolean special = false, evo = false;
        for (WanderingTrader w : inMarket) {
            int k = ShopCatalog.kindOf(w);
            special |= k == ShopCatalog.K_SPECIAL;
            evo |= k == ShopCatalog.K_EVO;
        }
        if (!special || !evo) { h.fail("market lacks special/evolution stall"); return; }
        h.succeed();
    }
}
