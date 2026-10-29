package com.hukhong.city;

import com.hukhong.block.ModBlocks;
import com.hukhong.structures.ProcKit;
import com.hukhong.structures.StructureBuilds;
import com.hukhong.structures.StructurePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

import static com.hukhong.structures.ProcKit.air;
import static com.hukhong.structures.ProcKit.set;

/**
 * Dung thanh. Uu tien file cau truc (nbt/schem) theo the loai cua CityDef (xem com.hukhong.structures);
 * neu khong co file thi dung builder thu tuc theo the loai: city_large (tuong + thap + cong + nha mai doc),
 * city_mountain (3 tang bac thang), port (hai cang + cau tau + tau buom), city_ruined (nhu city_large nhung do nat).
 */
public final class CityBuilder {
    private CityBuilder() {}

    public static final int HALF = 20;
    private static final int FLAGS = 18; // UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE

    public static BlockPos chooseSite(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        RandomSource r = RandomSource.create(level.getSeed() ^ 0x4C4C43L);
        BlockPos best = spawn;
        for (int i = 0; i < 10; i++) {
            double ang = r.nextDouble() * Math.PI * 2;
            double dist = 300 + r.nextDouble() * 300;
            best = new BlockPos(spawn.getX() + (int) (Math.cos(ang) * dist), 64, spawn.getZ() + (int) (Math.sin(ang) * dist));
            var b = level.getBiome(best);
            if (!b.is(BiomeTags.IS_OCEAN) && !b.is(BiomeTags.IS_RIVER)) break;
        }
        return DrySites.resolve(level, CityDef.ALL.get(0).id(), best, HALF + 4, 48);
    }

    /** Vi tri kho rao da luu (khong nuoc) cho thanh/tong; tim moi quanh anchor neu chua co. key: id thanh hoac "sect:"+id. */
    public static BlockPos adjustSite(ServerLevel l, String key, BlockPos anchor, int half, String category) {
        return DrySites.resolve(l, key, anchor, half, category.equals("city_mountain") ? 90 : 48);
    }

    /** Tim mat nen tai (cx,cz), nap chunk, san phang o (2*half+1)^2 (co, dat dem, don trong phia tren 14 block). Tra y0. */
    public static int prepare(ServerLevel l, int cx, int cz, int half) { return prepare(l, cx, cz, half, 14); }

