package com.hukhong.client.minimap;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bộ nhớ đệm màu bản đồ theo chunk (client). Mỗi chunk: 256 màu ABGR (màu bản đồ vanilla của khối mặt trên + đổ bóng độ cao).
 * Quét tăng dần: chunk quanh người chơi thường xuyên, vòng ngoài luân phiên vài chunk mỗi tick. Chỉ đọc chunk client đã nạp.
 */
public final class MapCache {
    private MapCache() {}

    /** Màu ô chưa biết (ABGR). */
    public static final int UNKNOWN = 0xFF1A1018;

    private static final Map<Long, int[]> CHUNKS = new HashMap<>();
    private static int version;
    private static ResourceKey<Level> dim;
    private static int cursor;
    private static List<int[]> ring = List.of();
    private static int ringRadius = -1;
    private static int tickCounter;

    public static long key(int cx, int cz) { return ((long) cx << 32) | (cz & 0xFFFFFFFFL); }

    public static int version() { return version; }

    public static void clear() { CHUNKS.clear(); version++; cursor = 0; dim = null; }

    // ---- quét (gọi mỗi client tick) ----

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        if (dim != level.dimension()) { CHUNKS.clear(); dim = level.dimension(); version++; cursor = 0; }
        tickCounter++;
        int pcx = mc.player.blockPosition().getX() >> 4, pcz = mc.player.blockPosition().getZ() >> 4;
        int hint = mc.player.blockPosition().getY() + 1;
        boolean ceil = level.dimensionType().hasCeiling();
        // chunk người chơi + 8 lân cận: mỗi 10 tick (đổi khối/đi lại)
        if (tickCounter % 10 == 0) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) scan(level, pcx + dx, pcz + dz, hint, ceil);
        }
        int radius = Math.max(2, Math.min(mc.options.getEffectiveRenderDistance(), 10));
        if (radius != ringRadius) buildRing(radius);
        for (int i = 0; i < 3 && !ring.isEmpty(); i++) {
            int[] o = ring.get(cursor);
            cursor = (cursor + 1) % ring.size();
            scan(level, pcx + o[0], pcz + o[1], hint, ceil);
        }
        if (tickCounter % 200 == 0 && CHUNKS.size() > 4000) evict(pcx, pcz);
    }

    private static void buildRing(int r) {
        List<int[]> l = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) if (dx * dx + dz * dz <= r * r + r) l.add(new int[]{dx, dz});
        l.sort((a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]));
        ring = l;
        ringRadius = r;
        cursor = 0;
    }

    private static void evict(int pcx, int pcz) {
        CHUNKS.keySet().removeIf(k -> {
            int cx = (int) (k >> 32), cz = (int) (long) k;
            return Math.abs(cx - pcx) > 48 || Math.abs(cz - pcz) > 48;
        });
    }

    private static void scan(ClientLevel level, int cx, int cz, int hint, boolean ceil) {
        LevelChunk ch = level.getChunkSource().getChunkNow(cx, cz);
        if (ch == null) return;
        LevelChunk north = level.getChunkSource().getChunkNow(cx, cz - 1);
        int[] out = CHUNKS.computeIfAbsent(key(cx, cz), k -> new int[256]);
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int[] ys = new int[16 * 17]; // hàng 0 = z-1 (chunk bắc), hàng 1..16 = z 0..15
        int bx = cx << 4, bz = cz << 4;
        for (int x = 0; x < 16; x++) {
            ys[x] = north != null ? top(level, north, x, 15, hint, ceil, mp) : Integer.MIN_VALUE;
            for (int z = 0; z < 16; z++) ys[(z + 1) * 16 + x] = top(level, ch, x, z, hint, ceil, mp);
        }
        boolean changed = false;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int y = ys[(z + 1) * 16 + x];
                int yn = ys[z * 16 + x];
                int col = color(level, ch, bx + x, y, bz + z, yn, mp);
                int i = (z << 4) | x;
                if (out[i] != col) { out[i] = col; changed = true; }
            }
        }
        if (changed) version++;
    }

    /** y của khối mặt trên có màu bản đồ (hoặc chất lỏng). Hầm trần (Nether): bắt đầu từ độ cao người chơi. */
    private static int top(ClientLevel level, LevelChunk ch, int x, int z, int hint, boolean ceil, BlockPos.MutableBlockPos mp) {
        int min = level.getMinBuildHeight();
        int y = ch.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        if (ceil) y = Math.min(y, hint + 2);
        int wx = (ch.getPos().x << 4) + x, wz = (ch.getPos().z << 4) + z;
        while (y > min) {
            BlockState s = ch.getBlockState(mp.set(wx, y, wz));
            if (!s.isAir() && s.getMapColor(level, mp) != MapColor.NONE) return y;
            y--;
        }
        return min;
    }

    private static int color(ClientLevel level, LevelChunk ch, int wx, int y, int wz, int yNorth, BlockPos.MutableBlockPos mp) {
        BlockState s = ch.getBlockState(mp.set(wx, y, wz));
        MapColor mc = s.getMapColor(level, mp);
        if (mc == MapColor.NONE) return UNKNOWN;
        MapColor.Brightness b;
        if (!s.getFluidState().isEmpty()) {
            int depth = 0, min = level.getMinBuildHeight();
            for (int yy = y; yy > min && depth < 24; yy--) {
                if (ch.getBlockState(mp.set(wx, yy, wz)).getFluidState().isEmpty()) break;
                depth++;
            }
            b = depth <= 2 ? MapColor.Brightness.HIGH : depth <= 5 ? MapColor.Brightness.NORMAL
                    : depth <= 10 ? MapColor.Brightness.LOW : MapColor.Brightness.LOWEST;
        } else {
            int d = yNorth == Integer.MIN_VALUE ? 0 : y - yNorth;
            b = d >= 1 ? MapColor.Brightness.HIGH : d == 0 ? MapColor.Brightness.NORMAL
                    : d >= -2 ? MapColor.Brightness.LOW : MapColor.Brightness.LOWEST;
        }
        return mc.calculateRGBColor(b);
    }

    // ---- vẽ ra NativeImage ----

    /**
     * Tô img (w x h) bằng màu cache quanh (cx,cz), mỗi pixel = step block. circleDiameter > 0: pixel ngoài đường tròn
     * (đường kính đó, tâm ảnh) có alpha 0 để minimap tròn.
     */
    public static void render(NativeImage img, int cx, int cz, int step, int circleDiameter) {
        int w = img.getWidth(), h = img.getHeight();
        double rad = circleDiameter / 2.0, mx = w / 2.0, mz = h / 2.0;
        long lastK = Long.MIN_VALUE;
        int[] arr = null;
        for (int j = 0; j < h; j++) {
            int bz = cz + (j - h / 2) * step;
            for (int i = 0; i < w; i++) {
                if (circleDiameter > 0) {
                    double dx = i + 0.5 - mx, dz = j + 0.5 - mz;
                    if (dx * dx + dz * dz > rad * rad) { img.setPixelRGBA(i, j, 0); continue; }
                }
                int bx = cx + (i - w / 2) * step;
                long k = key(bx >> 4, bz >> 4);
                if (k != lastK) { arr = CHUNKS.get(k); lastK = k; }
                int c = arr == null ? 0 : arr[((bz & 15) << 4) | (bx & 15)];
                img.setPixelRGBA(i, j, c == 0 ? UNKNOWN : c);
            }
        }
    }
}
