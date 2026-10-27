package com.hukhong.city;

import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.item.ModItems;
import com.hukhong.structures.StructureBuilds;
import com.hukhong.structures.StructurePlacer;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Khu cho / Thuong Hoi (the loai cau truc "trade"): dung ke ben thanh co file trade, roi sinh thuong nhan (thuong nhan lang thang
 * dung yen, bat tu, gan tag {@link #TAG} de CityProtection bao ve) o marker hukhong:trader (hoac npc). Ban thao duoc, dan duoc, Hon Tinh lay luc bao.
 */
public final class TradeDistrict {
    private TradeDistrict() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String TAG = "hukhong_trader";
    /** Khoang lech tam khu cho so voi tam cau truc thanh (thanh toi da 256 + cho toi da 256 => 280 chua cham nhau). */
    public static final int OFFSET = 280;

    public static boolean has(CityDef d) {
        return switch (d.id()) { case "luu_ly_cang", "co_tien_thanh", "dan_ha_thanh" -> true; default -> false; };
    }

    private static boolean wet(ServerLevel l, BlockPos p) {
        var b = l.getBiome(p);
        return b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_RIVER);
    }

    /** Dat khu cho canh thanh (thu 4 huong, bo huong ngap nuoc). Khong co file "trade" thi bo qua (khong co du phong thu tuc). */
    public static void tryBuild(ServerLevel l, CityDef d, BlockPos citySite) {
        if (!has(d)) return;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        BlockPos pick = null;
        for (int[] o : dirs) {
            BlockPos p = new BlockPos(citySite.getX() + o[0] * OFFSET, 64, citySite.getZ() + o[1] * OFFSET);
            if (pick == null) pick = p;
            if (!wet(l, p)) { pick = p; break; }
        }
        BlockPos c = StructureBuilds.tryBuild(l, "trade", d.id() + "_trade", pick, "altar", TradeDistrict::populate);
        LOG.info("Trade district for {}: {}", d.id(), c == null ? "no 'trade' structure file" : "queued near " + pick);
    }

    /** Sau khi dat xong: sinh thuong nhan (xoa thuong nhan cu trong vung de dat lai khong bi nhan doi). */
    public static void populate(StructurePlacer.Placed p) {
        ServerLevel l = p.level();
        BlockPos o = p.origin();
        for (var e : l.getEntitiesOfClass(WanderingTrader.class, new AABB(o.getX(), o.getY() - 8, o.getZ(),
                o.getX() + p.st().sx, o.getY() + p.st().sy + 8, o.getZ() + p.st().sz), t -> t.getTags().contains(TAG))) e.discard();
        List<BlockPos> pts = new ArrayList<>(p.markers("trader"));
        if (pts.isEmpty()) pts.addAll(p.markers("npc"));
        BlockPos altar = p.marker("altar", o.offset(p.st().sx / 2, 1, p.st().sz / 2));
        int[][] ring = {{3, 3}, {-3, 3}, {3, -3}, {-3, -3}};
        for (int i = 0; pts.size() < 3; i++) pts.add(altar.offset(ring[i % 4][0], 0, ring[i % 4][1]));
        int n = 0;
        for (BlockPos pos : pts) spawn(l, pos, n++ % 3);
        LOG.info("Trade district populated at {}: {} merchants", o, n);
    }

    private static void spawn(ServerLevel l, BlockPos pos, int kind) {
        WanderingTrader t = EntityType.WANDERING_TRADER.create(l);
        if (t == null) return;
        t.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, l.random.nextFloat() * 360f, 0f);
        t.addTag(TAG);
        t.setNoAi(true);
        t.setInvulnerable(true);
        t.setPersistenceRequired();
        t.setCustomName(Component.translatable(kind == 0 ? "hukhong.trade.herb" : kind == 1 ? "hukhong.trade.pill" : "hukhong.trade.soul"));
        t.setCustomNameVisible(true);
        MerchantOffers offers = t.getOffers(); // sinh san roi xoa de chi con hang cua mod
        offers.clear();
        switch (kind) {
            case 0 -> {
                add(offers, 1, AlchemyRegistry.LINH_THAO.get(), 3);
                add(offers, 2, AlchemyRegistry.THANH_DIEP.get(), 2);
                add(offers, 3, AlchemyRegistry.HUYET_THAM.get(), 1);
                add(offers, 3, AlchemyRegistry.HOA_LIEN.get(), 1);
            }
            case 1 -> {
                add(offers, 3, AlchemyRegistry.CHI_HUYET_DAN.get(), 1);
                add(offers, 4, AlchemyRegistry.HOI_XUAN_DAN.get(), 1);
                add(offers, 3, AlchemyRegistry.GIAI_DOC_DAN.get(), 1);
            }
            default -> add(offers, 12, ModItems.HON_TINH.get(), 1);
        }
        l.addFreshEntity(t);
    }

    private static void add(MerchantOffers offers, int emeralds, Item item, int count) {
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), new ItemStack(item, count), 9999, 0, 0f));
    }
}