    public static int prepare(ServerLevel l, int cx, int cz, int half, int clearH) {
        for (int x = (cx - half - 2) >> 4; x <= (cx + half + 2) >> 4; x++)
            for (int z = (cz - half - 2) >> 4; z <= (cz + half + 2) >> 4; z++) l.getChunk(x, z);
        int y = l.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz) - 1;
        while (y > l.getMinBuildHeight() + 8) {
            BlockState s = l.getBlockState(new BlockPos(cx, y, cz));
            if (s.isAir() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced()) y--; else break;
        }
        int y0 = Math.max(y, l.getSeaLevel());
        for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
            int x = cx + dx, z = cz + dz;
            set(l, x, y0, z, Blocks.GRASS_BLOCK.defaultBlockState());
            for (int d = 1; d <= 16; d++) {
                BlockPos bp = new BlockPos(x, y0 - d, z);
                BlockState s = l.getBlockState(bp);
                if (s.blocksMotion() && l.getFluidState(bp).isEmpty()) break;
                l.setBlock(bp, Blocks.DIRT.defaultBlockState(), FLAGS);
            }
            for (int h = 1; h <= clearH; h++) {
                BlockPos bp = new BlockPos(x, y0 + h, z);
                if (!l.getBlockState(bp).isAir()) l.setBlock(bp, Blocks.AIR.defaultBlockState(), FLAGS);
            }
        }
        return y0;
    }

    /** Ne dai duong/song: thu toi da 2 vong x 8 huong (96 / 192 block), tat dinh theo seed + biome. */
    public static BlockPos adjustSite(ServerLevel l, BlockPos site) {
        if (!wet(l, site)) return site;
        for (int ring = 1; ring <= 2; ring++) for (int k = 0; k < 8; k++) {
            double ang = k * Math.PI / 4;
            BlockPos p = new BlockPos(site.getX() + (int) Math.round(Math.cos(ang) * 96 * ring), 64,
                    site.getZ() + (int) Math.round(Math.sin(ang) * 96 * ring));
            if (!wet(l, p)) return p;
        }
        return site;
    }

    private static boolean wet(ServerLevel l, BlockPos p) {
        var b = l.getBiome(p);
        return b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_RIVER);
    }

    // ---------------------------------------------------------------- entry points

    /** Dung Luu Ly Cang (thanh goc, cap Nhan) theo CityDef 0. */
    public static BlockPos build(ServerLevel l, BlockPos site) { return build(l, site, CityDef.ALL.get(0)); }

    /** Tuong thich cu: builder thu tuc the loai city_large. */
    public static BlockPos build(ServerLevel l, BlockPos site, String nameKey, Tier tier) {
        return buildProcedural(l, site, nameKey, tier, "city_large");
    }

    /** Dung thanh: file cau truc (neu co) roi toi thu tuc. Tra tam (y = mat nen cua Ấn Hồn Đài). */
    public static BlockPos build(ServerLevel l, BlockPos site, CityDef d) {
        boolean port = d.category().equals("port");
        BlockPos imported = StructureBuilds.tryBuild(l, d.category(), d.id(), site, "altar",
                port ? Integer.valueOf(l.getSeaLevel() - 1) : null, p -> placeCore(p, d));
        if (imported != null) return imported;
        BlockPos c = buildProcedural(l, site, d.nameKey(), d.tier(), d.category());
        WorldBuildData.get(l).put(new WorldBuildData.Settlement(d.id(), kindOf(d), c.getX() - HALF, c.getZ() - HALF, c.getX() + HALF, c.getZ() + HALF,
                c.getY() + 1, c.getX(), c.getZ()));
        if (d.category().equals("city_ruined")) RuinGuard.spawnBoss(l, d, c.above());
        else {
            MarketDistricts.get(l).setPlazaT(d.id(), AltarShrine.procT(d.category())); // khong con thuong nhan o An Hon Dai: Khu Cho dung lazily (MarketDistrict)
            com.hukhong.guild.GuildBuilder.ensure(l, d); // Hiep Hoi Mao Hiem Gia
        }
        return c;
    }

    static String kindOf(CityDef d) {
        return switch (d.category()) { case "port" -> "port"; case "city_ruined" -> "ruin"; default -> "city"; };
    }

    /** Gameplay cho cau truc import: Ấn Hồn Đài, Bảng Thành, NPC (marker hukhong:altar / board / npc, khong co thi o tam). */
    private static void placeCore(StructurePlacer.Placed p, CityDef d) {
        String nameKey = d.nameKey();
        ServerLevel l = p.level();
        BlockPos fallback = p.origin().offset(p.st().sx / 2, 1, p.st().sz / 2);
        boolean marked = p.st().marker("altar") != null;
        BlockPos altar = p.marker("altar", fallback);
        // Quang Truong An Hon lon (R toi da 18). Co marker altar: neu chat thi vet sach vung R=12 (quyet dinh: altar la tam gameplay). Khong marker: thu nho dan theo cho trong.
        int shrineT = AltarShrine.build(l, altar, AltarShrine.MAX_R, false, marked);
        MarketDistricts.get(l).setPlazaT(d.id(), shrineT);
        boolean grand = shrineT >= AltarShrine.MIN_GRAND + 3;
        set(l, altar.getX(), altar.getY(), altar.getZ(), ModBlocks.AN_HON_DAI.get());
        for (int h = 1; h <= 5; h++) {
            BlockPos up = altar.above(h);
            if (l.getBlockState(up).isAir()) set(l, up.getX(), up.getY(), up.getZ(), Blocks.END_ROD);
        }
        BlockPos board = p.marker("board", altar.offset(grand ? 2 : 3, 0, grand ? -1 : -2));
        if (grand && AltarShrine.oct(board.getX() - altar.getX(), board.getZ() - altar.getZ()) <= shrineT) board = altar.offset(2, 0, -1);
        set(l, board.getX(), board.getY(), board.getZ(), com.hukhong.society.SocietyRegistry.THANH_BANG.get());
        if (!marked) sign(l, altar.getX(), altar.getY(), altar.getZ() + (grand ? 2 : 3), 0, "hukhong.city.altar_sign");
        List<BlockPos> npc = p.markers("npc");
        if (npc.isEmpty()) {
            npc = new ArrayList<>();
            int[][] vp = {{3, 3}, {-3, 3}, {3, -3}};
            for (int[] o : vp) npc.add(altar.offset(o[0], 0, o[1]));
        }
        BlockPos o = p.origin();
        WorldBuildData.get(l).put(new WorldBuildData.Settlement(d.id(), kindOf(d), o.getX(), o.getZ(), o.getX() + p.st().sx - 1, o.getZ() + p.st().sz - 1,
                altar.getY(), altar.getX(), altar.getZ()));
        if (d.category().equals("city_ruined")) { RuinGuard.spawnBoss(l, d, altar); return; }
        for (BlockPos np : npc) spawnVillager(l, np.getX(), np.getY(), np.getZ());
        com.hukhong.guild.GuildBuilder.placeImported(p, d); // marker hukhong:guild; khong co thi GuildEvents dung lazy
    }

    private static void spawnVillager(ServerLevel l, int x, int y, int z) {
        Villager v = EntityType.VILLAGER.create(l);
        if (v == null) return;
        v.moveTo(x + 0.5, y, z + 0.5, l.random.nextFloat() * 360f, 0f);
        v.finalizeSpawn(l, l.getCurrentDifficultyAt(v.blockPosition()), MobSpawnType.STRUCTURE, null);
        v.setPersistenceRequired();
        l.addFreshEntity(v);
    }

    // ---------------------------------------------------------------- procedural

    private static boolean inBasin(boolean port, int dx, int dz) { return port && Math.abs(dx) <= 16 && dz >= 7 && dz <= 17; }

    private static Direction side(int k) {
        return switch (k & 3) { case 0 -> Direction.SOUTH; case 1 -> Direction.NORTH; case 2 -> Direction.EAST; default -> Direction.WEST; };
    }

    /** Dung thanh tai site (x,z); tra tam (y = mat nen Ấn Hồn Đài). tier quyet dinh do cao tuong. */
    public static BlockPos buildProcedural(ServerLevel l, BlockPos site, String nameKey, Tier tier, String cat) {
        final boolean mountain = cat.equals("city_mountain"), port = cat.equals("port"), ruined = cat.equals("city_ruined");
        final boolean bigPlaza = AltarShrine.procR(cat) >= AltarShrine.MIN_GRAND;
        int cx = site.getX(), cz = site.getZ();
        int y0 = prepare(l, cx, cz, HALF, 26);
        int ex = tier.ordinal();
        long salt = l.getSeed() ^ ((long) cx * 31 + cz);
        int plazaY = mountain ? y0 + 8 : y0;

        // 1) nen: bac thang (mountain), hai cang (port), quang truong + duong
        for (int dx = -HALF; dx <= HALF; dx++) for (int dz = -HALF; dz <= HALF; dz++) {
            int x = cx + dx, z = cz + dz, m = Math.max(Math.abs(dx), Math.abs(dz));
            boolean plaza = m <= 5 && Math.abs(dx) <= 5 && Math.abs(dz) <= 5;
            boolean road = Math.abs(dx) <= 1 || Math.abs(dz) <= 1;
            int top = y0;
            if (mountain) top = m <= 8 ? y0 + 8 : (m <= 13 ? y0 + 4 : y0);
            Block surface = Blocks.GRASS_BLOCK;
            boolean ramp = false;
            if (mountain && m >= 9 && m <= 17 && (Math.abs(dx) <= 1 || Math.abs(dz) <= 1)) {
                if (m >= 14) { top = y0 + (18 - m); ramp = true; }
                else if (m >= 9 && m <= 12) { top = y0 + 4 + (13 - m); ramp = true; }
            }
            if (mountain) for (int y = y0 + 1; y < top; y++) set(l, x, y, z, ((x * 7 + y * 3 + z) & 3) == 0 ? Blocks.ANDESITE : Blocks.STONE_BRICKS);
            if (inBasin(port, dx, dz)) {
                for (int y = y0 - 3; y <= y0 - 1; y++) set(l, x, y, z, Blocks.WATER);
                set(l, x, y0 - 4, z, Blocks.STONE_BRICKS);
                if (Math.abs(dx) <= 1) set(l, x, y0, z, Blocks.SPRUCE_PLANKS); // cau tau chinh
                else air(l, x, y0, z);
                continue;
            }
            if (port && (Math.abs(dx) == 17 && dz >= 6 && dz <= 18 || (dz == 6 || dz == 18) && Math.abs(dx) <= 17)) surface = Blocks.STONE_BRICKS;
            else if (plaza) surface = ((dx + dz) & 1) == 0 ? Blocks.POLISHED_ANDESITE : Blocks.SMOOTH_STONE;
            else if (road && !ramp) surface = Blocks.COBBLESTONE;
            else if (mountain && top > y0) surface = Blocks.GRASS_BLOCK;
            if (ramp) {
                Direction face = Math.abs(dx) <= 1 && Math.abs(dz) > Math.abs(dx) ? (dz > 0 ? Direction.NORTH : Direction.SOUTH)
                        : (dx > 0 ? Direction.WEST : Direction.EAST);
                set(l, x, top - 1, z, Blocks.STONE_BRICKS);
                ProcKit.stair(l, x, top, z, Blocks.STONE_BRICK_STAIRS, face);
            } else set(l, x, top, z, surface);
        }

        // 2) tuong day 2 lop + cong + cau thang len tuong
        int wallTop = 5 + ex;
        for (int dx = -HALF; dx <= HALF; dx++) for (int dz = -HALF; dz <= HALF; dz++) {
            int m = Math.max(Math.abs(dx), Math.abs(dz));
            if (m < HALF - 1) continue;
            int along = Math.abs(dx) >= Math.abs(dz) ? dz : dx;
            boolean gate = Math.abs(along) <= 2;
            Block wb = mountain ? Blocks.DEEPSLATE_BRICKS : Blocks.STONE_BRICKS;
            for (int h = 1; h <= wallTop; h++) {
                if (gate && h <= 4) continue;
                set(l, cx + dx, y0 + h, cz + dz, wb);
            }
            if (m == HALF && !gate && ((dx + dz) & 1) == 0) set(l, cx + dx, y0 + wallTop + 1, cz + dz, wb); // lỗ chấu mai
            if (gate && m == HALF) {
                if (Math.abs(along) == 2) for (int h = 1; h <= 4; h++) set(l, cx + dx, y0 + h, cz + dz, Blocks.DARK_OAK_LOG);
                set(l, cx + dx, y0 + 4, cz + dz, Blocks.DARK_OAK_PLANKS);
                if (Math.abs(along) <= 1) set(l, cx + dx, y0 + 3, cz + dz, Blocks.IRON_BARS); // cua sat
            }
            if (m == HALF - 1 && gate && Math.abs(along) <= 1) { /* loi vao de trong */ }
        }
        for (int k = 0; k < 4; k++) {
            Direction d = side(k); // huong tu tam ra tuong
            Direction along = d.getClockWise(); // truc doc tuong
            // cau thang ben trong tuong
            for (int s = 1; s <= wallTop; s++) {
                int a = 3 + s;
                int x = cx + d.getStepX() * (HALF - 2) + along.getStepX() * a, z = cz + d.getStepZ() * (HALF - 2) + along.getStepZ() * a;
                for (int y = y0 + 1; y < y0 + s; y++) set(l, x, y, z, Blocks.STONE_BRICKS);
                ProcKit.stair(l, x, y0 + s, z, Blocks.STONE_BRICK_STAIRS, along);
            }
            // thap giua tuong
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                int tx = cx + d.getStepX() * (HALF - 1) + along.getStepX() * 12 * sgn, tz = cz + d.getStepZ() * (HALF - 1) + along.getStepZ() * 12 * sgn;
                ProcKit.tower(l, tx, tz, y0, 1, wallTop + 3, mountain ? Blocks.DEEPSLATE_BRICKS : Blocks.STONE_BRICKS,
                        Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_PLANKS, d.getOpposite(), false);
            }
            // bien ten + den cong
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                int lx = cx + d.getStepX() * (HALF + 1) + along.getStepX() * 3 * sgn, lz = cz + d.getStepZ() * (HALF + 1) + along.getStepZ() * 3 * sgn;
                for (int h = 1; h <= 2; h++) set(l, lx, y0 + h, lz, Blocks.DARK_OAK_FENCE);
                set(l, lx, y0 + 3, lz, Blocks.LANTERN);
            }
        }
        // thap goc (mai chop; hai cang: hai dang goc DN)
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            int tx = cx + sx * (HALF - 2), tz = cz + sz * (HALF - 2);
            Direction out = sx > 0 ? Direction.WEST : Direction.EAST;
            int h = 9 + ex * 2;
            if (port && sx > 0 && sz > 0) {
                ProcKit.tower(l, tx, tz, y0, 2, 16 + ex * 2, Blocks.WHITE_CONCRETE, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICKS, out, false);
                int ty = y0 + 16 + ex * 2;
                set(l, tx, ty + 1, tz, Blocks.SEA_LANTERN);
                set(l, tx, ty + 2, tz, Blocks.SEA_LANTERN);
                set(l, tx, ty + 3, tz, Blocks.LIGHTNING_ROD);
            } else {
                ProcKit.tower(l, tx, tz, y0, 2, h, mountain ? Blocks.DEEPSLATE_BRICKS : Blocks.STONE_BRICKS,
                        mountain ? Blocks.DEEPSLATE_TILE_STAIRS : Blocks.DARK_OAK_STAIRS,
                        mountain ? Blocks.DEEPSLATE_TILES : Blocks.DARK_OAK_PLANKS, out, true);
            }
        }

        // 3) mountain: thap canh o tang tren
        if (mountain) {
            for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2)
                ProcKit.tower(l, cx + sx * 7, cz + sz * 7, y0 + 8, 1, 6 + ex, Blocks.STONE_BRICKS,
                        Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES, sx > 0 ? Direction.WEST : Direction.EAST, true);
        }

        // 4) nha: moi goc phan tu 3 nha (A,B,C), mountain: nha nho tang 2, port: bo phia nam (hai cang)
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            if (port && sz > 0) continue;
            // Quang truong An Hon lon (R=14, vet T=17) chiem tam thanh: chi con 1 nha o moi goc phan tu, nam ngoai vet
            int[][] sites = mountain ? new int[][]{{11, 11, 0}} : bigPlaza ? new int[][]{{13, 14, 0}} : new int[][]{{9, 9, 0}, {14, 9, 1}, {9, 14, 2}};
            for (int[] st : sites) {
                int hx = cx + sx * st[0], hz = cz + sz * st[1];
                int hs = ProcKit.hash(hx, 0, hz, salt);
                int floors = mountain ? 1 : (hs % 3 == 0 ? 2 : 1);
                int hy = mountain ? y0 + 4 : y0;
                Direction out = st[2] == 1 ? (sz > 0 ? Direction.NORTH : Direction.SOUTH) : (sx > 0 ? Direction.WEST : Direction.EAST);
                ProcKit.house(l, hx, hz, hy, 2, 2, floors, ProcKit.mat(hs), out);
                if (!mountain) { // loi di tu cua ra duong
                    int px = hx + out.getStepX() * 3, pz = hz + out.getStepZ() * 3;
                    for (int t = 0; t < 6; t++) {
                        set(l, px, y0, pz, Blocks.COBBLESTONE);
                        if (Math.abs(px - cx) <= 1 || Math.abs(pz - cz) <= 1) break;
                        px += out.getStepX();
                        pz += out.getStepZ();
                    }
                }
            }
        }

        // 5) den duong
        if (!mountain) for (int k = 0; k < 4; k++) for (int sgn = -1; sgn <= 1; sgn += 2) for (int t : new int[]{7, 12, 17}) {
            Direction d = side(k), a = d.getClockWise();
            int lx = cx + d.getStepX() * t + a.getStepX() * 2 * sgn, lz = cz + d.getStepZ() * t + a.getStepZ() * 2 * sgn;
            if (inBasin(port, lx - cx, lz - cz) || inBasin(port, lx - cx, lz - cz + 1)) continue;
            for (int h = 1; h <= 3; h++) set(l, lx, y0 + h, lz, Blocks.DARK_OAK_FENCE);
            set(l, lx, y0 + 4, lz, Blocks.LANTERN);
        }

        // 6) hai cang: cau tau ngang + tau + hang hoa
        if (port) {
            for (int zz : new int[]{10, 14}) for (int xx = 2; xx <= 14; xx++) for (int sgn = -1; sgn <= 1; sgn += 2) {
                set(l, cx + sgn * xx, y0, cz + zz, Blocks.SPRUCE_PLANKS);
                if (xx == 14 || xx == 8) { set(l, cx + sgn * xx, y0 + 1, cz + zz, Blocks.SPRUCE_FENCE); set(l, cx + sgn * xx, y0 + 2, cz + zz, Blocks.LANTERN); }
            }
            ProcKit.ship(l, cx + 8, cz + 12, y0 - 1, 9, true);
            ProcKit.ship(l, cx - 8, cz + 12, y0 - 1, 9, true);
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                set(l, cx + sgn * 5, y0 + 1, cz + 10, Blocks.BARREL);
                set(l, cx + sgn * 4, y0 + 1, cz + 14, Blocks.BARREL);
                set(l, cx + sgn * 4, y0 + 2, cz + 14, Blocks.BARREL);
                set(l, cx + sgn * 12, y0 + 1, cz + 14, Blocks.HAY_BLOCK);
            }
        }

        // 7) Ấn Hồn Đài + cot sang
        AltarShrine.build(l, new BlockPos(cx, plazaY + 1, cz), AltarShrine.procR(cat), true);
        set(l, cx, plazaY + 1, cz, ModBlocks.AN_HON_DAI.get());
        for (int h = 2; h <= 6; h++) set(l, cx, plazaY + h, cz, Blocks.END_ROD);
        // 8) bien ten + bang Thanh
        sign(l, cx, y0 + 1, cz + 23, 0, nameKey);
        sign(l, cx, y0 + 1, cz - 23, 8, nameKey);
        sign(l, cx + 23, y0 + 1, cz, 12, nameKey);
        sign(l, cx - 23, y0 + 1, cz, 4, nameKey);
        sign(l, cx, plazaY + 1, cz + (bigPlaza ? 2 : 3), 0, "hukhong.city.altar_sign");
        set(l, cx + (bigPlaza ? 2 : 3), plazaY + 1, cz - (bigPlaza ? 1 : 2), com.hukhong.society.SocietyRegistry.THANH_BANG.get());

        // 9) hoang phe
        if (ruined) ProcKit.decay(l, cx, cz, y0, HALF + 1, 26, salt);

        // 10) dan lang
        int[][] vp = bigPlaza ? new int[][]{{6, 6}, {-6, 6}, {6, -6}} : new int[][]{{3, 3}, {-3, 3}, {3, -3}};
        for (int[] p : vp) spawnVillager(l, cx + p[0], bigPlaza ? plazaY - 1 : plazaY + 1, cz + p[1]); // bigPlaza: nen thap cua quang truong lom 2 o
        return new BlockPos(cx, plazaY, cz);
    }

    static void sign(ServerLevel l, int x, int y, int z, int rot, String key) {
        BlockPos p = new BlockPos(x, y, z);
        l.setBlock(p, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rot), FLAGS);
        if (l.getBlockEntity(p) instanceof SignBlockEntity s) {
            s.setText(s.getFrontText().setMessage(1, Component.translatable(key)), true);
            s.setWaxed(true);
            s.setChanged();
        }
    }

    /** Hoang phe (xam lang): doi mot phan block thanh da den/tim, pha mot so nha. Tat dinh theo vi tri. */
    public static void ruin(ServerLevel l, BlockPos c) {
        for (int dx = -HALF - 1; dx <= HALF + 1; dx++) for (int dz = -HALF - 1; dz <= HALF + 1; dz++) {
            l.getChunk((c.getX() + dx) >> 4, (c.getZ() + dz) >> 4);
            for (int h = 0; h <= 14; h++) {
                BlockPos bp = new BlockPos(c.getX() + dx, c.getY() + h, c.getZ() + dz);
                BlockState s = l.getBlockState(bp);
                if (s.isAir() || s.is(ModBlocks.AN_HON_DAI.get()) || s.is(Blocks.END_ROD) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT)) continue;
                int hash = (int) ((bp.asLong() * 0x9E3779B97F4A7C15L) >>> 56) & 0xFF; // 0..255
                BlockState to = null;
                if (s.is(Blocks.STONE_BRICKS)) {
                    if (hash < 70) to = Blocks.BLACKSTONE.defaultBlockState();
                    else if (hash < 100) to = Blocks.AIR.defaultBlockState();
                    else if (hash < 120) to = Blocks.PURPUR_BLOCK.defaultBlockState();
                } else if (s.is(BlockTags.PLANKS) || s.is(BlockTags.LOGS) || s.is(BlockTags.STAIRS) || s.is(BlockTags.SLABS)
                        || s.is(Blocks.GLASS_PANE) || s.is(Blocks.LANTERN)) {
                    if (hash < 90) to = Blocks.AIR.defaultBlockState();
                    else if (hash < 130) to = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                    else if (hash < 150) to = Blocks.AMETHYST_BLOCK.defaultBlockState();
                } else if (h == 0 && (s.is(Blocks.POLISHED_ANDESITE) || s.is(Blocks.SMOOTH_STONE) || s.is(Blocks.COBBLESTONE))) {
                    if (hash < 90) to = Blocks.BLACKSTONE.defaultBlockState();
                    else if (hash < 120) to = Blocks.PURPUR_BLOCK.defaultBlockState();
                }
                if (to != null) l.setBlock(bp, to, FLAGS);
            }
        }
    }
}
