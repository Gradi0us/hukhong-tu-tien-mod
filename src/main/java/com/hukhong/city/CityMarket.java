package com.hukhong.city;

import com.hukhong.structures.StructurePlacer;
import com.mojang.logging.LogUtils;
import com.hukhong.structures.ProcKit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Cho trong thanh: moi thanh co cac thuong nhan dung yen (thuong nhan lang thang bat tu, tag {@link #TAG} de CityProtection bao ve)
 * dat o marker hukhong:trader (hoac vong quanh quang truong/ban tho). Thay cho khu cho rieng (TradeDistrict) cu.
 * Hang hoa + gia bang Tu Tinh: xem ShopCatalog. Loai: 0 thao duoc, 1 dan duoc, 2 vu khi, 3 giap, 4 Hon Tinh/hiem, 5 tap hoa, 6 bi tich ky nang (thanh cap Dia tro len).
 */
public final class CityMarket {
    private CityMarket() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String TAG = "hukhong_trader";
    private static final String[] KEYS = {"herb", "pill", "weapon", "armor", "soul", "general", "scroll"};

    /** Sau khi dat xong cau truc: xoa thuong nhan cu trong vung roi sinh lai. */
    public static void populate(StructurePlacer.Placed p, CityDef d) { populate(p, d, 0); }

    /** shrineT = ban kinh vet dien dan (0 = khong co): thuong nhan dung trong quay hang quanh mep quang truong. */
    public static void populate(StructurePlacer.Placed p, CityDef d, int shrineT) {
        BlockPos o = p.origin();
        clear(p.level(), new AABB(o.getX(), o.getY() - 8, o.getZ(), o.getX() + p.st().sx, o.getY() + p.st().sy + 8, o.getZ() + p.st().sz));
        BlockPos altar = p.marker("altar", o.offset(p.st().sx / 2, 1, p.st().sz / 2));
        populate(p.level(), d, altar, p.markers("trader"), shrineT);
    }

    public static void populate(ServerLevel l, CityDef d, BlockPos altar, List<BlockPos> markers) { populate(l, d, altar, markers, 0); }

    public static void populate(ServerLevel l, CityDef d, BlockPos altar, List<BlockPos> markers, int shrineT) {
        int kinds = d.tier() == Tier.NHAN ? 6 : 7;
        List<BlockPos> pts = new ArrayList<>(markers);
        int nm = pts.size();
        if (shrineT > 0) { // 8 quay hang quanh mep, huong vao altar
            double rad = AltarShrine.stallRadius(shrineT);
            for (int i = 0; pts.size() < kinds; i++) {
                double ang = Math.toRadians(22.5 + 45 * i);
                pts.add(altar.offset((int) Math.round(Math.cos(ang) * rad), 0, (int) Math.round(Math.sin(ang) * rad)));
            }
        } else {
            int[][] ring = {{7, 0}, {-7, 0}, {0, 7}, {0, -7}, {5, 5}, {-5, 5}, {5, -5}, {-5, -5}};
            for (int i = 0; pts.size() < kinds; i++) {
                int[] r = ring[i % ring.length];
                pts.add(altar.offset(r[0], 0, r[1]));
            }
        }
        int n = 0;
        for (BlockPos pos : pts) {
            BlockPos g = ground(l, pos);
            if (shrineT > 0 && n >= nm) g = stall(l, g, altar, n);
            spawn(l, g, n++ % kinds, d);
        }
        LOG.info("Market of {}: {} merchants around {}", d.id(), n, altar);
    }

    /** Quay hang 3x3 quanh pos (thuong nhan dung o pos, mat huong ve altar) neu cho trong; tra vi tri thuong nhan. */
    static BlockPos stall(ServerLevel l, BlockPos pos, BlockPos altar, int idx) {
        if (!l.hasChunkAt(pos)) return pos;
        int dx = altar.getX() - pos.getX(), dz = altar.getZ() - pos.getZ();
        Direction f = Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
        Direction s = f.getClockWise();
        int y = pos.getY();
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
            BlockPos base = pos.offset(s.getStepX() * a + f.getStepX() * b, 0, s.getStepZ() * a + f.getStepZ() * b);
            if (!l.getBlockState(base.below()).blocksMotion() || !l.getFluidState(base.below()).isEmpty()) return pos;
            for (int h = 0; h <= 3; h++) if (!l.getBlockState(base.above(h)).isAir() && !(a == 0 && b == 0 && h == 0)) {
                var st = l.getBlockState(base.above(h));
                if (!(st.canBeReplaced() || st.is(net.minecraft.tags.BlockTags.LEAVES))) return pos;
            }
        }
        Block[] wool = {Blocks.RED_WOOL, Blocks.WHITE_WOOL, Blocks.PURPLE_WOOL, Blocks.WHITE_WOOL};
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
            int x = pos.getX() + s.getStepX() * a + f.getStepX() * b, z = pos.getZ() + s.getStepZ() * a + f.getStepZ() * b;
            for (int h = 0; h <= 3; h++) ProcKit.air(l, x, y + h, z);
            ProcKit.set(l, x, y + 3, z, wool[(a + 1 + idx) & 3]);               // mai vai soc
            if (b == -1) { // vach sau
                ProcKit.set(l, x, y, z, Blocks.SPRUCE_PLANKS);
                ProcKit.set(l, x, y + 1, z, a == 0 ? Blocks.SPRUCE_PLANKS : Blocks.BARREL);
                ProcKit.set(l, x, y + 2, z, Blocks.SPRUCE_PLANKS);
            }
        }
        for (int a : new int[]{-1, 1}) { // cot truoc
            int x = pos.getX() + s.getStepX() * a + f.getStepX(), z = pos.getZ() + s.getStepZ() * a + f.getStepZ();
            for (int h = 0; h <= 2; h++) ProcKit.set(l, x, y + h, z, Blocks.SPRUCE_FENCE);
        }
        int cx = pos.getX() + f.getStepX(), cz = pos.getZ() + f.getStepZ();
        ProcKit.set(l, cx, y, cz, Blocks.SPRUCE_SLAB);   // quay
        ProcKit.hangLantern(l, pos.getX(), y + 2, pos.getZ());
        // thuong nhan dung sau vach: dich 0 (giua), quay o phia truoc
        return pos;
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
        t.getOffers().clear(); // gia/hang ban: com.hukhong.currency.ShopCatalog (Tu Tinh); vanilla trade bi chan o ShopService
        l.addFreshEntity(t);
    }
}
