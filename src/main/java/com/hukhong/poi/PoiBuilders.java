package com.hukhong.poi;

import com.hukhong.core.Path;
import com.hukhong.structures.ProcKit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Dung dia diem theo thu tuc (ProcKit). Moi builder dung quanh tam (cx,cz), san di bo y0 (khoi dat nen = y0-1), tra ve cac diem tuong tac.
 * Chi goi khi toan bo chunk trong dien tich da nap (PoiPlacer kiem tra).
 */
public final class PoiBuilders {
    private PoiBuilders() {}

    public record Built(int y0, List<PoiInstance.Act> acts) {}

    /** Nua canh dien tich (vuong) cua builder: dung de kiem tra dat kho, nap chunk, san phang. */
    public static int half(String b) {
        return switch (b) {
            case "stele" -> 5; case "shrine" -> 6; case "spring" -> 7; case "inn" -> 13; case "waterfall" -> 8; case "rift" -> 5;
            case "cave" -> 11; case "battlefield" -> 15; case "grave" -> 5; case "ruin" -> 17; case "sword_mound" -> 13;
            case "abyss" -> 13; case "dao_quan" -> 10; case "arena" -> 11; case "wuxing" -> 9; default -> 6;
        };
    }

    public static int height(String b) {
        return switch (b) {
            case "inn" -> 14; case "waterfall" -> 12; case "sword_mound" -> 11; case "dao_quan" -> 10; case "cave" -> 10; case "ruin" -> 9;
            case "battlefield", "abyss", "shrine", "stele" -> 8; default -> 6;
        };
    }

    public static boolean known(String b) {
        return switch (b) {
            case "stele", "shrine", "spring", "inn", "waterfall", "rift", "cave", "battlefield", "grave", "ruin", "sword_mound", "abyss", "dao_quan", "arena", "wuxing" -> true;
            default -> false;
        };
    }

    /** Mat dat that: bo qua than cay, la, co, tuyet; dung ngay tren nuoc/dat. */
    public static int groundY(ServerLevel l, int x, int z) {
        int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int min = l.getMinBuildHeight() + 1;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        while (y > min) {
            BlockState s = l.getBlockState(mp.set(x, y - 1, z));
            if (s.isAir() || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || (s.canBeReplaced() && s.getFluidState().isEmpty())) y--;
            else break;
        }
        return y;
    }

