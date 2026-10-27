package com.hukhong.structures;

import com.hukhong.HuKhong;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dat cau truc bat dong bo qua nhieu tick, theo tung o chunk (16x16 cot) co ngan sach thoi gian moi tick.
 * Moi cot: don dia hinh phia tren, dat block, do nen (foundation) phia duoi neu cau truc cham dat.
 * Lop nay KHONG tu luu hang doi: neu server tat giua chung, cong trinh do dang (xem GUIDE).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class StructurePlacer {
    private StructurePlacer() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final int FLAGS = 18; // UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE
    private static final long BUDGET_NS = 6_000_000L; // 6 ms / tick
    private static final int MAX_FOUNDATION = 48;
    private static final int CLEAR_ABOVE = 10;

    /** Ket qua dat: vi tri the gioi cua cau truc + marker. */
    public record Placed(ServerLevel level, LoadedStructure st, BlockPos origin) {
        public List<BlockPos> markers(String id) {
            List<BlockPos> out = new ArrayList<>();
            for (LoadedStructure.Marker m : st.markers(id)) out.add(origin.offset(m.x(), m.y(), m.z()));
            return out;
        }

        /** Marker dau tien hoac fallback. */
        public BlockPos marker(String id, BlockPos fallback) {
            LoadedStructure.Marker m = st.marker(id);
            return m == null ? fallback : origin.offset(m.x(), m.y(), m.z());
        }
    }

    private static final class Job {
        ServerLevel level;
        LoadedStructure st;
        BlockPos origin;
        int[] cols; // packed (lx | lz << 16), sap theo o chunk
        int next = 0;
        int errors = 0;
        Consumer<Placed> done;
    }

    private static final ArrayDeque<Job> QUEUE = new ArrayDeque<>();

    public static int pending() { return QUEUE.size(); }

    /** y mat nen tai (x,z): bo la/cay/thay the duoc, toi thieu muc bien. */
    public static int surfaceY(ServerLevel l, int x, int z) {
        l.getChunk(x >> 4, z >> 4);
        int y = l.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        while (y > l.getMinBuildHeight() + 8) {
            BlockState s = l.getBlockState(new BlockPos(x, y, z));
            if (s.isAir() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced()) y--; else break;
        }
        return Math.max(y, l.getSeaLevel());
    }

    /** Xep hang doi dat; tang 0 cua cau truc nam o origin.y (thay lop mat dat). */
    public static void enqueue(ServerLevel level, LoadedStructure st, BlockPos origin, Consumer<Placed> done) {
        Job j = new Job();
        j.level = level;
        j.st = st;
        j.origin = origin;
        j.done = done;
        List<Integer> cols = new ArrayList<>();
        int cx0 = origin.getX() >> 4, cx1 = (origin.getX() + st.sx - 1) >> 4;
        int cz0 = origin.getZ() >> 4, cz1 = (origin.getZ() + st.sz - 1) >> 4;
        for (int cz = cz0; cz <= cz1; cz++) for (int cx = cx0; cx <= cx1; cx++)
            for (int wz = cz << 4; wz < (cz << 4) + 16; wz++) for (int wx = cx << 4; wx < (cx << 4) + 16; wx++) {
                int lx = wx - origin.getX(), lz = wz - origin.getZ();
                if (lx < 0 || lz < 0 || lx >= st.sx || lz >= st.sz) continue;
                cols.add(lx | (lz << 16));
            }
        j.cols = cols.stream().mapToInt(Integer::intValue).toArray();
        QUEUE.add(j);
        LOG.info("Queued structure {} at {} ({} columns)", st.name, origin, j.cols.length);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        Job j = QUEUE.peek();
        if (j == null) return;
        long end = System.nanoTime() + BUDGET_NS;
        try {
            while (j.next < j.cols.length && System.nanoTime() < end) {
                int c = j.cols[j.next++];
                try {
                    column(j, c & 0xFFFF, c >>> 16);
                } catch (RuntimeException ex) {
                    if (j.errors++ < 3) LOG.error("Column ({},{}) of {} failed, continuing: {}", c & 0xFFFF, c >>> 16, j.st.name, ex.toString(), ex);
                }
            }
            if (j.next >= j.cols.length) {
                QUEUE.poll();
                finish(j);
            }
        } catch (RuntimeException ex) {
            LOG.error("Structure placement failed for {}: {}", j.st.name, ex.toString(), ex);
            QUEUE.poll();
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        if (!QUEUE.isEmpty()) LOG.warn("Server stopping with {} unfinished structure job(s)", QUEUE.size());
        QUEUE.clear();
    }

    private static void column(Job j, int lx, int lz) {
        ServerLevel l = j.level;
        LoadedStructure st = j.st;
        int wx = j.origin.getX() + lx, wz = j.origin.getZ() + lz, oy = j.origin.getY();
        l.getChunk(wx >> 4, wz >> 4);
        int top = Math.min(l.getMaxBuildHeight() - 1, oy + st.sy + CLEAR_ABOVE);
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int lowest = -1;
        for (int ly = 0; ly < st.sy; ly++) if (st.at(lx, ly, lz) != null) { lowest = ly; break; }
        // 1) don phia tren
        for (int y = oy; y <= top; y++) {
            mp.set(wx, y, wz);
            if (!l.getBlockState(mp).isAir()) l.setBlock(mp, Blocks.AIR.defaultBlockState(), FLAGS);
        }
        // 2) do nen
        if (lowest >= 0 && lowest <= 3) {
            for (int d = 1; d <= MAX_FOUNDATION + lowest; d++) {
                int y = oy + lowest - d;
                if (y <= l.getMinBuildHeight() + 1) break;
                mp.set(wx, y, wz);
                BlockState cur = l.getBlockState(mp);
                if (cur.blocksMotion() && l.getFluidState(mp).isEmpty()) break;
                l.setBlock(mp, (d <= 3 ? Blocks.DIRT : Blocks.STONE).defaultBlockState(), FLAGS);
            }
        }
        // 3) dat block
        for (int ly = 0; ly < st.sy; ly++) {
            BlockState s = st.at(lx, ly, lz);
            if (s == null) continue;
            int y = oy + ly;
            if (y >= l.getMaxBuildHeight() || y < l.getMinBuildHeight()) continue;
            try {
                l.setBlock(mp.set(wx, y, wz), s, FLAGS);
            } catch (RuntimeException ex) {
                // vd CarvedPumpkinBlock.onPlace (do tim golem) NPE khi o canh chunk chua nap: khoi da duoc dat, bo qua de khong mat phan tren cua cot
                j.errors++;
            }
        }
    }

    private static void finish(Job j) {
        for (LoadedStructure.Be b : j.st.blockEntities) {
            BlockPos p = j.origin.offset(b.x(), b.y(), b.z());
            try {
                BlockEntity be = j.level.getBlockEntity(p);
                if (be == null) continue;
                CompoundTag t = b.tag().copy();
                t.remove("id");
                be.loadWithComponents(t, j.level.registryAccess());
                be.setChanged();
                j.level.sendBlockUpdated(p, be.getBlockState(), be.getBlockState(), 3);
            } catch (RuntimeException ex) {
                LOG.warn("Block entity at {} skipped: {}", p, ex.toString());
            }
        }
        LOG.info("Structure {} placed at {} (column errors: {})", j.st.name, j.origin, j.errors);
        if (j.done != null) {
            try {
                j.done.accept(new Placed(j.level, j.st, j.origin));
            } catch (RuntimeException ex) {
                LOG.error("Structure completion hook failed: {}", ex.toString(), ex);
            }
        }
    }
}
