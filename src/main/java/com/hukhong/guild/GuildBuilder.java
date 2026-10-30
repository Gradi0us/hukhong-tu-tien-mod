package com.hukhong.guild;

import com.hukhong.city.AltarShrine;
import com.hukhong.city.CityDef;
import com.hukhong.city.TradeDistrict;
import com.hukhong.city.WorldBuildData;
import com.hukhong.structures.StructurePlacer;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

/**
 * Dung Hiep Hoi Mao Hiem Gia o moi thanh / cang: dai sanh luon nam ben PHAI Ấn Hồn Đài (cong torii o truc Nam => nhin tu loi vao
 * huong Bac, ben phai = Dong), sat vanh ngoai dien dan, mat tien quay ve dien dan, noi bang loi di ngan. Chi san/don dung vet dat dai sanh.
 */
public final class GuildBuilder {
    private GuildBuilder() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String NPC_TAG = "hukhong_guild_npc";

    /** Be rong vet dat quanh dai sanh (u: +-(W+2), v: -(D+2)..D+4). */
    private static final int HALF_U = GuildHallBuild.W + 2, V_BACK = GuildHallBuild.D + 2, V_FRONT = GuildHallBuild.D + 4;
    /** Khoang duong di ngan giua vanh ngoai Ấn Hồn Đài va mep dat dai sanh. */
    private static final int PATH_GAP = 3;

    public static boolean eligible(CityDef d) { return !d.category().equals("city_ruined"); }

    /** Ban kinh vet ngoai cua dien dan (T): quet ve phia dong tim vien da/ngoi sam; khong thay thi uoc luong theo loai. */
    static int shrineOuter(ServerLevel l, WorldBuildData.Settlement st, CityDef d) {
        int fy = st.y() - 1;
        for (int t = AltarShrine.MAX_R + 3; t >= 5; t--) { // vet T toi da = MAX_R + 3 (Quang Truong An Hon lon)
            BlockPos p = new BlockPos(st.ax() + t, fy, st.az());
            if (!l.hasChunkAt(p)) break;
            var b = l.getBlockState(p);
            if (b.is(Blocks.POLISHED_BLACKSTONE_BRICKS) || b.is(Blocks.DEEPSLATE_TILES)) return t;
        }
        return AltarShrine.procT(d.category());
    }

    /** Tam dai sanh theo quy tac: ben phai Ấn Hồn Đài (huong Dong), sat vanh ngoai. y = mat nen dien dan. */
    public static BlockPos hallCenter(WorldBuildData.Settlement st, int shrineT) {
        return new BlockPos(st.ax() + shrineT + PATH_GAP + 1 + HALF_U, st.y() - 1, st.az());
    }

    /** Bao dam co Hiep Hoi o dung cho (da dung xong thanh = co Settlement). true neu da co hoac vua dung/dung lai. */
    public static boolean ensure(ServerLevel ow, CityDef d) {
        if (!eligible(d)) return false;
        GuildHalls halls = GuildHalls.get(ow);
        var lay = com.hukhong.city.CityRelayout.ensurePlanned(ow, d); // bo cuc + di cu hall cu truoc moi viec
        if (lay == null) return false;
        if (halls.has(d.id()) && halls.isCurrent(d.id())) return true;
        if (lay.guild() != null) return ensurePlot(ow, d, halls, lay.guild());
        WorldBuildData.Settlement st = WorldBuildData.get(ow).settlements().get(d.id());
        if (st == null) return false;
        int t = shrineOuter(ow, st, d);
        BlockPos hc = hallCenter(st, t);
        if (!footprintLoaded(ow, hc, t, st)) return false;
        if (halls.has(d.id())) removeOld(ow, halls.get(d.id()));
        flatten(ow, hc.getX(), hc.getZ(), hc.getY() - 1);
        pathToShrine(ow, st, t, hc);
        BlockPos npc = GuildHallBuild.build(ow, hc.getX(), hc.getZ(), hc.getY() - 1, Direction.WEST);
        spawnNpc(ow, npc);
        halls.put(d.id(), npc);
        LOG.info("Guild hall of {} built at {} (east of the Altar Shrine)", d.id(), npc);
        return true;
    }

    /** Dung hall tai o rieng da quy hoach (khong de len tuong/nha/dien dan): nen mong, hall quay mat ve thanh, loi di tu cong thanh. */
    private static boolean ensurePlot(ServerLevel ow, CityDef d, GuildHalls halls, com.hukhong.city.CityLayout.Plot plot) {
        var r = com.hukhong.city.CityLayout.guildRect(plot);
        for (int x : new int[]{r.minX() - 1, r.cx(), r.maxX() + 1}) for (int z : new int[]{r.minZ() - 1, r.cz(), r.maxZ() + 1})
            if (!ow.hasChunkAt(new BlockPos(x, plot.y(), z))) return false;
        flattenRect(ow, r, plot.y() - 1);
        BlockPos npc = GuildHallBuild.build(ow, plot.cx(), plot.cz(), plot.y() - 1, plot.dir());
        spawnNpc(ow, npc);
        halls.put(d.id(), npc);
        try { com.hukhong.city.CityRelayout.path(ow, d, false); }
        catch (RuntimeException ex) { LOG.error("Guild path failed for {}: {}", d.id(), ex.toString(), ex); }
        LOG.info("Guild hall of {} built at {} on its own plot {}", d.id(), npc, plot);
        return true;
    }

    /** Cong trinh import khong con 'tiep quan' / marker: dat theo quy tac chung. */
    public static void placeImported(StructurePlacer.Placed p, CityDef d) {
        if (eligible(d)) ensure(p.level(), d);
    }

