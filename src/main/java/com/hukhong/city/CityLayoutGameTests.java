package com.hukhong.city;

import com.hukhong.HuKhong;
import com.hukhong.guild.GuildHalls;
import com.hukhong.network.MapMarkerSync;
import com.hukhong.network.MapMarkersPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Bo cuc Khu Cho / Hiep Hoi: o rieng khong de len nhau/tuong, dia hinh phang, di cu the gioi cu chuyen cho + dau ban do. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class CityLayoutGameTests {
    private CityLayoutGameTests() {}

    private static final CityLayout.Rect CITY = new CityLayout.Rect(-20, -20, 20, 20);

    /** Dia hinh nhan tao: song nhe, ho nuoc phia dong, vach da dung phia dong bac tuy chon. */
    private static CityLayout.Terrain terrain(boolean eastCliff) {
        return new CityLayout.Terrain() {
            @Override public int height(int x, int z) {
                int base = 80 + (int) Math.round(1.4 * Math.sin(x / 23.0) + 1.4 * Math.cos(z / 19.0));
                return eastCliff && x > 25 ? base + (x - 25) / 2 : base;
            }
            @Override public boolean wet(int x, int z) { return x >= 60 && x <= 130 && Math.abs(z) < 30; }
        };
    }

    private static String check(String cat, CityLayout.Layout lay, CityLayout.Terrain t) {
        if (lay.market() == null || lay.guild() == null) return cat + ": missing plot " + lay;
        CityLayout.Rect m = CityLayout.marketRect(lay.market()), g = CityLayout.guildRect(lay.guild());
        CityLayout.Rect city = CITY.inflate(CityLayout.EXCL);
        if (m.intersects(g)) return cat + ": market and guild plots overlap";
        if (m.intersects(city)) return cat + ": market overlaps city walls/margin";
        if (g.intersects(city)) return cat + ": guild overlaps city walls/margin";
        for (var e : new CityLayout.Rect[]{m, g}) {
            int[] sv = CityLayout.survey(t, e);
            if (sv == null) return cat + ": plot on water";
            if (sv[1] - sv[0] > CityLayout.MAX_VAR) return cat + ": height variance " + (sv[1] - sv[0]);
        }
        if (lay.market().y() != CityLayout.survey(t, m)[1]) return cat + ": market floor is not at the highest ground (cut into hill)";
        for (int[] s : CityLayout.stallXZ(lay.market().cx(), lay.market().cz(), lay.market().dir())) {
            double dd = Math.sqrt((double) s[0] * s[0] + (double) s[1] * s[1]);
            if (dd < CityLayout.ALTAR_MIN) return cat + ": stall " + dd + " blocks from the altar";
        }
        return null;
    }

    @GameTest(template = "empty")
    public static void plotsAreDisjointFlatAndDry(GameTestHelper h) {
        for (String cat : new String[]{"city_large", "port", "city_mountain"}) {
            for (boolean cliff : new boolean[]{false, true}) {
                CityLayout.Terrain t = terrain(cliff);
                CityLayout.Layout a = CityLayout.plan(cat, CITY, 0, 0, t, List.of(), null);
                String err = check(cat + (cliff ? "+cliff" : ""), a, t);
                if (err != null) { h.fail(err); return; }
                if (!a.equals(CityLayout.plan(cat, CITY, 0, 0, t, List.of(), null))) { h.fail(cat + ": plan is not deterministic"); return; }
                if (cliff && CityLayout.marketRect(a.market()).minX() > CITY.maxX() && CityLayout.survey(t, CityLayout.marketRect(a.market()))[1] > 100) {
                    h.fail(cat + ": market placed up the east cliff"); return;
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void plotsSkipOtherSettlements(GameTestHelper h) {
        CityLayout.Terrain t = terrain(false);
        CityLayout.Layout base = CityLayout.plan("city_large", CITY, 0, 0, t, List.of(), null);
        CityLayout.Rect blocked = CityLayout.marketRect(base.market()).inflate(1);
        CityLayout.Layout lay = CityLayout.plan("city_large", CITY, 0, 0, t, List.of(blocked), null);
        if (lay.market() == null || CityLayout.marketRect(lay.market()).intersects(blocked)) { h.fail("market ignored the avoid rect"); return; }
        if (lay.guild() != null && CityLayout.guildRect(lay.guild()).intersects(blocked)) { h.fail("guild ignored the avoid rect"); return; }
        h.succeed();
    }

    /** Di cu: Khu Cho + Hiep Hoi cu (cho cu) bi go + khoi phuc, o moi duoc luu, Khu Cho dung lai o o moi, dau ban do chuyen theo. */
    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void relayoutMovesMarketAndMarkers(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        CityDef d = new CityDef(98, "gt_relayout_city", Tier.NHAN, 0, 0); // khong thuoc 10 thanh that: khong dinh Settlement/the gioi that
        BlockPos city = new BlockPos(o.getX() + 900, 100, o.getZ() + 900);
        MarketDistricts md = MarketDistricts.get(l);
        GuildHalls halls = GuildHalls.get(l);
        CityLayouts store = CityLayouts.get(l);
        BlockPos newC = null;
        java.util.List<long[]> forced = new java.util.ArrayList<>();
        for (int cx = (city.getX() - 40) >> 4; cx <= (city.getX() + 130) >> 4; cx++) for (int cz = (city.getZ() - 90) >> 4; cz <= (city.getZ() + 90) >> 4; cz++) {
            l.setChunkForced(cx, cz, true);
            forced.add(new long[]{cx, cz});
        }
        try {
            // the gioi cu: Khu Cho 55 block ve phia dong + Hiep Hoi cu (cao 3 block de kiem tra don dep)
            BlockPos oldC = MarketDistrict.build(l, d, new BlockPos(city.getX() + 55, 100, city.getZ()), city);
            md.setBuilt(d.id(), oldC);
            final int H = oldC.getY();
            CityLayout.Terrain flat = new CityLayout.Terrain() {
                @Override public int height(int x, int z) { return H; }
                @Override public boolean wet(int x, int z) { return false; }
            };
            BlockPos npc = new BlockPos(city.getX() + 31, H + 2, city.getZ());
            halls.put(d.id(), npc);
            for (int dz = -3; dz <= 3; dz++) for (int y = H + 1; y <= H + 5; y++) l.setBlock(new BlockPos(city.getX() + 28, y, city.getZ() + dz), Blocks.STONE_BRICKS.defaultBlockState(), 2);
            if (!hasMarker(l, d.id(), oldC.getX())) { h.fail("old market marker missing before relayout"); return; }

            CityLayout.Layout lay = CityRelayout.migrateAndPlan(l, d, city, flat, null);
            if (lay.market() == null || lay.guild() == null) { h.fail("no plots planned: " + lay); return; }
            if (md.built(d.id()) != null || halls.get(d.id()) != null) { h.fail("old market/guild records not forgotten"); return; }
            if (!store.has(d.id())) { h.fail("layout not stored"); return; }
            // cong trinh cu da bi go, mat dat tra ve nguyen thuy
            CityLayout.Rect old = new CityLayout.Rect(oldC.getX() - 17, oldC.getZ() - 17, oldC.getX() + 17, oldC.getZ() + 17);
            for (int x = old.minX(); x <= old.maxX(); x += 3) for (int z = old.minZ(); z <= old.maxZ(); z += 3) {
                for (int y = H + 1; y <= H + 12; y++)
                    if (!l.getBlockState(new BlockPos(x, y, z)).isAir()) { h.fail("old market block left at " + x + "," + y + "," + z); return; }
                if (!l.getBlockState(new BlockPos(x, H, z)).is(Blocks.GRASS_BLOCK)) { h.fail("old market ground not restored at " + x + "," + z); return; }
            }
            for (int dz = -3; dz <= 3; dz++) for (int y = H + 1; y <= H + 5; y++)
                if (!l.getBlockState(new BlockPos(city.getX() + 28, y, city.getZ() + dz)).isAir()) { h.fail("old guild block left"); return; }
            if (!l.getEntitiesOfClass(WanderingTrader.class, new AABB(oldC).inflate(20, 20, 20), t -> t.getTags().contains(CityMarket.TAG)).isEmpty()) {
                h.fail("old traders not removed"); return;
            }
            CityLayout.Rect cr = new CityLayout.Rect(city.getX() - 20, city.getZ() - 20, city.getX() + 20, city.getZ() + 20).inflate(CityLayout.EXCL);
            CityLayout.Rect mr = CityLayout.marketRect(lay.market()), gr = CityLayout.guildRect(lay.guild());
            if (mr.intersects(gr) || mr.intersects(cr) || gr.intersects(cr)) { h.fail("new plots overlap each other or the city walls"); return; }

            // dung lai o o moi (nhu MarketDistrict.ensure)
            CityLayout.Plot p = lay.market();
            newC = MarketDistrict.build(l, d, new BlockPos(p.cx(), p.y(), p.cz()), city, p.y());
            md.setBuilt(d.id(), newC);
            if (newC.equals(oldC) || Math.abs(newC.getX() - oldC.getX()) + Math.abs(newC.getZ() - oldC.getZ()) < 30) { h.fail("market did not move: " + oldC + " -> " + newC); return; }
            if (!hasMarker(l, d.id(), newC.getX()) || hasMarker(l, d.id(), oldC.getX())) { h.fail("map marker did not move to the new market"); return; }
            int n = l.getEntitiesOfClass(WanderingTrader.class, new AABB(newC).inflate(MarketDistrict.HALF, 12, MarketDistrict.HALF),
                    t -> t.getTags().contains(CityMarket.TAG)).size();
            if (n < 6) { h.fail("only " + n + " traders in the relocated market; total near=" + l.getEntitiesOfClass(WanderingTrader.class, new AABB(newC).inflate(60, 40, 60)).size() + " newC=" + newC + " oldC=" + oldC); return; }
            // khong troi: goc + canh deu co nen duoi san
            for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
                BlockPos corner = newC.offset(sx * MarketDistrict.HALF, -1, sz * MarketDistrict.HALF);
                if (!l.getBlockState(corner).blocksMotion()) { h.fail("market floating at corner " + corner); return; }
            }
            h.succeed();
        } finally {
            if (newC != null) for (WanderingTrader t : l.getEntitiesOfClass(WanderingTrader.class, new AABB(newC).inflate(30, 20, 30))) t.discard();
            md.forget(d.id());
            halls.remove(d.id());
            store.remove(d.id());
            for (long[] c : forced) l.setChunkForced((int) c[0], (int) c[1], false);
        }
    }

    private static boolean hasMarker(ServerLevel l, String id, int x) {
        for (var m : MapMarkerSync.collect(l.getServer()))
            if (m.kind() == MapMarkersPayload.MARKET && m.id().equals(id) && m.x() == x) return true;
        return false;
    }
}
