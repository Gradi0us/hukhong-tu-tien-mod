package com.hukhong.society;

import com.hukhong.city.CityBuilder;
import com.hukhong.structures.ProcKit;
import com.hukhong.structures.StructureBuilds;
import com.hukhong.structures.StructurePlacer;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;

/**
 * Sơn môn đơn giản dựng bằng code: sân vuông 29x29, tường thấp có cổng nam, chính điện phía bắc,
 * bệ trung tâm đặt Linh Khí Đài (R22) và bảng Tông Môn cạnh cổng. Vật liệu tường khác nhau theo tông.
 */
public final class SectBuilder {
    private SectBuilder() {}

    public static final int HALF = 14;
    private static final int FLAGS = 18;

    private static void set(ServerLevel l, int x, int y, int z, Block b) { ProcKit.set(l, x, y, z, b); }

    private static void hangLantern(ServerLevel l, int x, int y, int z) { ProcKit.hangLantern(l, x, y, z); }

    private static Block wall(String id) {
        return switch (id) {
            case "ba_the_mon" -> Blocks.DEEPSLATE_BRICKS;
            case "dan_dinh_tong" -> Blocks.RED_NETHER_BRICKS;
            case "thien_cong_khi_cac" -> Blocks.POLISHED_ANDESITE;
            case "thien_kiem_tong" -> Blocks.QUARTZ_BRICKS;
            case "quang_minh_dao_cung" -> Blocks.SMOOTH_QUARTZ;
            case "van_tuong_thanh_tong" -> Blocks.PURPUR_BLOCK;
            case "huyet_anh_ma_mon" -> Blocks.CRIMSON_PLANKS;
            default -> Blocks.STONE_BRICKS;
        };
    }

    /** Dung tai site (x,z): file cau truc the loai "sect" neu co, nguoc lai builder thu tuc. Tra tam (y = mat nen). */
    public static BlockPos build(ServerLevel l, BlockPos site, SectDef def) {
        BlockPos imported = StructureBuilds.tryBuild(l, def.category(), def.id(), site, "linhkhi", p -> placeCore(p, def));
        if (imported != null) return imported;
        return buildProcedural(l, site, def);
    }

