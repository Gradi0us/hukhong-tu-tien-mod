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
            case "cave" -> 12; case "battlefield" -> 18; case "grave" -> 9; case "ruin" -> 20; case "sword_mound" -> 14;
            case "abyss" -> 13; case "dao_quan" -> 11; case "arena" -> 12; case "wuxing" -> 10; default -> 6;
        };
    }

    public static int height(String b) {
        return switch (b) {
            case "inn" -> 14; case "waterfall" -> 12; case "sword_mound" -> 20; case "dao_quan" -> 14; case "cave" -> 12; case "ruin" -> 12;
            case "battlefield" -> 12; case "grave" -> 10; case "abyss", "shrine", "stele" -> 9; case "arena" -> 8; case "wuxing" -> 8; default -> 6;
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
        connect(l, cx, cz, half, y0, height(d.builder()));
        return new Built(y0, c.acts);
    }

    /** Nối hàng rào / tường / song sắt / kính (ProcKit đặt không cập nhật hàng xóm nên chúng thành cột rời). */
    static void connect(ServerLevel l, int cx, int cz, int half, int y0, int h) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) for (int y = y0 - 7; y <= y0 + h + 3; y++) {
            BlockState st = l.getBlockState(mp.set(cx + dx, y, cz + dz));
            Block b = st.getBlock();
            if (b instanceof net.minecraft.world.level.block.FenceBlock || b instanceof net.minecraft.world.level.block.WallBlock
                    || b instanceof net.minecraft.world.level.block.IronBarsBlock || b instanceof net.minecraft.world.level.block.StairBlock) {
                BlockState n = Block.updateFromNeighbourShapes(st, l, mp);
                if (n != st) l.setBlock(mp, n, 2 | 16);
            }
        }
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
        void chest(int dx, int dy, int dz, Direction face) { chest(dx, dy, dz, face, 1); }

        /** Bảo Rương cấp {@code tier} (0 Gỗ Đồng .. 3 Hư Không) thay rương thường. */
        void chest(int dx, int dy, int dz, Direction face, int tier) {
            s(dx, dy, dz, com.hukhong.treasure.TreasureRegistry.BAO_RUONG.get().defaultBlockState()
                    .setValue(com.hukhong.treasure.BaoRuongBlock.FACING, face).setValue(com.hukhong.treasure.BaoRuongBlock.TIER, tier));
        }
        void lectern(int dx, int dy, int dz, Direction face) { s(dx, dy, dz, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, face)); }
        void lever(int dx, int dy, int dz, Direction face) {
            s(dx, dy, dz, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, face).setValue(LeverBlock.POWERED, false));
        }
        void bed(int dx, int dz, Direction face) {
            s(dx, 1, dz, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, face).setValue(BedBlock.PART, BedPart.FOOT));
            s(dx + face.getStepX(), 1, dz + face.getStepZ(), Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, face).setValue(BedBlock.PART, BedPart.HEAD));
        }
        void ladder(int dx, int dy, int dz, Direction face) { s(dx, dy, dz, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, face)); }

        // ---------------------------------------------------------- trang trí hoành tráng (2026-11-06)
        void st(int dx, int dy, int dz, Block stairs, Direction facing) { ProcKit.stair(l, cx + dx, y0 + dy, cz + dz, stairs, facing); }
        void stUp(int dx, int dy, int dz, Block stairs, Direction facing) {
            s(dx, dy, dz, stairs.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, facing)
                    .setValue(net.minecraft.world.level.block.StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
        }
        boolean air(int dx, int dy, int dz) { return l.getBlockState(new BlockPos(cx + dx, y0 + dy, cz + dz)).isAir(); }

        /** Đèn đá kiểu cổ: chân tường đá, mâm phiến, đèn, nắp. */
        void stoneLantern(int dx, int dz) {
            b(dx, 0, dz, Blocks.STONE_BRICK_WALL); b(dx, 1, dz, Blocks.STONE_BRICK_WALL);
            b(dx, 2, dz, Blocks.CHISELED_STONE_BRICKS); b(dx, 3, dz, Blocks.LANTERN);
            s(dx, 4, dz, Blocks.STONE_BRICK_SLAB.defaultBlockState());
        }

        /** Lò lửa: đế đá đen, lửa hồn / lửa thường, xích treo bốn góc. */
        void brazier(int dx, int dz, boolean soul) {
            b(dx, 0, dz, Blocks.POLISHED_BLACKSTONE_BRICKS); b(dx, 1, dz, Blocks.POLISHED_BLACKSTONE_WALL);
            s(dx, 2, dz, (soul ? Blocks.SOUL_CAMPFIRE : Blocks.CAMPFIRE).defaultBlockState());
        }

        /** Cổng tam quan (bài phường): 2 trụ, 2 xà, mái ngói hai tầng, biển hiệu giữa, đèn treo. Trục theo X tại hàng z. */
        void paifang(int z, int w, int h, Block post, Block beam, Block roofStair, Block roofFull) {
            for (int sx = -1; sx <= 1; sx += 2) {
                f(sx * w, 0, z, sx * w, h, z, post);
                b(sx * w, 0, z - 1, Blocks.POLISHED_ANDESITE); b(sx * w, 0, z + 1, Blocks.POLISHED_ANDESITE); // chân đế
                st(sx * w, 1, z - 1, Blocks.POLISHED_ANDESITE_STAIRS, Direction.SOUTH);
                st(sx * w, 1, z + 1, Blocks.POLISHED_ANDESITE_STAIRS, Direction.NORTH);
            }
            f(-w, h - 1, z, w, h - 1, z, beam);
            f(-w - 1, h + 1, z, w + 1, h + 1, z, beam);
            for (int x = -w - 2; x <= w + 2; x++) { st(x, h + 2, z - 1, roofStair, Direction.SOUTH); st(x, h + 2, z + 1, roofStair, Direction.NORTH); b(x, h + 2, z, roofFull); }
            st(-w - 3, h + 2, z, roofStair, Direction.EAST); st(w + 3, h + 2, z, roofStair, Direction.WEST);
            for (int x = -1; x <= 1; x++) { st(x, h + 3, z - 1, roofStair, Direction.SOUTH); st(x, h + 3, z + 1, roofStair, Direction.NORTH); b(x, h + 3, z, roofFull); }
            b(0, h, z, Blocks.CHISELED_STONE_BRICKS); b(-1, h, z, Blocks.GOLD_BLOCK); b(1, h, z, Blocks.GOLD_BLOCK); // biển hiệu
            ProcKit.hangLantern(l, cx - w + 1, y0 + h - 2, cz + z); ProcKit.hangLantern(l, cx + w - 1, y0 + h - 2, cz + z);
        }

        /** Gạch vụn rải rác trong bán kính r. */
        void rubble(int r, int count, int salt) {
            for (int i = 0; i < count; i++) {
                int hx = h(i, salt, 1), hz = h(i, salt, 2);
                int dx = hx % (2 * r + 1) - r, dz = hz % (2 * r + 1) - r;
                if (dx * dx + dz * dz > r * r || !air(dx, 0, dz)) continue;
                Block b = switch (hx % 5) { case 0 -> Blocks.COBBLESTONE_SLAB; case 1 -> Blocks.MOSSY_COBBLESTONE; case 2 -> Blocks.CRACKED_STONE_BRICKS; case 3 -> Blocks.STONE_BRICK_SLAB; default -> Blocks.MOSSY_STONE_BRICK_SLAB; };
                b(dx, 0, dz, b);
            }
        }

        /** Cây khô: thân gỗ sẫm, cành rào chĩa ra. */
        void deadTree(int dx, int dz, int ht) {
            f(dx, 0, dz, dx, ht, dz, Blocks.DARK_OAK_LOG);
            b(dx + 1, ht - 1, dz, Blocks.DARK_OAK_FENCE); b(dx + 2, ht, dz, Blocks.DARK_OAK_FENCE);
            b(dx - 1, ht - 2, dz, Blocks.DARK_OAK_FENCE); b(dx, ht - 1, dz + 1, Blocks.DARK_OAK_FENCE); b(dx, ht + 1, dz, Blocks.DARK_OAK_FENCE);
        }

        /** Kiếm cắm đất: lưỡi song sắt cao {@code blade}, chắn kiếm xích nằm ngang, núm lõi nặng. */
        void plantedSword(int dx, int dz, int blade) {
            f(dx, 0, dz, dx, blade - 1, dz, Blocks.IRON_BARS);
            s(dx, blade, dz, Blocks.CHAIN.defaultBlockState().setValue(net.minecraft.world.level.block.ChainBlock.AXIS, h(dx, 7, dz) % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z));
            b(dx, blade + 1, dz, Blocks.HEAVY_CORE);
        }

        /** Sọ xoay ngẫu nhiên. */
        void skull(int dx, int dy, int dz) {
            s(dx, dy, dz, Blocks.SKELETON_SKULL.defaultBlockState().setValue(net.minecraft.world.level.block.SkullBlock.ROTATION, h(dx, dy, dz) % 16));
        }

        /** Tượng kỳ lân đá canh cửa (nhìn về {@code face}). */
        void guardian(int dx, int dz, Direction face) {
            b(dx, 0, dz, Blocks.POLISHED_ANDESITE); b(dx, 1, dz, Blocks.POLISHED_DEEPSLATE);
            st(dx, 2, dz, Blocks.POLISHED_DEEPSLATE_STAIRS, face.getOpposite());
            b(dx + face.getStepX(), 1, dz + face.getStepZ(), Blocks.POLISHED_DEEPSLATE_WALL);
            b(dx, 3, dz, Blocks.DEEPSLATE_TILE_SLAB);
        }

        /** Cờ chiến trên cán cao. */
        void warBanner(int dx, int dz, int ht, boolean red) {
            f(dx, 0, dz, dx, ht, dz, Blocks.DARK_OAK_FENCE);
            s(dx, ht + 1, dz, (red ? Blocks.RED_BANNER : Blocks.BLACK_BANNER).defaultBlockState()
                    .setValue(net.minecraft.world.level.block.BannerBlock.ROTATION, h(dx, ht, dz) % 16));
        }
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
        // bệ bậc thềm, mái che, đèn đá, rùa đội bia
        for (int k = -3; k <= 3; k++) { c.st(k, -1, 4, Blocks.STONE_BRICK_STAIRS, Direction.NORTH); }
        c.b(0, 0, 1, Blocks.MOSSY_STONE_BRICKS); c.st(0, 0, 2, Blocks.STONE_BRICK_STAIRS, Direction.NORTH); // đầu rùa
        c.b(-1, 0, -1, Blocks.MOSSY_STONE_BRICKS); c.b(1, 0, -1, Blocks.MOSSY_STONE_BRICKS);
        c.stoneLantern(-3, -2); c.stoneLantern(3, -2);
        for (int x = -2; x <= 2; x++) { c.st(x, 5, -1, Blocks.DARK_OAK_STAIRS, Direction.SOUTH); c.st(x, 5, 1, Blocks.DARK_OAK_STAIRS, Direction.NORTH); c.b(x, 5, 0, Blocks.DARK_OAK_PLANKS); }
        c.b(-2, 4, 0, Blocks.DARK_OAK_FENCE); c.b(2, 4, 0, Blocks.DARK_OAK_FENCE);
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
        // cổng tam quan đỏ, đèn đá, lư hương lớn trước miếu
        c.paifang(5, 3, 4, Blocks.STRIPPED_MANGROVE_LOG, Blocks.RED_TERRACOTTA, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
        c.stoneLantern(-3, 1); c.stoneLantern(3, 1);
        c.b(0, 0, 1, Blocks.CAULDRON); c.s(0, 1, 1, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
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
        // đình nghỉ bốn trụ có mái trên Linh Tuyền, đèn đá, hoa sen
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) c.f(sx * 4, 0, sz * 4, sx * 4, 4, sz * 4, Blocks.STRIPPED_CHERRY_LOG);
        ProcKit.hipRoof(c.l, c.cx, c.cz, c.y0 + 5, 5, 5, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
        ProcKit.hangLantern(c.l, c.cx, c.y0 + 4, c.cz);
        c.b(-1, 0, 2, Blocks.LILY_PAD); c.b(2, 0, -1, Blocks.LILY_PAD); c.b(0, 0, -2, Blocks.LILY_PAD);
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
        // đất nứt tím, tinh thể nhọn, mảnh hắc diệu thạch lơ lửng
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4; int x = (int) Math.round(Math.cos(a) * 4), z = (int) Math.round(Math.sin(a) * 4);
            c.b(x, -1, z, Blocks.CRYING_OBSIDIAN); c.b(x, 0, z, Blocks.AMETHYST_CLUSTER);
        }
        c.b(-3, 6, 0, Blocks.OBSIDIAN); c.b(3, 7, 1, Blocks.CRYING_OBSIDIAN); c.b(0, 8, -2, Blocks.OBSIDIAN);
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
        // cửa động: cổng đá khắc ngoài lối vào, đèn đá, nhũ đá trong động
        c.f(-3, 0, 10, -2, 4, 10, Blocks.CHISELED_STONE_BRICKS); c.f(2, 0, 10, 3, 4, 10, Blocks.CHISELED_STONE_BRICKS);
        c.f(-3, 3, 10, 3, 4, 10, Blocks.STONE_BRICKS); c.fa(-1, 0, 10, 1, 2, 10);
        c.b(0, 4, 10, Blocks.GILDED_BLACKSTONE);
        c.stoneLantern(-4, 11); c.stoneLantern(4, 11);
        for (int dx = -4; dx <= 4; dx++) for (int dz = -5; dz <= 5; dz++)
            if (c.h(dx, 9, dz) % 11 == 0 && c.air(dx, 3, dz) && !c.air(dx, 4, dz))
                c.s(dx, 3, dz, Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(net.minecraft.world.level.block.PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN));
    }

    /** Mo / dong an khoa phong bao vat. */
    public static void setSeal(ServerLevel l, PoiInstance i, boolean open) {
        PoiInstance.Act v = i.firstAct("vault");
        if (v == null) return;
        for (int dy = 0; dy <= 1; dy++) ProcKit.set(l, v.x(), v.y() + dy, v.z() + 2, open ? Blocks.AIR : Blocks.CRYING_OBSIDIAN);
    }

    // ------------------------------------------------------------------ A3 Co Chien Truong
    private static void battlefield(C c) {
        // nền đất cháy: đất thô, sỏi, đất hồn, bùn; hố bom có than hồng
        for (int dx = -17; dx <= 17; dx++) for (int dz = -17; dz <= 17; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 > 17 * 17) continue;
            int hs = c.h(dx, -1, dz) % 100;
            c.b(dx, -1, dz, hs < 30 ? Blocks.COARSE_DIRT : hs < 48 ? Blocks.GRAVEL : hs < 62 ? Blocks.SOUL_SOIL : hs < 72 ? Blocks.MUD : hs < 80 ? Blocks.PODZOL : Blocks.ROOTED_DIRT);
        }
        int[][] craters = {{-9, -6, 3}, {8, 7, 3}, {10, -9, 2}, {-6, 10, 2}};
        for (int[] k : craters) for (int dx = -k[2]; dx <= k[2]; dx++) for (int dz = -k[2]; dz <= k[2]; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 > k[2] * k[2]) continue;
            c.a(k[0] + dx, -1, k[1] + dz);
            c.b(k[0] + dx, -2, k[1] + dz, d2 <= 1 ? Blocks.MAGMA_BLOCK : c.h(dx, -2, dz) % 3 == 0 ? Blocks.BLACKSTONE : Blocks.BASALT);
            if (d2 == 0) c.s(k[0], -1, k[1], Blocks.CAMPFIRE.defaultBlockState()); // tàn lửa còn cháy
        }
        // tường thành đổ nát hai phía (răng cưa, lỗ thủng)
        for (int sx = -1; sx <= 1; sx += 2) for (int dz = -12; dz <= 12; dz++) {
            int ht = 2 + c.h(sx, 0, dz) % 4;
            if (c.h(sx, 1, dz) % 7 == 0) continue; // lỗ thủng
            for (int y = 0; y < ht; y++) c.b(sx * 15, y, dz, c.h(sx, y, dz) % 3 == 0 ? Blocks.CRACKED_STONE_BRICKS : c.h(sx, y, dz) % 3 == 1 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS);
            if (dz % 2 == 0 && ht >= 4) c.b(sx * 15, ht, dz, Blocks.STONE_BRICK_WALL);
            if (c.h(sx, 2, dz) % 5 == 0) c.b(sx * 14, 0, dz, Blocks.COBBLESTONE_SLAB);
        }
        // đài tưởng niệm giữa: bệ ba bậc, trụ đá đen cao, cờ trên đỉnh, 4 lò lửa hồn
        c.f(-3, 0, -3, 3, 0, 3, Blocks.POLISHED_BLACKSTONE_BRICKS);
        c.f(-2, 1, -2, 2, 1, 2, Blocks.POLISHED_BLACKSTONE);
        for (int k = -3; k <= 3; k++) { c.st(k, 1, -3, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.SOUTH); c.st(k, 1, 3, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.NORTH); }
        for (int k = -2; k <= 2; k++) { c.st(-3, 1, k, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST); c.st(3, 1, k, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST); }
        c.f(0, 2, 0, 0, 8, 0, Blocks.POLISHED_BLACKSTONE_BRICKS);
        c.b(0, 4, 0, Blocks.CHISELED_POLISHED_BLACKSTONE); c.b(0, 7, 0, Blocks.GILDED_BLACKSTONE);
        for (int k = 0; k < 4; k++) { Direction d = Direction.from2DDataValue(k); c.st(d.getStepX(), 2, d.getStepZ(), Blocks.POLISHED_BLACKSTONE_STAIRS, d.getOpposite()); }
        c.warBanner(0, 0, 9, false);
        c.b(0, 9, 0, Blocks.DARK_OAK_FENCE);
        for (int k = 0; k < 4; k++) { int dx = (k & 1) == 0 ? 2 : -2, dz = (k & 2) == 0 ? 2 : -2; c.s(dx, 2, dz, Blocks.SOUL_LANTERN.defaultBlockState()); }
        for (int k = 0; k < 4; k++) { int dx = (k & 1) == 0 ? 6 : -6, dz = (k & 2) == 0 ? 6 : -6; c.brazier(dx, dz, true); }
        // cờ chiến rách, cọc giáo, rừng vũ khí cắm đất, đống xương, sọ
        int[][] banners = {{-11, -12, 6, 1}, {11, -11, 5, 0}, {-12, 9, 5, 0}, {12, 11, 6, 1}, {-4, -14, 4, 1}, {5, 14, 4, 0}};
        for (int[] bn : banners) c.warBanner(bn[0], bn[1], bn[2], bn[3] == 1);
        for (int dx = -16; dx <= 16; dx++) for (int dz = -16; dz <= 16; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 < 30 || d2 > 16 * 16 || !c.air(dx, 0, dz)) continue;
            int hs = c.h(dx, 0, dz) % 1000;
            if (hs < 22) c.plantedSword(dx, dz, 1 + hs % 2); // kiếm cắm đất
            else if (hs < 34) { c.b(dx, 0, dz, Blocks.DARK_OAK_FENCE); c.b(dx, 1, dz, Blocks.DARK_OAK_FENCE); c.b(dx, 2, dz, Blocks.END_ROD); } // giáo
            else if (hs < 37) c.s(dx, 0, dz, Blocks.BONE_BLOCK.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, hs % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z));
            else if (hs < 54) c.skull(dx, 0, dz);
            else if (hs < 64) c.b(dx, 0, dz, Blocks.DEAD_BUSH);
            else if (hs < 70) c.b(dx, 0, dz, Blocks.COBWEB);
            else if (hs < 76) c.b(dx, 0, dz, Blocks.CHAIN);
        }
        // xác máy bắn đá đổ nghiêng
        c.f(-9, 0, 2, -5, 0, 2, Blocks.DARK_OAK_LOG); c.f(-9, 0, 4, -5, 0, 4, Blocks.DARK_OAK_LOG);
        c.f(-7, 1, 2, -7, 1, 4, Blocks.STRIPPED_DARK_OAK_LOG);
        c.st(-8, 1, 3, Blocks.DARK_OAK_STAIRS, Direction.EAST); c.b(-6, 2, 3, Blocks.DARK_OAK_FENCE); c.b(-5, 3, 3, Blocks.DARK_OAK_FENCE); c.b(-4, 4, 3, Blocks.DARK_OAK_FENCE);
        c.b(-3, 4, 3, Blocks.CAULDRON); c.b(-9, 1, 2, Blocks.DARK_OAK_FENCE); c.b(-9, 1, 4, Blocks.DARK_OAK_FENCE);
        for (int k = 0; k < 3; k++) c.b(-10 + k, 0, 6, Blocks.COBBLESTONE); // đạn đá rơi
        // trống trận vỡ, xe chở tên
        c.b(7, 0, -4, Blocks.BARREL); c.b(8, 0, -4, Blocks.BARREL); c.b(7, 1, -4, Blocks.TARGET);
        c.f(5, 0, -6, 9, 0, -6, Blocks.SPRUCE_SLAB);
        c.rubble(16, 60, 7);
        c.act("center", 0, 0, 0);
    }

    // ------------------------------------------------------------------ A20 Mo Vo Danh
    private static void grave(C c) {
        // sân mộ lát đá có tường thấp, gò mộ phía sau, bia cao có mái, thần đạo với đèn đá và kỳ lân
        for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) {
            int ad = Math.max(Math.abs(dx), Math.abs(dz));
            if (ad <= 7) c.b(dx, -1, dz, c.h(dx, -1, dz) % 4 == 0 ? Blocks.MOSSY_STONE_BRICKS : c.h(dx, -1, dz) % 4 == 1 ? Blocks.CRACKED_STONE_BRICKS : Blocks.PODZOL);
            if (ad == 8 && !(Math.abs(dx) <= 1 && dz == 8)) {
                c.b(dx, 0, dz, Blocks.MOSSY_COBBLESTONE_WALL);
                if ((dx + dz) % 4 == 0) { c.b(dx, 1, dz, Blocks.MOSSY_COBBLESTONE_WALL); c.b(dx, 2, dz, Blocks.SOUL_LANTERN); }
            }
        }
        // gò mộ hình vòm
        for (int dx = -4; dx <= 4; dx++) for (int dz = -8; dz <= -3; dz++) {
            double r = Math.sqrt(dx * dx + (dz + 6) * (dz + 6) * 1.4);
            int ht = (int) Math.round(3.2 - r * 0.8);
            for (int y = 0; y < ht; y++) c.b(dx, y, dz, y == ht - 1 ? (c.h(dx, y, dz) % 3 == 0 ? Blocks.MOSS_BLOCK : Blocks.PODZOL) : Blocks.COARSE_DIRT);
        }
        // bia mộ: bệ, thân đá sẫm cao, chữ khắc, mái nhỏ
        c.f(-1, 0, -3, 1, 0, -3, Blocks.POLISHED_DEEPSLATE);
        c.b(0, 0, -3, Blocks.COBBLED_DEEPSLATE);
        c.f(0, 1, -3, 0, 3, -3, Blocks.POLISHED_DEEPSLATE);
        c.b(0, 2, -3, Blocks.CHISELED_DEEPSLATE);
        c.b(-1, 1, -3, Blocks.DEEPSLATE_TILE_WALL); c.b(1, 1, -3, Blocks.DEEPSLATE_TILE_WALL);
        c.st(-1, 4, -3, Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST); c.st(1, 4, -3, Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST);
        c.b(0, 4, -3, Blocks.DEEPSLATE_TILES); c.b(0, 5, -3, Blocks.DEEPSLATE_TILE_SLAB);
        // lư hương + nến + mâm lễ
        c.b(0, 0, -1, Blocks.CAULDRON); c.s(0, 1, -1, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        c.b(-1, 0, -2, Blocks.POLISHED_DEEPSLATE_SLAB); c.b(1, 0, -2, Blocks.POLISHED_DEEPSLATE_SLAB);
        c.s(-1, 1, -2, Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
        c.s(1, 1, -2, Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
        c.b(-2, 0, -2, Blocks.FLOWER_POT); c.b(2, 0, -2, Blocks.POTTED_WITHER_ROSE);
        // thần đạo
        c.f(-1, -1, 0, 1, -1, 8, Blocks.STONE_BRICKS);
        c.f(0, -1, 0, 0, -1, 8, Blocks.POLISHED_ANDESITE);
        c.stoneLantern(-3, 2); c.stoneLantern(3, 2); c.stoneLantern(-3, 6); c.stoneLantern(3, 6);
        c.guardian(-2, 7, Direction.SOUTH); c.guardian(2, 7, Direction.SOUTH);
        c.deadTree(-6, -6, 5); c.deadTree(6, -5, 4); c.deadTree(-6, 5, 3);
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++)
            if (c.air(dx, 0, dz) && c.h(dx, 0, dz) % 23 == 0) c.b(dx, 0, dz, c.h(dx, 1, dz) % 2 == 0 ? Blocks.DEAD_BUSH : Blocks.COBWEB);
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
        // cổng tam quan đổ nghiêng ngoài cửa nam, trụ gãy, tượng canh, lò lửa sân trong
        c.paifang(19, 3, 5, Blocks.CHISELED_STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
        c.a(3, 7, 19); c.a(4, 7, 19); c.a(5, 7, 19); c.a(-5, 6, 19); // mái sứt
        c.guardian(-3, 20, Direction.SOUTH); c.guardian(3, 20, Direction.SOUTH);
        for (int sx = -1; sx <= 1; sx += 2) for (int z = 6; z <= 14; z += 4) {
            int ht = 1 + c.h(sx, z, 0) % 4;
            c.f(sx * 3, 0, z, sx * 3, ht, z, Blocks.QUARTZ_PILLAR);
            if (ht < 3) c.b(sx * 3 + sx, 0, z, Blocks.QUARTZ_SLAB);
        }
        c.brazier(-3, -10, false); c.brazier(3, -10, false);
        c.rubble(18, 70, 11);
    }

    // ------------------------------------------------------------------ A11 Kiem Trung
    private static void swordMound(C c) {
        // núi đá giữa, cự kiếm cao ~16 khối cắm xuống; rừng kiếm quanh vòng tròn; vòng đá khắc phù
        for (int dx = -13; dx <= 13; dx++) for (int dz = -13; dz <= 13; dz++) {
            double r = Math.sqrt(dx * dx + dz * dz);
            if (r > 13) continue;
            c.b(dx, -1, dz, r < 4 ? Blocks.POLISHED_ANDESITE : r < 5 ? Blocks.CHISELED_STONE_BRICKS : c.h(dx, -1, dz) % 3 == 0 ? Blocks.GRAVEL : Blocks.STONE);
            if (r >= 9.5 && r < 10.5) c.b(dx, -1, dz, c.h(dx, 0, dz) % 4 == 0 ? Blocks.CHISELED_STONE_BRICKS : Blocks.POLISHED_DEEPSLATE);
            if (r > 4.5 && r < 11 && c.h(dx, 0, dz) % 9 == 0) { // kiếm cắm đất
                c.plantedSword(dx, dz, 1 + c.h(dx, 1, dz) % 3);
            }
        }
        // núi đá
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            double r = Math.sqrt(dx * dx + dz * dz);
            int ht = (int) Math.round(3 - r * 0.75);
            for (int y = 0; y < ht; y++) c.b(dx, y, dz, c.h(dx, y, dz) % 3 == 0 ? Blocks.MOSSY_COBBLESTONE : c.h(dx, y, dz) % 3 == 1 ? Blocks.COBBLESTONE : Blocks.ANDESITE);
        }
        // lưỡi kiếm: lõi khối sắt, mép song sắt, rãnh xích
        for (int y = 1; y <= 10; y++) {
            c.b(0, y, 0, Blocks.IRON_BLOCK);
            c.b(-1, y, 0, y > 2 ? Blocks.IRON_BARS : Blocks.IRON_BLOCK); c.b(1, y, 0, y > 2 ? Blocks.IRON_BARS : Blocks.IRON_BLOCK);
        }
        c.b(0, 0, 0, Blocks.IRON_BLOCK);
        // chắn kiếm vàng + thạch anh
        c.f(-3, 11, 0, 3, 11, 0, Blocks.GOLD_BLOCK);
        c.st(-4, 11, 0, Blocks.QUARTZ_STAIRS, Direction.EAST); c.st(4, 11, 0, Blocks.QUARTZ_STAIRS, Direction.WEST);
        c.b(0, 11, -1, Blocks.QUARTZ_BLOCK); c.b(0, 11, 1, Blocks.QUARTZ_BLOCK);
        c.b(0, 11, 0, Blocks.EMERALD_BLOCK); // ngọc chắn kiếm
        // chuôi + núm
        c.f(0, 12, 0, 0, 14, 0, Blocks.STRIPPED_DARK_OAK_LOG);
        c.b(0, 15, 0, Blocks.GOLD_BLOCK); c.b(0, 16, 0, Blocks.GLOWSTONE);
        c.b(0, 13, -1, Blocks.RED_WOOL); c.b(0, 13, 1, Blocks.RED_WOOL); // tua kiếm
        // dây xích trấn kiếm từ 4 trụ đá
        for (int k = 0; k < 4; k++) {
            int sx = (k & 1) == 0 ? 6 : -6, sz = (k & 2) == 0 ? 6 : -6;
            c.f(sx, 0, sz, sx, 6, sz, Blocks.POLISHED_DEEPSLATE);
            c.b(sx, 3, sz, Blocks.CHISELED_DEEPSLATE);
            c.b(sx, 7, sz, Blocks.SOUL_LANTERN);
            int mx = sx / 2, mz = sz / 2;
            c.b(sx - Integer.signum(sx), 6, sz - Integer.signum(sz), Blocks.CHAIN); c.b(mx + Integer.signum(sx), 7, mz + Integer.signum(sz), Blocks.CHAIN);
        }
        c.stoneLantern(0, 12); c.stoneLantern(0, -12); c.stoneLantern(12, 0); c.stoneLantern(-12, 0);
        c.chest(3, 0, 0, Direction.WEST, 2);
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
        // gai hắc diệu thạch quanh vực, xích treo xuống đáy, lửa hồn đáy vực
        for (int k = 0; k < 10; k++) {
            double a = k * Math.PI / 5; int x = (int) Math.round(Math.cos(a) * 7.5), z = (int) Math.round(Math.sin(a) * 7.5);
            int ht = 2 + c.h(x, 0, z) % 4;
            c.f(x, 0, z, x, ht, z, k % 2 == 0 ? Blocks.OBSIDIAN : Blocks.CRYING_OBSIDIAN);
            c.b(x, ht + 1, z, Blocks.POINTED_DRIPSTONE);
        }
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) for (int y = -4; y <= 4; y++) c.b(sx * 3, y, sz * 3, y <= 0 ? Blocks.CHAIN : Blocks.AIR);
        for (int k = 0; k < 6; k++) { double a = k * Math.PI / 3 + 0.5; c.s((int) Math.round(Math.cos(a) * 2), -5, (int) Math.round(Math.sin(a) * 2), Blocks.SOUL_CAMPFIRE.defaultBlockState()); }
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
        // mái tầng hai, chuông đồng, cổng tam quan, đèn đá, lư hương, bậc thềm
        ProcKit.hipRoof(c.l, c.cx, c.cz, c.y0 + 8, 3, 3, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
        c.f(-2, 6, -2, 2, 7, 2, Blocks.QUARTZ_PILLAR); c.fa(-1, 6, -1, 1, 7, 1);
        c.b(0, 4, 0, Blocks.BELL);
        c.paifang(10, 3, 4, Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_BLOCK, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
        c.stoneLantern(-6, 6); c.stoneLantern(6, 6); c.stoneLantern(-6, -6); c.stoneLantern(6, -6);
        c.b(0, 0, 3, Blocks.CAULDRON); c.s(0, 1, 3, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
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
        // khán đài bậc hai bên, cờ, lò lửa bốn góc, giá binh khí
        for (int z = -7; z <= 7; z++) for (int sx = -1; sx <= 1; sx += 2) {
            c.st(sx * 10, 0, z, Blocks.STONE_BRICK_STAIRS, sx < 0 ? Direction.EAST : Direction.WEST);
            c.b(sx * 11, 0, z, Blocks.STONE_BRICKS); c.st(sx * 11, 1, z, Blocks.STONE_BRICK_STAIRS, sx < 0 ? Direction.EAST : Direction.WEST);
        }
        for (int k = 0; k < 4; k++) { int dx = (k & 1) == 0 ? 10 : -10, dz = (k & 2) == 0 ? 10 : -10; c.brazier(dx, dz, false); }
        c.warBanner(-11, -9, 4, true); c.warBanner(11, -9, 4, true); c.warBanner(-11, 9, 4, false); c.warBanner(11, 9, 4, false);
        for (int x = -3; x <= 3; x += 2) { c.b(x, 0, 10, Blocks.SPRUCE_FENCE); c.b(x, 1, 10, Blocks.LIGHTNING_ROD); }
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
        // âm dương giữa trận, vòng ngoài đèn đá, mỗi trụ có đế bậc
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (dx * dx + dz * dz > 5 || (dx == 0 && dz == 0)) continue;
            boolean yang = dx > 0 || (dx == 0 && dz < 0);
            if (Math.abs(dx) + Math.abs(dz) == 1) yang = !yang; // chấm ngược màu
            c.b(dx, -1, dz, yang ? Blocks.WHITE_CONCRETE : Blocks.BLACK_CONCRETE);
        }
        for (int k = 0; k < 10; k++) { double a = k * Math.PI / 5 + Math.PI / 10; c.stoneLantern((int) Math.round(Math.sin(a) * 8.5), (int) Math.round(-Math.cos(a) * 8.5)); }
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
