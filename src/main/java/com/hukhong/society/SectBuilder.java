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

    public static final int HALF = 52;
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
        BlockPos o = p.origin();
        com.hukhong.city.WorldBuildData.get(l).put(new com.hukhong.city.WorldBuildData.Settlement(def.id(), "sect", o.getX(), o.getZ(),
                o.getX() + p.st().sx - 1, o.getZ() + p.st().sz - 1, a.getY(), a.getX(), a.getZ()));
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
     * Sơn môn thủ tục (2026-11-05, bản lớn): sân 53x53, tường cao có lỗ châu mai + 4 tháp góc mái cong, cổng tam quan,
     * sân ngoài có hai hồ sen + cầu đá + cây anh đào + tháp chuông / tháp trống, bảo tháp 5 tầng ở giữa ôm Linh Khí Đài
     * và sân luyện võ tròn, đài cao phía bắc với bậc thang lớn, chính điện mái hai tầng và hai điện phụ.
     */
    public static BlockPos buildProcedural(ServerLevel l, BlockPos site, SectDef def) {
        int cx = site.getX(), cz = site.getZ();
        int y0 = CityBuilder.prepare(l, cx, cz, HALF, 44);
        Block w = wall(def.id());
        Block rs = roofStair(def.id()), rf = roofFull(def.id());
        Block pil = Blocks.STRIPPED_MANGROVE_LOG; // cột son
        int ex = def.tier().ordinal();
        // ---- nền: đá lát xen kẽ, trục chính rộng 7 ô
        for (int dx = -HALF; dx <= HALF; dx++) for (int dz = -HALF; dz <= HALF; dz++) {
            boolean path = Math.abs(dx) <= 3;
            Block b = path ? Blocks.POLISHED_ANDESITE : (((dx + dz) & 1) == 0 ? Blocks.STONE_BRICKS : Blocks.SMOOTH_STONE);
            if (path && Math.abs(dx) == 3) b = Blocks.CHISELED_STONE_BRICKS;
            set(l, cx + dx, y0, cz + dz, b);
        }
        // ---- tường ngoài cao + lỗ châu mai + cột đèn
        int wallH = 8 + ex;
        for (int dx = -HALF; dx <= HALF; dx++) for (int dz = -HALF; dz <= HALF; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != HALF) continue;
            if (Math.abs(dx) >= HALF - 4 && Math.abs(dz) >= HALF - 4) continue; // tháp góc
            if (dz == HALF && Math.abs(dx) <= 7) continue; // cổng
            boolean pillar = (Math.abs(dx) + Math.abs(dz)) % 7 == 0;
            for (int h = 1; h <= wallH; h++) set(l, cx + dx, y0 + h, cz + dz, pillar ? pil : (h <= 2 ? Blocks.MOSSY_STONE_BRICKS : w));
            set(l, cx + dx, y0 + wallH + 1, cz + dz, ((dx + dz) & 1) == 0 ? w : Blocks.STONE_BRICK_SLAB);
            if (pillar) set(l, cx + dx, y0 + wallH + 2, cz + dz, Blocks.LANTERN);
        }
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2)
            ProcKit.tower(l, cx + sx * (HALF - 3), cz + sz * (HALF - 3), y0, 4, wallH + 7, w, rs, rf, sx > 0 ? Direction.WEST : Direction.EAST, true);
        // ---- cổng tam quan chính (nam) + cổng phụ giữa sân
        gate(l, cx, cz + HALF, y0, w, rs, rf, pil);
        gate(l, cx, cz + LAKE_Z0 - 4, y0, w, rs, rf, pil);
        // ---- Linh Khí Hồ (sân ngoài): hồ lớn hai bên trục, cầu đá giữa, đèn linh khí dưới đáy, đình nghỉ giữa hồ
        lake(l, cx, cz, y0);
        for (int sx = -1; sx <= 1; sx += 2) {
            pavilion(l, cx + sx * 18, cz + (LAKE_Z0 + LAKE_Z1) / 2, y0, rs, rf, pil);
            for (int t = LAKE_Z0 - 2; t <= HALF - 4; t += 6) sakura(l, cx + sx * (HALF - 7), cz + t, y0);
        }
        for (int sx = -5; sx <= 5; sx += 10) for (int t = LAKE_Z1 + 2; t <= HALF - 4; t += 4) stoneLantern(l, cx + sx, y0, cz + t);
        // ---- sân luyện võ tròn + bảo tháp 7 tầng (Linh Khí Đài ở tầng trệt)
        int pz = cz + 6;
        for (int dx = -14; dx <= 14; dx++) for (int dz = -14; dz <= 14; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d <= 14.4) set(l, cx + dx, y0, pz + dz, d > 13.4 ? Blocks.CHISELED_STONE_BRICKS : (d < 7.5 ? Blocks.POLISHED_DIORITE
                    : (((int) d) % 2 == 0 ? Blocks.SMOOTH_STONE : Blocks.STONE_BRICKS)));
        }
        for (int a = 0; a < 8; a++) { // 8 cột cờ quanh sân
            double ang = Math.PI * 2 * a / 8;
            int fx = cx + (int) Math.round(Math.cos(ang) * 12), fz = pz + (int) Math.round(Math.sin(ang) * 12);
            for (int h = 1; h <= 6; h++) set(l, fx, y0 + h, fz, Blocks.DARK_OAK_FENCE);
            set(l, fx, y0 + 7, fz, Blocks.RED_WOOL);
            set(l, fx, y0 + 6, fz, Blocks.RED_WOOL);
        }
        pagoda(l, cx, pz, y0, w, rs, rf, pil, 7, 5);
        set(l, cx, y0 + 1, pz, SocietyRegistry.LINH_KHI_DAI.get());
        // ---- tháp chuông / tháp trống + hai dãy nhà đệ tử
        ProcKit.tower(l, cx - 24, pz, y0, 3, 12, w, rs, rf, Direction.EAST, true);
        ProcKit.tower(l, cx + 24, pz, y0, 3, 12, w, rs, rf, Direction.WEST, true);
        set(l, cx - 24, y0 + 11, pz, Blocks.BELL);
        set(l, cx + 24, y0 + 11, pz, Blocks.NOTE_BLOCK);
        hall(l, cx - 39, pz, y0, 6, 9, 6, w, rs, rf, pil, false);
        hall(l, cx + 39, pz, y0, 6, 9, 6, w, rs, rf, pil, false);
        // ---- Ấn Hồn Đài
        altar(l, cx, cz + ALTAR_Z, y0);
        // ---- đài cao phía bắc (4 bậc) + bậc thang lớn rộng 9
        int tz1 = cz - HALF + 3, tz2 = cz - 27, th = 4;
        for (int dx = -42; dx <= 42; dx++) for (int dz = tz1; dz <= tz2; dz++) {
            for (int h = 1; h <= th; h++) set(l, cx + dx, y0 + h, dz, h == th ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS);
            if (Math.abs(dx) == 42 || dz == tz2) set(l, cx + dx, y0 + th + 1, dz, Blocks.STONE_BRICK_WALL);
        }
        for (int dx = -5; dx <= 5; dx++) {
            set(l, cx + dx, y0 + th + 1, tz2, Blocks.AIR);
            for (int k = 0; k < th; k++) ProcKit.stair(l, cx + dx, y0 + th - k, tz2 + 1 + k, Blocks.STONE_BRICK_STAIRS, Direction.NORTH);
        }
        for (int sx = -1; sx <= 1; sx += 2) { stoneLantern(l, cx + sx * 8, y0 + th, tz2 - 1); stoneLantern(l, cx + sx * 8, y0, tz2 + 6); }
        int ty = y0 + th;
        // chính điện mái hai tầng + hai điện phụ
        hall(l, cx, tz1 + 12, ty, 15, 8, 9, w, rs, rf, pil, true);
        set(l, cx - 6, ty + 1, tz1 + 12, Blocks.LECTERN);
        set(l, cx + 6, ty + 1, tz1 + 12, Blocks.BOOKSHELF);
        set(l, cx, ty + 1, tz1 + 7, Blocks.GOLD_BLOCK);
        set(l, cx, ty + 2, tz1 + 7, Blocks.ENCHANTING_TABLE);
        hall(l, cx - 29, tz1 + 12, ty, 7, 6, 7, w, rs, rf, pil, true);
        hall(l, cx + 29, tz1 + 12, ty, 7, 6, 7, w, rs, rf, pil, true);
        // ---- bảng Tông Môn + biển tên cạnh cổng
        set(l, cx + 5, y0 + 1, cz + HALF - 3, SocietyRegistry.TONG_MON_BANG.get());
        BlockPos sp = new BlockPos(cx, y0 + 1, cz + HALF + 2);
        l.setBlock(sp, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 0), FLAGS);
        if (l.getBlockEntity(sp) instanceof SignBlockEntity s) {
            s.setText(s.getFrontText().setMessage(1, Component.translatable(def.nameKey())), true);
            s.setWaxed(true);
            s.setChanged();
        }
        decorate(l, def.id(), cx, y0, cz);
        com.hukhong.city.WorldBuildData.get(l).put(new com.hukhong.city.WorldBuildData.Settlement(def.id(), "sect", cx - HALF, cz - HALF,
                cx + HALF, cz + HALF, y0 + 1, cx, cz));
        return new BlockPos(cx, y0, cz);
    }

    /** Vị trí (tương đối tâm) Linh Khí Hồ và Ấn Hồn Đài - SectZones dùng lại. */
    public static final int LAKE_Z0 = 28, LAKE_Z1 = 46, LAKE_HALF_X = 30, ALTAR_Z = -18, ALTAR_R = 6;

    /** Linh Khí Hồ: hai nửa hồ lớn hai bên trục, đáy có đèn biển (linh khí), sen, cầu đá rộng trên trục. */
    private static void lake(ServerLevel l, int cx, int cz, int y0) {
        for (int dx = -LAKE_HALF_X - 1; dx <= LAKE_HALF_X + 1; dx++) for (int dz = LAKE_Z0 - 1; dz <= LAKE_Z1 + 1; dz++) {
            int x = cx + dx, z = cz + dz;
            boolean rim = Math.abs(dx) == LAKE_HALF_X + 1 || dz == LAKE_Z0 - 1 || dz == LAKE_Z1 + 1;
            boolean bridge = Math.abs(dx) <= 3;
            if (bridge) {
                set(l, x, y0 - 2, z, Blocks.CLAY);
                set(l, x, y0 - 1, z, Blocks.WATER);
                set(l, x, y0, z, Math.abs(dx) == 3 ? Blocks.STONE_BRICK_WALL : Blocks.SMOOTH_STONE_SLAB); // cầu
                if (Math.abs(dx) == 3 && (dz % 4 == 0)) set(l, x, y0 + 1, z, Blocks.LANTERN);
                continue;
            }
            if (rim) { set(l, x, y0, z, Blocks.MOSSY_STONE_BRICKS); set(l, x, y0 + 1, z, Blocks.MOSSY_STONE_BRICK_WALL); continue; }
            int hh = ProcKit.hash(x, y0, z, 91);
            set(l, x, y0 - 3, z, (hh % 9 == 0) ? Blocks.SEA_LANTERN : Blocks.PRISMARINE_BRICKS);
            set(l, x, y0 - 2, z, Blocks.WATER);
            set(l, x, y0 - 1, z, Blocks.WATER);
            set(l, x, y0, z, Blocks.WATER);
            if (hh % 7 == 0) set(l, x, y0 + 1, z, Blocks.LILY_PAD);
        }
    }

    /** Đình nghỉ giữa hồ: nền đá, 4 cột son, mái chóp. */
    private static void pavilion(ServerLevel l, int px, int pz, int y0, Block rs, Block rf, Block pil) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) set(l, px + dx, y0, pz + dz, Blocks.POLISHED_ANDESITE);
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) for (int h = 1; h <= 4; h++) set(l, px + sx * 3, y0 + h, pz + sz * 3, pil);
        ProcKit.hipRoof(l, px, pz, y0 + 5, 3, 3, rs, rf);
        hangLantern(l, px, y0 + 4, pz);
        set(l, px, y0 + 1, pz, Blocks.CHISELED_STONE_BRICKS);
    }

    /** Ấn Hồn Đài: đàn tròn 3 bậc, mặt đàn đá đen khắc ấn, lửa hồn ở tâm, 8 cột đèn hồn quanh vành. */
    private static void altar(ServerLevel l, int ax, int az, int y0) {
        for (int step = 0; step < 3; step++) {
            double r = ALTAR_R + 2 - step;
            for (int dx = -ALTAR_R - 2; dx <= ALTAR_R + 2; dx++) for (int dz = -ALTAR_R - 2; dz <= ALTAR_R + 2; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r) continue;
                Block b = step < 2 ? Blocks.POLISHED_BLACKSTONE_BRICKS : (d < 2 ? Blocks.CRYING_OBSIDIAN
                        : ((int) (Math.atan2(dz, dx) * 4 / Math.PI + 8) % 2 == 0 ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE));
                set(l, ax + dx, y0 + 1 + step, az + dz, b);
            }
        }
        int top = y0 + 4;
        set(l, ax, top, az, Blocks.SOUL_CAMPFIRE);
        for (int a = 0; a < 8; a++) {
            double ang = Math.PI * 2 * a / 8;
            int x = ax + (int) Math.round(Math.cos(ang) * (ALTAR_R - 1)), z = az + (int) Math.round(Math.sin(ang) * (ALTAR_R - 1));
            for (int h = 0; h < 3; h++) set(l, x, top + h, z, Blocks.POLISHED_BLACKSTONE_WALL);
            set(l, x, top + 3, z, Blocks.SOUL_LANTERN);
        }
    }

    /** Cổng tam quan: 4 cột son, 3 lối (giữa rộng 5, hai bên rộng 3), mái hai tầng, đèn lồng treo. */
    private static void gate(ServerLevel l, int cx, int gz, int y0, Block w, Block rs, Block rf, Block pil) {
        int[] posts = {-7, -3, 3, 7};
        for (int px : posts) for (int h = 1; h <= 8; h++) { set(l, cx + px, y0 + h, gz, pil); set(l, cx + px, y0 + h, gz - 1, pil); }
        for (int dx = -8; dx <= 8; dx++) for (int dz = -1; dz <= 0; dz++) {
            set(l, cx + dx, y0 + 9, gz + dz, Blocks.DARK_OAK_PLANKS);
            if (Math.abs(dx) <= 6) set(l, cx + dx, y0 + 6, gz + dz, Math.abs(dx) <= 2 ? Blocks.AIR : w); // dầm lối phụ
        }
        for (int dx = -2; dx <= 2; dx++) set(l, cx + dx, y0 + 8, gz, Blocks.GOLD_BLOCK); // bảng hiệu vàng
        ProcKit.hipRoof(l, cx, gz, y0 + 10, 8, 1, rs, rf);
        ProcKit.hipRoof(l, cx, gz, y0 + 12, 4, 1, rs, rf);
        hangLantern(l, cx, y0 + 7, gz + 1);
        hangLantern(l, cx - 5, y0 + 5, gz + 1);
        hangLantern(l, cx + 5, y0 + 5, gz + 1);
        for (int sx = -1; sx <= 1; sx += 2) { // sư tử đá
            set(l, cx + sx * 10, y0 + 1, gz + 2, Blocks.CHISELED_STONE_BRICKS);
            set(l, cx + sx * 10, y0 + 2, gz + 2, Blocks.CHISELED_POLISHED_BLACKSTONE);
        }
    }

    /** Hồ sen chữ nhật (nửa rộng hx, hz): viền đá, nước, lá sen. */
    private static void pond(ServerLevel l, int px, int pz, int y0, int hx, int hz) {
        for (int dx = -hx - 1; dx <= hx + 1; dx++) for (int dz = -hz - 1; dz <= hz + 1; dz++) {
            boolean rim = Math.abs(dx) == hx + 1 || Math.abs(dz) == hz + 1;
            if (rim) { set(l, px + dx, y0 + 1, pz + dz, Blocks.MOSSY_STONE_BRICK_WALL); continue; }
            set(l, px + dx, y0 - 1, pz + dz, Blocks.CLAY);
            set(l, px + dx, y0, pz + dz, Blocks.WATER);
            if (ProcKit.hash(px + dx, y0, pz + dz, 77) % 5 == 0) set(l, px + dx, y0 + 1, pz + dz, Blocks.LILY_PAD);
        }
    }

    /** Cây anh đào nhỏ. */
    private static void sakura(ServerLevel l, int x, int z, int y0) {
        for (int h = 1; h <= 5; h++) set(l, x, y0 + h, z, Blocks.CHERRY_LOG);
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) for (int dy = 4; dy <= 7; dy++) {
            double d = Math.sqrt(dx * dx + dz * dz + (dy - 5.5) * (dy - 5.5) * 2);
            if (d <= 3.3 && !(dx == 0 && dz == 0 && dy <= 5)) ProcKit.set(l, x + dx, y0 + dy, z + dz, Blocks.CHERRY_LEAVES.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        }
    }

    /** Đèn đá: chân đá, thân tường, đèn, nắp phiến. */
    private static void stoneLantern(ServerLevel l, int x, int y0, int z) {
        set(l, x, y0 + 1, z, Blocks.CHISELED_STONE_BRICKS);
        set(l, x, y0 + 2, z, Blocks.STONE_BRICK_WALL);
        set(l, x, y0 + 3, z, Blocks.LANTERN);
        set(l, x, y0 + 4, z, Blocks.SMOOTH_STONE_SLAB);
    }

    /** Bảo tháp n tầng: mỗi tầng cột góc + tường nửa, mái hiên một vòng bậc thang, đỉnh mái chóp + chóp nhọn. */
    private static void pagoda(ServerLevel l, int px, int pz, int y0, Block w, Block rs, Block rf, Block pil, int tiers, int baseR) {
        int y = y0;
        for (int t = 0; t < tiers; t++) {
            int r = Math.max(2, baseR - t / 2), hh = t == 0 ? 6 : 4;
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                boolean edge = Math.abs(dx) == r || Math.abs(dz) == r, corner = Math.abs(dx) == r && Math.abs(dz) == r;
                set(l, px + dx, y, pz + dz, t == 0 ? Blocks.CHISELED_STONE_BRICKS : Blocks.DARK_OAK_PLANKS);
                if (!edge) { for (int h = 1; h <= hh; h++) set(l, px + dx, y + h, pz + dz, Blocks.AIR); continue; }
                boolean open = t == 0 ? Math.abs(dx) <= 1 || Math.abs(dz) <= 1 : (Math.abs(dx) == 0 || Math.abs(dz) == 0);
                for (int h = 1; h <= hh; h++) {
                    Block b = corner ? pil : (open && h <= hh - 1 ? Blocks.AIR : (h == hh ? w : Blocks.WHITE_TERRACOTTA));
                    set(l, px + dx, y + h, pz + dz, b);
                }
            }
            y += hh + 1;
            // mái hiên: vòng bậc thang ở bán kính r+1 (+ góc vểnh)
            int er = r + 1;
            for (int dx = -er; dx <= er; dx++) for (int dz = -er; dz <= er; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != er) continue;
                if (Math.abs(dx) == er && Math.abs(dz) == er) { set(l, px + dx, y, pz + dz, rf); set(l, px + dx, y + 1, pz + dz, Blocks.LANTERN); continue; }
                Direction f = Math.abs(dx) == er ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
                ProcKit.stair(l, px + dx, y, pz + dz, rs, f);
            }
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) set(l, px + dx, y, pz + dz, rf);
        }
        int r = Math.max(2, baseR - (tiers - 1) / 2);
        ProcKit.hipRoof(l, px, pz, y + 1, r - 1, r - 1, rs, rf);
        int top = y + r + 2;
        set(l, px, top, pz, Blocks.GOLD_BLOCK);
        set(l, px, top + 1, pz, Blocks.END_ROD);
        set(l, px, top + 2, pz, Blocks.LIGHTNING_ROD);
    }

    /** Điện: nền gỗ, cột son góc + mỗi 3 ô, tường vữa trắng, cửa giữa; doubleEave: thêm một vòng mái hiên rồi gác trên. */
    private static void hall(ServerLevel l, int hx0, int hz0, int y0, int hx, int hz, int wallH, Block w, Block rs, Block rf, Block pil, boolean doubleEave) {
        for (int dx = -hx; dx <= hx; dx++) for (int dz = -hz; dz <= hz; dz++) {
            boolean edge = Math.abs(dx) == hx || Math.abs(dz) == hz;
            boolean door = dz == hz && Math.abs(dx) <= 1;
            boolean post = (Math.abs(dx) == hx && Math.abs(dz) == hz) || (edge && ((Math.abs(dx) % 3 == 0 && Math.abs(dz) == hz) || (Math.abs(dz) % 3 == 0 && Math.abs(dx) == hx)));
            set(l, hx0 + dx, y0, hz0 + dz, Blocks.SPRUCE_PLANKS);
            for (int h = 1; h <= wallH; h++) {
                if (!edge) { set(l, hx0 + dx, y0 + h, hz0 + dz, Blocks.AIR); continue; }
                if (door && h <= 3) { set(l, hx0 + dx, y0 + h, hz0 + dz, Blocks.AIR); continue; }
                Block b = post ? pil : (h == wallH ? w : (h == 3 && !door && (dx + dz) % 2 == 0 ? Blocks.WHITE_STAINED_GLASS_PANE : Blocks.WHITE_TERRACOTTA));
                set(l, hx0 + dx, y0 + h, hz0 + dz, b);
            }
            if (!edge) set(l, hx0 + dx, y0 + wallH, hz0 + dz, Blocks.SPRUCE_PLANKS);
        }
        hangLantern(l, hx0, y0 + wallH - 1, hz0);
        hangLantern(l, hx0, y0 + 4, hz0 + hz + 1);
        if (!doubleEave) { ProcKit.hipRoof(l, hx0, hz0, y0 + wallH + 1, hx, hz, rs, rf); return; }
        // mái hiên dưới: một vòng bậc thang rộng hơn tường 2 ô
        int ey = y0 + wallH + 1;
        for (int dx = -hx - 2; dx <= hx + 2; dx++) for (int dz = -hz - 2; dz <= hz + 2; dz++) {
            int ax = Math.abs(dx), az = Math.abs(dz);
            if (ax <= hx && az <= hz) { set(l, hx0 + dx, ey, hz0 + dz, rf); continue; }
            if (ax == hx + 2 && az == hz + 2) { set(l, hx0 + dx, ey + 1, hz0 + dz, rf); continue; } // góc vểnh
            Direction f = ax > hx ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
            ProcKit.stair(l, hx0 + dx, ey, hz0 + dz, rs, f);
        }
        // gác trên (lùi 2 ô) + mái chóp
        int gx = hx - 2, gz = hz - 2, gy = ey + 1;
        for (int dx = -gx; dx <= gx; dx++) for (int dz = -gz; dz <= gz; dz++) {
            boolean edge = Math.abs(dx) == gx || Math.abs(dz) == gz;
            if (!edge) continue;
            for (int h = 0; h < 3; h++) set(l, hx0 + dx, gy + h, hz0 + dz, (Math.abs(dx) == gx && Math.abs(dz) == gz) ? pil : (h == 1 ? Blocks.WHITE_STAINED_GLASS_PANE : Blocks.WHITE_TERRACOTTA));
        }
        ProcKit.hipRoof(l, hx0, hz0, gy + 3, gx + 1, gz + 1, rs, rf);
    }

    /** Nét riêng của từng sơn môn (4 góc sân, ngoài bệ trung tâm và chính điện). */
    private static void decorate(ServerLevel l, String id, int cx, int y0, int cz) {
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            int x = cx + sx * 47, z = cz + sz * 4 - 12; // hai bên sân (bản lớn)
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
