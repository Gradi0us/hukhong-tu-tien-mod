package com.hukhong.city;

import com.hukhong.alchemy.AlchemyRegistry;
import com.hukhong.item.ModItems;
import com.hukhong.skillbook.SkillBookRewards;
import com.hukhong.skillbook.SkillGrade;
import com.hukhong.structures.StructurePlacer;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import java.util.Random;

/**
 * Cho trong thanh: moi thanh co cac thuong nhan dung yen (thuong nhan lang thang bat tu, tag {@link #TAG} de CityProtection bao ve)
 * dat o marker hukhong:trader (hoac vong quanh quang truong/ban tho). Thay cho khu cho rieng (TradeDistrict) cu.
 * Loai: 0 thao duoc, 1 dan duoc, 2 vu khi, 3 giap, 4 Hon Tinh/hiem, 5 tap hoa, 6 bi tich ky nang (thanh cap Dia tro len).
 */
public final class CityMarket {
    private CityMarket() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String TAG = "hukhong_trader";
    private static final String[] KEYS = {"herb", "pill", "weapon", "armor", "soul", "general", "scroll"};

    /** Sau khi dat xong cau truc: xoa thuong nhan cu trong vung roi sinh lai. */
    public static void populate(StructurePlacer.Placed p, CityDef d) {
        BlockPos o = p.origin();
        clear(p.level(), new AABB(o.getX(), o.getY() - 8, o.getZ(), o.getX() + p.st().sx, o.getY() + p.st().sy + 8, o.getZ() + p.st().sz));
        BlockPos altar = p.marker("altar", o.offset(p.st().sx / 2, 1, p.st().sz / 2));
        populate(p.level(), d, altar, p.markers("trader"));
    }

    public static void populate(ServerLevel l, CityDef d, BlockPos altar, List<BlockPos> markers) {
        int kinds = d.tier() == Tier.NHAN ? 6 : 7;
        List<BlockPos> pts = new ArrayList<>(markers);
        int[][] ring = {{7, 0}, {-7, 0}, {0, 7}, {0, -7}, {5, 5}, {-5, 5}, {5, -5}, {-5, -5}};
        for (int i = 0; pts.size() < kinds; i++) {
            int[] r = ring[i % ring.length];
            pts.add(altar.offset(r[0], 0, r[1]));
        }
        int n = 0;
        for (BlockPos pos : pts) spawn(l, ground(l, pos), n++ % kinds, d);
        LOG.info("Market of {}: {} merchants around {}", d.id(), n, altar);
    }

    public static void clear(ServerLevel l, AABB box) {
        for (var e : l.getEntitiesOfClass(WanderingTrader.class, box, t -> t.getTags().contains(TAG))) e.discard();
    }

    /** Mat dung gan nhat (khoi dac duoi, 2 o khong khi tren) trong +-4 quanh pos; neu khong co thi giu pos. */
    static BlockPos ground(ServerLevel l, BlockPos pos) {
        if (!l.hasChunkAt(pos)) return pos;
        for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3, 4, -4}) {
            BlockPos p = pos.above(dy);
            if (l.getBlockState(p).isAir() && l.getBlockState(p.above()).isAir() && l.getBlockState(p.below()).blocksMotion()
                    && l.getFluidState(p.below()).isEmpty()) return p;
        }
        return pos;
    }

    private static void spawn(ServerLevel l, BlockPos pos, int kind, CityDef d) {
        WanderingTrader t = EntityType.WANDERING_TRADER.create(l);
        if (t == null) return;
        t.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, l.random.nextFloat() * 360f, 0f);
        t.addTag(TAG);
        t.setNoAi(true);
        t.setInvulnerable(true);
        t.setPersistenceRequired();
        t.setDespawnDelay(0);
        t.setCustomName(Component.translatable("hukhong.trade." + KEYS[kind]));
        t.setCustomNameVisible(true);
        MerchantOffers offers = t.getOffers();
        offers.clear();
        int mul = d.tier().ordinal() + 1;
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
                add(offers, 8 * mul, AlchemyRegistry.THO_NGUYEN_DAN.get(), 1);
            }
            case 2 -> {
                add(offers, 6, Items.IRON_SWORD, 1);
                add(offers, 8, Items.BOW, 1);
                add(offers, 2, Items.ARROW, 16);
                add(offers, 14 * mul, Items.DIAMOND_SWORD, 1);
            }
            case 3 -> {
                add(offers, 8, Items.IRON_CHESTPLATE, 1);
                add(offers, 5, Items.IRON_HELMET, 1);
                add(offers, 4, Items.SHIELD, 1);
                add(offers, 18 * mul, Items.DIAMOND_CHESTPLATE, 1);
            }
            case 4 -> {
                add(offers, 12, ModItems.HON_TINH.get(), 1);
                add(offers, 30, ModItems.HON_TINH.get(), 3);
                add(offers, 20, Items.ENDER_PEARL, 1);
            }
            case 5 -> {
                add(offers, 1, Items.BREAD, 6);
                add(offers, 1, Items.TORCH, 16);
                add(offers, 3, Items.COOKED_BEEF, 8);
                add(offers, 5, Items.BUCKET, 1);
                add(offers, 2, Items.LEAD, 1);
            }
            default -> {
                Random r = new Random(l.getSeed() ^ d.id().hashCode());
                for (int i = 0; i < 3; i++) {
                    SkillGrade g = d.tier() == Tier.THIEN ? (i == 0 ? SkillGrade.DIA : SkillGrade.HUYEN) : (i == 2 ? SkillGrade.HUYEN : SkillGrade.PHAM);
                    ItemStack book = SkillBookRewards.stack(r, g, null);
                    int price = g == SkillGrade.DIA ? 48 : g == SkillGrade.HUYEN ? 24 : 12;
                    offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, price), book, 9999, 0, 0f));
                }
            }
        }
        l.addFreshEntity(t);
    }

    private static void add(MerchantOffers offers, int emeralds, Item item, int count) {
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, Math.min(64, emeralds)), new ItemStack(item, count), 9999, 0, 0f));
    }
}
