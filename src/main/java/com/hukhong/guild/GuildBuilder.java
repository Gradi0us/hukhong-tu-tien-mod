package com.hukhong.guild;

import com.hukhong.city.CityBuilder;
import com.hukhong.city.CityDef;
import com.hukhong.city.TradeDistrict;
import com.hukhong.city.WorldBuildData;
import com.hukhong.structures.ProcKit;
import com.hukhong.structures.StructurePlacer;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.hukhong.structures.ProcKit.air;
import static com.hukhong.structures.ProcKit.set;

/**
 * Dung Hiep Hoi Mao Hiem Gia o moi thanh / cang: nha 9x7 canh quang truong neu con cho trong, khong thi ngay ben ngoai tuong.
 * Cau truc import co marker hukhong:guild thi dung ban co san (chi sinh le tan + bang tai marker).
 */
public final class GuildBuilder {
    private GuildBuilder() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String NPC_TAG = "hukhong_guild_npc";
    private static final int W = GuildHallBuild.SPAN, D = GuildHallBuild.SPAN; // nua o vuong can cho (dai sanh 15x11 + hien)

    private static final ProcKit.Mat MAT = new ProcKit.Mat(Blocks.STONE_BRICKS, Blocks.DARK_OAK_LOG, Blocks.SPRUCE_PLANKS,
            Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_PLANKS);

    public static boolean eligible(CityDef d) { return !d.category().equals("city_ruined"); }

    /** Bao dam co Hiep Hoi (da dung xong thanh = co Settlement). true neu da co hoac vua dung. */
    public static boolean ensure(ServerLevel ow, CityDef d) {
        if (!eligible(d)) return false;
        GuildHalls halls = GuildHalls.get(ow);
        if (halls.has(d.id())) return true;
        WorldBuildData.Settlement st = WorldBuildData.get(ow).settlements().get(d.id());
        if (st == null) return false;
        int cx = st.cx(), cz = st.cz();
        GuildScan.Hit hit = GuildScan.find(ow, st.ax(), st.az(), st.y()); // thanh import: tan dung cong trinh lon co san
        if (hit != null) {
            set(ow, hit.board().getX(), hit.board().getY(), hit.board().getZ(), GuildRegistry.BOARD.get());
            set(ow, hit.board().getX(), hit.board().getY() + 1, hit.board().getZ(), Blocks.LANTERN);
            spawnNpc(ow, hit.npc());
            halls.put(d.id(), hit.npc());
            LOG.info("Guild hall of {} adopted existing building ({} cells) at {}", d.id(), hit.area(), hit.npc());
            return true;
        }
        int[] spot = findInside(ow, st);
        int floorY, hx, hz;
        Direction out;
        if (spot != null) {
            hx = spot[0]; hz = spot[1]; floorY = spot[2];
            out = Math.abs(cx - hx) >= Math.abs(cz - hz) ? (cx > hx ? Direction.EAST : Direction.WEST) : (cz > hz ? Direction.SOUTH : Direction.NORTH);
        } else {
            int[] adj = adjacent(ow, st);
            if (adj == null) return false;
            hx = adj[0]; hz = adj[1]; floorY = adj[2];
            out = Direction.from3DDataValue(adj[3]);
        }
        BlockPos npc = GuildHallBuild.build(ow, hx, hz, floorY, out);
        spawnNpc(ow, npc);
        halls.put(d.id(), npc);
        LOG.info("Guild hall of {} built at {} ({})", d.id(), npc, spot != null ? "inside" : "adjacent");
        return true;
    }

    /** Chi cong nhan marker hukhong:guild cua cau truc import. */
    public static void placeImported(StructurePlacer.Placed p, CityDef d) {
        if (!eligible(d) || p.st().marker("guild") == null) return;
        ServerLevel l = p.level();
        GuildHalls halls = GuildHalls.get(l);
        if (halls.has(d.id())) return;
        BlockPos m = p.marker("guild", p.origin());
        BlockPos board = l.getBlockState(m.east()).isAir() ? m.east() : m.west();
        set(l, board.getX(), board.getY(), board.getZ(), GuildRegistry.BOARD.get());
        spawnNpc(l, m);
        halls.put(d.id(), m);
    }

    // ---- tim cho ----

    private static boolean flat(ServerLevel l, int x, int z, int[] yOut) {
        int y0 = -9999;
        for (int xx = x - W - 1; xx <= x + W + 1; xx++) for (int zz = z - D - 1; zz <= z + D + 1; zz++) {
            if (!l.hasChunkAt(new BlockPos(xx, 64, zz))) return false;
            int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xx, zz) - 1;
            if (y0 == -9999) y0 = y;
            if (y != y0) return false;
            BlockPos bp = new BlockPos(xx, y, zz);
            if (!l.getFluidState(bp).isEmpty() || !l.getBlockState(bp).blocksMotion()) return false;
        }
        yOut[0] = y0;
        return true;
    }

    private static int[] findInside(ServerLevel l, WorldBuildData.Settlement st) {
        int cx = st.cx(), cz = st.cz();
        List<int[]> cand = new ArrayList<>();
        for (int x = st.minX() + W + 3; x <= st.maxX() - W - 3; x += 3)
            for (int z = st.minZ() + D + 3; z <= st.maxZ() - D - 3; z += 3) {
                if (Math.abs(x - cx) <= 8 || Math.abs(z - cz) <= 8) continue; // quang truong + truc duong
                cand.add(new int[]{x, z});
            }
        cand.sort(Comparator.comparingInt(c -> (c[0] - cx) * (c[0] - cx) + (c[1] - cz) * (c[1] - cz)));
        int[] y = new int[1];
        for (int[] c : cand) if (flat(l, c[0], c[1], y)) return new int[]{c[0], c[1], y[0]};
        return null;
    }

    /** Ngay ben ngoai tuong: thu Dong, Tay, Nam, Bac, 3 lech moi huong. Tra {x, z, floorY, huong cua (3D data value)}. */
    private static int[] adjacent(ServerLevel l, WorldBuildData.Settlement st) {
        int cx = st.cx(), cz = st.cz();
        Direction[] sides = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
        for (Direction s : sides) for (int off : new int[]{0, 12, -12}) {
            int ax = s.getAxis() == Direction.Axis.X ? (s == Direction.EAST ? st.maxX() + 8 + W : st.minX() - 8 - W) : cx + off;
            int az = s.getAxis() == Direction.Axis.Z ? (s == Direction.SOUTH ? st.maxZ() + 8 + D : st.minZ() - 8 - D) : cz + off;
            BlockPos p = new BlockPos(ax, 64, az);
            if (!l.hasChunkAt(p)) continue;
            var b = l.getBiome(p);
            if (b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_RIVER)) continue;
            int gy = l.getHeight(Heightmap.Types.WORLD_SURFACE_WG, ax, az) - 1;
            if (!l.getFluidState(new BlockPos(ax, gy, az)).isEmpty()) continue;
            int y0 = CityBuilder.prepare(l, ax, az, W + 2, 24);
            return new int[]{ax, az, y0, s.getOpposite().get3DDataValue()};
        }
        return null;
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
