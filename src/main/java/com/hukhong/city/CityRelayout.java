package com.hukhong.city;

import com.hukhong.auction.AuctionData;
import com.hukhong.city.ambient.AmbientNpc;
import com.hukhong.guild.GuildBuilder;
import com.hukhong.guild.GuildHalls;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Bo cuc Khu Cho / Hiep Hoi: lap ke hoach mot lan (luu CityLayouts), di cu the gioi cu (go cong trinh cu o cho sai, khoi phuc dia hinh,
 * sua tuong/nha thanh thu tuc bi de), noi loi di tu cong thanh, lenh {@code /hukhong city relayout <idx|all>}. Chi chay tren server thread.
 */
public final class CityRelayout {
    private CityRelayout() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final int FLAGS = 18;

    // ---------------------------------------------------------------- dia hinh tu nhien (khong sinh chunk)

    private static int base(ServerLevel l, int x, int z, Heightmap.Types t) {
        return l.getChunkSource().getGenerator().getBaseHeight(x, z, t, l, l.getChunkSource().randomState());
    }

    private static boolean wetBiome(ServerLevel l, int x, int z) {
        var b = l.getBiome(new BlockPos(x, l.getSeaLevel(), z));
        return b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_RIVER);
    }

    /** Dia hinh goc theo bo sinh the gioi (khong doc chunk): cao do = block tren cung, nuoc = bioma bien/song hoac day bien thap hon muc bien. */
    public static CityLayout.Terrain generatorTerrain(ServerLevel l) {
        return new CityLayout.Terrain() {
            @Override public int height(int x, int z) { return base(l, x, z, Heightmap.Types.WORLD_SURFACE_WG) - 1; }
            @Override public boolean wet(int x, int z) {
                return wetBiome(l, x, z) || base(l, x, z, Heightmap.Types.OCEAN_FLOOR_WG) < l.getSeaLevel() - 1;
            }
        };
    }

    // ---------------------------------------------------------------- ngu canh thanh

    static CityLayout.Rect cityRect(ServerLevel ow, CityDef d, BlockPos center) {
        var st = WorldBuildData.get(ow).settlements().get(d.id());
        if (st != null) return new CityLayout.Rect(st.minX(), st.minZ(), st.maxX(), st.maxZ());
        return new CityLayout.Rect(center.getX() - CityBuilder.HALF, center.getZ() - CityBuilder.HALF, center.getX() + CityBuilder.HALF, center.getZ() + CityBuilder.HALF);
    }

    private static List<CityLayout.Rect> avoidList(ServerLevel ow, String selfId) {
        List<CityLayout.Rect> out = new ArrayList<>();
        for (var s : WorldBuildData.get(ow).settlements().values())
            if (!s.id().equals(selfId)) out.add(new CityLayout.Rect(s.minX(), s.minZ(), s.maxX(), s.maxZ()).inflate(CityLayout.EXCL));
        for (var e : CityLayouts.get(ow).all().entrySet()) {
            if (e.getKey().equals(selfId)) continue;
            if (e.getValue().market() != null) out.add(CityLayout.marketRect(e.getValue().market()));
            if (e.getValue().guild() != null) out.add(CityLayout.guildRect(e.getValue().guild()));
        }
        for (BlockPos c : com.hukhong.society.SectSavedData.get(ow).all().values())
            out.add(new CityLayout.Rect(c.getX(), c.getZ(), c.getX(), c.getZ()).inflate(com.hukhong.society.SectBuilder.HALF + 6));
        return out;
    }

    private static boolean manMade(BlockState s) {
        return !(s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.PODZOL) || s.is(Blocks.MYCELIUM)
                || s.is(Blocks.SAND) || s.is(Blocks.RED_SAND) || s.is(Blocks.GRAVEL) || s.is(Blocks.STONE) || s.is(Blocks.SNOW_BLOCK)
                || s.is(Blocks.SNOW) || s.is(Blocks.MUD) || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.is(BlockTags.TERRACOTTA)
                || s.is(BlockTags.FLOWERS) || s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN) || s.isAir());
    }

    /** Vung trong thanh la dat tu nhien (bat ky cong trinh nao -> khong). Chunk chua nap -> khong. */
    private static Predicate<CityLayout.Rect> naturalInside(ServerLevel l) {
        return r -> {
            for (int x = r.minX(); x <= r.maxX(); x += 2) for (int z = r.minZ(); z <= r.maxZ(); z += 2) {
                if (!l.hasChunkAt(new BlockPos(x, 64, z))) return false;
                if (manMade(l.getBlockState(new BlockPos(x, l.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z)))) return false;
            }
            return true;
        };
    }

    // ---------------------------------------------------------------- ke hoach + di cu

    /**
     * Bo cuc cua thanh d; neu chua co thi di cu cong trinh cu (neu co) roi lap + luu ke hoach moi. null = chua san sang
     * (thanh chua dung / chunk cua cong trinh cu chua nap -> thu lai lan sau).
     */
    public static CityLayout.Layout ensurePlanned(ServerLevel ow, CityDef d) {
        if (!MarketDistrict.eligible(d)) return null;
        CityLayouts store = CityLayouts.get(ow);
        CityLayout.Layout lay = store.get(d.id());
        if (lay != null) return lay;
        BlockPos city = CityService.builtCenter(ow, d);
        if (city == null) return null;
        if (!oldLoaded(ow, d, city)) return null;
        return migrateAndPlan(ow, d, city, generatorTerrain(ow), naturalInside(ow));
    }

    /** Loi (kiem thu duoc): go cong trinh cu + lap ke hoach + luu. */
    static CityLayout.Layout migrateAndPlan(ServerLevel ow, CityDef d, BlockPos city, CityLayout.Terrain nat, Predicate<CityLayout.Rect> insideOk) {
        migrateOld(ow, d, city, nat);
        var rect = cityRect(ow, d, city);
        CityLayout.Layout lay = CityLayout.plan(d.category(), rect, city.getX(), city.getZ(), nat, avoidList(ow, d.id()), insideOk);
        if (lay.market() == null || lay.guild() == null)
            LOG.warn("City layout of {}: no valid plot for {}{} (falling back to legacy placement)", d.id(), lay.market() == null ? "market " : "", lay.guild() == null ? "guild" : "");
        CityLayouts.get(ow).put(d.id(), lay);
        LOG.info("City layout of {}: market {} guild {}", d.id(), lay.market(), lay.guild());
        return lay;
    }

    // ---------------------------------------------------------------- cong trinh cu

    /** Vet cu cua Khu Cho = vuong +-(HALF+1) quanh tam da dung; cua Hiep Hoi = vet dai sanh quanh vi tri le tan (huong Tay neu chua co o). */
    private static CityLayout.Rect oldMarketRect(ServerLevel ow, CityDef d) {
        CityLayout.Plot p = CityLayouts.get(ow).market(d.id());
        BlockPos b = MarketDistricts.get(ow).built(d.id());
        if (b == null) return null;
        int h = (p != null && p.cx() == b.getX() && p.cz() == b.getZ()) ? CityLayout.MARKET_HALF : MarketDistrict.HALF + 2;
        return new CityLayout.Rect(b.getX() - h, b.getZ() - h, b.getX() + h, b.getZ() + h);
    }

    private static CityLayout.Plot oldGuildPlot(ServerLevel ow, CityDef d, BlockPos npc) {
        CityLayout.Plot p = CityLayouts.get(ow).guild(d.id());
        if (p != null) { // o moi: le tan o (cx,cz) lui 1 theo huong mat tien
            Direction o = p.dir();
            if (npc.getX() == p.cx() - o.getStepX() && npc.getZ() == p.cz() - o.getStepZ()) return p;
        }
        return new CityLayout.Plot(npc.getX() - 1, npc.getZ(), npc.getY() - 2, Direction.WEST.get2DDataValue()); // duong cu: huong Tay, le tan o hx+1
    }

    private static boolean loaded(ServerLevel l, CityLayout.Rect r) {
        for (int x : new int[]{r.minX(), r.cx(), r.maxX()}) for (int z : new int[]{r.minZ(), r.cz(), r.maxZ()})
            if (!l.hasChunkAt(new BlockPos(x, 64, z))) return false;
        return true;
    }

    private static boolean oldLoaded(ServerLevel ow, CityDef d, BlockPos city) {
        var mr = oldMarketRect(ow, d);
        if (mr != null && !loaded(ow, mr)) return false;
        BlockPos g = GuildHalls.get(ow).get(d.id());
        if (g != null && !loaded(ow, CityLayout.guildRect(oldGuildPlot(ow, d, g)))) return false;
        return true;
    }

    /** Nap (dong bo) cac chunk cua hop. */
    public static void load(ServerLevel l, CityLayout.Rect r) {
        for (int cx = r.minX() >> 4; cx <= r.maxX() >> 4; cx++) for (int cz = r.minZ() >> 4; cz <= r.maxZ() >> 4; cz++) l.getChunk(cx, cz);
    }

    /** Go Khu Cho / Hiep Hoi o vi tri cu (neu da dung), khoi phuc dia hinh, sua tuong/nha bi de, don NPC + diem dich chuyen. Idempotent. */
    static void migrateOld(ServerLevel ow, CityDef d, BlockPos city, CityLayout.Terrain nat) {
        MarketDistricts md = MarketDistricts.get(ow);
        GuildHalls halls = GuildHalls.get(ow);
        var cityRect = cityRect(ow, d, city);
        var st = WorldBuildData.get(ow).settlements().get(d.id());
        boolean procedural = st != null && st.maxX() - st.minX() == 2 * CityBuilder.HALF && !d.category().equals("city_ruined");
        int cityBase = st == null ? city.getY() : (d.category().equals("city_mountain") ? st.y() - 9 : st.y() - 1);

        CityLayout.Rect mr = oldMarketRect(ow, d);
        if (mr != null) {
            BlockPos b = md.built(d.id());
            load(ow, mr);
            wipeMarketNpcs(ow, mr);
            restore(ow, mr, cityRect, cityBase, nat, b.getY() + 16);
            AuctionData.get(ow).halls.remove(d.id());
            AuctionData.get(ow).setDirty();
            LOG.info("City relayout {}: removed old market at {}", d.id(), b);
        }
        md.forget(d.id());

        BlockPos g = halls.get(d.id());
        if (g != null) {
            CityLayout.Plot op = oldGuildPlot(ow, d, g);
            CityLayout.Rect gr = CityLayout.guildRect(op);
            load(ow, gr);
            GuildBuilder.removeOld(ow, g);
            restore(ow, gr, cityRect, cityBase, nat, g.getY() + 24);
            if (procedural && gr.intersects(cityRect)) { // hall cu da de len tuong/nha: dung lai dung phan bi de
                int x0 = Math.max(gr.minX(), cityRect.minX()), z0 = Math.max(gr.minZ(), cityRect.minZ());
                int x1 = Math.min(gr.maxX(), cityRect.maxX()), z1 = Math.min(gr.maxZ(), cityRect.maxZ());
                CityBuilder.repairProcedural(ow, city, d, new int[]{x0, z0, x1, z1}, cityBase);
            }
            LOG.info("City relayout {}: removed old guild hall at {}", d.id(), g);
        }
        halls.remove(d.id());
    }

    private static void wipeMarketNpcs(ServerLevel l, CityLayout.Rect r) {
        AABB box = new AABB(r.minX(), l.getMinBuildHeight(), r.minZ(), r.maxX() + 1, l.getMaxBuildHeight(), r.maxZ() + 1);
        for (WanderingTrader t : l.getEntitiesOfClass(WanderingTrader.class, box, t -> t.getTags().contains(CityMarket.TAG))) t.discard();
        for (AmbientNpc n : l.getEntitiesOfClass(AmbientNpc.class, box))
            if (n.role() == AmbientNpc.AUCTIONEER || n.role() == AmbientNpc.SEER) n.discard();
    }

    private static boolean ground(BlockState s) {
        return s.blocksMotion() && s.getFluidState().isEmpty() && !s.is(BlockTags.LEAVES) && !s.is(BlockTags.LOGS);
    }

    /**
     * Khoi phuc dia hinh trong hop r: don moi thu tren mat dat (toi topY), dap lai mat dat tu nhien (cot nam trong thanh -> mat nen cua thanh,
     * cot ngoai -> cao do goc cua the gioi), co/dat theo vien quanh. Khong dao sau hon mat dat.
     */
    static void restore(ServerLevel l, CityLayout.Rect r, CityLayout.Rect city, int cityBase, CityLayout.Terrain nat, int topY) {
        BlockState top = Blocks.GRASS_BLOCK.defaultBlockState(), filler = Blocks.DIRT.defaultBlockState();
        for (int[] o : new int[][]{{r.cx(), r.minZ() - 3}, {r.cx(), r.maxZ() + 3}, {r.minX() - 3, r.cz()}, {r.maxX() + 3, r.cz()}}) {
            BlockPos p = new BlockPos(o[0], 64, o[1]);
            if (!l.hasChunkAt(p) || city.contains(o[0], o[1])) continue;
            BlockPos s = new BlockPos(o[0], l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, o[0], o[1]) - 1, o[1]);
            BlockState st = l.getBlockState(s);
            if (st.is(Blocks.WATER) || !ground(st)) continue;
            top = st;
            BlockState below = l.getBlockState(s.below());
            filler = ground(below) ? below : Blocks.DIRT.defaultBlockState();
            break;
        }
        for (int x = r.minX(); x <= r.maxX(); x++) for (int z = r.minZ(); z <= r.maxZ(); z++) {
            int tgt = city.contains(x, z) ? cityBase : nat.height(x, z);
            for (int y = topY; y > tgt; y--) {
                BlockPos bp = new BlockPos(x, y, z);
                if (!l.getBlockState(bp).isAir()) l.setBlock(bp, Blocks.AIR.defaultBlockState(), FLAGS);
            }
            l.setBlock(new BlockPos(x, tgt, z), top, FLAGS);
            for (int dd = 1; dd <= 14; dd++) {
                BlockPos bp = new BlockPos(x, tgt - dd, z);
                BlockState cur = l.getBlockState(bp);
                if (dd > 2 && ground(cur)) break;
                l.setBlock(bp, dd <= 2 ? filler : Blocks.STONE.defaultBlockState(), FLAGS);
            }
        }
    }

    // ---------------------------------------------------------------- nen mong + loi di

    /** Vien dem quanh o Khu Cho (vong ngoai HALF+1): lap tru da xuong dat chac, don vat can phia tren, phu co. */
    public static void marginFoundation(ServerLevel l, BlockPos c, int innerHalf, int outerHalf) {
        int y0 = c.getY();
        for (int x = c.getX() - outerHalf; x <= c.getX() + outerHalf; x++) for (int z = c.getZ() - outerHalf; z <= c.getZ() + outerHalf; z++) {
            if (Math.max(Math.abs(x - c.getX()), Math.abs(z - c.getZ())) <= innerHalf) continue;
            for (int h = 1; h <= 12; h++) {
                BlockPos bp = new BlockPos(x, y0 + h, z);
                if (!l.getBlockState(bp).isAir()) l.setBlock(bp, Blocks.AIR.defaultBlockState(), FLAGS);
            }
            for (int dd = 1; dd <= 16; dd++) {
                BlockPos bp = new BlockPos(x, y0 - dd, z);
                if (ground(l.getBlockState(bp))) break;
                l.setBlock(bp, (dd == 1 ? Blocks.DIRT : Blocks.STONE).defaultBlockState(), FLAGS);
            }
            l.setBlock(new BlockPos(x, y0, z), Blocks.GRASS_BLOCK.defaultBlockState(), FLAGS);
        }
    }

    private static int surface(ServerLevel l, int x, int z) {
        l.getChunk(x >> 4, z >> 4);
        int y = l.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        while (y > l.getMinBuildHeight() + 8) {
            BlockState s = l.getBlockState(new BlockPos(x, y, z));
            if (s.isAir() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced()) y--; else break;
        }
        return y;
    }

    private static void pathLine(ServerLevel l, int x1, int z1, int x2, int z2) {
        boolean alongX = z1 == z2;
        int a = alongX ? Math.min(x1, x2) : Math.min(z1, z2), b = alongX ? Math.max(x1, x2) : Math.max(z1, z2);
        for (int t = a; t <= b; t++) for (int w = -1; w <= 1; w++) {
            int x = alongX ? t : x1 + w, z = alongX ? z1 + w : t;
            int y = surface(l, x, z);
            BlockPos bp = new BlockPos(x, y, z);
            if (!l.getFluidState(bp).isEmpty() || !l.getFluidState(bp.above()).isEmpty()) continue; // khong lat len nuoc
            l.setBlock(bp, (w == 0 ? Blocks.COBBLESTONE : Blocks.STONE_BRICKS).defaultBlockState(), FLAGS);
            for (int h = 1; h <= 3; h++) {
                BlockState s = l.getBlockState(bp.above(h));
                if (!s.isAir() && (s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced())) l.setBlock(bp.above(h), Blocks.AIR.defaultBlockState(), FLAGS);
            }
        }
    }

    /** Loi di 3 o rong tu cong thanh toi mat tien o (O ngoai thanh). market: true = Khu Cho, false = Hiep Hoi. Bo qua neu o nam trong thanh. */
    public static void path(ServerLevel l, CityDef d, boolean market) {
        CityLayout.Plot p = market ? CityLayouts.get(l).market(d.id()) : CityLayouts.get(l).guild(d.id());
        BlockPos city = CityService.builtCenter(l, d);
        if (p == null || city == null) return;
        path(l, cityRect(l, d, city), p, market);
    }

    static void path(ServerLevel l, CityLayout.Rect city, CityLayout.Plot p, boolean market) {
        CityLayout.Rect r = market ? CityLayout.marketRect(p) : CityLayout.guildRect(p);
        if (r.intersects(city.inflate(CityLayout.EXCL))) return; // o nam trong thanh: khong can loi di
        Direction face = p.dir(), out = face.getOpposite(); // out = tu thanh ra o
        int half = market ? CityLayout.MARKET_HALF : CityLayout.GUILD_FRONT;
        boolean zAxis = out.getAxis() == Direction.Axis.Z;
        int[] gate = CityLayout.gatePoint(city, out);
        int gAx = zAxis ? gate[1] : gate[0], gLat = zAxis ? gate[0] : gate[1];
        int pAx = zAxis ? p.cz() : p.cx(), pLat = zAxis ? p.cx() : p.cz();
        int s = out.getAxisDirection().getStep();
        int near = pAx - s * (half + 1), end = pAx - s * (market ? half - 2 : half);
        for (int[] seg : new int[][]{{gAx, gLat, near, gLat}, {near, gLat, near, pLat}, {near, pLat, end, pLat}}) {
            int x1 = zAxis ? seg[1] : seg[0], z1 = zAxis ? seg[0] : seg[1], x2 = zAxis ? seg[3] : seg[2], z2 = zAxis ? seg[2] : seg[3];
            if (x1 == x2 && z1 == z2) continue;
            pathLine(l, x1, z1, x2, z2);
        }
    }

    // ---------------------------------------------------------------- lenh

    /** /hukhong city relayout <idx|all>: go + khoi phuc + lap lai bo cuc va dung lai Khu Cho/Hiep Hoi o o moi. */
    public static int relayout(ServerLevel ow, CityDef d) {
        BlockPos city = CityService.builtCenter(ow, d);
        if (city == null || !MarketDistrict.eligible(d)) return 0;
        var cr = cityRect(ow, d, city);
        load(ow, cr.inflate(8));
        migrateOld(ow, d, city, generatorTerrain(ow));
        CityLayouts.get(ow).remove(d.id());
        CityLayout.Layout lay = ensurePlanned(ow, d);
        if (lay == null) return 0;
        if (lay.market() != null) load(ow, CityLayout.marketRect(lay.market()).inflate(2));
        if (lay.guild() != null) load(ow, CityLayout.guildRect(lay.guild()).inflate(2));
        boolean a = MarketDistrict.ensure(ow, d), b = GuildBuilder.ensure(ow, d);
        LOG.info("City relayout {} done: market {} guild {}", d.id(), a, b);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("city").then(Commands.literal("relayout")
                .then(Commands.argument("target", StringArgumentType.word()).suggests((c, b) -> {
                    b.suggest("all");
                    for (CityDef d : CityDef.ALL) b.suggest(d.index());
                    return b.buildFuture();
                }).executes(c -> {
                    ServerLevel ow = c.getSource().getServer().overworld();
                    String t = StringArgumentType.getString(c, "target");
                    int n = 0;
                    for (CityDef d : CityDef.ALL) {
                        if (!t.equals("all") && !t.equals(String.valueOf(d.index()))) continue;
                        try { n += relayout(ow, d); }
                        catch (RuntimeException ex) { LOG.error("relayout {} failed: {}", d.id(), ex.toString(), ex); }
                    }
                    final int done = n;
                    c.getSource().sendSuccess(() -> Component.literal("city relayout: " + done + " city(ies) re-laid out"), true);
                    return n;
                })));
    }
}
