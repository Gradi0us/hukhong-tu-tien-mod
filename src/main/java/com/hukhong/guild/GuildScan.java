package com.hukhong.guild;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Quet cong trinh lon quanh quang truong (thanh import): o "trong nha" = khong khi dung duoc (2 o), nen dac, co mai che phia tren.
 * Gom thanh thanh phan lien thong 2D; chon thanh phan lon nhat trong ban kinh quet. Chi server thread, chi doc chunk da nap.
 */
final class GuildScan {
    private GuildScan() {}

    static final int RADIUS = 60, MIN_AREA = 60, MAX_AREA = 900;

    record Hit(BlockPos npc, BlockPos board, int area) {}

    private static boolean solid(ServerLevel l, int x, int y, int z) {
        BlockState s = l.getBlockState(new BlockPos(x, y, z));
        return s.blocksMotion() && l.getFluidState(new BlockPos(x, y, z)).isEmpty();
    }

    private static boolean free(ServerLevel l, int x, int y, int z) {
        BlockPos p = new BlockPos(x, y, z);
        BlockState s = l.getBlockState(p);
        return !s.blocksMotion() && s.getFluidState().isEmpty() && !s.is(net.minecraft.tags.BlockTags.LEAVES);
    }

    /** y san trong nha tai cot (x,z) (khoi nen), hoac Integer.MIN_VALUE neu khong phai trong nha. */
    private static int floorOf(ServerLevel l, int x, int z, int y0) {
        for (int y = y0 - 1; y <= y0 + 4; y++) {
            if (!solid(l, x, y, z) || !free(l, x, y + 1, z) || !free(l, x, y + 2, z)) continue;
            // mai: gap khoi dac trong khoang y+3..y+16 ma khong bi gian doan boi vat dac khac (cot khong khi lien tuc toi mai)
            for (int h = 3; h <= 16; h++) {
                if (solid(l, x, y + h, z)) return y;
                if (!free(l, x, y + h, z)) break;
            }
            return Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    /** Tim cong trinh lon nhat quanh (cx,cz) theo mat altar y0 (y noi nguoi dung dung). null neu khong co. */
    static Hit find(ServerLevel l, int cx, int cz, int y0) {
        int n = 2 * RADIUS + 1;
        int[][] fl = new int[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
            int x = cx - RADIUS + i, z = cz - RADIUS + j;
            fl[i][j] = l.hasChunkAt(new BlockPos(x, y0, z)) ? floorOf(l, x, z, y0) : Integer.MIN_VALUE;
        }
        boolean[][] seen = new boolean[n][n];
        List<int[]> best = null;
        int[] dx = {1, -1, 0, 0}, dz = {0, 0, 1, -1};
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
            if (seen[i][j] || fl[i][j] == Integer.MIN_VALUE) continue;
            List<int[]> comp = new ArrayList<>();
            ArrayDeque<int[]> q = new ArrayDeque<>();
            q.add(new int[]{i, j});
            seen[i][j] = true;
            while (!q.isEmpty() && comp.size() <= MAX_AREA) {
                int[] c = q.poll();
                comp.add(c);
                for (int k = 0; k < 4; k++) {
                    int a = c[0] + dx[k], b = c[1] + dz[k];
                    if (a < 0 || b < 0 || a >= n || b >= n || seen[a][b] || fl[a][b] == Integer.MIN_VALUE) continue;
                    if (Math.abs(fl[a][b] - fl[c[0]][c[1]]) > 1) continue;
                    seen[a][b] = true;
                    q.add(new int[]{a, b});
                }
            }
            if (comp.size() > MAX_AREA || comp.size() < MIN_AREA) continue;
            if (best == null || comp.size() > best.size()) best = comp;
        }
        if (best == null) return null;
        long sx = 0, sz = 0;
        for (int[] c : best) { sx += c[0]; sz += c[1]; }
        double mx = (double) sx / best.size(), mz = (double) sz / best.size();
        // le tan: o trong gan tam nhat; bang: o sat tuong (co hang xom dac o tang dung) cach le tan >= 3, uu tien cach ~4-6 o
        int[] npc = null;
        double bd = Double.MAX_VALUE;
        for (int[] c : best) {
            double d = (c[0] - mx) * (c[0] - mx) + (c[1] - mz) * (c[1] - mz);
            if (d < bd) { bd = d; npc = c; }
        }
        int[] board = null;
        double bs = Double.MAX_VALUE;
        for (int[] c : best) {
            int y = fl[c[0]][c[1]];
            int x = cx - RADIUS + c[0], z = cz - RADIUS + c[1];
            int walls = 0;
            for (int k = 0; k < 4; k++) if (solid(l, x + dx[k], y + 1, z + dz[k]) && solid(l, x + dx[k], y + 2, z + dz[k])) walls++;
            if (walls != 1) continue; // chi sat dung 1 tuong (khong phai goc / cua)
            double dist = Math.hypot(c[0] - npc[0], c[1] - npc[1]);
            if (dist < 3) continue;
            double sc = Math.abs(dist - 5);
            if (sc < bs) { bs = sc; board = c; }
        }
        BlockPos np = new BlockPos(cx - RADIUS + npc[0], fl[npc[0]][npc[1]] + 1, cz - RADIUS + npc[1]);
        BlockPos bp = board == null ? null : new BlockPos(cx - RADIUS + board[0], fl[board[0]][board[1]] + 1, cz - RADIUS + board[1]);
        if (bp == null) { // khong thay: dung o ke le tan
            for (int k = 0; k < 4 && bp == null; k++) {
                BlockPos c = np.offset(dx[k], 0, dz[k]);
                if (free(l, c.getX(), c.getY(), c.getZ()) && floorOf(l, c.getX(), c.getZ(), y0) != Integer.MIN_VALUE) bp = c;
            }
            if (bp == null) return null;
        }
        return new Hit(np, bp, best.size());
    }
}
