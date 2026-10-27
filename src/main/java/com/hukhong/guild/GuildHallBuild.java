package com.hukhong.guild;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.properties.SlabType;

import static com.hukhong.structures.ProcKit.air;
import static com.hukhong.structures.ProcKit.set;
import static com.hukhong.structures.ProcKit.stair;

/**
 * Dai sanh Hiep Hoi Mao Hiem Gia: 15x11, gac lung (phong ban nhiem vu), mai hip da phien, mat tien co mai hien + bien + banner,
 * quay le tan giua sanh, tuong bang nhiem vu o phia sau. Toa do cuc bo: u doc mat tien (side), v theo huong ra (out, v=D la mat tien).
 */
final class GuildHallBuild {
    private GuildHallBuild() {}

    static final int W = 7, D = 5;
    /** Nua kich thuoc vung can (vuong) de tim cho: W+1; mat tien + hien keo dai toi v = D+3. */
    static final int SPAN = W + 1;

    private static final class F {
        final ServerLevel l; final int hx, hz, by; final Direction out, side;
        F(ServerLevel l, int hx, int hz, int by, Direction out) {
            this.l = l; this.hx = hx; this.hz = hz; this.by = by; this.out = out; this.side = out.getClockWise();
        }
        int x(int u, int v) { return hx + side.getStepX() * u + out.getStepX() * v; }
        int z(int u, int v) { return hz + side.getStepZ() * u + out.getStepZ() * v; }
        void put(int u, int v, int h, Block b) { set(l, x(u, v), by + h, z(u, v), b); }
        void put(int u, int v, int h, net.minecraft.world.level.block.state.BlockState s) { set(l, x(u, v), by + h, z(u, v), s); }
        void clr(int u, int v, int h) { air(l, x(u, v), by + h, z(u, v)); }
        void stairs(int u, int v, int h, Block b, Direction f) { stair(l, x(u, v), by + h, z(u, v), b, f); }
        BlockPos pos(int u, int v, int h) { return new BlockPos(x(u, v), by + h, z(u, v)); }
    }

    /** Dung dai sanh; floorY = mat dat (lop khoi nen); tra vi tri le tan. */
    static BlockPos build(ServerLevel l, int hx, int hz, int floorY, Direction out) {
        F f = new F(l, hx, hz, floorY + 1, out);
        final Block log = Blocks.DARK_OAK_LOG, plank = Blocks.DARK_OAK_PLANKS, stone = Blocks.STONE_BRICKS;

        // 0) don khong gian + nen (be dai 1 lop)
        for (int u = -W - 2; u <= W + 2; u++) for (int v = -D - 2; v <= D + 4; v++) {
            for (int h = 1; h <= 22; h++) f.clr(u, v, h);
            if (Math.abs(u) <= W + 1 && v >= -D - 1 && v <= D + 3) f.put(u, v, 0, stone);
        }
        // 1) san
        for (int u = -W; u <= W; u++) for (int v = -D; v <= D; v++)
            f.put(u, v, 0, ((u + v) & 1) == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES);
        for (int v = 1; v <= D; v++) for (int u = -1; u <= 1; u++) f.put(u, v, 1, Blocks.RED_CARPET);

        // 2) tuong
        for (int u = -W; u <= W; u++) for (int v = -D; v <= D; v++) {
            boolean ex = Math.abs(u) == W, ez = Math.abs(v) == D;
            if (!ex && !ez) continue;
            boolean corner = ex && ez;
            boolean pillar = corner || (ez && (Math.abs(u) == 2 || ex)) || (ex && v == 0);
            boolean win = !pillar && ((ez && (Math.abs(u) == 4 || Math.abs(u) == 5)) || (ex && Math.abs(v) >= 2 && Math.abs(v) <= 3));
            boolean lowWin = win && !(v == -D);
            for (int h = 1; h <= 11; h++) {
                Block b = pillar ? log : stone;
                if (!pillar && h == 6) b = plank;
                else if (win && (h == 9 || h == 10 || (lowWin && (h == 3 || h == 4)))) b = Blocks.GLASS_PANE;
                f.put(u, v, h, b);
            }
            f.put(u, v, 12, plank);
        }
        // cua chinh (mat tien v=D) rong 3
        for (int u = -1; u <= 1; u++) {
            for (int h = 1; h <= 4; h++) f.clr(u, D, h);
            f.put(u, D, 5, plank);
            for (int h = 8; h <= 11; h++) f.put(u, D, h, Blocks.GLASS_PANE); // cua so nave
        }
        for (int u : new int[]{-1, 1}) f.stairs(u, D + 1, 5, Blocks.DARK_OAK_STAIRS, out); // mong lan tren cua
        // cua so mat sau cao
        for (int u : new int[]{-4, 0, 4}) for (int h : new int[]{9, 10}) f.put(u, -D, h, Blocks.GLASS_PANE);

        // 3) gac lung (v = -D+1..-D+3), lan can, cau thang len tu phia trai
        for (int u = -W + 1; u <= W - 1; u++) for (int v = -D + 1; v <= -D + 3; v++) f.put(u, v, 6, plank);
        for (int u = 0; u <= W - 1; u++) f.put(u, -D + 3, 7, Blocks.DARK_OAK_FENCE);
        for (int s = 0; s <= 5; s++) {
            int u = -W + 1 + s;
            for (int h = 1; h <= s; h++) f.put(u, -D + 3, h, stone);
            f.stairs(u, -D + 3, 1 + s, Blocks.STONE_BRICK_STAIRS, f.side);
        }
        f.put(-W + 1, -D + 3, 0, stone);
        // den + banner gac
        for (int u = -W + 2; u <= W - 2; u += 3) f.put(u, -D + 1, 7, Blocks.LANTERN);

        // 4) ruong nhiem vu: tuong bang phia sau (panel + 5 bang + banner)
        for (int u = -5; u <= 5; u++) for (int h = 1; h <= 3; h++) f.put(u, -D + 1, h, plank);
        for (int u = -4; u <= 4; u += 2) {
            f.put(u, -D + 1, 2, GuildRegistry.BOARD.get());
            f.put(u, -D + 1, 1, Blocks.BARREL);
        }
        for (int u : new int[]{-5, -3, -1, 1, 3, 5}) {
            Block bn = (u & 2) == 0 ? Blocks.RED_WALL_BANNER : Blocks.BLACK_WALL_BANNER;
            f.put(u, -D + 1, 4, bn.defaultBlockState().setValue(WallBannerBlock.FACING, out));
        }
        for (int u : new int[]{-5, 5}) { f.put(u, -D + 2, 1, Blocks.BARREL); f.put(u, -D + 2, 2, Blocks.BARREL); }

        // 5) quay le tan giua sanh
        for (int u = -3; u <= 3; u++)
            f.put(u, 0, 1, (Math.abs(u) == 3 ? Blocks.BARREL.defaultBlockState()
                    : Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM)));
        f.put(-2, 0, 2, Blocks.LANTERN);
        f.put(2, 0, 2, Blocks.LANTERN);
        f.put(0, -1, 0, Blocks.POLISHED_BLACKSTONE);

