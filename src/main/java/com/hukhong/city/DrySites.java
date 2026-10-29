package com.hukhong.city;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;

/**
 * Chon dat lien (khong nuoc) cho thanh / tong mon / hiep hoi: tim theo xoan oc quanh neo tat dinh, khong sinh chunk
 * (ChunkGenerator.getBaseHeight + biome noise). Dat hop le: biome khong phai ocean/river, ti le nuoc trong dien tich &lt; 5%, do doc vua phai.
 * Ket qua luu vao WorldBuildData (khong tim lai; the gioi cu giu nguyen cac cong trinh da dung).
 */
public final class DrySites {
    private DrySites() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final int MAX_R = 1280, STEP = 64, SAMPLE = 6;
    /** Tran thoi gian tim (ms) de khong treo luong server; het gio -> bo qua (dung neo). */
    public static final long MAX_MS = 20_000;
    /** Goi moi ung vien (Prebuild giu watchdog max-tick-time song trong luc dung dong bo). */
    public static volatile Runnable heartbeat;
    public static final double MAX_WATER = 0.05;

    /** Vi tri da luu hoac tim moi quanh anchor (nua canh half). maxSlope = chenh cao toi da trong dien tich. */
    public static BlockPos resolve(ServerLevel l, String key, BlockPos anchor, int half0, int maxSlope) {
        int half = Math.min(100, half0);
        WorldBuildData data = WorldBuildData.get(l);
        int[] saved = data.drySite(key);
        if (saved != null) return new BlockPos(saved[0], 64, saved[1]);
        BlockPos r = search(l, anchor, half, maxSlope);
        data.setDrySite(key, r.getX(), r.getZ());
        if (r.getX() != anchor.getX() || r.getZ() != anchor.getZ())
            LOG.info("DrySite {}: anchor {} was not dry land, moved to {}", key, anchor, r);
        return r;
    }

    public static BlockPos search(ServerLevel l, BlockPos anchor, int half, int maxSlope) {
        long deadline = System.nanoTime() + MAX_MS * 1_000_000L;
        int rings = MAX_R / STEP;
        for (int ring = 0; ring <= rings; ring++) {
            int count = ring == 0 ? 1 : Math.max(1, (int) Math.round(2 * Math.PI * ring));
            // lech goc theo ring de khong luon bat dau cung huong
            double off = ring * 0.7;
            for (int i = 0; i < count; i++) {
                double ang = off + i * 2 * Math.PI / count;
                int x = anchor.getX() + (int) Math.round(Math.cos(ang) * ring * STEP);
                int z = anchor.getZ() + (int) Math.round(Math.sin(ang) * ring * STEP);
                Runnable hb = heartbeat;
                if (hb != null) hb.run();
                if (suitable(l, x, z, half, maxSlope)) return new BlockPos(x, 64, z);
                if (System.nanoTime() > deadline) { LOG.warn("DrySite: search timed out near {}, using anchor", anchor); return anchor; }
            }
        }
        if (maxSlope < 1000) return search(l, anchor, half, 1000); // noi long do doc, van bat buoc khong nuoc
        LOG.warn("DrySite: no dry land within {} of {}, using anchor", MAX_R, anchor);
        return anchor;
    }

    private static int base(ServerLevel l, int x, int z, Heightmap.Types t) {
        return l.getChunkSource().getGenerator().getBaseHeight(x, z, t, l, l.getChunkSource().randomState());
    }

    public static boolean suitable(ServerLevel l, int cx, int cz, int half, int maxSlope) {
        if (wetBiome(l, cx, cz) || wetBiome(l, cx - half, cz - half) || wetBiome(l, cx + half, cz + half)
                || wetBiome(l, cx - half, cz + half) || wetBiome(l, cx + half, cz - half)) return false;
        int sea = l.getSeaLevel();
        int sea0 = sea;
        // loai nhanh: tam + 4 goc phai la dat
        if (base(l, cx, cz, Heightmap.Types.OCEAN_FLOOR_WG) < sea0 - 1) return false;
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2)
            if (base(l, cx + sx * half, cz + sz * half, Heightmap.Types.OCEAN_FLOOR_WG) < sea0 - 1) return false;
        int step = Math.max(SAMPLE, half / 8);
        double total = Math.pow(2 * half / step + 1.0, 2);
        int wet = 0, lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int dx = -half; dx <= half; dx += step) for (int dz = -half; dz <= half; dz += step) {
            if (base(l, cx + dx, cz + dz, Heightmap.Types.OCEAN_FLOOR_WG) < sea - 1 && ++wet > total * MAX_WATER) return false;
            int h = base(l, cx + dx, cz + dz, Heightmap.Types.WORLD_SURFACE_WG);
            lo = Math.min(lo, h); hi = Math.max(hi, h);
        }
        return hi - lo <= maxSlope && hi > sea;
    }

    private static boolean wetBiome(ServerLevel l, int x, int z) {
        var b = l.getBiome(new BlockPos(x, l.getSeaLevel(), z));
        return b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_RIVER);
    }
}
