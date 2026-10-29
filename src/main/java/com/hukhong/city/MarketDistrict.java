package com.hukhong.city;

import com.hukhong.structures.StructurePlacer;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.hukhong.structures.ProcKit.air;
import static com.hukhong.structures.ProcKit.set;

/**
 * Khu Cho (Thuong Hoi) rieng cua moi thanh: o dat kho cach tam thanh 40-70 block (chon bang {@link DrySites#suitable},
 * tat dinh theo seed), nen 33x33 co cong chao huong ve thanh + 6 sap (CityMarket.stall) + gieng giua.
 * Thuong nhan chi dung o day (khong con quanh An Hon Dai). Dung lazily khi nguoi choi o gan va chunk da nap
 * (the gioi cu: dung o lan toi gan dau tien va don thuong nhan cu trong thanh). Chi chay tren server thread.
 */
public final class MarketDistrict {
    private MarketDistrict() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final int HALF = 16, MIN_DIST = 40, MAX_DIST = 70;
    /** Khoang cam thuong nhan quanh An Hon Dai (kiem thu + don dep). */
    public static final int ALTAR_CLEAR = 32;

    /** Loai thuong nhan theo tung sap: {u, v, kind}. u doc mat tien cong, v huong ve cong (+ = cong). */
    private static final int[][] STALLS = {
            {-10, -9, 0}, {0, -11, 8}, {10, -9, 1}, {-11, 0, 4}, {11, 0, 6}, {-10, 8, 7}};

    public static boolean eligible(CityDef d) { return !d.category().equals("city_ruined"); }

    // ---------------------------------------------------------------- chon vi tri

    private static final int[] DISTS = {55, 62, 48, 68, 42};
    /** 5 huong khong gan Dong (Hiep Hoi o phia Dong An Hon Dai): 0=E,1=SE,...; goc = i*45 do. */
    private static final int[] DIRS = {2, 3, 4, 5, 6};
    public static final int MAX_CANDIDATES = DISTS.length * DIRS.length;

    /** Ung vien (x,z) thu k: huong & khoang cach tat dinh theo seed + id. Thuan (kiem thu duoc). */
    public static int[] candidate(long seed, String id, int cx, int cz, int k) {
        int start = Math.floorMod((int) ((seed ^ (id.hashCode() * 0x9E3779B1L)) >>> 3), DIRS.length);
        int dist = DISTS[Math.min(k / DIRS.length, DISTS.length - 1)];
        int a = DIRS[Math.floorMod(start + k % DIRS.length, DIRS.length)];
        double ang = Math.toRadians(a * 45.0);
        return new int[]{cx + (int) Math.round(Math.cos(ang) * dist), cz + (int) Math.round(Math.sin(ang) * dist)};
    }

    /** Chon & luu vi tri khu cho. */
    static BlockPos pickSite(ServerLevel ow, CityDef d, BlockPos city) {
        MarketDistricts md = MarketDistricts.get(ow);
        BlockPos s = md.site(d.id());
        if (s != null) return s;
        for (int slope : new int[]{6, 14, 1000}) {
            for (int k = 0; k < MAX_CANDIDATES; k++) {
                int[] c = candidate(ow.getSeed(), d.id(), city.getX(), city.getZ(), k);
                if (DrySites.suitable(ow, c[0], c[1], HALF + 2, slope)) {
                    s = new BlockPos(c[0], 64, c[1]);
                    md.setSite(d.id(), s);
                    LOG.info("Market site of {}: {} (slope <= {})", d.id(), s, slope);
                    return s;
                }
            }
        }
        int[] c = candidate(ow.getSeed(), d.id(), city.getX(), city.getZ(), 0); // khong tim duoc: dung ung vien dau (se san phang)
        s = new BlockPos(c[0], 64, c[1]);
        md.setSite(d.id(), s);
        LOG.warn("Market site of {}: no dry candidate, forcing {}", d.id(), s);
        return s;
    }

    private static boolean footprintLoaded(ServerLevel l, BlockPos c) {
        int h = HALF + 2;
        for (int sx = -1; sx <= 1; sx++) for (int sz = -1; sz <= 1; sz++)
            if (!l.hasChunkAt(new BlockPos(c.getX() + sx * h, 64, c.getZ() + sz * h))) return false;
        return true;
    }

    // ---------------------------------------------------------------- ensure (lazy)

    /** Bao dam khu cho da dung + thuong nhan o dung cho. true neu da co hoac vua dung xong. Goi khi nguoi choi o gan thanh. */
    public static boolean ensure(ServerLevel ow, CityDef d) {
        if (!eligible(d)) return false;
        BlockPos city = CityService.builtCenter(ow, d);
        if (city == null) return false;
        MarketDistricts md = MarketDistricts.get(ow);
        BlockPos done = md.built(d.id());
        if (done != null) {
            ensureTraders(ow, d, done);
            cleanupAltar(ow, city);
            return true;
        }
        BlockPos site = pickSite(ow, d, city);
        if (site == null || !footprintLoaded(ow, site)) return false;
        BlockPos c = build(ow, d, site, city);
        md.setBuilt(d.id(), c);
        cleanupAltar(ow, city);
        LOG.info("Market district of {} built at {}", d.id(), c);
        return true;
    }

