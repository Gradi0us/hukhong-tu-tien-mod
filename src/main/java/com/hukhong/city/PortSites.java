package com.hukhong.city;

import com.hukhong.structures.LoadedStructure;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;

/**
 * Tim bo bien cho cang (xem docs/research/RESEARCH-v1-port-placement.md). Khong sinh chunk: do nuoc bang
 * ChunkGenerator.getBaseHeight(OCEAN_FLOOR_WG) thap hon muc bien + biome IS_OCEAN (noise biome, khong can chunk).
 * Tim theo xoan oc quanh neo, chia nho theo ngan sach thoi gian de khong chan tick.
 */
public final class PortSites {
    private static final Logger LOG = LogUtils.getLogger();
    public static final int MAX_R = 2000, STEP = 48;

    private final ServerLevel l;
    private final LoadedStructure st;
    private final BlockPos anchor;
    private final Direction waterSide;
    private int ring = 0, idx = 0;
    private BlockPos result;
    private boolean finished;

    public PortSites(ServerLevel l, LoadedStructure st, BlockPos anchor) {
        this.l = l; this.st = st; this.anchor = anchor; this.waterSide = waterSide(st);
    }

    public boolean finished() { return finished; }
    public BlockPos result() { return result; }

    /** Canh (dai 16 o ngoai cung) cua cau truc co nhieu nuoc nhat o 3 tang thap nhat; null neu khong co nuoc. */
    public static Direction waterSide(LoadedStructure st) {
        double[] f = new double[4]; // W, E, N, S
        int[] n = new int[4];
        int band = 16;
        for (int z = 0; z < st.sz; z += 2) for (int x = 0; x < st.sx; x += 2) {
            boolean wet = false;
            for (int y = 0; y < Math.min(3, st.sy) && !wet; y++) {
                var s = st.at(x, y, z);
                wet = s != null && s.getFluidState().is(FluidTags.WATER);
            }
            int w = wet ? 1 : 0;
            if (x < band) { f[0] += w; n[0]++; }
            if (x >= st.sx - band) { f[1] += w; n[1]++; }
            if (z < band) { f[2] += w; n[2]++; }
            if (z >= st.sz - band) { f[3] += w; n[3]++; }
        }
        int best = -1;
        double bf = 0.25;
        for (int i = 0; i < 4; i++) if (n[i] > 0 && f[i] / n[i] > bf) { bf = f[i] / n[i]; best = i; }
        return best < 0 ? null : new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}[best];
    }

    private int base(int x, int z, Heightmap.Types t) {
        return l.getChunkSource().getGenerator().getBaseHeight(x, z, t, l, l.getChunkSource().randomState());
    }

    private boolean water(int x, int z) { return base(x, z, Heightmap.Types.OCEAN_FLOOR_WG) < l.getSeaLevel() - 1; }

    /** Ung vien tam (cx,cz): bien lon ngoai canh nuoc, dat o khung giua, khong qua cao. */
    private boolean ok(int cx, int cz) {
        Direction ws = waterSide == null ? Direction.WEST : waterSide;
        int hx = st.sx / 2, hz = st.sz / 2;
        int ox = cx + ws.getStepX() * (hx + 70), oz = cz + ws.getStepZ() * (hz + 70);
        if (!l.getBiome(new BlockPos(ox, l.getSeaLevel(), oz)).is(BiomeTags.IS_OCEAN)) return false;
        int w = 0, t = 0;
        for (int i = -4; i <= 4; i++) for (int j = -4; j <= 4; j++) { t++; if (water(ox + i * 16, oz + j * 16)) w++; }
        if (w < t * 0.8) return false;
        int land = 0, tall = 0;
        for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
            int h = base(cx + i * hx / 2, cz + j * hz / 2, Heightmap.Types.WORLD_SURFACE_WG);
            if (h > l.getSeaLevel()) land++;
            if (h > l.getSeaLevel() + 24) tall++;
        }
        return land >= 3 && tall == 0;
    }

    /** Chay toi da budgetNs; sau do finished()/result() cho ket qua. */
    public void step(long budgetNs) {
        if (finished) return;
        long end = System.nanoTime() + budgetNs;
        while (System.nanoTime() < end) {
            if (ring > MAX_R / STEP) { fallback(); finished = true; return; }
            int count = ring == 0 ? 1 : Math.max(1, (int) Math.round(2 * Math.PI * ring));
            if (idx >= count) { ring++; idx = 0; continue; }
            double ang = idx * 2 * Math.PI / count;
            idx++;
            int cx = anchor.getX() + (int) Math.round(Math.cos(ang) * ring * STEP);
            int cz = anchor.getZ() + (int) Math.round(Math.sin(ang) * ring * STEP);
            if (ok(cx, cz)) { result = new BlockPos(cx, l.getSeaLevel(), cz); finished = true; return; }
        }
    }

    /** Khong co bo bien trong 2000: bien gan nhat (findClosestBiome3d, is_ocean), dat canh nuoc ra phia bien. */
    private void fallback() {
        var found = l.findClosestBiome3d(b -> b.is(BiomeTags.IS_OCEAN), anchor, 6400, 32, 64);
        if (found == null) { result = anchor; LOG.warn("Port: no ocean biome found, using anchor {}", anchor); return; }
        BlockPos o = found.getFirst();
        Direction ws = waterSide == null ? Direction.WEST : waterSide;
        result = new BlockPos(o.getX() - ws.getStepX() * (st.sx / 2 - 20), l.getSeaLevel(), o.getZ() - ws.getStepZ() * (st.sz / 2 - 20));
        LOG.info("Port: fallback to nearest ocean biome at {} -> site {}", o, result);
    }
}