        // 6) dam mai + den chum
        for (int v : new int[]{-3, 1}) for (int u = -W + 1; u <= W - 1; u++) f.put(u, v, 12, plank);
        for (int v : new int[]{-3, 1}) for (int u : new int[]{-4, 0, 4}) {
            f.put(u, v, 11, Blocks.CHAIN);
            f.put(u, v, 10, Blocks.CHAIN);
            f.put(u, v, 9, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        // cot den doc loi di
        for (int u : new int[]{-3, 3}) for (int v : new int[]{3, 4}) {
            if (v != 3) continue;
            f.put(u, v, 1, Blocks.DARK_OAK_FENCE);
            f.put(u, v, 2, Blocks.DARK_OAK_FENCE);
            f.put(u, v, 3, Blocks.LANTERN);
        }

        // 7) mai hip da phien (to hon nha 1 o)
        boolean xAxis = out.getAxis() == Direction.Axis.X;
        com.hukhong.structures.ProcKit.hipRoof(l, hx, hz, f.by + 13, xAxis ? D : W, xAxis ? W : D,
                Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
        // dinh mai: den + coc
        f.put(0, 0, 20, Blocks.LIGHTNING_ROD);

        // 8) mat tien: hien + cot + mai hien, bac thang, banner, bien treo
        for (int u : new int[]{-4, 4}) for (int h = 1; h <= 5; h++) f.put(u, D + 2, h, log);
        for (int u = -5; u <= 5; u++) for (int v = D + 1; v <= D + 2; v++) f.put(u, v, 6, plank);
        for (int u = -5; u <= 5; u++) for (int v = D + 1; v <= D + 2; v++)
            f.put(u, v, 7, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        for (int u : new int[]{-2, 2}) f.put(u, D + 1, 5, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        for (int u = -2; u <= 2; u++) f.stairs(u, D + 3, 0, Blocks.STONE_BRICK_STAIRS, out.getOpposite());
        for (int u : new int[]{-3, 3}) {
            f.put(u, D + 1, 3, Blocks.RED_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, out));
            f.put(u, D + 1, 4, Blocks.RED_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, out));
        }
        BlockPos sp = f.pos(0, D + 1, 6);
        l.setBlock(sp, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, out), 18);
        if (l.getBlockEntity(sp) instanceof SignBlockEntity s) {
            s.setText(s.getFrontText().setMessage(1, Component.translatable("hukhong.guild.sign")), true);
            s.setWaxed(true);
            s.setChanged();
        }
        // den dung truoc bac thang
        for (int u : new int[]{-5, 5}) {
            for (int h = 1; h <= 3; h++) f.put(u, D + 3, h, Blocks.DARK_OAK_FENCE);
            f.put(u, D + 3, 4, Blocks.LANTERN);
        }
        return f.pos(0, -1, 1);
    }
}