    /** Don thuong nhan con sot trong ban kinh ALTAR_CLEAR quanh An Hon Dai (the gioi cu / thanh vua dung). Tra so con bi xoa. */
    public static int cleanupAltar(ServerLevel ow, BlockPos city) {
        BlockPos a = city.above();
        if (!ow.hasChunkAt(a)) return 0;
        int r = ALTAR_CLEAR + 8, n = 0;
        for (WanderingTrader t : ow.getEntitiesOfClass(WanderingTrader.class, new AABB(a).inflate(r, 24, r),
                t -> t.getTags().contains(CityMarket.TAG))) {
            double dx = t.getX() - a.getX(), dz = t.getZ() - a.getZ();
            if (dx * dx + dz * dz <= (double) r * r) { t.discard(); n++; }
        }
        if (n > 0) LOG.info("Removed {} legacy merchants around the altar at {}", n, a);
        return n;
    }

    /** Sinh lai thuong nhan bi mat o khu cho (kiem tra theo loai). */
    static void ensureTraders(ServerLevel l, CityDef d, BlockPos c) {
        if (!l.hasChunkAt(c)) return;
        Direction gate = gateDir(l, c, d);
        AABB box = new AABB(c).inflate(HALF + 2, 8, HALF + 2);
        List<WanderingTrader> have = l.getEntitiesOfClass(WanderingTrader.class, box, t -> t.getTags().contains(CityMarket.TAG));
        for (int[] s : STALLS) {
            BlockPos p = stallPos(c, gate, s[0], s[1]).above();
            boolean found = false;
            for (WanderingTrader t : have)
                if (t.blockPosition().distSqr(p) <= 9 && com.hukhong.currency.ShopCatalog.kindOf(t) == s[2]) { found = true; break; }
            if (!found && l.hasChunkAt(p)) CityMarket.spawn(l, p, s[2], d);
        }
    }

    // ---------------------------------------------------------------- hinh hoc