    private static boolean footprintLoaded(ServerLevel l, BlockPos hc, int t, WorldBuildData.Settlement st) {
        int[][] pts = {{st.ax() + t + 1, st.az()}, {hc.getX() - V_FRONT, hc.getZ() - HALF_U}, {hc.getX() + V_BACK, hc.getZ() + HALF_U},
                {hc.getX() - V_FRONT, hc.getZ() + HALF_U}, {hc.getX() + V_BACK, hc.getZ() - HALF_U}, {hc.getX(), hc.getZ()}};
        for (int[] p : pts) if (!l.hasChunkAt(new BlockPos(p[0], hc.getY(), p[1]))) return false;
        return true;
    }

    /** Chi san/don dung vet dat dai sanh: lap dat duoi toi 24 o, don 22 o phia tren. */
    private static void flatten(ServerLevel l, int hx, int hz, int floorY) {
        flattenRect(l, new com.hukhong.city.CityLayout.Rect(hx - V_FRONT, hz - HALF_U, hx + V_BACK, hz + HALF_U), floorY);
    }

    private static void flattenRect(ServerLevel l, com.hukhong.city.CityLayout.Rect r, int floorY) {
        for (int x = r.minX(); x <= r.maxX(); x++) for (int z = r.minZ(); z <= r.maxZ(); z++) {
            for (int d = 0; d <= 24; d++) {
                BlockPos bp = new BlockPos(x, floorY - d, z);
                var s = l.getBlockState(bp);
                if (d > 0 && s.blocksMotion() && l.getFluidState(bp).isEmpty() && !s.is(net.minecraft.tags.BlockTags.LEAVES) && !s.is(net.minecraft.tags.BlockTags.LOGS)) break;
                l.setBlock(bp, (d == 0 ? Blocks.STONE_BRICKS : Blocks.STONE).defaultBlockState(), 18);
            }
            for (int h = 1; h <= 22; h++) {
                BlockPos bp = new BlockPos(x, floorY + h, z);
                if (!l.getBlockState(bp).isAir()) l.setBlock(bp, Blocks.AIR.defaultBlockState(), 18);
            }
        }
    }

    /** Loi di ngan 3 o rong tu vanh ngoai dien dan toi bac thang hien dai sanh. */
    private static void pathToShrine(ServerLevel l, WorldBuildData.Settlement st, int t, BlockPos hc) {
        int fy = st.y() - 1;
        for (int x = st.ax() + t + 1; x <= hc.getX() - V_FRONT; x++) for (int dz = -1; dz <= 1; dz++) {
            int z = st.az() + dz;
            for (int d = 1; d <= 24; d++) {
                BlockPos bp = new BlockPos(x, fy - d, z);
                if (l.getBlockState(bp).blocksMotion() && l.getFluidState(bp).isEmpty()) break;
                l.setBlock(bp, Blocks.STONE.defaultBlockState(), 18);
            }
            l.setBlock(new BlockPos(x, fy, z), (Math.abs(dz) == 1 ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState(), 18);
            for (int h = 1; h <= 4; h++) {
                BlockPos bp = new BlockPos(x, fy + h, z);
                if (!l.getBlockState(bp).isAir()) l.setBlock(bp, Blocks.AIR.defaultBlockState(), 18);
            }
        }
    }

    /** Go le tan + bang cua hall cu (vi tri sai) truoc khi dung lai o cho dung. */
    public static void removeOld(ServerLevel l, BlockPos old) {
        if (l.hasChunkAt(old)) {
            for (Villager v : l.getEntitiesOfClass(Villager.class, new AABB(old).inflate(24, 16, 24), v -> v.getTags().contains(NPC_TAG))) v.discard();
        }
        for (int x = old.getX() - 12; x <= old.getX() + 12; x++) for (int z = old.getZ() - 12; z <= old.getZ() + 12; z++) {
            if (!l.hasChunkAt(new BlockPos(x, old.getY(), z))) continue;
            for (int y = old.getY() - 6; y <= old.getY() + 10; y++) {
                BlockPos bp = new BlockPos(x, y, z);
                if (l.getBlockState(bp).is(GuildRegistry.BOARD.get())) {
                    l.setBlock(bp, Blocks.AIR.defaultBlockState(), 18);
                    if (l.getBlockState(bp.above()).is(Blocks.LANTERN)) l.setBlock(bp.above(), Blocks.AIR.defaultBlockState(), 18);
                }
            }
        }
    }

    // ---- le tan ----

    public static void spawnNpc(ServerLevel l, BlockPos pos) {
        Villager v = EntityType.VILLAGER.create(l);
        if (v == null) return;
        v.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        v.setVillagerData(v.getVillagerData().setProfession(VillagerProfession.LIBRARIAN));
        v.addTag(NPC_TAG);
        v.addTag(TradeDistrict.TAG);
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setPersistenceRequired();
        v.setCustomName(Component.translatable("hukhong.guild.receptionist"));
        v.setCustomNameVisible(true);
        l.addFreshEntity(v);
    }

    /** Le tan mat (bi /kill...) thi sinh lai. */
    public static void ensureNpc(ServerLevel l, BlockPos pos) {
        if (!l.hasChunkAt(pos)) return;
        AABB box = new AABB(pos).inflate(6);
        if (l.getEntitiesOfClass(Villager.class, box, v -> v.getTags().contains(NPC_TAG)).isEmpty()) spawnNpc(l, pos);
    }
}