    /** San phang vuong: don sach tu y0 den y0+h, lap day dat ben duoi. */
    static void prepare(ServerLevel l, int cx, int cz, int half, int y0, int h) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
            int x = cx + dx, z = cz + dz;
            int gy = groundY(l, x, z);
            for (int y = gy; y < y0; y++) ProcKit.set(l, x, y, z, y == y0 - 1 ? Blocks.GRASS_BLOCK : Blocks.DIRT);
            for (int y = y0; y <= y0 + h; y++) {
                if (!l.getBlockState(mp.set(x, y, z)).isAir()) ProcKit.air(l, x, y, z);
            }
            if (gy > y0 && l.getBlockState(mp.set(x, y0 - 1, z)).isAir()) ProcKit.set(l, x, y0 - 1, z, Blocks.DIRT);
        }
    }

    public static Built build(ServerLevel l, PoiDef d, int cx, int cz, int seed) {
        int half = half(d.builder());
        int y0 = groundY(l, cx, cz);
        prepare(l, cx, cz, half, y0, height(d.builder()));
        C c = new C(l, cx, cz, y0, seed);
        switch (d.builder()) {
            case "stele" -> stele(c);
            case "shrine" -> shrine(c);
            case "spring" -> spring(c);
            case "inn" -> inn(c);
            case "waterfall" -> waterfall(c);
            case "rift" -> rift(c);
            case "cave" -> cave(c);
            case "battlefield" -> battlefield(c);
            case "grave" -> grave(c);
            case "ruin" -> ruin(c);
            case "sword_mound" -> swordMound(c);
            case "abyss" -> abyss(c);
            case "dao_quan" -> daoQuan(c);
            case "arena" -> arena(c);
            case "wuxing" -> wuxing(c);
            default -> stele(c);
        }
        return new Built(y0, c.acts);
    }

    /** Ngu hanh: Moc Hoa Tho Kim Thuy. */
    static Block elemBlock(int e) {
        return switch (e) {
            case 0 -> Blocks.LIME_CONCRETE; case 1 -> Blocks.RED_CONCRETE; case 2 -> Blocks.YELLOW_CONCRETE;
            case 3 -> Blocks.WHITE_CONCRETE; default -> Blocks.BLUE_CONCRETE;
        };
    }

    static Block pathEmblem(Path p) {
        return switch (p) {
            case KIEM -> Blocks.LAPIS_BLOCK; case DAO -> Blocks.GOLD_BLOCK; case THE -> Blocks.REDSTONE_BLOCK;
            case MA -> Blocks.AMETHYST_BLOCK; default -> Blocks.EMERALD_BLOCK;
        };
    }

    /** Dao bi an khoa cua Dong Phu (tat dinh theo seed). */
    public static Path sealPath(int seed) { return Path.values()[Math.floorMod(seed / 7, Path.values().length)]; }

    // ------------------------------------------------------------------ ngu canh
    private static final class C {
        final ServerLevel l; final int cx, cz, y0, seed; final List<PoiInstance.Act> acts = new ArrayList<>(); final RandomSource r;

        C(ServerLevel l, int cx, int cz, int y0, int seed) {
            this.l = l; this.cx = cx; this.cz = cz; this.y0 = y0; this.seed = seed; this.r = RandomSource.create(seed * 131L + cx * 7L + cz);
        }

        void b(int dx, int dy, int dz, Block b) { ProcKit.set(l, cx + dx, y0 + dy, cz + dz, b); }
        void s(int dx, int dy, int dz, BlockState s) { ProcKit.set(l, cx + dx, y0 + dy, cz + dz, s); }
        void f(int x1, int y1, int z1, int x2, int y2, int z2, Block b) { ProcKit.fill(l, cx + x1, y0 + y1, cz + z1, cx + x2, y0 + y2, cz + z2, b); }
        void a(int dx, int dy, int dz) { ProcKit.air(l, cx + dx, y0 + dy, cz + dz); }
        void fa(int x1, int y1, int z1, int x2, int y2, int z2) { f(x1, y1, z1, x2, y2, z2, Blocks.AIR); }
        int h(int dx, int dy, int dz) { return ProcKit.hash(cx + dx, y0 + dy, cz + dz, seed); }
        void act(String k, int dx, int dy, int dz) { acts.add(new PoiInstance.Act(k, cx + dx, y0 + dy, cz + dz)); }

        void post(int dx, int dz) { b(dx, 0, dz, Blocks.OAK_FENCE); b(dx, 1, dz, Blocks.LANTERN); }
        void chest(int dx, int dy, int dz, Direction face) { s(dx, dy, dz, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, face)); }
        void lectern(int dx, int dy, int dz, Direction face) { s(dx, dy, dz, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, face)); }
        void lever(int dx, int dy, int dz, Direction face) {
            s(dx, dy, dz, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, face).setValue(LeverBlock.POWERED, false));
        }
        void bed(int dx, int dz, Direction face) {
            s(dx, 1, dz, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, face).setValue(BedBlock.PART, BedPart.FOOT));
            s(dx + face.getStepX(), 1, dz + face.getStepZ(), Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, face).setValue(BedBlock.PART, BedPart.HEAD));
        }
        void ladder(int dx, int dy, int dz, Direction face) { s(dx, dy, dz, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, face)); }
    }

    // ------------------------------------------------------------------ A1 Bia Da
    private static void stele(C c) {
        c.f(-3, -1, -3, 3, -1, 3, Blocks.STONE_BRICKS);
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) if (c.h(dx, -1, dz) < 70) c.b(dx, -1, dz, Blocks.MOSSY_STONE_BRICKS);
        c.f(-1, 0, -1, 1, 0, 1, Blocks.POLISHED_ANDESITE);
        c.f(-1, 1, 0, 1, 3, 0, Blocks.STONE_BRICKS);
        c.b(0, 2, 0, Blocks.CHISELED_STONE_BRICKS);
        c.f(-1, 4, 0, 1, 4, 0, Blocks.STONE_BRICK_SLAB);
        c.post(-2, 1); c.post(2, 1);
        c.act("stele", 0, 2, 0);
        ProcKit.decay(c.l, c.cx, c.cz, c.y0, 4, 5, c.seed);
    }

    // ------------------------------------------------------------------ A4 Tho Dia Mieu
    private static void shrine(C c) {
        c.f(-4, -1, -4, 4, -1, 4, Blocks.STONE_BRICKS);
        c.f(-2, 0, -3, 2, 0, -1, Blocks.POLISHED_ANDESITE);
        c.f(-2, 1, -3, 2, 3, -3, Blocks.SPRUCE_PLANKS);
        for (int dz = -3; dz <= -1; dz++) { c.f(-2, 1, dz, -2, 3, dz, Blocks.SPRUCE_PLANKS); c.f(2, 1, dz, 2, 3, dz, Blocks.SPRUCE_PLANKS); }
        c.f(-2, 1, -1, -2, 3, -1, Blocks.DARK_OAK_LOG); c.f(2, 1, -1, 2, 3, -1, Blocks.DARK_OAK_LOG);
        ProcKit.hipRoof(c.l, c.cx, c.cz - 2, c.y0 + 4, 3, 2, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS);
        c.b(0, 1, -2, Blocks.POLISHED_ANDESITE);
        c.s(0, 2, -2, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
        c.b(0, 2, -3, Blocks.YELLOW_GLAZED_TERRACOTTA);
        c.b(-1, 1, -2, Blocks.LANTERN); c.b(1, 1, -2, Blocks.LANTERN);
        c.f(-3, 0, 3, -3, 3, 3, Blocks.RED_TERRACOTTA); c.f(3, 0, 3, 3, 3, 3, Blocks.RED_TERRACOTTA);
        c.f(-4, 4, 3, 4, 4, 3, Blocks.RED_TERRACOTTA);
        c.f(0, -1, 1, 0, -1, 5, Blocks.DIRT_PATH);
        c.act("offer", 0, 2, -2);
    }

    // ------------------------------------------------------------------ A5 Linh Tuyen
    private static void spring(C c) {
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 <= 9) { c.b(dx, -2, dz, Blocks.SEA_LANTERN); c.b(dx, -1, dz, Blocks.WATER); }
            else if (d2 <= 15) { c.b(dx, -1, dz, c.h(dx, 0, dz) < 90 ? Blocks.MOSSY_COBBLESTONE : Blocks.STONE_BRICKS); if (c.h(dx, 1, dz) < 40) c.b(dx, 0, dz, Blocks.AMETHYST_CLUSTER); }
        }
        c.post(5, 0); c.post(-5, 0); c.post(0, 5); c.post(0, -5);
        c.act("spring", 0, -1, 0);
    }

    // ------------------------------------------------------------------ A6 Khach Diem
    private static void inn(C c) {
        ProcKit.house(c.l, c.cx, c.cz, c.y0, 5, 4, 2, ProcKit.mat(2), Direction.SOUTH);
        c.bed(1, -3, Direction.SOUTH); c.bed(3, -3, Direction.SOUTH);
        c.f(-3, 1, 1, -1, 1, 1, Blocks.BARREL);
        c.f(-1, -1, 5, 1, -1, 9, Blocks.DIRT_PATH);
        c.post(-2, 5); c.post(2, 5);
        for (int x = 7; x <= 12; x++) for (int z = -3; z <= 3; z++) {
            boolean edge = x == 7 || x == 12 || z == -3 || z == 3;
            if (edge && !(x == 7 && z == 0)) c.b(x, 0, z, Blocks.OAK_FENCE);
        }
        c.b(9, 0, 0, Blocks.HAY_BLOCK); c.b(10, 0, 1, Blocks.HAY_BLOCK); c.b(11, 0, -1, Blocks.BARREL);
        c.act("innkeeper", -2, 1, 2);
        c.act("inn_door", 0, 1, 6);
    }

    // ------------------------------------------------------------------ A19 Thac Linh Khi
    private static void waterfall(C c) {
        c.f(-3, 0, -6, 3, 8, -3, Blocks.STONE);
        for (int dx = -3; dx <= 3; dx++) for (int dy = 0; dy <= 8; dy++) for (int dz = -6; dz <= -3; dz++)
            if (c.h(dx, dy, dz) < 60) c.b(dx, dy, dz, Blocks.MOSSY_COBBLESTONE);
        c.fa(-1, 0, -5, 1, 2, -4);
        c.fa(0, 0, -3, 0, 1, -3);
        for (int dx = -3; dx <= 3; dx++) for (int dz = -2; dz <= 2; dz++) {
            boolean in = Math.abs(dx) <= 2;
            c.b(dx, -1, dz, in ? Blocks.WATER : Blocks.STONE_BRICKS);
            if (in) c.b(dx, -2, dz, Blocks.SEA_LANTERN);
        }
        c.b(-1, 9, -3, Blocks.STONE); c.b(1, 9, -3, Blocks.STONE); c.b(0, 9, -4, Blocks.STONE);
        c.b(0, 9, -3, Blocks.WATER);
        c.b(0, 0, -5, Blocks.MOSS_BLOCK);
        c.b(-1, 1, -5, Blocks.LANTERN);
        c.act("meditate", 0, 0, -4);
    }

    // ------------------------------------------------------------------ A23 Khe Nut Hu Khong
    private static void rift(C c) {
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++)
            if (c.h(dx, -1, dz) < 150 && dx * dx + dz * dz <= 18) c.b(dx, -1, dz, Blocks.BLACKSTONE);
        for (int dx = -2; dx <= 2; dx++) for (int dy = 0; dy <= 4; dy++) {
            boolean border = Math.abs(dx) == 2 || dy == 0 || dy == 4;
            if (border) c.b(dx, dy, 0, c.h(dx, dy, 0) < 100 ? Blocks.CRYING_OBSIDIAN : Blocks.OBSIDIAN);
            else c.b(dx, dy, 0, Blocks.PURPLE_STAINED_GLASS);
        }
        c.b(-3, 0, 1, Blocks.AMETHYST_CLUSTER); c.b(3, 0, -1, Blocks.AMETHYST_CLUSTER); c.b(2, 0, 2, Blocks.AMETHYST_CLUSTER);
        c.act("rift", 0, 2, 0);
    }

    /** Mo / dong khe nut (kinh chan tim giua khung). */
    public static void setRift(ServerLevel l, PoiInstance i, boolean open) {
        PoiInstance.Act a = i.firstAct("rift");
        if (a == null) return;
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++)
            ProcKit.set(l, a.x() + dx, a.y() + dy, a.z(), open ? Blocks.PURPLE_STAINED_GLASS : Blocks.AIR);
    }

    // ------------------------------------------------------------------ A2 Dong Phu Tien Boi
    private static void cave(C c) {
        for (int dx = -9; dx <= 9; dx++) for (int dz = -10; dz <= 10; dz++) for (int dy = 0; dy <= 8; dy++) {
            double v = (dx / 9.0) * (dx / 9.0) + (dz / 10.0) * (dz / 10.0) + (dy / 8.0) * (dy / 8.0);
            if (v <= 1.0) { int hs = c.h(dx, dy, dz); c.b(dx, dy, dz, hs < 45 ? Blocks.MOSSY_COBBLESTONE : hs < 90 ? Blocks.ANDESITE : Blocks.STONE); }
        }
        c.f(-4, -1, -6, 4, -1, 6, Blocks.STONE_BRICKS);
        c.fa(-4, 0, -6, 4, 3, 6);
        c.fa(-1, 0, 6, 1, 2, 10);
        c.f(-2, 0, -10, 2, 3, -7, Blocks.STONE_BRICKS);
        c.fa(-1, 0, -9, 1, 2, -8);
        c.f(-4, 0, -7, -2, 3, -7, Blocks.STONE_BRICKS); c.f(2, 0, -7, 4, 3, -7, Blocks.STONE_BRICKS);
        int[] xs = {-3, -1, 1, 3};
        for (int s = 0; s < 4; s++) {
            int x = xs[s];
            c.lever(x, 1, -6, Direction.SOUTH);
            c.b(x, 2, -7, elemBlock(PoiPuzzle.elementAt(c.seed, 4, s)));
            c.b(x, 3, -7, Blocks.SEA_LANTERN);
            c.act("lever:" + s, x, 1, -6);
        }
        c.b(0, 0, -7, Blocks.CRYING_OBSIDIAN); c.b(0, 1, -7, Blocks.CRYING_OBSIDIAN);
        c.b(0, 2, -7, pathEmblem(sealPath(c.seed)));
        c.chest(0, 0, -9, Direction.SOUTH);
        c.act("vault", 0, 0, -9);
        c.lectern(0, 0, 2, Direction.NORTH);
        c.act("tablet", 0, 0, 2);
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) ProcKit.hangLantern(c.l, c.cx + sx * 3, c.y0 + 3, c.cz + (sz > 0 ? 3 : -2));
    }

    /** Mo / dong an khoa phong bao vat. */
    public static void setSeal(ServerLevel l, PoiInstance i, boolean open) {
        PoiInstance.Act v = i.firstAct("vault");
        if (v == null) return;
        for (int dy = 0; dy <= 1; dy++) ProcKit.set(l, v.x(), v.y() + dy, v.z() + 2, open ? Blocks.AIR : Blocks.CRYING_OBSIDIAN);
    }

    // ------------------------------------------------------------------ A3 Co Chien Truong
    private static void battlefield(C c) {
        for (int dx = -14; dx <= 14; dx++) for (int dz = -14; dz <= 14; dz++) {
            if (dx * dx + dz * dz > 14 * 14) continue;
            int hs = c.h(dx, 0, dz);
            if (hs < 70) c.b(dx, -1, dz, Blocks.COARSE_DIRT);
            else if (hs < 78) { int hh = 2 + (hs & 1); for (int k = 0; k < hh; k++) c.b(dx, k, dz, Blocks.IRON_BARS); }
            else if (hs < 84) c.b(dx, 0, dz, Blocks.BONE_BLOCK);
            else if (hs < 89) c.b(dx, 0, dz, Blocks.DEAD_BUSH);
            else if (hs < 91) c.b(dx, 0, dz, Blocks.COBWEB);
            else if (hs < 93) c.b(dx, 0, dz, Blocks.SKELETON_SKULL);
        }
        c.f(0, 0, 0, 0, 6, 0, Blocks.DARK_OAK_FENCE);
        c.b(0, 7, 0, Blocks.BLACK_BANNER);
        for (int k = 0; k < 4; k++) { int dx = (k & 1) == 0 ? 6 : -6, dz = (k & 2) == 0 ? 6 : -6; c.b(dx, 0, dz, Blocks.DARK_OAK_FENCE); c.b(dx, 1, dz, Blocks.SOUL_LANTERN); }
        c.act("center", 0, 0, 0);
    }

    // ------------------------------------------------------------------ A20 Mo Vo Danh
    private static void grave(C c) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 <= 5) c.b(dx, 0, dz, Blocks.PODZOL);
            if (d2 <= 1) c.b(dx, 1, dz, Blocks.COARSE_DIRT);
        }
        c.f(0, 1, -3, 0, 2, -3, Blocks.POLISHED_DEEPSLATE);
        c.b(0, 3, -3, Blocks.COBBLED_DEEPSLATE_SLAB);
        c.b(0, 0, -3, Blocks.COBBLED_DEEPSLATE);
        c.b(-2, 0, -3, Blocks.DEAD_BUSH); c.b(2, 0, -3, Blocks.DEAD_BUSH);
        c.b(-3, 0, 1, Blocks.DARK_OAK_FENCE); c.b(-3, 1, 1, Blocks.SOUL_LANTERN);
        c.act("grave", 0, 1, -3);
    }

    // ------------------------------------------------------------------ A8 Tan Tich Tong Mon
    private static void ruin(C c) {
        c.f(-14, -1, -17, 14, -1, 17, Blocks.STONE_BRICKS);
        c.f(-14, 0, -17, -14, 5, 17, Blocks.STONE_BRICKS); c.f(14, 0, -17, 14, 5, 17, Blocks.STONE_BRICKS);
        c.f(-14, 0, -17, 14, 5, -17, Blocks.STONE_BRICKS);
        c.f(-14, 0, 17, 14, 4, 17, Blocks.STONE_BRICKS); c.fa(-1, 0, 17, 1, 3, 17);
        c.f(-14, 0, 2, 14, 5, 2, Blocks.STONE_BRICKS); c.fa(-1, 0, 2, 1, 3, 2);
        c.f(-14, 0, -8, 14, 5, -8, Blocks.STONE_BRICKS); c.fa(-1, 0, -8, 1, 3, -8);
        c.f(-14, 6, -17, 14, 6, 2, Blocks.STONE_BRICKS);
        for (int sx = -1; sx <= 1; sx += 2) {
            c.f(sx * 5, 0, -2, sx * 5, 5, -2, Blocks.CHISELED_STONE_BRICKS);
            c.f(sx * 6, 0, -12, sx * 6, 5, -12, Blocks.CHISELED_STONE_BRICKS);
            ProcKit.hangLantern(c.l, c.cx + sx * 4, c.y0 + 5, c.cz - 2);
            ProcKit.hangLantern(c.l, c.cx + sx * 5, c.y0 + 5, c.cz - 12);
            c.f(sx * 10, 0, 6, sx * 10, 3, 6, Blocks.STONE_BRICKS);
        }
        c.f(-3, 0, -15, 3, 0, -13, Blocks.POLISHED_BLACKSTONE);
        c.chest(0, 1, -16, Direction.SOUTH);
        c.act("chest", 0, 1, -16);
        c.lectern(5, 0, -16, Direction.SOUTH);
        c.act("lore_tablet", 5, 0, -16);
        for (int sx = -1; sx <= 1; sx += 2) {
            c.act("sp_room", sx * 6, 0, 10); c.act("sp_room", sx * 6, 0, 14);
            c.act("sp_room", sx * 5, 0, -4); c.act("sp_room", sx * 5, 0, 0);
        }
        c.act("sp_boss", 0, 0, -12);
        ProcKit.decay(c.l, c.cx, c.cz, c.y0, 17, 8, c.seed);
        c.b(0, 1, -16, Blocks.AIR);
        c.chest(0, 1, -16, Direction.SOUTH);
        c.fa(-1, 0, 17, 1, 3, 17);
    }

    // ------------------------------------------------------------------ A11 Kiem Trung
    private static void swordMound(C c) {
        for (int dx = -11; dx <= 11; dx++) for (int dz = -11; dz <= 11; dz++) {
            double r = Math.sqrt(dx * dx + dz * dz);
            if (r > 11 || r < 3.5) continue;
            int hs = c.h(dx, 0, dz);
            if (hs % 5 == 0) { int hh = 1 + (int) ((11 - r) / 4) + (hs & 1); for (int k = 0; k < hh; k++) c.b(dx, k, dz, Blocks.IRON_BARS); if (hs % 10 == 0) c.b(dx, hh, dz, Blocks.CHAIN); }
        }
        c.f(-3, -1, -3, 3, -1, 3, Blocks.POLISHED_ANDESITE);
        c.f(0, 0, 0, 0, 8, 0, Blocks.IRON_BLOCK);
        c.f(-2, 2, 0, 2, 2, 0, Blocks.QUARTZ_BLOCK);
        c.b(0, 9, 0, Blocks.GOLD_BLOCK);
        c.chest(3, 0, 0, Direction.WEST);
        c.act("chest", 3, 0, 0);
        for (int k = 0; k < 8; k++) { double a = k * Math.PI / 4; c.act("sp_room", (int) Math.round(Math.cos(a) * 7), 0, (int) Math.round(Math.sin(a) * 7)); }
    }

    // ------------------------------------------------------------------ A12 Ma Uyen
    private static void abyss(C c) {
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 <= 25) {
                for (int dy = -5; dy <= 0; dy++) c.a(dx, dy, dz);
                c.b(dx, -6, dz, d2 <= 4 ? Blocks.POLISHED_BLACKSTONE : (c.h(dx, -6, dz) < 90 ? Blocks.SOUL_SAND : Blocks.BLACKSTONE));
            } else if (d2 <= 40) {
                c.b(dx, -1, dz, c.h(dx, -1, dz) < 110 ? Blocks.CRYING_OBSIDIAN : Blocks.BLACKSTONE);
            }
        }
        for (int dy = -5; dy <= -1; dy++) c.ladder(5, dy, 0, Direction.WEST);
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            c.f(sx * 8, 0, sz * 8, sx * 8, 4, sz * 8, Blocks.POLISHED_BLACKSTONE_BRICKS);
            c.b(sx * 8, 5, sz * 8, Blocks.SOUL_LANTERN);
        }
        c.chest(0, -5, 0, Direction.EAST);
        c.act("chest", 0, -5, 0);
        for (int k = 0; k < 6; k++) { double a = k * Math.PI / 3; c.act("sp_room", (int) Math.round(Math.cos(a) * 4), -5, (int) Math.round(Math.sin(a) * 4)); }
    }

    // ------------------------------------------------------------------ A13 Dao Quan Son Dinh
    private static void daoQuan(C c) {
        c.f(-8, -1, -8, 8, -1, 8, Blocks.QUARTZ_BRICKS);
        for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 >= 9 && d2 <= 13) c.b(dx, -1, dz, Blocks.POLISHED_ANDESITE);
            if (d2 <= 2) c.b(dx, -1, dz, Blocks.CHISELED_QUARTZ_BLOCK);
        }
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) c.f(sx * 4, 0, sz * 4, sx * 4, 4, sz * 4, Blocks.QUARTZ_PILLAR);
        ProcKit.hipRoof(c.l, c.cx, c.cz, c.y0 + 5, 5, 5, Blocks.QUARTZ_STAIRS, Blocks.QUARTZ_BLOCK);
        c.lectern(0, 0, 0, Direction.SOUTH);
        c.act("claim", 0, 0, 0);
        for (int sx = -1; sx <= 1; sx += 2) { ProcKit.hangLantern(c.l, c.cx + sx * 4, c.y0 + 4, c.cz); }
        c.f(-1, -1, 9, 1, -1, 9, Blocks.QUARTZ_STAIRS);
    }

    // ------------------------------------------------------------------ A14 Vo Dai Thiet Coc
    private static void arena(C c) {
        c.f(-8, -1, -8, 8, -1, 8, Blocks.SMOOTH_STONE);
        for (int k = -9; k <= 9; k++) {
            if (Math.abs(k) > 1) { c.b(k, 0, -9, Blocks.STONE_BRICKS); c.b(k, 0, 9, Blocks.STONE_BRICKS); c.b(-9, 0, k, Blocks.STONE_BRICKS); c.b(9, 0, k, Blocks.STONE_BRICKS); }
        }
        c.b(-8, 0, -8, Blocks.ANVIL); c.b(8, 0, -8, Blocks.BLAST_FURNACE); c.b(-8, 0, 8, Blocks.GRINDSTONE); c.b(8, 0, 8, Blocks.SMITHING_TABLE);
        c.b(-5, 0, 5, Blocks.HAY_BLOCK); c.b(-5, 1, 5, Blocks.CARVED_PUMPKIN); c.b(5, 0, 5, Blocks.HAY_BLOCK); c.b(5, 1, 5, Blocks.CARVED_PUMPKIN);
        c.f(-2, 0, -10, 2, 0, -10, Blocks.IRON_BLOCK);
        c.chest(0, 1, -8, Direction.SOUTH);
        c.act("chest", 0, 1, -8);
        c.act("sp_room", -4, 0, 0); c.act("sp_room", 4, 0, 0); c.act("sp_room", 0, 0, 4); c.act("sp_room", 0, 0, -3);
    }

    // ------------------------------------------------------------------ A15 Ngu Hanh Tran Dai
    private static void wuxing(C c) {
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 <= 49) c.b(dx, -1, dz, d2 >= 25 && d2 <= 36 ? Blocks.POLISHED_ANDESITE : Blocks.SMOOTH_QUARTZ);
        }
        for (int slot = 0; slot < 5; slot++) {
            double a = slot * 2 * Math.PI / 5;
            int px = (int) Math.round(Math.sin(a) * 5), pz = (int) Math.round(-Math.cos(a) * 5);
            Block e = elemBlock(PoiPuzzle.elementAt(c.seed, 5, slot));
            c.b(px, 0, pz, e); c.b(px, 1, pz, e); c.b(px, 2, pz, e);
            c.b(px, 3, pz, Blocks.SEA_LANTERN);
            c.act("pillar:" + slot, px, 2, pz);
        }
        c.chest(0, 0, 0, Direction.SOUTH);
        c.act("claim", 0, 0, 0);
        c.lectern(0, 0, 7, Direction.NORTH);
        c.act("tablet", 0, 0, 7);
    }

    /** Dat tru/can o trang thai sang (da kich hoat) hay tat: chi dung cho Ngu Hanh Tran Dai (SEA_LANTERN <-> POLISHED_ANDESITE nhe). */
    public static void setPillarLit(ServerLevel l, PoiInstance.Act a, boolean lit) {
        ProcKit.set(l, a.x(), a.y() + 1, a.z(), lit ? Blocks.SEA_LANTERN : Blocks.POLISHED_ANDESITE);
    }

    public static void resetPillars(ServerLevel l, PoiInstance i, int seed) {
        for (PoiInstance.Act a : i.acts) if (a.kind().startsWith("pillar:")) {
            int slot = Integer.parseInt(a.kind().substring(7));
            ProcKit.set(l, a.x(), a.y() + 1, a.z(), elemBlock(PoiPuzzle.elementAt(seed, 5, slot)));
        }
    }

    /** Dat lai can gat ve tat. */
    public static void resetLevers(ServerLevel l, PoiInstance i) {
        for (PoiInstance.Act a : i.acts) if (a.kind().startsWith("lever:")) {
            BlockState s = l.getBlockState(a.pos());
            if (s.is(Blocks.LEVER)) l.setBlock(a.pos(), s.setValue(LeverBlock.POWERED, false), 3);
        }
    }

    public static void setLever(ServerLevel l, PoiInstance.Act a, boolean on) {
        BlockState s = l.getBlockState(a.pos());
        if (s.is(Blocks.LEVER)) l.setBlock(a.pos(), s.setValue(LeverBlock.POWERED, on), 3);
    }
}