    /** Gameplay cho cau truc import: Linh Khi Dai (marker hukhong:linhkhi), bang Tong Mon (hukhong:sect_board). */
    private static void placeCore(StructurePlacer.Placed p, SectDef def) {
        ServerLevel l = p.level();
        boolean marked = p.st().marker("linhkhi") != null;
        BlockPos a = p.marker("linhkhi", p.origin().offset(p.st().sx / 2, 1, p.st().sz / 2));
        if (!marked) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                set(l, a.getX() + dx, a.getY() - 1, a.getZ() + dz, Blocks.CHISELED_STONE_BRICKS);
                for (int h = 1; h <= 3; h++) set(l, a.getX() + dx, a.getY() + h, a.getZ() + dz, Blocks.AIR);
            }
        }
        set(l, a.getX(), a.getY(), a.getZ(), SocietyRegistry.LINH_KHI_DAI.get());
        BlockPos b = p.marker("sect_board", a.offset(3, 0, 2));
        set(l, b.getX(), b.getY(), b.getZ(), SocietyRegistry.TONG_MON_BANG.get());
    }

    private static Block roofStair(String id) {
        return switch (id) {
            case "thien_kiem_tong", "quang_minh_dao_cung" -> Blocks.QUARTZ_STAIRS;
            case "van_tuong_thanh_tong" -> Blocks.PURPUR_STAIRS;
            case "dan_dinh_tong" -> Blocks.RED_NETHER_BRICK_STAIRS;
            case "huyet_anh_ma_mon" -> Blocks.CRIMSON_STAIRS;
            case "ba_the_mon" -> Blocks.DEEPSLATE_BRICK_STAIRS;
            default -> Blocks.DARK_OAK_STAIRS;
        };
    }

    private static Block roofFull(String id) {
        return switch (id) {
            case "thien_kiem_tong", "quang_minh_dao_cung" -> Blocks.QUARTZ_BLOCK;
            case "van_tuong_thanh_tong" -> Blocks.PURPUR_BLOCK;
            case "dan_dinh_tong" -> Blocks.RED_NETHER_BRICKS;
            case "huyet_anh_ma_mon" -> Blocks.CRIMSON_PLANKS;
            case "ba_the_mon" -> Blocks.DEEPSLATE_BRICKS;
            default -> Blocks.DARK_OAK_PLANKS;
        };
    }

    /**
     * Son mon thu tuc: san lat 29x29, tuong co mu + tru, cong kieu torii phia nam, 4 vong dinh goc mai chop,
     * chinh dien phia bac mai hip, duong lat co den, be trung tam Linh Khi Dai, bang Tong Mon canh cong.
     */
    public static BlockPos buildProcedural(ServerLevel l, BlockPos site, SectDef def) {
        int cx = site.getX(), cz = site.getZ();
        int y0 = CityBuilder.prepare(l, cx, cz, HALF, 18);
        Block w = wall(def.id());
        Block rs = roofStair(def.id()), rf = roofFull(def.id());
        int ex = def.tier().ordinal();
        // san lat + duong chinh
        for (int dx = -HALF; dx <= HALF; dx++) for (int dz = -HALF; dz <= HALF; dz++) {
            boolean path = Math.abs(dx) <= 1;
            set(l, cx + dx, y0, cz + dz, path ? Blocks.COBBLESTONE : (((dx + dz) & 1) == 0 ? Blocks.STONE_BRICKS : Blocks.SMOOTH_STONE));
        }
        // tuong + cong nam (dz = +HALF, |dx| <= 1)
        for (int dx = -HALF; dx <= HALF; dx++) for (int dz = -HALF; dz <= HALF; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != HALF) continue;
            boolean corner = Math.abs(dx) == HALF && Math.abs(dz) == HALF;
            boolean gate = dz == HALF && Math.abs(dx) <= 1;
            if (corner) continue; // vong dinh dung rieng
            int top = 3 + ex;
            boolean pillar = (Math.abs(dx) + Math.abs(dz)) % 5 == 0;
            for (int h = 1; h <= top; h++) {
                if (gate && h <= 4) continue;
                set(l, cx + dx, y0 + h, cz + dz, pillar ? Blocks.DARK_OAK_LOG : w);
            }
            if (!gate) set(l, cx + dx, y0 + top + 1, cz + dz, pillar ? Blocks.LANTERN : Blocks.STONE_BRICK_SLAB);
        }
        // torii o cong
        for (int sx = -2; sx <= 2; sx += 4) for (int h = 1; h <= 6; h++) set(l, cx + sx, y0 + h, cz + HALF, Blocks.RED_NETHER_BRICKS);
        for (int dx = -4; dx <= 4; dx++) { set(l, cx + dx, y0 + 6, cz + HALF, Blocks.DARK_OAK_PLANKS); set(l, cx + dx, y0 + 7, cz + HALF, Blocks.DARK_OAK_SLAB); }
        for (int dx = -5; dx <= 5; dx++) if (Math.abs(dx) >= 4) set(l, cx + dx, y0 + 8, cz + HALF, Blocks.DARK_OAK_SLAB);
        hangLantern(l, cx, y0 + 5, cz + HALF);
        // vong dinh 4 goc
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2)
            ProcKit.tower(l, cx + sx * (HALF - 1), cz + sz * (HALF - 1), y0, 1, 4 + ex, w, rs, rf, sx > 0 ? Direction.WEST : Direction.EAST, true);
        // chinh dien (phia bac): thanh ben + mai hip
        int hz0 = cz - HALF + 2;
        for (int dx = -5; dx <= 5; dx++) for (int dz = 0; dz <= 6; dz++) {
            boolean edge = Math.abs(dx) == 5 || dz == 0 || dz == 6;
            boolean door = dz == 6 && Math.abs(dx) <= 1;
            boolean post = (Math.abs(dx) == 5 && (dz == 0 || dz == 6 || dz == 3)) || (dz == 6 && Math.abs(dx) == 3);
            set(l, cx + dx, y0, hz0 + dz, Blocks.SPRUCE_PLANKS);
            for (int h = 1; h <= 4; h++) if (edge && !(door && h <= 2)) set(l, cx + dx, y0 + h, hz0 + dz, post ? Blocks.DARK_OAK_LOG : (h == 4 ? w : Blocks.OAK_PLANKS));
            if (!edge) set(l, cx + dx, y0 + 4, hz0 + dz, Blocks.SPRUCE_PLANKS);
        }
        ProcKit.hipRoof(l, cx, hz0 + 3, y0 + 5, 5, 3, rs, rf);
        hangLantern(l, cx, y0 + 3, hz0 + 3);
        set(l, cx - 3, y0 + 1, hz0 + 3, Blocks.LECTERN);
        set(l, cx + 3, y0 + 1, hz0 + 3, Blocks.BOOKSHELF);
        // bac thang truoc chinh dien
        for (int dx = -2; dx <= 2; dx++) ProcKit.stair(l, cx + dx, y0 + 1, hz0 + 7, Blocks.STONE_BRICK_STAIRS, Direction.NORTH);
        // be trung tam 5x5 + Linh Khi Dai
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) set(l, cx + dx, y0, cz + dz, Blocks.CHISELED_STONE_BRICKS);
        set(l, cx, y0 + 1, cz, SocietyRegistry.LINH_KHI_DAI.get());
        for (int sx = -2; sx <= 2; sx += 4) for (int sz = -2; sz <= 2; sz += 4) {
            set(l, cx + sx, y0 + 1, cz + sz, Blocks.END_ROD);
            set(l, cx + sx, y0 + 2, cz + sz, Blocks.END_ROD);
        }
        // den doc duong
        for (int sx = -2; sx <= 2; sx += 4) for (int t = 4; t <= 12; t += 4) {
            for (int h = 1; h <= 2; h++) set(l, cx + sx, y0 + h, cz + t, Blocks.DARK_OAK_FENCE);
            set(l, cx + sx, y0 + 3, cz + t, Blocks.LANTERN);
        }
        // bang Tong Mon + bien ten canh cong
        set(l, cx + 3, y0 + 1, cz + HALF - 3, SocietyRegistry.TONG_MON_BANG.get());
        BlockPos sp = new BlockPos(cx, y0 + 1, cz + HALF + 2);
        l.setBlock(sp, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 0), FLAGS);
        if (l.getBlockEntity(sp) instanceof SignBlockEntity s) {
            s.setText(s.getFrontText().setMessage(1, Component.translatable(def.nameKey())), true);
            s.setWaxed(true);
            s.setChanged();
        }
        decorate(l, def.id(), cx, y0, cz);
        return new BlockPos(cx, y0, cz);
    }

    /** Nét riêng của từng sơn môn (4 góc sân, ngoài bệ trung tâm và chính điện). */
    private static void decorate(ServerLevel l, String id, int cx, int y0, int cz) {
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            int x = cx + sx * 9, z = cz + sz * 4 + 5;
            switch (id) {
                case "ba_the_mon" -> { set(l, x, y0 + 1, z, Blocks.HAY_BLOCK); set(l, x, y0 + 2, z, Blocks.CARVED_PUMPKIN); set(l, x, y0 + 1, z + 2, Blocks.ANVIL); }
                case "dan_dinh_tong" -> { set(l, x, y0 + 1, z, Blocks.CAULDRON); set(l, x, y0 + 1, z + 2, Blocks.BREWING_STAND); set(l, x + sx, y0 + 1, z, Blocks.SMOKER); }
                case "thien_cong_khi_cac" -> { set(l, x, y0 + 1, z, Blocks.SMITHING_TABLE); set(l, x, y0 + 1, z + 2, Blocks.GRINDSTONE); set(l, x + sx, y0 + 1, z, Blocks.IRON_BLOCK); }
                case "thien_kiem_tong" -> { for (int h = 1; h <= 4; h++) set(l, x, y0 + h, z, Blocks.QUARTZ_PILLAR); set(l, x, y0 + 5, z, Blocks.LIGHTNING_ROD); }
                case "quang_minh_dao_cung" -> { set(l, x, y0 + 1, z, Blocks.GOLD_BLOCK); set(l, x, y0 + 2, z, Blocks.GLOWSTONE); set(l, x, y0 + 3, z, Blocks.WHITE_STAINED_GLASS); }
                case "van_tuong_thanh_tong" -> { for (int h = 1; h <= 3; h++) set(l, x, y0 + h, z, Blocks.AMETHYST_BLOCK); set(l, x, y0 + 4, z, Blocks.AMETHYST_CLUSTER); set(l, x + sx, y0 + 1, z, Blocks.LECTERN); }
                case "huyet_anh_ma_mon" -> { set(l, x, y0, z, Blocks.CRIMSON_NYLIUM); set(l, x, y0 + 1, z, Blocks.RED_NETHER_BRICK_WALL); set(l, x, y0 + 2, z, Blocks.SOUL_LANTERN); set(l, x, y0 + 1, z + 2, Blocks.REDSTONE_BLOCK); }
                default -> { }
            }
        }
    }
}
