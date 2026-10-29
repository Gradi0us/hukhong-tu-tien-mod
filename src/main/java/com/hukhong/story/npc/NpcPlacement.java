package com.hukhong.story.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tim cho dung vung cho NPC truyen: nen dac o duoi, 2 o trong tren dau, khong phai mai nha / trong nha.
 * Uu tien vanh quang truong (ngoai troi, cao do mat Ấn Hồn Đài), tim tu trong ra ngoai.
 */
public final class NpcPlacement {
    private NpcPlacement() {}

    /**
     * @param floor khoi mat san tham chieu (vd tam thanh = san quang truong); cho dung duoc tim o y trong [floor.y, floor.y+2] (dung o floor.y+1 la binh thuong)
     * @return vi tri dung chan (o khong khi tren khoi dac), hoac null neu khong co
     */
    public static BlockPos findStand(ServerLevel l, BlockPos floor, int rMin, int rMax, long salt) {
        for (int pass = 0; pass < 2; pass++) { // pass 0: chi cho troi mo (khong mai) ; pass 1: bat ky
            for (int r = rMin; r <= rMax; r++) {
                int steps = Math.max(8, r * 4);
                int off = (int) Math.floorMod(salt, steps);
                for (int k = 0; k < steps; k++) {
                    double a = (k + off) % steps * Math.PI * 2 / steps;
                    int x = floor.getX() + (int) Math.round(Math.cos(a) * r), z = floor.getZ() + (int) Math.round(Math.sin(a) * r);
                    if (!l.hasChunk(x >> 4, z >> 4)) continue;
                    BlockPos s = standAt(l, x, z, floor.getY(), pass == 0);
                    if (s != null) return s;
                }
            }
        }
        return null;
    }

    /** Cot (x,z): o dung thap nhat hop le trong [baseY+1, baseY+3] (chan), hoac null. */
    public static BlockPos standAt(ServerLevel l, int x, int z, int baseY, boolean openSky) {
        for (int y = baseY + 1; y <= baseY + 3; y++) {
            BlockPos p = new BlockPos(x, y, z);
            if (!standable(l, p)) continue;
            if (openSky && !l.canSeeSky(p.above())) return null; // cot nay co mai: bo
            return p;
        }
        return null;
    }

    public static boolean standable(ServerLevel l, BlockPos p) {
        BlockPos below = p.below();
        BlockState b = l.getBlockState(below);
        if (!b.isFaceSturdy(l, below, Direction.UP) || !b.getFluidState().isEmpty()) return false;
        if (b.is(Blocks.MAGMA_BLOCK) || b.is(Blocks.CACTUS) || b.is(Blocks.CAMPFIRE) || b.is(Blocks.SOUL_CAMPFIRE)) return false;
        for (int i = 0; i < 2; i++) {
            BlockPos q = p.above(i);
            BlockState s = l.getBlockState(q);
            if (!s.getCollisionShape(l, q).isEmpty() || !s.getFluidState().isEmpty() || s.is(Blocks.FIRE) || s.is(Blocks.LAVA)) return false;
        }
        return true;
    }

    /** Dung chan (floor = san), fallback: nhin tu mat dat nhieu nhat (heightmap) neu khong tim ra. */
    public static BlockPos plazaStand(ServerLevel l, BlockPos center, int rMin, int rMax, long salt) {
        BlockPos s = findStand(l, center, rMin, rMax, salt);
        if (s != null) return s;
        return l.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.offset(rMin, 0, rMin));
    }

    /**
     * Kiem tra dinh ky (goi moi ~40 tick): dua NPC ve mat dat neu lo lung / ket tuong / len mai nha. Tra true neu da dich chuyen.
     * home = tam thanh (null neu NPC di theo nguoi choi); strikes tinh o ngoai.
     */
    public static boolean needsReground(ServerLevel l, StoryNpc n, BlockPos home) {
        if (n.isNoGravity()) n.setNoGravity(false);
        if (n.isInWall()) return true;
        if (home != null) {
            double dx = n.getX() - home.getX(), dz = n.getZ() - home.getZ();
            if (n.getY() > home.getY() + 3.5 || n.getY() < home.getY() - 6 || dx * dx + dz * dz > 48 * 48) return true;
        }
        if (n.onGround() || n.isInWater() || n.isPassenger()) return false;
        BlockPos p = n.blockPosition();
        for (int i = 1; i <= 4; i++) if (!l.getBlockState(p.below(i)).getCollisionShape(l, p.below(i)).isEmpty()) return false; // dang roi, se cham dat
        return true;
    }
}