    /** Huong tu khu cho ve thanh (truc troi): cong chao nam o canh nay. */
    static Direction gateDir(ServerLevel l, BlockPos c, CityDef d) {
        BlockPos city = CityService.builtCenter(l, d);
        if (city == null) return Direction.NORTH;
        int dx = city.getX() - c.getX(), dz = city.getZ() - c.getZ();
        return Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    private static int wx(BlockPos c, Direction g, int u, int v) { return c.getX() + g.getStepX() * v + g.getClockWise().getStepX() * u; }
    private static int wz(BlockPos c, Direction g, int u, int v) { return c.getZ() + g.getStepZ() * v + g.getClockWise().getStepZ() * u; }

    /** O san (mat nen = c.y) tai toa do cuc bo (u,v). */
    static BlockPos stallPos(BlockPos c, Direction g, int u, int v) { return new BlockPos(wx(c, g, u, v), c.getY(), wz(c, g, u, v)); }

    /** Cac o dung cua thuong nhan trong khu cho da dung. */
    public static List<BlockPos> traderSpots(ServerLevel l, CityDef d, BlockPos c) {
        Direction g = gateDir(l, c, d);
        List<BlockPos> out = new ArrayList<>();
        for (int[] s : STALLS) out.add(stallPos(c, g, s[0], s[1]).above());
        return out;
    }

    /** Dung khu cho tai site (tam x,z) cho thanh d. Tra tam (y = mat san). */
    public static BlockPos build(ServerLevel l, CityDef d, BlockPos site, BlockPos city) {
        int cx = site.getX(), cz = site.getZ();
        int y0 = StructurePlacer.surfaceY(l, cx, cz);
        BlockPos c = new BlockPos(cx, y0, cz);
        Direction g = gateDir(l, c, d);
        // 1) san phang + lat nen
        for (int u = -HALF - 1; u <= HALF + 1; u++) for (int v = -HALF - 1; v <= HALF + 1; v++) {
            int x = wx(c, g, u, v), z = wz(c, g, u, v);
            boolean edge = Math.abs(u) >= HALF || Math.abs(v) >= HALF;
            boolean skirt = Math.abs(u) > HALF || Math.abs(v) > HALF;
            for (int dd = 1; dd <= 14; dd++) {
                BlockPos bp = new BlockPos(x, y0 - dd, z);
                var s = l.getBlockState(bp);
                if (s.blocksMotion() && l.getFluidState(bp).isEmpty()) break;
                l.setBlock(bp, (dd == 1 ? Blocks.DIRT : Blocks.STONE).defaultBlockState(), 18);
            }
            for (int h = 1; h <= 14; h++) air(l, x, y0 + h, z);
            if (skirt) { set(l, x, y0, z, Blocks.GRASS_BLOCK); continue; }
            Block floor;
            if (edge) floor = Blocks.DEEPSLATE_BRICKS;
            else if (Math.abs(u) <= 1 || Math.abs(v) <= 1) floor = Blocks.STONE_BRICKS;           // loi di chu thap
            else floor = ((u + v) & 1) == 0 ? Blocks.POLISHED_ANDESITE : Blocks.SMOOTH_STONE;
            set(l, x, y0, z, floor);
        }
        // 2) hang rao + cot den
        for (int t = -HALF; t <= HALF; t++) for (int side = 0; side < 4; side++) {
            int u = side < 2 ? t : (side == 2 ? -HALF : HALF), v = side < 2 ? (side == 0 ? -HALF : HALF) : t;
            if (v == HALF && Math.abs(u) <= 3) continue; // cong
            int x = wx(c, g, u, v), z = wz(c, g, u, v);
            set(l, x, y0 + 1, z, Blocks.DARK_OAK_FENCE);
            if (Math.floorMod(t, 6) == 0) {
                for (int h = 1; h <= 3; h++) set(l, x, y0 + h, z, Blocks.STRIPPED_DARK_OAK_LOG);
                set(l, x, y0 + 4, z, Blocks.LANTERN);
            }
        }
        // 3) cong chao huong ve thanh (v = HALF)
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            int u = 3 * sgn;
            set(l, wx(c, g, u, HALF), y0, wz(c, g, u, HALF), Blocks.POLISHED_BLACKSTONE_BRICKS);
            for (int h = 1; h <= 6; h++) set(l, wx(c, g, u, HALF), y0 + h, wz(c, g, u, HALF), Blocks.STRIPPED_CRIMSON_STEM);
            set(l, wx(c, g, 4 * sgn, HALF), y0 + 8, wz(c, g, 4 * sgn, HALF), slab(Blocks.CRIMSON_SLAB));
            set(l, wx(c, g, 5 * sgn, HALF), y0 + 8, wz(c, g, 5 * sgn, HALF), slab(Blocks.CRIMSON_SLAB));
            set(l, wx(c, g, 2 * sgn, HALF), y0 + 5, wz(c, g, 2 * sgn, HALF), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        for (int u = -4; u <= 4; u++) set(l, wx(c, g, u, HALF), y0 + 7, wz(c, g, u, HALF), Blocks.CRIMSON_PLANKS);
        for (int u = -3; u <= 3; u++) set(l, wx(c, g, u, HALF), y0 + 6, wz(c, g, u, HALF), Blocks.CRIMSON_PLANKS);
        for (int u = -3; u <= 3; u++) set(l, wx(c, g, u, HALF), y0 + 8, wz(c, g, u, HALF), slab(Blocks.CRIMSON_SLAB));
        set(l, wx(c, g, 0, HALF), y0 + 6, wz(c, g, 0, HALF), Blocks.GOLD_BLOCK);
        int rot = g.get2DDataValue() * 4; // bien quay ra phia nguoi vao (huong ve thanh)
        CityBuilder.sign(l, wx(c, g, -5, HALF - 1), y0 + 1, wz(c, g, -5, HALF - 1), rot, "hukhong.market.sign");
        CityBuilder.sign(l, wx(c, g, 5, HALF - 1), y0 + 1, wz(c, g, 5, HALF - 1), rot, "hukhong.market.sign");
        // 4) gieng giua
        for (int u = -2; u <= 2; u++) for (int v = -2; v <= 2; v++) {
            int x = wx(c, g, u, v), z = wz(c, g, u, v);
            boolean rim = Math.abs(u) == 2 || Math.abs(v) == 2;
            set(l, x, y0, z, rim ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.WATER);
            if (rim) set(l, x, y0 + 1, z, ((u + v) & 1) == 0 ? Blocks.STONE_BRICK_WALL : Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
            else set(l, x, y0 - 1, z, Blocks.SEA_LANTERN);
        }
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            int x = wx(c, g, 2 * sx, 2 * sz), z = wz(c, g, 2 * sx, 2 * sz);
            for (int h = 1; h <= 3; h++) set(l, x, y0 + h, z, Blocks.STONE_BRICK_WALL);
            set(l, x, y0 + 4, z, Blocks.LANTERN);
        }
        // 5) sap + thuong nhan (mat sap huong ve tam khu cho)
        BlockPos center = new BlockPos(cx, y0 + 1, cz);
        int idx = 0;
        for (int[] s : STALLS) {
            BlockPos p = stallPos(c, g, s[0], s[1]).above();
            BlockPos at = CityMarket.stall(l, p, center, idx++);
            if (s[2] == com.hukhong.currency.ShopCatalog.K_SPECIAL) { // Quay Dac Biet: tham tim + thach anh
                for (Direction f : Direction.Plane.HORIZONTAL)
                    set(l, p.getX() + f.getStepX() * 2, p.getY(), p.getZ() + f.getStepZ() * 2, Blocks.PURPLE_CARPET);
                set(l, p.getX(), p.getY() + 3, p.getZ(), Blocks.AMETHYST_BLOCK);
            }
            CityMarket.spawn(l, at, s[2], d);
        }
        return c;
    }

    private static BlockState slab(Block b) { return b.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM); }
}
